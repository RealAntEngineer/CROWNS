#version 150

#define DEBUG_STEPS 0
#define DEBUG_BRICKS 0
#define DEBUG_TFETCH 0

in vec3 vWorldPos; // from vertex shader
out vec4 fragColor;

uniform sampler3D colorVolume;   // main 3D texture (RGBA)
uniform sampler3D brickMinMax;   // brick occupancy texture (R=min, G=max)

uniform vec3 cameraPos;
uniform vec3 volumeMin;          // world-space volume bounds
uniform vec3 volumeMax;
uniform ivec3 bricksCount;       // number of bricks along each axis


uniform vec3 boxMin;             // optional tight bounding box
uniform vec3 boxMax;

uniform int maxSteps;
uniform float stepScale;
uniform float skipThreshold;

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
vec3 brickSizeWorld() { return volumeSize() / vec3(bricksCount); }

// Compute AABB of a brick
void brickAABB(ivec3 bIdx, out vec3 bMin, out vec3 bMax) {
    vec3 bs = brickSizeWorld();
    bMin = volumeMin + vec3(bIdx) * bs;
    bMax = bMin + bs;
}

void main() {

    int stepCount = 0;
    int brickSkips = 0;
    int texFetches = 0;

    vec3 startPos = vWorldPos;
    vec3 rayDir = normalize(vWorldPos - cameraPos);

    // --- ray-box intersection with optional tight box ---
    float tEnter, tExit;
    if (!intersectBox(startPos, rayDir, boxMin, boxMax, tEnter, tExit)) discard;

    float t = tEnter + 1e-6;
    vec3 accumColor = vec3(0.0);
    float accumAlpha = 0.0;

    float rayLength = tExit - tEnter;
    float baseStep = (1 / float(maxSteps)) * stepScale;
    //float baseStep = (1.0 / float(maxSteps)) * stepScale * 2.0;
    ivec3 prevBrick = ivec3(-1); // invalid initial value


    for (int i = 0; i < maxSteps && t < tExit; i++) {
        stepCount++;
        vec3 pos = startPos + rayDir * t;

        // --- compute brick index ---
        vec3 rel = (pos - volumeMin) / (volumeMax - volumeMin);
        ivec3 bIdx = ivec3(floor(rel * vec3(bricksCount)));
        bIdx = clamp(bIdx, ivec3(0), bricksCount - ivec3(1));

        // --- brick skipping ---
        if (bIdx != prevBrick) {
            prevBrick = bIdx;
            vec3 brickUV = (vec3(bIdx) + vec3(0.5)) / vec3(bricksCount);
            vec2 mm = texture(brickMinMax, brickUV).rg; texFetches++;
            if (mm.g < skipThreshold) {
                vec3 bMin, bMax;
                brickAABB(bIdx, bMin, bMax);
                float bt0, bt1;
                if (intersectBox(startPos, rayDir, bMin, bMax, bt0, bt1)) {
                    if (bt1 > t + 1e-6) {
                        brickSkips++;
                        t = bt1 + 1e-6;
                        continue;
                    }
                }
            }
        }


        // --- volume sampling ---
        vec3 volumeUV = (pos - volumeMin) / (volumeMax - volumeMin);  // map to 0-1
        vec4 vol = texture(colorVolume, volumeUV);texFetches++;

        //fragColor = texture(colorVolume, vec3(0.5));
        //return;
        vec3 col = vol.rgb;
        float density = vol.a;

        // directional gradient (cheap)
        float eps = baseStep;
        float d0 = density;
        vec3 posNext = pos + rayDir * eps;
        vec3 uvNext = (posNext - volumeMin) / (volumeMax - volumeMin);
        float d1 = texture(colorVolume, uvNext).a; texFetches++;
        float gradAlongRay = abs(d1 - d0) / eps;

        float adapt = mix(1.0, 0.25, clamp(gradAlongRay * 10.0, 0.0, 1.0));
        float stepSize = clamp(baseStep * adapt, baseStep * 0.25, baseStep * 2.0);

        //fragColor = vec4(vol);
        //return;
        // accumulate color
        accumColor += (1.0 - accumAlpha) * col;
        accumAlpha += (1.0 - accumAlpha) * density;

        if (accumAlpha > 0.99) break;

        t += stepSize;

    }

    #if DEBUG_STEPS || DEBUG_BRICKS || DEBUG_TFETCH
    float r = 0.0, g = 0.0, b = 0.0;
    #if DEBUG_BRICKS
        r = float(brickSkips) / 8.0;
    #endif
    #if DEBUG_TFETCH
        g = float(texFetches) / 256.0;
    #endif
    #if DEBUG_STEPS
        b = float(stepCount) / float(maxSteps);
    #endif
    fragColor = vec4(r, g, b, 1);//t/(baseStep * 4));
    return;
    #endif

    fragColor = vec4(accumColor, accumAlpha);
}