package de.pottgames.vengine.core;

public interface Application {

    void onCreate();


    void onRender();


    void onResize();


    default boolean canClose() {
        return true;
    }


    void onDispose();

}
