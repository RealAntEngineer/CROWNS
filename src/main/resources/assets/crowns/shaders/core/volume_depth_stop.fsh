#version 150

#define DEBUG_STEPS 0
#define DEBUG_TFETCH 0
#define EPS 1e-3

in vec3 vWorldPos; // from vertex shader
out vec4 fragColor;

uniform sampler3D colorVolume;   // main 3D texture (RGBA)
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

vec3 depthToWorldPos(vec2 uv, float depth) {
    // UV → NDC
    vec2 ndc = uv * 2.0 - 1.0;

    // NDC depth
    float z = depth * 2.0 - 1.0;

    // Clip → view
    vec4 clip = vec4(ndc, z, 1.0);
    vec4 view = InvProjMat * clip;
    view /= view.w;

    // View → world
    vec4 world = InvViewMat * view;
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

    vec3 startPos = vWorldPos;
    vec3 rayDir = normalize(vWorldPos - cameraPos);

    vec3 rayDirInv = 1 / rayDir;
    // --- ray-box intersection with optional tight box ---
    float tEnter, tExit;
    if (!intersectBox(startPos,rayDir, volumeMin, volumeMax, tEnter, tExit)) discard;

    float t = EPS;
    vec3 accumColor = vec3(0.0);
    float accumAlpha = 0.0;

    // Clamp against scene
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    float sceneDepthRaw = texture(sceneDepth, uv).r;
    vec3 sceneWorldPos = depthToWorldPos(uv, sceneDepthRaw);
    float tScene = dot(sceneWorldPos - startPos, rayDir);
    //tExit = min(tExit, tScene);
    //fragColor = vec4(vec3(tScene), 1.0);
    //return;

    while (t < tExit && accumAlpha < 0.999 && stepCount < maxSteps) {
        stepCount++;
        vec3 pos = startPos + rayDir * t;

        // --- compute brick index ---
        vec3 rel = (pos - volumeMin) / (volumeMax - volumeMin);

        ivec3 vIdx = ivec3(floor(rel * vec3(volumesCount)));
        vIdx = clamp(vIdx, ivec3(0), volumesCount - ivec3(1));

        vec3 volumeUV = (vec3(vIdx) + vec3(0.5)) / vec3(volumesCount);
        vec4 vol = texture(colorVolume, volumeUV);texFetches++;

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

    #if DEBUG_STEPS || DEBUG_TFETCH
    float r = 0.0, g = 0.0, b = 0.0;
    #if DEBUG_TFETCH
        g = float(texFetches) / 256.0;
    #endif
    #if DEBUG_STEPS
        b = float(stepCount) / float(256);
    #endif
    fragColor = vec4(r, g, b, 1);
    return;
    #endif

    fragColor = vec4(accumColor, accumAlpha);
}