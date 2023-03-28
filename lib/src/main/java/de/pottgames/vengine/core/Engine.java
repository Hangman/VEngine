package de.pottgames.vengine.core;

import org.lwjgl.glfw.GLFW;

public class Engine {

    public Engine(ApplicationConfiguration config, Application app) {
        final WindowConfiguration windowConfig = config.getWindowConfiguration();
        boolean running = true;

        // SETUP
        final GlfwInitializer glfwInitializer = GlfwInitializer.get();
        glfwInitializer.init();
        final GlfwWindow window = new GlfwWindow(windowConfig);
        VulkanInitializer.create(window.window);
        final VulkanInitializer vulkanInitializer = VulkanInitializer.get();
        vulkanInitializer.init(windowConfig.getTitle(), config.isDebugMode(), window, windowConfig.getSwapMode());
        window.setResizeCallBack(vulkanInitializer);

        // APPLICATION HANDLING
        app.onCreate();
        while (running) {
            GLFW.glfwPollEvents();
            app.onRender();
            vulkanInitializer.drawFrame(); // FIXME: remove later
            if (window.shouldClose()) {
                if (app.canClose()) {
                    running = false;
                } else {
                    window.setShouldClose(false);
                }
            }
        }
        app.onDispose();

        // SHUTDOWN
        window.dispose();
        vulkanInitializer.dispose();
        glfwInitializer.dispose();
    }

}
