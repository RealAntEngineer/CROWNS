package com.rae.crowns.content.rendering;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL46C.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Minimal LWJGL-based 3D volume renderer with raymarching.
 */
public class VolumeRendererRGBA {
    private long window;
    private int width = 1024, height = 768;

    // GL objects
    private int program;
    private int vao, vbo;
    private int colorTex3D = -1;
    private int brickTex3D = -1;

    // volume dims
    private int Nx, Ny, Nz;
    private int brickSize = 8;

    // raymarch params
    private int maxSteps = 256;
    private float stepScale = 1f; // scale step length relative to unit cube / maxSteps

    private long lastTime = System.nanoTime();
    private int frames = 0;
    private double rotY = 0.0f;   // horizontal rotation
    private double rotX = 0.0f;   // vertical rotation
    private double camDist = 1f;
    private float opacityCutoff = 0.005f;

    public static void main(String[] args) throws Exception {
        new VolumeRendererRGBA().run();
    }

    public void run() throws Exception {
        initGL();
        loadVolume();
        loop();

        // cleanup
        glDeleteProgram(program);
        glDeleteTextures(colorTex3D);
        glDeleteTextures(brickTex3D);

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void initGL() throws IOException {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        window = glfwCreateWindow(width, height, "Plasma Volume Renderer", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Failed to create GLFW window");

        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);
        glfwShowWindow(window);


        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {

                float speed = 0.05f;   // degrees per press/hold

                switch (key) {
                    case GLFW_KEY_LEFT:
                        rotY -= speed;  // yaw left
                        break;

                    case GLFW_KEY_RIGHT:
                        rotY += speed;  // yaw right
                        break;

                    case GLFW_KEY_UP:
                        rotX -= speed;  // pitch up
                        break;

                    case GLFW_KEY_DOWN:
                        rotX += speed;  // pitch down
                        break;
                }

                // Clamp pitch to avoid flipping
                rotX = Math.max(-89f, Math.min(89f, rotX));
            }
        });

        glfwSetScrollCallback(window, (w, xoffset, yoffset) -> {
            float zoomSpeed = 0.05f;   // adjust sensitivity

            camDist -= yoffset * zoomSpeed;

            // Clamp zoom so you can't go inside or too far away
            camDist = Math.max(.01f, Math.min(100.0f, camDist));
        });



        // simple quad
        float[] quad = {
            -1f, -1f, 0f, 0f,
             1f, -1f, 1f, 0f,
             1f,  1f, 1f, 1f,
            -1f,  1f, 0f, 1f
        };
        int[] indices = {0,1,2, 2,3,0};

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer fb = memAllocFloat(quad.length);
        fb.put(quad).flip();
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        memFree(fb);

        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer ib = memAllocInt(indices.length);
        ib.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        memFree(ib);

