package de.pottgames.vengine.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;
import org.lwjgl.vulkan.VkViewport;

import de.pottgames.vengine.core.GlfwWindow.ResizeCallBack;
import de.pottgames.vengine.core.validation.Utils;

public class VulkanInitializer implements Disposable, ResizeCallBack {
    private static VulkanInitializer instance;

    // CONFIG
    private static final int  MAX_FRAMES_IN_FLIGHT = 2;
    private static final long UINT64_MAX           = 0xFFFFFFFFFFFFFFFFL;

    // VULKAN OBJECTS
    private VkInstance            vkInstance;
    private PhysicalDevice        physicalDevice;
    private VkDevice              device;
    private long                  surface;
    private long                  swapChain;
    private List<Long>            swapChainImages;
    private List<Long>            swapChainImageViews;
    private int                   swapChainImageFormat;
    private VkExtent2D            swapChainExtent;
    private List<Long>            swapChainFramebuffers;
    private long                  pipelineLayout;
    private long                  renderPass;
    private long                  graphicsPipeline;
    private long                  commandPool;
    private List<VkCommandBuffer> commandBuffers;

    // GLFW OBJECTS
    private final long window;

    // QUEUES
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;

    // VALIDATION AND DEBUGGING
    private final Set<String> validationLayers = new HashSet<>();
    private long              debugMessenger   = -1L;

    // EXTENSIONS
    private static final Set<String> DEVICE_EXTENSIONS = Stream.of(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(Collectors.toSet());

    // STATE
    private boolean             initialized = false;
    private List<Frame>         inFlightFrames;
    private Map<Integer, Frame> imagesInFlight;
    private int                 currentFrame;
    private boolean             framebufferResize;
    private SwapMode            desiredSwapMode;


    public static void create(long window) {
        VulkanInitializer.instance = new VulkanInitializer(window);
    }


    public static VulkanInitializer get() {
        return VulkanInitializer.instance;
    }


    private VulkanInitializer(long window) {
        this.validationLayers.add("VK_LAYER_KHRONOS_validation");
        this.window = window;
    }


    public void init(String title, boolean debugMode, GlfwWindow window, SwapMode desiredSwapMode) {
        if (this.initialized) {
            throw new RuntimeException("Vulkan is already initialized");
        }

        this.desiredSwapMode = desiredSwapMode;
        this.createInstance(title, debugMode);
        if (debugMode) {
            this.setupDebugMessenger();
        }
        this.createSurface(window.window);
        this.pickPhysicalDevice();
        if (debugMode) {
            System.out.println("Selected GPU: " + this.physicalDevice.getName());
            System.out.println("Supported API version: " + this.physicalDevice.getApiVersion().toString());
        }
        this.createLogicalDevice(debugMode);
        this.createCommandPool();
        this.createSwapChainObjects(desiredSwapMode);
        this.createSyncObjects();

        this.initialized = true;
    }


    @Override
    public void resizeCallback(int width, int height) {
        this.framebufferResize = true;
    }


    private void createSwapChainObjects(SwapMode swapMode) {
        this.createSwapChain(swapMode);
        this.createImageViews();
        this.createRenderPass();
        try {
            this.createGraphicsPipeline();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.createFrameBuffers();
        this.createCommandBuffers();
    }


    private void recreateSwapChain(SwapMode swapMode) {
        int width = 0;
        int height = 0;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer widthBuffer = stack.ints(0);
            final IntBuffer heightBuffer = stack.ints(0);

            while (widthBuffer.get(0) == 0 && heightBuffer.get(0) == 0) {
                GLFW.glfwGetFramebufferSize(this.window, widthBuffer, heightBuffer);
                GLFW.glfwWaitEvents();
                width = widthBuffer.get(0);
                height = heightBuffer.get(0);
            }
        }

        VK10.vkDeviceWaitIdle(this.device);
        this.disposeSwapChain();
        this.createSwapChainObjects(swapMode);
    }


    private void createCommandPool() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final QueueFamilyIndices queueFamilyIndices = this.findQueueFamilies(this.physicalDevice.getDevice());

            final VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily);

