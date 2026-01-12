#version 150
#define EPS 1e-3

in vec3 vWorldPos; // from vertex shader
out vec4 fragColor;

uniform sampler3D colorVolume;   // main 3D texture (RGBA)
uniform sampler3D brickMinMax;   // main 3D texture (RGBA)
uniform sampler2D sceneDepth;
uniform vec2 ScreenSize;

uniform mat4 InvProjMat;    // inverse projection
uniform mat4 InvViewMat;    // from Java
//uniform mat4 ModelViewMat;  // model × view

uniform vec3 cameraPos;
uniform vec3 volumeMin;          // world-space volume bounds
uniform vec3 volumeMax;
uniform ivec3 volumesCount;       // number of volume voxel along each axis

uniform int maxSteps;
uniform float skipThreshold;

vec3 depthToWorldPos(vec2 uv, float depth)
{
    // 1. Convert UV to NDC [-1, 1]
    float x = uv.x * 2.0 - 1.0;
    float y = uv.y * 2.0 - 1.0;
    float z = depth * 2.0 - 1.0; // assuming depth texture is default non-linear [0,1]

    // 2. Reconstruct clip space
    vec4 clip = vec4(x, y, z, 1.0);

    // 3. Transform to view space
    vec4 view = InvProjMat * clip;
    view.xyz /= view.w;  // perspective divide

    // 4. Transform to world space
    vec4 world = InvViewMat * vec4(view.xyz, 1.0);

    return world.xyz;
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

void volumeAABB(ivec3 vIdx, out vec3 vMin, out vec3 vMax) {
    vec3 bs = volumeSize() / vec3(volumesCount);
    vMin = volumeMin + vec3(vIdx) * bs;
    vMax = vMin + bs;
}

void main() {

    int stepCount = 0;
    int texFetches = 0;

    //vec3 startPos = vWorldPos;
    //vec3 rayDir = normalize(vWorldPos - cameraPos);
    vec3 ro = cameraPos;
    vec3 rd = normalize(vWorldPos - cameraPos);

    vec3 rayDirInv = 1 / rd;
    // --- ray-box intersection with optional tight box ---
    float tEnter, tExit;
    if (!intersectBox(ro,rd, volumeMin, volumeMax, tEnter, tExit)) discard;

    float t = tEnter + EPS;
    vec3 accumColor = vec3(0.0);
    float accumAlpha = 0.0;

    // Clamp against scene
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float sceneDepthRaw = texture(sceneDepth, uv).r;
    vec3 sceneWorldPos = depthToWorldPos(uv, sceneDepthRaw);
    float tScene = dot(sceneWorldPos - ro, rd);

    //need to be absolute
    tExit = min(tExit, tScene);
    //fragColor = vec4(vec3(tExit/10), 1.0);
    //return;

    while (t < tExit && accumAlpha < 0.999 && stepCount < maxSteps) {
        stepCount++;
        vec3 pos = ro + rd * t;

        // --- compute brick index ---
        vec3 rel = (pos - volumeMin) / (volumeMax - volumeMin);

        ivec3 vIdx = ivec3(floor(rel * vec3(volumesCount)));
        vIdx = clamp(vIdx, ivec3(0), volumesCount - ivec3(1));

        vec3 volumeUV = (vec3(vIdx) + vec3(0.5)) / vec3(volumesCount);
        //fragColor = vec4(pos, 1);
        //return;
        vec4 vol = texture(colorVolume, volumeUV);texFetches++;

        vec3 vMin, vMax;
        volumeAABB(vIdx, vMin, vMax);
        float vt0, vt1;
        if (intersectBox(ro, rd, vMin, vMax, vt0, vt1)) {
            t = vt1 + EPS;
        }


        float L = vt1 - vt0;

        vec3 col = vol.rgb;
        float density = vol.a;

        // --- analytic integration ---
        float alphaSeg = 1.0 - exp(-density * L * 10);

        vec3 Cseg = col * alphaSeg;

        float oneMinus = 1.0 - accumAlpha;
        accumColor += oneMinus * Cseg;
        accumAlpha += oneMinus * alphaSeg;
    }

    fragColor = vec4(accumColor, accumAlpha);
}