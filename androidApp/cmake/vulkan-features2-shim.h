#pragma once

#include <vulkan/vulkan_core.h>

#define VULKAN_HPP_DISPATCH_LOADER_DYNAMIC 1
#if VK_HEADER_VERSION >= 301
namespace vk::detail { class DispatchLoaderDynamic; }
using vk::detail::DispatchLoaderDynamic;
#else
namespace vk { class DispatchLoaderDynamic; }
using vk::DispatchLoaderDynamic;
#endif
DispatchLoaderDynamic & ggml_vk_default_dispatcher();
#define VULKAN_HPP_DEFAULT_DISPATCHER ggml_vk_default_dispatcher()

#include <vulkan/vulkan.hpp>

#undef VULKAN_HPP_DEFAULT_DISPATCHER

#define vkGetPhysicalDeviceFeatures2(...) \
    ggml_vk_default_dispatcher().vkGetPhysicalDeviceFeatures2(__VA_ARGS__)
