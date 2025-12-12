// debug_fsh.glsl
#version 150
in vec3 vWorldPos;
uniform vec3 volumeMin;          // world-space volume bounds
uniform vec3 volumeMax;


out vec4 fragColor;
void main() {
    // normalize world pos to a visible color (clamp in a reasonable world region)
    vec3 c = clamp((vWorldPos - volumeMin)/(volumeMax - volumeMin), 0.0, 1.0);
    fragColor = vec4(c, 1.0);
}