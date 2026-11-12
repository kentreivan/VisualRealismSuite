package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Visual Realism Suite (Minggu 11)
 *
 * Kontrol:
 *   B = Phong <-> Blinn-Phong        F = Flat <-> Smooth Shading
 *   Z = Z-Buffer ON/OFF               C = Back-Face Culling ON/OFF
 *   T = Texture ON/OFF                N = Filtering Nearest <-> Bilinear+Mipmap
 *   H = Shadow Mapping ON/OFF         M = Ganti mesh (Sphere/Torus/House)
 *   L = Jeda/jalankan animasi cahaya  SPACE = Jeda/jalankan rotasi objek
 *   V = VSync ON/OFF (untuk ukur FPS) Mouse drag = orbit kamera, Scroll = zoom, ESC = keluar
 */
public class VisualRealismApp {

    private static final float GROUND_Y = -1.0f;
    private static final float HOVER = 0.5f; // objek melayang agar bayangan terlihat jelas

    private long window;
    private int fbWidth = 1280, fbHeight = 720;

    private ShaderProgram sceneShader, depthShader;
    private ShadowMapFBO shadowFBO;
    private Texture2D texture;
    private Mesh[] meshes;
    private final String[] meshNames = {"Sphere", "Torus", "House"};
    private Mesh ground;
    private final Camera camera = new Camera();

    // ---- state yang bisa di-toggle ----
    private int meshIndex = 0;
    private boolean blinn = true, smooth = true, depthTest = true, cullFace = true;
    private boolean useTexture = true, mipmapFilter = true, shadows = true;
    private boolean lightAnimating = true, spinning = true, vsync = true;
    private boolean titleDirty = true;

    private float lightAngle = 0.8f, spinAngle = 0f;
    private double lastMouseX, lastMouseY;
    private boolean dragging = false;
    private int fps = 0;

    public static void main(String[] args) {
        new VisualRealismApp().run();
    }

