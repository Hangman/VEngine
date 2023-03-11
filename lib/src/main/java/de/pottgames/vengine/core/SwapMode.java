package de.pottgames.vengine.core;

import org.lwjgl.vulkan.KHRSurface;

public enum SwapMode {
    /**
     * Avoids screen tearing by synchronizing rendering and screen refresh. If the render queue is full, the program has to wait until the next screen refresh
     * frees a slot.
     */
    VSYNC(KHRSurface.VK_PRESENT_MODE_FIFO_KHR),
    /**
     * Avoids screen tearing by synchronizing display updates and screen refresh. If the render queue is full, the program does not have to wait and unpresented
     * frames will just be replaced by newer ones.
     */
    TRIPLE_BUFFERING(KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR),
    /**
     * Rendered images are immediately transferred to the screen, so tearing may occur.
     */
    IMMEDIATE(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR),
    /**
     * If the application is late and the render queue was empty at the last vertical blank, instead of waiting for the next vertical blank, the image is
     * transferred right away. This may result in visible tearing.
     */
    IMMEDIATE_VSYNC(KHRSurface.VK_PRESENT_MODE_FIFO_RELAXED_KHR);


    private final int vulkanId;


    SwapMode(int vulkanId) {
        this.vulkanId = vulkanId;
    }


    protected int getVulkanId() {
        return this.vulkanId;
    }

}