        // position (vec2) and uv (vec2)
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4*Float.BYTES, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4*Float.BYTES, 2*Float.BYTES);

        String fragmentSrc = loadResourceAsString("volume.frag"); // load from file
        program = createProgram(vertexShaderSource, fragmentSrc);
        glUseProgram(program);

        // uniforms default
        glUniform1i(glGetUniformLocation(program, "colorVolume"), 0);
        glUniform1i(glGetUniformLocation(program, "brickMinMax"), 1);
        glUniform1i(glGetUniformLocation(program, "maxSteps"), maxSteps);
        glUniform1f(glGetUniformLocation(program, "stepScale"), stepScale);

        // enable blending for compositing (shader handles accumulation but keep blending off)
        glDisable(GL_BLEND);
    }
    public static float[][] computeTightBoundingBox(float[] opacity, int Nx, int Ny, int Nz, float threshold, int padding) {
        // Initialize min/max indices
        int minX = Nx, minY = Ny, minZ = Nz;
        int maxX = 0, maxY = 0, maxZ = 0;

        // Scan the volume
        for (int z = 0; z < Nz; z++) {
            for (int y = 0; y < Ny; y++) {
                for (int x = 0; x < Nx; x++) {
                    int idx = x + Nx * (y + Ny * z);
                    if (opacity[idx] > threshold) {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                        if (z < minZ) minZ = z;
                        if (z > maxZ) maxZ = z;
                    }
                }
            }
        }

        // Apply padding
        minX = Math.max(0, minX - padding);
        minY = Math.max(0, minY - padding);
        minZ = Math.max(0, minZ - padding);
        maxX = Math.min(Nx - 1, maxX + padding);
        maxY = Math.min(Ny - 1, maxY + padding);
        maxZ = Math.min(Nz - 1, maxZ + padding);

        // Convert to normalized coordinates [0,1]
        float boxMinX = (float)minX / (Nx - 1);
        float boxMaxX = (float)maxX / (Nx - 1);
        float boxMinY = (float)minY / (Ny - 1);
        float boxMaxY = (float)maxY / (Ny - 1);
        float boxMinZ = (float)minZ / (Nz - 1);
        float boxMaxZ = (float)maxZ / (Nz - 1);

        // Return as 2D array: [0] = min, [1] = max
        return new float[][] {
                { boxMinX, boxMinY, boxMinZ },
                { boxMaxX, boxMaxY, boxMaxZ }
        };
    }
    /**
     * Build a min/max brick volume from a full-resolution opacity array.
     *
     * @param opacity linear float array size Nx*Ny*Nz (row-major x + Nx*(y + Ny*z))
     * @param Nx,Ny,Nz volume dims
     * @param brickSize number of voxels per brick (e.g. 8)
     * @param padding voxels padding applied when computing bbox (optional)
     * @return float[] brickData with layout [Bx*By*Bz*2], channels: r=min, g=max
     */
    public static float[] buildBrickMinMax(float[] opacity, int Nx, int Ny, int Nz, int brickSize, int padding) {
        int Bx = (Nx + brickSize - 1) / brickSize;
        int By = (Ny + brickSize - 1) / brickSize;
        int Bz = (Nz + brickSize - 1) / brickSize;

        float[] brickData = new float[Bx * By * Bz * 2];

        for (int bz = 0; bz < Bz; bz++) {
            int z0 = Math.max(0, bz * brickSize - padding);
            int z1 = Math.min(Nz - 1, (bz + 1) * brickSize - 1 + padding);

            for (int by = 0; by < By; by++) {
                int y0 = Math.max(0, by * brickSize - padding);
                int y1 = Math.min(Ny - 1, (by + 1) * brickSize - 1 + padding);

                for (int bx = 0; bx < Bx; bx++) {
                    int x0 = Math.max(0, bx * brickSize - padding);
                    int x1 = Math.min(Nx - 1, (bx + 1) * brickSize - 1 + padding);

                    float minVal = Float.POSITIVE_INFINITY;
                    float maxVal = Float.NEGATIVE_INFINITY;

                    for (int z = z0; z <= z1; z++) {
                        int baseZ = z * (Nx * Ny);
                        for (int y = y0; y <= y1; y++) {
                            int baseY = y * Nx;
                            int idx = baseZ + baseY + x0;
                            // scan x range
                            for (int x = x0; x <= x1; x++, idx++) {
                                float v = opacity[idx];
                                if (v < minVal) minVal = v;
                                if (v > maxVal) maxVal = v;
                            }
                        }
                    }

                    if (minVal == Float.POSITIVE_INFINITY) {
                        minVal = 0.0f;
                        maxVal = 0.0f;
                    }

                    int brickIndex = (bx + by * Bx + bz * (Bx * By));
                    int write = brickIndex * 2;
                    brickData[write + 0] = minVal;
                    brickData[write + 1] = maxVal;
                }
            }
        }

        return brickData;
    }

    private void loadVolume() throws Exception {
        // read metadata.json (simple manual parse)
        String metaText = loadResourceAsString("metadata.json");
        Map<String, Object> meta = parseSimpleJson(metaText);

        String colorsFile = (String)meta.get("colors_file");
        String opacityFile = (String)meta.get("opacity_file");

        // shapes
        // colors_shape is [Nz, Ny, Nx, 3]
        Object csObj = meta.get("colors_shape");
        if (!(csObj instanceof int[] cshape)) throw new RuntimeException("metadata parsing failed");
        Nz = cshape[0]; Ny = cshape[1]; Nx = cshape[2];
        int C = cshape[3];

        // load raw float32 arrays
        float[] colors = readFloatArrayResource(colorsFile, Nx * Ny * Nz * C);
        float[] opacity = readFloatArrayResource(opacityFile, Nx * Ny * Nz);

        float[][] bbox = computeTightBoundingBox(opacity, Nx, Ny, Nz, opacityCutoff, 10);
        float[] boxMin = bbox[0];
        float[] boxMax = bbox[1];

        int locBoxMin = glGetUniformLocation(program, "boxMin");
        int locBoxMax = glGetUniformLocation(program, "boxMax");

        //glUniform3f(locBoxMin, 0,0,0);
        //glUniform3f(locBoxMax,1,1,1);
        glUniform3f(locBoxMin, boxMin[0], boxMin[1], boxMin[2]);
        glUniform3f(locBoxMax, boxMax[0], boxMax[1], boxMax[2]);

        // create GL 3D textures
        colorTex3D = glGenTextures();
        glBindTexture(GL_TEXTURE_3D, colorTex3D);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

        // pack colors into a float buffer (RGBA where A=1.0)
        int voxels = Nz * Ny * Nx;
        FloatBuffer colorBuf = memAllocFloat(voxels * 4);
        // colors layout: Nz,Ny,Nx,3  -> iterate z,y,x
        int idx = 0;
        for (int z=0; z<Nz; z++){
            for (int y=0; y<Ny; y++){
                for (int x=0; x<Nx; x++){
                    int base = ((z*Ny + y)*Nx + x)*3;
                    float a = opacity[idx] < opacityCutoff ? 0 : opacity[idx];
                    float r = colors[base] * a;
                    float g = colors[base+1] * a;
                    float b = colors[base+2] * a;
                    colorBuf.put(r).put(g).put(b).put(a);
                    idx++;
                }
            }
        }
        colorBuf.flip();
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA32F, Nx, Ny, Nz, 0, GL_RGBA, GL_FLOAT, colorBuf);
        memFree(colorBuf);

        //brick
        float[] brickData = buildBrickMinMax(opacity, Nx, Ny, Nz, brickSize, 1);

        int Bx = (Nx + brickSize - 1) / brickSize;
        int By = (Ny + brickSize - 1) / brickSize;
        int Bz = (Nz + brickSize - 1) / brickSize;

        int locBricks = glGetUniformLocation(program, "bricksCount");
        glUniform3i(locBricks, Bx, By, Bz);

        int locBrickSize = glGetUniformLocation(program, "brickSize");
        glUniform1i(locBrickSize, brickSize);

        int locSkip = glGetUniformLocation(program, "skipThreshold");
        glUniform1f(locSkip, opacityCutoff);


        glUniform3f(glGetUniformLocation(program, "volumeMin"), 0f, 0f, 0f);
        glUniform3f(glGetUniformLocation(program, "volumeMax"), 1f, 1f, 1f);
        // Generate texture, bind, set parameters
        brickTex3D = glGenTextures();
        glBindTexture(GL_TEXTURE_3D, brickTex3D);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

        // brickData is float[], convert to FloatBuffer
        FloatBuffer brickBuffer = memAllocFloat(brickData.length);
        brickBuffer.put(brickData).flip();

        // Use GL_RG32F internal format with GL_RG + GL_FLOAT
        glTexImage3D(GL_TEXTURE_3D, 0, GL_RG32F, Bx, By, Bz, 0, GL_RG, GL_FLOAT, brickBuffer);
        memFree(brickBuffer);

        glBindTexture(GL_TEXTURE_3D, 0);

        // bind textures
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_3D, colorTex3D);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_3D, brickTex3D);

        glBindVertexArray(vao);
    }

    int frameCount = 0;
    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            // Count frames
            frames++;

            long now = System.nanoTime();
            if (now - lastTime >= 1_000_000_000L) {  // 1 second
                int fps = frames;

                glfwSetWindowTitle(window, "Plasma Volume Renderer - FPS: " + fps);

                frames = 0;
                lastTime = now;
            }

            glfwPollEvents();

            try (MemoryStack stack = stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                glfwGetFramebufferSize(window, w, h);
                width = w.get(0);
                height = h.get(0);
            }

            glViewport(0,0,width,height);
            glClearColor(0f,0f,0f,1f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            frameCount++;

            int loc = GL20.glGetUniformLocation(program, "frameCount");
            GL20.glUniform1i(loc, frameCount);

            // set camera uniforms
            int locCam = glGetUniformLocation(program, "cameraPos");
            // basic orbit camera
            float cx = (float)(camDist * Math.cos(rotX) * Math.cos(rotY));
            float cy = (float)(camDist * Math.sin(rotX));
            float cz = (float)(camDist * Math.cos(rotX) * Math.sin(rotY));

            // ✅ Orbit around volume center (CRITICAL)
            cx += 0.5f;
            cy += 0.5f;
            cz += 0.5f;

            // Send to shader
            glUniform3f(locCam, cx, cy, cz);

            //int locInvProj = glGetUniformLocation(program, "invViewProj");
            // we will pass identity and do ray direction calculation in shader using gl_FragCoord
            // (more advanced approach omitted for brevity)


            glUseProgram(program);

            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

            glfwSwapBuffers(window);
        }
    }

    // ----- helpers -----
    private static String loadResourceAsString(String filename) throws IOException {
        try (InputStream in = VolumeRendererRGBA.class.getResourceAsStream("/" + filename)) {
            if (in == null) throw new IOException("Shader not found: " + filename);
            return new String(in.readAllBytes());
        }
    }


    private int createProgram(String vsrc, String fsrc) {
        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vsrc);
        glCompileShader(vs);
        checkCompile(vs, "VERT");

        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fsrc);
        glCompileShader(fs);
        checkCompile(fs, "FRAG");

        int prog = glCreateProgram();
        glAttachShader(prog, vs);
        glAttachShader(prog, fs);
        glBindAttribLocation(prog, 0, "inPos");
        glBindAttribLocation(prog, 1, "inUV");
        glLinkProgram(prog);
        checkLink(prog);

        glDeleteShader(vs);
        glDeleteShader(fs);
        return prog;
    }

    private void checkCompile(int shader, String tag) {
        int stat = glGetShaderi(shader, GL_COMPILE_STATUS);
        if (stat == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            throw new RuntimeException(tag + " SHADER COMPILE ERROR: " + log);
        }
    }
    private void checkLink(int prog) {
        int stat = glGetProgrami(prog, GL_LINK_STATUS);
        if (stat == GL_FALSE) {
            String log = glGetProgramInfoLog(prog);
            throw new RuntimeException("PROGRAM LINK ERROR: " + log);
        }
    }

    // read float32 little-endian raw file
    private static float[] readFloatArrayResource(String filename, int expectedLength) throws IOException {
        try (InputStream in = VolumeRendererRGBA.class.getResourceAsStream("/" + filename)) {
            if (in == null) throw new IOException("Resource not found: " + filename);
            byte[] bytes = in.readAllBytes();
            if (bytes.length != expectedLength * 4)
                throw new IOException("Unexpected file size for " + filename);
            FloatBuffer fb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
            float[] arr = new float[fb.remaining()];
            fb.get(arr);
            return arr;
        }
    }

    // very naive minimal JSON parser for this known structure (colors_shape => [Nz,Ny,Nx,3])
    private Map<String,Object> parseSimpleJson(String txt) {
        Map<String,Object> out = new HashMap<>();
        txt = txt.replaceAll("[\\n\\r\\t ]", "");
        // extract filenames
        String cfile = between(txt, "\"colors_file\":\"", "\"");
        String ofile = between(txt, "\"opacity_file\":\"", "\"");
        out.put("colors_file", cfile);
        out.put("opacity_file", ofile);

        String cshapeStr = between(txt, "\"colors_shape\":[", "]");
        String[] parts = cshapeStr.split(",");
        int[] cshape = new int[parts.length];
        for (int i=0;i<parts.length;i++) cshape[i] = Integer.parseInt(parts[i]);
        out.put("colors_shape", cshape);

        return out;
    }
    private static String between(String s, String a, String b) {
        int i = s.indexOf(a);
        if (i<0) return null;
        i += a.length();
        int j = s.indexOf(b, i);
        if (j<0) return null;
        return s.substring(i, j);
    }

    // --- shaders ---
    private static final String vertexShaderSource =
        "#version 450 core\n" +
        "layout(location=0) in vec2 inPos;\n" +
        "layout(location=1) in vec2 inUV;\n" +
        "out vec2 vUV;\n" +
        "void main(){ vUV = inUV; gl_Position = vec4(inPos, 0.0, 1.0); }";
}
