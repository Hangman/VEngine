package de.pottgames.vengine.core;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

public class GlfwInitializer implements Disposable {
    private static GlfwInitializer instance = new GlfwInitializer();

    private boolean initialized = false;


    public static GlfwInitializer get() {
        return GlfwInitializer.instance;
    }


    private GlfwInitializer() {
        // singleton
    }


    public void init() {
        if (this.initialized) {
            throw new RuntimeException("GLFW is already initialized");
        }

        GLFWErrorCallback.createPrint(System.err).set();
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        this.initialized = true;
    }


    @Override
    public void dispose() {
        GLFW.glfwTerminate();

        this.initialized = false;
    }

}
