package de.pottgames.vengine.core;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import de.pottgames.vengine.core.validation.Utils;

public class VulkanInitializer implements Disposable {
    private static VulkanInitializer instance         = new VulkanInitializer();
    private VkInstance               vkInstance;
    private PhysicalDevice           physicalDevice;
    private VkDevice                 device;
    private VkQueue                  graphicsQueue;
    private final Set<String>        validationLayers = new HashSet<>();
    private long                     debugMessenger   = -1L;
    private boolean                  initialized      = false;


    public static VulkanInitializer get() {
        return VulkanInitializer.instance;
    }


    private VulkanInitializer() {
        this.validationLayers.add("VK_LAYER_KHRONOS_validation");
    }


    public void init(String title, boolean debugMode) {
        if (this.initialized) {
            throw new RuntimeException("GLFW is already initialized");
        }

        this.initGlfw();
        this.initVulkan(title, debugMode);
        if (debugMode) {
            this.setupDebugMessenger();
        }
        this.pickPhysicalDevice();
        if (debugMode) {
            System.out.println("Selected GPU: " + this.physicalDevice.getName());
            System.out.println("Supported API version: " + this.physicalDevice.getApiVersion().toString());
        }
        this.createLogicalDevice(debugMode);

        this.initialized = true;
    }


    private void initGlfw() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
    }


    private void initVulkan(String title, boolean debugMode) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);
            appInfo.sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe(title));
            appInfo.applicationVersion(VK10.VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8Safe("VEngine"));
            appInfo.engineVersion(VK10.VK_MAKE_VERSION(0, 0, 1));
            appInfo.apiVersion(VK10.VK_API_VERSION_1_0);

            final VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            createInfo.ppEnabledExtensionNames(this.getRequiredExtensions(stack, debugMode));

            if (debugMode) {
                createInfo.ppEnabledLayerNames(this.validationLayersAsPointerBuffer(stack));
                final VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                this.populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }

            // We need to retrieve the pointer of the created instance
            final PointerBuffer instancePtr = stack.mallocPointer(1);
            if (VK10.vkCreateInstance(createInfo, null, instancePtr) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan instance");
            }
            this.vkInstance = new VkInstance(instancePtr.get(0), createInfo);
        }
    }


    private void createLogicalDevice(boolean debugMode) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final QueueFamilyIndices indices = this.findQueueFamilies(this.physicalDevice.getDevice());
            final VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(1, stack);

            queueCreateInfos.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
            queueCreateInfos.queueFamilyIndex(indices.graphicsFamily);
            queueCreateInfos.pQueuePriorities(stack.floats(1.0f));

            final VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            final VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pEnabledFeatures(deviceFeatures);

            if (debugMode) {
                createInfo.ppEnabledLayerNames(this.validationLayersAsPointerBuffer(stack));
            }

            final PointerBuffer pDevice = stack.pointers(VK10.VK_NULL_HANDLE);

            if (VK10.vkCreateDevice(this.physicalDevice.getDevice(), createInfo, null, pDevice) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }

            this.device = new VkDevice(pDevice.get(0), this.physicalDevice.getDevice(), createInfo);

            final PointerBuffer pGraphicsQueue = stack.pointers(VK10.VK_NULL_HANDLE);

            VK10.vkGetDeviceQueue(this.device, indices.graphicsFamily, 0, pGraphicsQueue);

            this.graphicsQueue = new VkQueue(pGraphicsQueue.get(0), this.device);
        }
    }


    private void pickPhysicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer deviceCount = stack.ints(0);
            VK10.vkEnumeratePhysicalDevices(this.vkInstance, deviceCount, null);
            if (deviceCount.get(0) == 0) {
                throw new RuntimeException("Failed to find GPUs with Vulkan support");
            }

            final PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));
            VK10.vkEnumeratePhysicalDevices(this.vkInstance, deviceCount, ppPhysicalDevices);
            for (int i = 0; i < ppPhysicalDevices.capacity(); i++) {
                final VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), this.vkInstance);
                if (this.isDeviceSuitable(device)) {
                    this.physicalDevice = new PhysicalDevice(device);
                    return;
                }
            }

            throw new RuntimeException("Failed to find a suitable GPU");
        }
    }


    private boolean isDeviceSuitable(VkPhysicalDevice device) {
        final QueueFamilyIndices indices = this.findQueueFamilies(device);
        return indices.isComplete();
    }


    private QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {
        final QueueFamilyIndices indices = new QueueFamilyIndices();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer queueFamilyCount = stack.ints(0);
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);
            final VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);
            IntStream.range(0, queueFamilies.capacity()).filter(index -> (queueFamilies.get(index).queueFlags() & VK10.VK_QUEUE_GRAPHICS_BIT) != 0).findFirst()
                    .ifPresent(index -> indices.graphicsFamily = index);
            return indices;
        }
    }


    private PointerBuffer getRequiredExtensions(MemoryStack stack, boolean debugMode) {
        final PointerBuffer glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();

        if (debugMode) {
            final PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);
            extensions.put(glfwExtensions);
            extensions.put(stack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
            return extensions.rewind();
        }

        return glfwExtensions;
    }


    private PointerBuffer validationLayersAsPointerBuffer(MemoryStack stack) {
        final PointerBuffer buffer = stack.mallocPointer(this.validationLayers.size());
        this.validationLayers.stream().map(stack::UTF8).forEach(buffer::put);
        return buffer.rewind();
    }


    private void setupDebugMessenger() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
            this.populateDebugMessengerCreateInfo(createInfo);
            final LongBuffer pDebugMessenger = stack.longs(VK10.VK_NULL_HANDLE);
            if (Utils.createDebugUtilsMessengerEXT(this.vkInstance, createInfo, null, pDebugMessenger) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to set up debug messenger");
            }
            this.debugMessenger = pDebugMessenger.get(0);
        }
    }


    private void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo) {
        debugCreateInfo.sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
        debugCreateInfo.messageSeverity(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
                | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
        debugCreateInfo.messageType(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
                | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
        debugCreateInfo.pfnUserCallback(Utils::debugCallback);
    }


    @Override
    public void dispose() {
        VK10.vkDestroyDevice(this.device, null);
        if (this.debugMessenger != -1L) {
            Utils.destroyDebugUtilsMessengerEXT(this.vkInstance, this.debugMessenger, null);
        }
        VK10.vkDestroyInstance(this.vkInstance, null);
        GLFW.glfwTerminate();
        this.initialized = false;
    }


    private static class QueueFamilyIndices {
        // We use Integer to use null as the empty value
        private Integer graphicsFamily;


        private boolean isComplete() {
            return this.graphicsFamily != null;
        }

    }

}
