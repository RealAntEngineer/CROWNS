#version 150

#define DEBUG_STEPS 0
#define DEBUG_BRICKS 0
#define DEBUG_TFETCH 0
#define EPS 1e-3

in vec3 vWorldPos; // from vertex shader
out vec4 fragColor;

uniform sampler3D colorVolume;   // main 3D texture (RGBA)
uniform sampler3D brickMinMax;   // brick occupancy texture (R=min, G=max)

uniform vec3 cameraPos;
uniform vec3 volumeMin;          // world-space volume bounds
uniform vec3 volumeMax;
uniform ivec3 bricksCount;       // number of bricks along each axis
uniform ivec3 volumesCount;       // number of volume voxel along each axis

uniform int maxSteps;
uniform float skipThreshold;


struct AABB {
    vec3 aamin;
    vec3 aamax;
};

// Clip a line along one axis
bool ClipLine(int d, AABB box, vec3 v0, vec3 v1, inout float f_low, inout float f_high)
{
    float f_dim_low = (box.aamin[d] - v0[d]) / (v1[d] - v0[d]);
    float f_dim_high = (box.aamax[d] - v0[d]) / (v1[d] - v0[d]);

    if (f_dim_high < f_dim_low) {
        float tmp = f_dim_high;
        f_dim_high = f_dim_low;
        f_dim_low = tmp;
    }

    if (f_dim_high < f_low) return false;
    if (f_dim_low > f_high) return false;

    f_low = max(f_dim_low, f_low);
    f_high = min(f_dim_high, f_high);

    if (f_low > f_high) return false;

    return true;
}

// Intersect a ray with an AABB using Liang–Barsky
bool intersectBoxLB(vec3 ro, vec3 rd, vec3 bMin, vec3 bMax, out float tEnter, out float tExit)
{
    AABB box;
    box.aamin = bMin;
    box.aamax = bMax;

    vec3 v0 = ro;
    vec3 v1 = ro + rd; // parametric: t=1 corresponds to unit step along rd

    float f_low = 0.0;
    float f_high = 1.0;

    if (!ClipLine(0, box, v0, v1, f_low, f_high)) return false;
    if (!ClipLine(1, box, v0, v1, f_low, f_high)) return false;
    if (!ClipLine(2, box, v0, v1, f_low, f_high)) return false;

    // Convert f_low / f_high to t along the ray
    tEnter = f_low; // since v1 = ro + rd * 1, t = f_low
    tExit  = f_high;

    return true;
}

bool intersectBox(vec3 ro, vec3 rd, vec3 bMin, vec3 bMax, out float tEnter, out float tExit)
{
    vec3 invDir = 1.0 / rd;
    vec3 t0 = (bMin - ro) * invDir;
    vec3 t1 = (bMax - ro) * invDir;
    vec3 tmin = min(t0, t1);
    vec3 tmax = max(t0, t1);
    tEnter = max(max(tmin.x, tmin.y), tmin.z);
    tExit  = min(min(tmax.x, tmax.y), tmax.z);
    return tExit >= tEnter;
}

// Compute world-space size of a single brick
vec3 volumeSize() { return volumeMax - volumeMin; }

// Compute AABB of a brick
void brickAABB(ivec3 bIdx, out vec3 bMin, out vec3 bMax) {
    vec3 bs = volumeSize() / vec3(bricksCount);
    bMin = volumeMin + vec3(bIdx) * bs;
    bMax = bMin + bs;
}

void volumeAABB(ivec3 vIdx, out vec3 vMin, out vec3 vMax) {
    vec3 bs = volumeSize() / vec3(volumesCount);
    vMin = volumeMin + vec3(vIdx) * bs;
    vMax = vMin + bs;
}

void main() {

    int stepCount = 0;
    //int brickSkips = 0;
    int texFetches = 0;

    vec3 startPos = vWorldPos;
    vec3 rayDir = normalize(vWorldPos - cameraPos);

    vec3 rayDirInv = 1 / rayDir;
    // --- ray-box intersection with optional tight box ---
    float tEnter, tExit;
    if (!intersectBox(startPos,rayDir, volumeMin, volumeMax, tEnter, tExit)) discard;

    float t = EPS;
    vec3 accumColor = vec3(0.0);
    float accumAlpha = 0.0;

    ivec3 prevBrick = ivec3(-1); // invalid initial value

    while (t < tExit && accumAlpha < 0.999 && stepCount < maxSteps) {
        stepCount++;
        vec3 pos = startPos + rayDir * t;

        // --- compute brick index ---
        vec3 rel = (pos - volumeMin) / (volumeMax - volumeMin);

        //ivec3 bIdx = ivec3(floor(rel * vec3(bricksCount)));

        //bIdx = clamp(bIdx, ivec3(0), bricksCount - ivec3(1));

        // --- brick skipping ---
        /*if (bIdx != prevBrick) {
            prevBrick = bIdx;
            vec3 brickUV = (vec3(bIdx) + vec3(0.5)) / vec3(bricksCount);
            vec2 mm = texture(brickMinMax, brickUV).rg; texFetches++;
            if (mm.g <= skipThreshold) {
                vec3 bMin, bMax;
                brickAABB(bIdx, bMin, bMax);
                float bt0, bt1;
                if (intersectBox(startPos,rayDir, bMin, bMax, bt0, bt1)) {
                    if (bt1 > t) {
                        brickSkips++;
                        t = bt1 + EPS;
                        continue;
                    }
                }
            }
        }*/
        ivec3 vIdx = ivec3(floor(rel * vec3(volumesCount)));
        vIdx = clamp(vIdx, ivec3(0), volumesCount - ivec3(1));

        vec3 volumeUV = (vec3(vIdx) + vec3(0.5)) / vec3(volumesCount);
        vec4 vol = texture(colorVolume, volumeUV);texFetches++;

        //fragColor = vec4(volumeUV, 1);
        //return;

        vec3 vMin, vMax;
        volumeAABB(vIdx, vMin, vMax);
        float vt0, vt1;
        if (intersectBox(startPos, rayDir, vMin, vMax, vt0, vt1)) {
            t = vt1 + EPS;
        }


        float L = vt1 - vt0;

        vec3 col = vol.rgb;
        float density = vol.a;

        // --- analytic integration ---
        float alphaSeg = 1.0 - exp(-density * L);

        vec3 Cseg = col * alphaSeg;

        float oneMinus = 1.0 - accumAlpha;
        accumColor += oneMinus * Cseg;
        accumAlpha += oneMinus * alphaSeg;
    }

    #if DEBUG_STEPS || DEBUG_BRICKS || DEBUG_TFETCH
    float r = 0.0, g = 0.0, b = 0.0;
    #if DEBUG_BRICKS
        r = 0;//r = float(brickSkips) / 8.0;
    #endif
    #if DEBUG_TFETCH
        g = float(texFetches) / 256.0;
    #endif
    #if DEBUG_STEPS
        b = float(stepCount) / float(256);
    #endif
    fragColor = vec4(r, g, b, 1);//t/(baseStep * 4));
    return;
    #endif

    fragColor = vec4(accumColor, accumAlpha);
}