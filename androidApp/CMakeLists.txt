cmake_minimum_required(VERSION 3.22)
project(llama_bridge LANGUAGES CXX)

# ============================================================
# llama.cpp JNI Bridge for ChromaLab
# ============================================================
#
# Phase 1: Stub-only build (no llama.cpp source yet)
# Phase 2+: Will add llama.cpp as subdirectory/submodule
#
# To add full llama.cpp support later:
#   1. git submodule add https://github.com/ggerganov/llama.cpp libs/llama.cpp
#   2. Uncomment the llama.cpp section below
#   3. Link against llama and ggml targets
# ============================================================

set(CMAKE_CXX_STANDARD 17)

# ---- Stub library (Phase 1) ----
add_library(llama_bridge SHARED
    src/main/cpp/llama_bridge.cpp
)

target_link_libraries(llama_bridge
    android
    log
)

# ---- Full llama.cpp integration (uncomment when ready) ----
# set(LLAMA_CPP_DIR "${CMAKE_SOURCE_DIR}/libs/llama.cpp")
#
# # Build options
# set(GGML_VULKAN ON CACHE BOOL "Enable Vulkan backend")
# set(GGML_OPENCL OFF CACHE BOOL "Enable OpenCL backend")
# set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build static libs")
# set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Skip tests")
# set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Skip examples")
#
# add_subdirectory(${LLAMA_CPP_DIR} llama.cpp)
#
# target_include_directories(llama_bridge PRIVATE
#     ${LLAMA_CPP_DIR}/include
#     ${LLAMA_CPP_DIR}/common
#     ${LLAMA_CPP_DIR}/examples/llava
# )
#
# target_link_libraries(llama_bridge
#     android
#     log
#     llama
#     ggml
#     common
# )