            final LongBuffer pCommandPool = stack.mallocLong(1);

            if (VK10.vkCreateCommandPool(this.device, poolInfo, null, pCommandPool) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }

            this.commandPool = pCommandPool.get(0);
        }
    }


    private void createCommandBuffers() {
        final int commandBuffersCount = this.swapChainFramebuffers.size();

        this.commandBuffers = new ArrayList<>(commandBuffersCount);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(this.commandPool);
            allocInfo.level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(commandBuffersCount);

            final PointerBuffer pCommandBuffers = stack.mallocPointer(commandBuffersCount);

            if (VK10.vkAllocateCommandBuffers(this.device, allocInfo, pCommandBuffers) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers");
            }

            for (int i = 0; i < commandBuffersCount; i++) {
                this.commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), this.device));
            }

            final VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            final VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
            renderPassInfo.sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
            renderPassInfo.renderPass(this.renderPass);
            final VkRect2D renderArea = VkRect2D.calloc(stack);
            renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
            renderArea.extent(this.swapChainExtent);
            renderPassInfo.renderArea(renderArea);
            final VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
            renderPassInfo.pClearValues(clearValues);

            for (int i = 0; i < commandBuffersCount; i++) {

                final VkCommandBuffer commandBuffer = this.commandBuffers.get(i);

                if (VK10.vkBeginCommandBuffer(commandBuffer, beginInfo) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to begin recording command buffer");
                }

                renderPassInfo.framebuffer(this.swapChainFramebuffers.get(i));

                VK10.vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK10.VK_SUBPASS_CONTENTS_INLINE);

                VK10.vkCmdBindPipeline(commandBuffer, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, this.graphicsPipeline);

                VK10.vkCmdDraw(commandBuffer, 3, 1, 0, 0);

                VK10.vkCmdEndRenderPass(commandBuffer);

                if (VK10.vkEndCommandBuffer(commandBuffer) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to record command buffer");
                }

            }

        }
    }


    private void createRenderPass() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.calloc(1, stack);
            colorAttachment.format(this.swapChainImageFormat);
            colorAttachment.samples(VK10.VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            final VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.calloc(1, stack);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            final VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK10.VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(colorAttachmentRef);

            final VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack);
            dependency.srcSubpass(VK10.VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            dependency.srcStageMask(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.srcAccessMask(0);
            dependency.dstStageMask(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.dstAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            final VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(colorAttachment);
            renderPassInfo.pSubpasses(subpass);
            renderPassInfo.pDependencies(dependency);

            final LongBuffer pRenderPass = stack.mallocLong(1);

            if (VK10.vkCreateRenderPass(this.device, renderPassInfo, null, pRenderPass) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }

            this.renderPass = pRenderPass.get(0);
        }
    }


    private void createSyncObjects() {

        this.inFlightFrames = new ArrayList<>(VulkanInitializer.MAX_FRAMES_IN_FLIGHT);
        this.imagesInFlight = new HashMap<>(this.swapChainImages.size());

        try (MemoryStack stack = MemoryStack.stackPush()) {

            final VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            final VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK10.VK_FENCE_CREATE_SIGNALED_BIT);

            final LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
            final LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
            final LongBuffer pFence = stack.mallocLong(1);

            for (int i = 0; i < VulkanInitializer.MAX_FRAMES_IN_FLIGHT; i++) {

                if (VK10.vkCreateSemaphore(this.device, semaphoreInfo, null, pImageAvailableSemaphore) != VK10.VK_SUCCESS
                        || VK10.vkCreateSemaphore(this.device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK10.VK_SUCCESS
                        || VK10.vkCreateFence(this.device, fenceInfo, null, pFence) != VK10.VK_SUCCESS) {

                    throw new RuntimeException("Failed to create synchronization objects for the frame " + i);
                }

                this.inFlightFrames.add(new Frame(pImageAvailableSemaphore.get(0), pRenderFinishedSemaphore.get(0), pFence.get(0)));
            }

        }
    }


    public void drawFrame() {

        try (MemoryStack stack = MemoryStack.stackPush()) {

            final Frame thisFrame = this.inFlightFrames.get(this.currentFrame);

            VK10.vkWaitForFences(this.device, thisFrame.pFence(), true, VulkanInitializer.UINT64_MAX);

            final IntBuffer pImageIndex = stack.mallocInt(1);

            int vkResult = KHRSwapchain.vkAcquireNextImageKHR(this.device, this.swapChain, VulkanInitializer.UINT64_MAX, thisFrame.imageAvailableSemaphore(),
                    VK10.VK_NULL_HANDLE, pImageIndex);

            if (vkResult == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                this.recreateSwapChain(this.desiredSwapMode);
                return;
            }

            final int imageIndex = pImageIndex.get(0);

            if (this.imagesInFlight.containsKey(imageIndex)) {
                VK10.vkWaitForFences(this.device, this.imagesInFlight.get(imageIndex).fence(), true, VulkanInitializer.UINT64_MAX);
            }

            this.imagesInFlight.put(imageIndex, thisFrame);

            final VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(thisFrame.pImageAvailableSemaphore());
            submitInfo.pWaitDstStageMask(stack.ints(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

            submitInfo.pSignalSemaphores(thisFrame.pRenderFinishedSemaphore());

            submitInfo.pCommandBuffers(stack.pointers(this.commandBuffers.get(imageIndex)));

            VK10.vkResetFences(this.device, thisFrame.pFence());

            if (VK10.vkQueueSubmit(this.graphicsQueue, submitInfo, thisFrame.fence()) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to submit draw command buffer");
            }

            final VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);

            presentInfo.pWaitSemaphores(thisFrame.pRenderFinishedSemaphore());

            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(this.swapChain));

            presentInfo.pImageIndices(pImageIndex);

            vkResult = KHRSwapchain.vkQueuePresentKHR(this.presentQueue, presentInfo);

            if (vkResult == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR || vkResult == KHRSwapchain.VK_SUBOPTIMAL_KHR || this.framebufferResize) {
                this.framebufferResize = false;
                this.recreateSwapChain(this.desiredSwapMode);
            } else if (vkResult != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to present swap chain image");
            }

            this.currentFrame = (this.currentFrame + 1) % VulkanInitializer.MAX_FRAMES_IN_FLIGHT;
        }
    }


    private void createFrameBuffers() {
        this.swapChainFramebuffers = new ArrayList<>(this.swapChainImageViews.size());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final LongBuffer attachments = stack.mallocLong(1);
            final LongBuffer pFramebuffer = stack.mallocLong(1);

            // Lets allocate the create info struct once and just update the pAttachments field each iteration
            final VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType(VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(this.renderPass);
            framebufferInfo.width(this.swapChainExtent.width());
            framebufferInfo.height(this.swapChainExtent.height());
            framebufferInfo.layers(1);

            for (final long imageView : this.swapChainImageViews) {

                attachments.put(0, imageView);

                framebufferInfo.pAttachments(attachments);

                if (VK10.vkCreateFramebuffer(this.device, framebufferInfo, null, pFramebuffer) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }

                this.swapChainFramebuffers.add(pFramebuffer.get(0));
            }
        }
    }


    private void createGraphicsPipeline() throws IOException {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final byte[] vertexBytes = Files.readAllBytes(Path.of("shaders/triangle/triangle_vert.spv"));
            final byte[] fragmentBytes = Files.readAllBytes(Path.of("shaders/triangle/triangle_frag.spv"));
            // final ByteBuffer vertexShaderBuffer = ByteBuffer.wrap(vertexBytes);
            // final ByteBuffer fragmentShaderBuffer = ByteBuffer.wrap(fragmentBytes);
            final ByteBuffer vertexShaderBuffer = ByteBuffer.allocateDirect(vertexBytes.length);
            final ByteBuffer fragmentShaderBuffer = ByteBuffer.allocateDirect(fragmentBytes.length);
            for (final byte vertexByte : vertexBytes) {
                vertexShaderBuffer.put(vertexByte);
            }
            for (final byte fragmentByte : fragmentBytes) {
                fragmentShaderBuffer.put(fragmentByte);
            }
            vertexShaderBuffer.position(0);
            fragmentShaderBuffer.position(0);

            final long vertShaderModule = this.createShaderModule(vertexShaderBuffer);
            final long fragShaderModule = this.createShaderModule(fragmentShaderBuffer);

            final ByteBuffer entryPoint = stack.UTF8("main");

            final VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);

            final VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);

            vertShaderStageInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertShaderStageInfo.stage(VK10.VK_SHADER_STAGE_VERTEX_BIT);
            vertShaderStageInfo.module(vertShaderModule);
            vertShaderStageInfo.pName(entryPoint);

            final VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);

            fragShaderStageInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK10.VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShaderModule);
            fragShaderStageInfo.pName(entryPoint);

            // ===> VERTEX STAGE <===

            final VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);

            // ===> ASSEMBLY STAGE <===

            final VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
            inputAssembly.primitiveRestartEnable(false);

            // ===> VIEWPORT & SCISSOR

            final VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width(this.swapChainExtent.width());
            viewport.height(this.swapChainExtent.height());
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            final VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
            scissor.extent(this.swapChainExtent);

            final VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            viewportState.pViewports(viewport);
            viewportState.pScissors(scissor);

            // ===> RASTERIZATION STAGE <===

            final VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(VK10.VK_POLYGON_MODE_FILL);
            rasterizer.lineWidth(1.0f);
            rasterizer.cullMode(VK10.VK_CULL_MODE_BACK_BIT);
            rasterizer.frontFace(VK10.VK_FRONT_FACE_CLOCKWISE);
            rasterizer.depthBiasEnable(false);

            // ===> MULTISAMPLING <===

            final VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK10.VK_SAMPLE_COUNT_1_BIT);

            // ===> COLOR BLENDING <===

            final VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachment.colorWriteMask(
                    VK10.VK_COLOR_COMPONENT_R_BIT | VK10.VK_COLOR_COMPONENT_G_BIT | VK10.VK_COLOR_COMPONENT_B_BIT | VK10.VK_COLOR_COMPONENT_A_BIT);
            colorBlendAttachment.blendEnable(false);

            final VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(false);
            colorBlending.logicOp(VK10.VK_LOGIC_OP_COPY);
            colorBlending.pAttachments(colorBlendAttachment);
            colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));

            // ===> PIPELINE LAYOUT CREATION <===

            final VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);

            final LongBuffer pPipelineLayout = stack.longs(VK10.VK_NULL_HANDLE);

            if (VK10.vkCreatePipelineLayout(this.device, pipelineLayoutInfo, null, pPipelineLayout) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            this.pipelineLayout = pPipelineLayout.get(0);

            final VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.layout(this.pipelineLayout);
            pipelineInfo.renderPass(this.renderPass);
            pipelineInfo.subpass(0);
            pipelineInfo.basePipelineHandle(VK10.VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            final LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if (VK10.vkCreateGraphicsPipelines(this.device, VK10.VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            this.graphicsPipeline = pGraphicsPipeline.get(0);

            // ===> RELEASE RESOURCES <===

            VK10.vkDestroyShaderModule(this.device, vertShaderModule, null);
            VK10.vkDestroyShaderModule(this.device, fragShaderModule, null);
        }
    }


    private long createShaderModule(ByteBuffer spirvCode) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            final LongBuffer pShaderModule = stack.mallocLong(1);

            if (VK10.vkCreateShaderModule(this.device, createInfo, null, pShaderModule) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module");
            }

            return pShaderModule.get(0);
        }
    }


    private void createInstance(String title, boolean debugMode) {
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
                createInfo.ppEnabledLayerNames(this.asPointerBuffer(stack, this.validationLayers));
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


    private void createImageViews() {
        this.swapChainImageViews = new ArrayList<>(this.swapChainImages.size());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final LongBuffer pImageView = stack.mallocLong(1);

            for (final long swapChainImage : this.swapChainImages) {
                final VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack);

                createInfo.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                createInfo.image(swapChainImage);
                createInfo.viewType(VK10.VK_IMAGE_VIEW_TYPE_2D);
                createInfo.format(this.swapChainImageFormat);

                createInfo.components().r(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().g(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().b(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().a(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);

                createInfo.subresourceRange().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
                createInfo.subresourceRange().baseMipLevel(0);
                createInfo.subresourceRange().levelCount(1);
                createInfo.subresourceRange().baseArrayLayer(0);
                createInfo.subresourceRange().layerCount(1);

                if (VK10.vkCreateImageView(this.device, createInfo, null, pImageView) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create image views");
                }

                this.swapChainImageViews.add(pImageView.get(0));
            }

        }
    }


    private void createLogicalDevice(boolean debugMode) {

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final QueueFamilyIndices indices = this.findQueueFamilies(this.physicalDevice.getDevice());
            final int[] uniqueQueueFamilies = indices.unique();
            final VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

            for (int i = 0; i < uniqueQueueFamilies.length; i++) {
                final VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            final VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            final VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pEnabledFeatures(deviceFeatures);
            createInfo.ppEnabledExtensionNames(this.asPointerBuffer(stack, VulkanInitializer.DEVICE_EXTENSIONS));

            if (debugMode) {
                createInfo.ppEnabledLayerNames(this.asPointerBuffer(stack, this.validationLayers));
            }

            final PointerBuffer pDevice = stack.pointers(VK10.VK_NULL_HANDLE);

            if (VK10.vkCreateDevice(this.physicalDevice.getDevice(), createInfo, null, pDevice) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }

            this.device = new VkDevice(pDevice.get(0), this.physicalDevice.getDevice(), createInfo);

            final PointerBuffer pQueue = stack.pointers(VK10.VK_NULL_HANDLE);
            VK10.vkGetDeviceQueue(this.device, indices.graphicsFamily, 0, pQueue);
            this.graphicsQueue = new VkQueue(pQueue.get(0), this.device);
            VK10.vkGetDeviceQueue(this.device, indices.presentFamily, 0, pQueue);
            this.presentQueue = new VkQueue(pQueue.get(0), this.device);
        }
    }


    private void createSwapChain(SwapMode desiredSwapMode) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final SwapChainSupportDetails swapChainSupport = this.querySwapChainSupport(this.physicalDevice.getDevice(), stack);

            final VkSurfaceFormatKHR surfaceFormat = this.chooseSwapSurfaceFormat(swapChainSupport.formats);
            final SwapMode presentMode = this.chooseSwapPresentMode(swapChainSupport.presentModes, desiredSwapMode);
            final VkExtent2D extent = this.chooseSwapExtent(stack, swapChainSupport.capabilities);

            final IntBuffer imageCount = stack.ints(swapChainSupport.capabilities.minImageCount() + 1);

            if (swapChainSupport.capabilities.maxImageCount() > 0 && imageCount.get(0) > swapChainSupport.capabilities.maxImageCount()) {
                imageCount.put(0, swapChainSupport.capabilities.maxImageCount());
            }

            final VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);

            createInfo.sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(this.surface);

            // Image settings
            createInfo.minImageCount(imageCount.get(0));
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            final QueueFamilyIndices indices = this.findQueueFamilies(this.physicalDevice.getDevice());

            if (!indices.graphicsFamily.equals(indices.presentFamily)) {
                createInfo.imageSharingMode(VK10.VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(indices.graphicsFamily, indices.presentFamily));
            } else {
                createInfo.imageSharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(swapChainSupport.capabilities.currentTransform());
            createInfo.compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode.getVulkanId());
            createInfo.clipped(true);

            createInfo.oldSwapchain(VK10.VK_NULL_HANDLE);

            final LongBuffer pSwapChain = stack.longs(VK10.VK_NULL_HANDLE);

            if (KHRSwapchain.vkCreateSwapchainKHR(this.device, createInfo, null, pSwapChain) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain");
            }

            this.swapChain = pSwapChain.get(0);

            KHRSwapchain.vkGetSwapchainImagesKHR(this.device, this.swapChain, imageCount, null);

            final LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

            KHRSwapchain.vkGetSwapchainImagesKHR(this.device, this.swapChain, imageCount, pSwapchainImages);

            this.swapChainImages = new ArrayList<>(imageCount.get(0));

            for (int i = 0; i < pSwapchainImages.capacity(); i++) {
                this.swapChainImages.add(pSwapchainImages.get(i));
            }

            this.swapChainImageFormat = surfaceFormat.format();
            this.swapChainExtent = VkExtent2D.create().set(extent);
        }
    }


    private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
        return availableFormats.stream().filter(availableFormat -> availableFormat.format() == VK10.VK_FORMAT_B8G8R8_UNORM)
                .filter(availableFormat -> availableFormat.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR).findAny()
                .orElse(availableFormats.get(0));
    }


    private SwapMode chooseSwapPresentMode(IntBuffer availablePresentModes, SwapMode desiredSwapMode) {
        for (int i = 0; i < availablePresentModes.capacity(); i++) {
            if (availablePresentModes.get(i) == desiredSwapMode.getVulkanId()) {
                return desiredSwapMode;
            }
        }

        // VSYNC is guaranteed to be available
        return SwapMode.VSYNC;
    }


    private VkExtent2D chooseSwapExtent(MemoryStack stack, VkSurfaceCapabilitiesKHR capabilities) {
        if (capabilities.currentExtent().width() != VulkanUtils.UINT32_MAX) {
            return capabilities.currentExtent();
        }

        final IntBuffer width = MemoryStack.stackGet().ints(0);
        final IntBuffer height = MemoryStack.stackGet().ints(0);

        GLFW.glfwGetFramebufferSize(this.window, width, height);

        final VkExtent2D actualExtent = VkExtent2D.malloc(stack).set(width.get(0), height.get(0));

        final VkExtent2D minExtent = capabilities.minImageExtent();
        final VkExtent2D maxExtent = capabilities.maxImageExtent();

        actualExtent.width(this.clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(this.clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

        return actualExtent;
    }


    private int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }


    private void createSurface(long windowHandle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final LongBuffer pSurface = stack.longs(VK10.VK_NULL_HANDLE);

            if (GLFWVulkan.glfwCreateWindowSurface(this.vkInstance, windowHandle, null, pSurface) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create window surface");
            }

            this.surface = pSurface.get(0);
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
        final boolean extensionsSupported = this.checkDeviceExtensionSupport(device);
        boolean swapChainAdequate = false;

        if (extensionsSupported) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                final SwapChainSupportDetails swapChainSupport = this.querySwapChainSupport(device, stack);
                swapChainAdequate = swapChainSupport.formats.hasRemaining() && swapChainSupport.presentModes.hasRemaining();
            }
        }

        return indices.isComplete() && extensionsSupported && swapChainAdequate;
    }


    private boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer extensionCount = stack.ints(0);
            VK10.vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, null);
            final VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0), stack);
            VK10.vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, availableExtensions);
            return availableExtensions.stream().map(VkExtensionProperties::extensionNameString).collect(Collectors.toSet())
                    .containsAll(VulkanInitializer.DEVICE_EXTENSIONS);
        }
    }


    private SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device, MemoryStack stack) {
        final SwapChainSupportDetails details = new SwapChainSupportDetails();

        details.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, this.surface, details.capabilities);

        final IntBuffer count = stack.ints(0);

        KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device, this.surface, count, null);

        if (count.get(0) != 0) {
            details.formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device, this.surface, count, details.formats);
        }

        KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device, this.surface, count, null);

        if (count.get(0) != 0) {
            details.presentModes = stack.mallocInt(count.get(0));
            KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device, this.surface, count, details.presentModes);
        }

        return details;
    }


    private QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {
        final QueueFamilyIndices indices = new QueueFamilyIndices();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer queueFamilyCount = stack.ints(0);
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);
            final VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);
            final IntBuffer presentSupport = stack.ints(VK10.VK_FALSE);

            for (int i = 0; i < queueFamilies.capacity() || !indices.isComplete(); i++) {
                if ((queueFamilies.get(i).queueFlags() & VK10.VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.graphicsFamily = i;
                }
                KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(device, i, this.surface, presentSupport);
                if (presentSupport.get(0) == VK10.VK_TRUE) {
                    indices.presentFamily = i;
                }
            }

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


    private PointerBuffer asPointerBuffer(MemoryStack stack, Collection<String> collection) {
        final PointerBuffer buffer = stack.mallocPointer(collection.size());
        collection.stream().map(stack::UTF8).forEach(buffer::put);
        return buffer.rewind();
    }


    private PointerBuffer asPointerBuffer(MemoryStack stack, List<? extends Pointer> list) {
        final PointerBuffer buffer = stack.mallocPointer(list.size());
        list.forEach(buffer::put);
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
        // Wait for the device to complete all operations before release resources
        VK10.vkDeviceWaitIdle(this.device);

        this.disposeSwapChain();
        this.inFlightFrames.forEach(frame -> {
            VK10.vkDestroySemaphore(this.device, frame.renderFinishedSemaphore(), null);
            VK10.vkDestroySemaphore(this.device, frame.imageAvailableSemaphore(), null);
            VK10.vkDestroyFence(this.device, frame.fence(), null);
        });
        this.imagesInFlight.clear();
        VK10.vkDestroyCommandPool(this.device, this.commandPool, null);
        VK10.vkDestroyDevice(this.device, null);
        if (this.debugMessenger != -1L) {
            Utils.destroyDebugUtilsMessengerEXT(this.vkInstance, this.debugMessenger, null);
        }
        KHRSurface.vkDestroySurfaceKHR(this.vkInstance, this.surface, null);
        VK10.vkDestroyInstance(this.vkInstance, null);

        this.initialized = false;
    }


    private void disposeSwapChain() {
        this.swapChainFramebuffers.forEach(framebuffer -> VK10.vkDestroyFramebuffer(this.device, framebuffer, null));
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VK10.vkFreeCommandBuffers(this.device, this.commandPool, this.asPointerBuffer(stack, this.commandBuffers));
        }
        VK10.vkDestroyPipeline(this.device, this.graphicsPipeline, null);
        VK10.vkDestroyPipelineLayout(this.device, this.pipelineLayout, null);
        VK10.vkDestroyRenderPass(this.device, this.renderPass, null);
        this.swapChainImageViews.forEach(imageView -> VK10.vkDestroyImageView(this.device, imageView, null));
        KHRSwapchain.vkDestroySwapchainKHR(this.device, this.swapChain, null);
    }


    private static class QueueFamilyIndices {
        // We use Integer to use null as the empty value
        private Integer graphicsFamily;
        private Integer presentFamily;


        private boolean isComplete() {
            return this.graphicsFamily != null && this.presentFamily != null;
        }


        private int[] unique() {
            return IntStream.of(this.graphicsFamily, this.presentFamily).distinct().toArray();
        }


        private int[] array() {
            return new int[] { this.graphicsFamily, this.presentFamily };
        }

    }


    private static class SwapChainSupportDetails {
        private VkSurfaceCapabilitiesKHR  capabilities;
        private VkSurfaceFormatKHR.Buffer formats;
        private IntBuffer                 presentModes;

    }

}
