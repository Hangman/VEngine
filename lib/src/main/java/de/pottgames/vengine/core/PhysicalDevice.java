package de.pottgames.vengine.core;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import de.pottgames.vengine.core.VulkanUtils.ApiVersion;

public class PhysicalDevice {
    private final VkPhysicalDevice device;
    private final String           name;
    private final int              vendorId;
    private final ApiVersion       apiVersion;


    public PhysicalDevice(VkPhysicalDevice device) {
        this.device = device;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final VkPhysicalDeviceProperties pProperties = VkPhysicalDeviceProperties.calloc(stack);
            VK10.vkGetPhysicalDeviceProperties(device, pProperties);
            this.name = pProperties.deviceNameString();
            this.vendorId = pProperties.vendorID();
            this.apiVersion = VulkanUtils.decodeApiVersionNumber(pProperties.apiVersion());
        }
    }


    public VkPhysicalDevice getDevice() {
        return this.device;
    }


    public ApiVersion getApiVersion() {
        return this.apiVersion;
    }


    public int getVendorId() {
        return this.vendorId;
    }


    public String getName() {
        return this.name;
    }

}
