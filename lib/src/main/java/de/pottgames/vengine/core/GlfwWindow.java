package de.pottgames.vengine.core;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;

public class GlfwWindow implements Disposable {
    private final long window;
    private boolean    visible = false;


    public GlfwWindow(WindowConfiguration config) {
        this.window = this.init(config);
    }


    private long init(WindowConfiguration config) {
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, config.isResizable() ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);

        final long window = GLFW.glfwCreateWindow(config.getWidth(), config.getHeight(), config.getTitle(), MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            throw new RuntimeException("Cannot create GLFW window");
        }

        final IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
        final IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
        GLFW.glfwGetWindowSize(window, widthBuffer, heightBuffer);
        final int actualWidth = widthBuffer.get(0);
        final int actualHeight = heightBuffer.get(0);

        if (config.isCenter()) {
            final GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
            GLFW.glfwSetWindowPos(window, (vidMode.width() - actualWidth) / 2, (vidMode.height() - actualHeight) / 2);
        } else {
            GLFW.glfwSetWindowPos(window, config.getPosX(), config.getPosY());
        }

        GLFW.glfwSetWindowTitle(window, config.getTitle());

        if (config.isVisible()) {
            GLFW.glfwShowWindow(window);
            this.visible = true;
        }

        return window;
    }


    public void setTitle(String title) {
        GLFW.glfwSetWindowTitle(this.window, title);
    }


    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(this.window);
    }


    public void setShouldClose(boolean value) {
        GLFW.glfwSetWindowShouldClose(this.window, value);
    }


    public void setVisible(boolean visible) {
        if (visible == this.visible) {
            return;
        }

        if (visible) {
            GLFW.glfwShowWindow(this.window);
        } else {
            GLFW.glfwHideWindow(this.window);
        }
        this.visible = visible;
    }


    @Override
    public void dispose() {
        GLFW.glfwDestroyWindow(this.window);
    }

}
