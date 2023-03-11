package de.pottgames.vengine.core;

public class WindowConfiguration {
    private String   title        = "VEngine Application";
    private int      width        = 800;
    private int      height       = 600;
    private boolean  visible      = true;
    private boolean  resizable    = true;
    private boolean  center       = true;
    private int      posX;
    private int      posY;
    private int      maxFramerate = 60;
    private SwapMode swapMode     = SwapMode.VSYNC;


    public String getTitle() {
        return this.title;
    }


    public void setTitle(String title) {
        this.title = title;
    }


    public int getWidth() {
        return this.width;
    }


    public void setWidth(int width) {
        this.width = width;
    }


    public int getHeight() {
        return this.height;
    }


    public void setHeight(int height) {
        this.height = height;
    }


    public boolean isVisible() {
        return this.visible;
    }


    public void setVisible(boolean visible) {
        this.visible = visible;
    }


    public boolean isResizable() {
        return this.resizable;
    }


    public void setResizable(boolean resizable) {
        this.resizable = resizable;
    }


    public boolean isCenter() {
        return this.center;
    }


    public void setCenter(boolean center) {
        this.center = center;
    }


    public int getPosX() {
        return this.posX;
    }


    public void setPosX(int posX) {
        this.posX = posX;
    }


    public int getPosY() {
        return this.posY;
    }


    public void setPosY(int posY) {
        this.posY = posY;
    }


    public int getMaxFramerate() {
        return this.maxFramerate;
    }


    public void setMaxFramerate(int framerate) {
        this.maxFramerate = framerate;
    }


    public SwapMode getSwapMode() {
        return this.swapMode;
    }


    public void setSwapMode(SwapMode swapMode) {
        this.swapMode = swapMode;
    }

}
