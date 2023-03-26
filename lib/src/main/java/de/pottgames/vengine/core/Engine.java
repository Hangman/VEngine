package de.pottgames.vengine.core;

import org.lwjgl.glfw.GLFW;

public class Engine {
    private static final GlfwInitializer   GLFW_INITIALIZER   = GlfwInitializer.get();
    private static final VulkanInitializer VULKAN_INITIALIZER = VulkanInitializer.get();
    private final GlfwWindow               window;
    private final int                      framerate;
    private final Sync                     sync;
    private boolean                        running            = false;


    public Engine(ApplicationConfiguration config, Application app) {
        final WindowConfiguration windowConfig = config.getWindowConfiguration();
        this.framerate = windowConfig.getMaxFramerate();
        this.sync = new Sync();
        this.running = true;

        // SETUP
        Engine.GLFW_INITIALIZER.init();
        this.window = new GlfwWindow(windowConfig);
        Engine.VULKAN_INITIALIZER.init(windowConfig.getTitle(), config.isDebugMode(), this.window, windowConfig.getSwapMode());

        // APPLICATION HANDLING
        app.onCreate();
        while (this.running) {
            GLFW.glfwPollEvents();
            app.onRender();
            Engine.VULKAN_INITIALIZER.drawFrame(); // FIXME: remove later
            // this.sync.sync(this.framerate); // FIXME: probably won't need it as vulkan does sync for us
            if (this.window.shouldClose()) {
                if (app.canClose()) {
                    this.running = false;
                } else {
                    this.window.setShouldClose(false);
                }
            }
        }
        app.onDispose();

        // SHUTDOWN
        this.window.dispose();
        Engine.VULKAN_INITIALIZER.dispose();
        Engine.GLFW_INITIALIZER.dispose();
    }

}
