package de.pottgames.vengine.basic;

import de.pottgames.vengine.core.Application;
import de.pottgames.vengine.core.ApplicationConfiguration;
import de.pottgames.vengine.core.Engine;
import de.pottgames.vengine.core.WindowConfiguration;

public class LaunchTest implements Application {
    private long startTime;


    @Override
    public void onCreate() {
        this.startTime = System.currentTimeMillis();
    }


    @Override
    public void onRender() {
        // TODO Auto-generated method stub

    }


    @Override
    public void onResize() {
        // TODO Auto-generated method stub

    }


    @Override
    public void onDispose() {
        // TODO Auto-generated method stub

    }


    public static void main(String[] args) {
        final ApplicationConfiguration config = new ApplicationConfiguration();
        config.setDebugMode(true);
        final WindowConfiguration windowConfig = config.getWindowConfiguration();
        windowConfig.setCenter(false);
        windowConfig.setPosX(400);
        windowConfig.setPosY(400);
        windowConfig.setTitle("LaunchTest");

        new Engine(config, new LaunchTest());
    }

}
