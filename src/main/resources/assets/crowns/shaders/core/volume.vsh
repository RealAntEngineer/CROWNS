#version 150

in vec3 Position;
uniform mat4 ModelMat;      // world transform only
uniform mat4 ModelViewMat;  // model × view
uniform mat4 ProjMat;

out vec3 vWorldPos;

void main() {
    // True world position
    vWorldPos = (ModelMat * vec4(Position, 1.0)).xyz;

    // Projection to screen space
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}