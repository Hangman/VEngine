package de.pottgames.vengine.core;

import org.lwjgl.glfw.GLFW;

public class Engine {
    private static final VulkanInitializer GLFW_INITIALIZER = VulkanInitializer.get();
    private final GlfwWindow               window;
    private final Application              app;
    private final int                      framerate;
    private final Sync                     sync;
    private boolean                        running          = false;


    public Engine(ApplicationConfiguration config, Application app) {
        final WindowConfiguration windowConfig = config.getWindowConfiguration();
        this.app = app;
        this.framerate = windowConfig.getFramerate();
        this.sync = new Sync();
        Engine.GLFW_INITIALIZER.init(windowConfig.getTitle(), config.isDebugMode());
        this.window = new GlfwWindow(windowConfig);
        this.running = true;

        app.onCreate();
        this.loop();
        this.app.onDispose();
        this.window.dispose();
        Engine.GLFW_INITIALIZER.dispose();
    }


    protected void loop() {
        while (this.running) {
            GLFW.glfwPollEvents();
            this.app.onRender();
            this.sync.sync(this.framerate);
            if (this.window.shouldClose()) {
                if (this.app.canClose()) {
                    this.running = false;
                } else {
                    this.window.setShouldClose(false);
                }
            }
        }
    }

}