    public void run() {
        init();
        loop();
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // wajib untuk macOS
        glfwWindowHint(GLFW_SAMPLES, 4);

        window = glfwCreateWindow(1280, 720, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Gagal membuat window");

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();
        glEnable(0x809D); // GL_MULTISAMPLE

        // ---- resource ----
        sceneShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        shadowFBO = new ShadowMapFBO();
        texture = new Texture2D("textures/texture.png"); // opsional; fallback checkerboard

        meshes = new Mesh[]{
                MeshFactory.sphere(1.0f, 48, 64),
                MeshFactory.torus(0.9f, 0.4f, 64, 32),
                MeshFactory.house()
        };
        ground = MeshFactory.plane(6f, GROUND_Y, 6f);

        setupCallbacks();
    }

    private void setupCallbacks() {
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action != GLFW_PRESS) return;
            switch (key) {
                case GLFW_KEY_ESCAPE -> glfwSetWindowShouldClose(w, true);
                case GLFW_KEY_B -> blinn = !blinn;
                case GLFW_KEY_F -> smooth = !smooth;
                case GLFW_KEY_Z -> depthTest = !depthTest;
                case GLFW_KEY_C -> cullFace = !cullFace;
                case GLFW_KEY_T -> useTexture = !useTexture;
                case GLFW_KEY_N -> { mipmapFilter = !mipmapFilter; texture.setFilter(mipmapFilter); }
                case GLFW_KEY_H -> shadows = !shadows;
                case GLFW_KEY_M -> meshIndex = (meshIndex + 1) % meshes.length;
                case GLFW_KEY_L -> lightAnimating = !lightAnimating;
                case GLFW_KEY_SPACE -> spinning = !spinning;
                case GLFW_KEY_V -> { vsync = !vsync; glfwSwapInterval(vsync ? 1 : 0); }
                default -> { return; }
            }
            titleDirty = true;
        });

        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) dragging = (action == GLFW_PRESS);
        });

        glfwSetCursorPosCallback(window, (w, x, y) -> {
            if (dragging) {
                camera.rotate((float) (-(x - lastMouseX) * 0.3), (float) ((y - lastMouseY) * 0.3));
            }
            lastMouseX = x;
            lastMouseY = y;
        });

        glfwSetScrollCallback(window, (w, dx, dy) -> camera.zoom((float) dy * 0.6f));
    }

    private void loop() {
        double last = glfwGetTime(), fpsTimer = last;
        int frames = 0;

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            if (lightAnimating) lightAngle += dt * 0.6f;
            if (spinning) spinAngle += dt * 0.5f;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1);
                glfwGetFramebufferSize(window, w, h);
                fbWidth = Math.max(1, w.get(0));
                fbHeight = Math.max(1, h.get(0));
            }

            render();
            glfwSwapBuffers(window);
            glfwPollEvents();

            frames++;
            if (now - fpsTimer >= 1.0) {
                fps = frames;
                frames = 0;
                fpsTimer = now;
                titleDirty = true;
            }
            if (titleDirty) updateTitle();
        }
    }

    private void render() {
        Mesh object = meshes[meshIndex];
        Vector3f lightPos = new Vector3f(
                (float) (4.5 * Math.cos(lightAngle)), 5.5f, (float) (4.5 * Math.sin(lightAngle)));

        Matrix4f objModel = new Matrix4f()
                .translate(0f, GROUND_Y - object.getMinY() + HOVER, 0f)
                .rotateY(spinAngle);
        Matrix4f groundModel = new Matrix4f();

        // Light space matrix (Directional light -> Ortografik)
        Matrix4f lightProj = new Matrix4f().ortho(-6f, 6f, -6f, 6f, 1f, 16f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0f), new Vector3f(0f, 1f, 0f));
        Matrix4f lightSpace = new Matrix4f(lightProj).mul(lightView);

        // ================= PASS 1: Depth Pass (dari sudut pandang cahaya) =================
        if (shadows) {
            shadowFBO.bindForWriting();
            HSRManager.setupDepthTest(true);
            HSRManager.setupBackFaceCulling(false);
            depthShader.use();
            depthShader.setMat4("lightSpaceMatrix", lightSpace);
            depthShader.setMat4("model", objModel);
            object.draw(smooth);
            shadowFBO.unbind(fbWidth, fbHeight);
        }

        // ================= PASS 2: Lighting Pass (dari sudut pandang kamera) =================
        glClearColor(0.53f, 0.74f, 0.92f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(cullFace);

        Matrix4f projection = new Matrix4f().perspective(
                (float) Math.toRadians(45.0), (float) fbWidth / fbHeight, 0.1f, 100f);

        sceneShader.use();
        sceneShader.setMat4("view", camera.getViewMatrix());
        sceneShader.setMat4("projection", projection);
        sceneShader.setMat4("lightSpaceMatrix", lightSpace);
        sceneShader.setVec3("lightPos", lightPos);
        sceneShader.setVec3("viewPos", camera.getPosition());
        sceneShader.setVec3("lightColor", 1f, 1f, 1f);
        sceneShader.setBool("useBlinnPhong", blinn);
        sceneShader.setBool("useTexture", useTexture);
        sceneShader.setBool("useShadow", shadows);
        sceneShader.setInt("textureSampler", 0);
        sceneShader.setInt("shadowMap", 1);

        texture.bind(0);
        shadowFBO.bindDepthTexture(1);

        // Lantai
        sceneShader.setMat4("model", groundModel);
        sceneShader.setVec3("objectColor", 0.55f, 0.6f, 0.55f);
        ground.draw(true);

        // Objek utama
        sceneShader.setMat4("model", objModel);
        sceneShader.setVec3("objectColor", 0.9f, 0.55f, 0.25f);
        object.draw(smooth);
    }

    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }

    private void updateTitle() {
        titleDirty = false;
        glfwSetWindowTitle(window, String.format(
                "3D Visual Realism Suite | %s | %s | %s | Z-Buffer %s | Cull %s | Tex %s (%s) | Shadow %s | FPS %d",
                meshNames[meshIndex],
                blinn ? "Blinn-Phong" : "Phong",
                smooth ? "Smooth" : "Flat",
                onOff(depthTest), onOff(cullFace), onOff(useTexture),
                mipmapFilter ? "Bilinear+Mipmap" : "Nearest",
                onOff(shadows), fps));
    }
}
