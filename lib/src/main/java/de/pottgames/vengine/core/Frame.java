package de.pottgames.vengine.core;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;

/**
 * Wraps the needed sync objects for an in flight frame
 *
 * This frame's sync objects must be deleted manually
 */
public class Frame {

    private final long imageAvailableSemaphore;
    private final long renderFinishedSemaphore;
    private final long fence;


    public Frame(long imageAvailableSemaphore, long renderFinishedSemaphore, long fence) {
        this.imageAvailableSemaphore = imageAvailableSemaphore;
        this.renderFinishedSemaphore = renderFinishedSemaphore;
        this.fence = fence;
    }


    public long imageAvailableSemaphore() {
        return this.imageAvailableSemaphore;
    }


    public LongBuffer pImageAvailableSemaphore() {
        return MemoryStack.stackGet().longs(this.imageAvailableSemaphore);
    }


    public long renderFinishedSemaphore() {
        return this.renderFinishedSemaphore;
    }


    public LongBuffer pRenderFinishedSemaphore() {
        return MemoryStack.stackGet().longs(this.renderFinishedSemaphore);
    }


    public long fence() {
        return this.fence;
    }


    public LongBuffer pFence() {
        return MemoryStack.stackGet().longs(this.fence);
    }

}
