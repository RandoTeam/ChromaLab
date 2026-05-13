cmake_minimum_required(VERSION 3.22)
project(llama_bridge LANGUAGES C CXX)

# ============================================================
# llama.cpp JNI Bridge for ChromaLab
# ============================================================
# Pinned to tag b9101 (2026-05-10).
# Builds: ggml + llama (text) + mtmd library (vision/audio).
# Links statically into llama_bridge.so for Android arm64.
# ============================================================

set(CMAKE_CXX_STANDARD 17)

# Android debug APKs are used for on-device model validation, but llama.cpp
# itself must stay optimized or CPU inference becomes unusably slow.
if (CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -O3 -DNDEBUG")
    set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -O3 -DNDEBUG")
endif()

# ---- llama.cpp source tree ----
set(LLAMA_CPP_DIR "${CMAKE_SOURCE_DIR}/libs/llama.cpp")

get_filename_component(CHROMALAB_CMAKE_BIN_DIR "${CMAKE_COMMAND}" DIRECTORY)

set(CHROMALAB_HOST_COMPILER_HINTS "")
if (DEFINED ENV{CHROMALAB_HOST_TOOLCHAIN})
    list(APPEND CHROMALAB_HOST_COMPILER_HINTS "$ENV{CHROMALAB_HOST_TOOLCHAIN}/bin")
endif()
if (DEFINED ENV{W64DEVKIT_HOME})
    list(APPEND CHROMALAB_HOST_COMPILER_HINTS "$ENV{W64DEVKIT_HOME}/bin")
endif()
if (CMAKE_HOST_WIN32 AND DEFINED ENV{USERPROFILE})
    list(APPEND CHROMALAB_HOST_COMPILER_HINTS
        "$ENV{USERPROFILE}/.chromalab/toolchains/w64devkit/bin"
        "$ENV{USERPROFILE}/w64devkit/bin"
        "C:/w64devkit/bin"
    )
endif()

if (CMAKE_ANDROID_NDK)
    if (CMAKE_HOST_WIN32)
        set(CHROMALAB_NDK_SHADER_TOOLS_PLATFORM "windows-x86_64")
        set(CHROMALAB_HOST_EXECUTABLE_SUFFIX ".exe")
    elseif (CMAKE_HOST_APPLE)
        set(CHROMALAB_NDK_SHADER_TOOLS_PLATFORM "darwin-x86_64")
        set(CHROMALAB_HOST_EXECUTABLE_SUFFIX "")
    else()
        set(CHROMALAB_NDK_SHADER_TOOLS_PLATFORM "linux-x86_64")
        set(CHROMALAB_HOST_EXECUTABLE_SUFFIX "")
    endif()
    set(CHROMALAB_NDK_GLSLC
        "${CMAKE_ANDROID_NDK}/shader-tools/${CHROMALAB_NDK_SHADER_TOOLS_PLATFORM}/glslc${CHROMALAB_HOST_EXECUTABLE_SUFFIX}")
    if (EXISTS "${CHROMALAB_NDK_GLSLC}")
        file(TO_CMAKE_PATH "${CHROMALAB_NDK_GLSLC}" CHROMALAB_NDK_GLSLC_CMAKE)
        set(Vulkan_GLSLC_EXECUTABLE "${CHROMALAB_NDK_GLSLC_CMAKE}" CACHE FILEPATH "" FORCE)
    endif()
endif()

find_program(CHROMALAB_HOST_C_COMPILER
    NAMES gcc clang cl
    HINTS ${CHROMALAB_HOST_COMPILER_HINTS}
    NO_CMAKE_FIND_ROOT_PATH
    REQUIRED
)
find_program(CHROMALAB_HOST_CXX_COMPILER
    NAMES g++ clang++ cl
    HINTS ${CHROMALAB_HOST_COMPILER_HINTS}
    NO_CMAKE_FIND_ROOT_PATH
    REQUIRED
)
file(TO_CMAKE_PATH "${CHROMALAB_HOST_C_COMPILER}" CHROMALAB_HOST_C_COMPILER_CMAKE)
file(TO_CMAKE_PATH "${CHROMALAB_HOST_CXX_COMPILER}" CHROMALAB_HOST_CXX_COMPILER_CMAKE)
get_filename_component(CHROMALAB_HOST_COMPILER_BIN "${CHROMALAB_HOST_C_COMPILER}" DIRECTORY)
file(TO_CMAKE_PATH "${CHROMALAB_HOST_COMPILER_BIN}" CHROMALAB_HOST_COMPILER_BIN_CMAKE)
set(CHROMALAB_HOST_COMPILER_BIN_FLAG "")
if (CHROMALAB_HOST_C_COMPILER MATCHES "gcc(\\.exe)?$")
    set(CHROMALAB_HOST_COMPILER_BIN_FLAG "-B${CHROMALAB_HOST_COMPILER_BIN_CMAKE}")
endif()

find_program(CHROMALAB_HOST_NINJA
    NAMES ninja ninja.exe
    HINTS "${CHROMALAB_CMAKE_BIN_DIR}"
    NO_CMAKE_FIND_ROOT_PATH
    REQUIRED
)
file(TO_CMAKE_PATH "${CHROMALAB_HOST_NINJA}" CHROMALAB_HOST_NINJA_CMAKE)
file(TO_CMAKE_PATH "${CMAKE_RUNTIME_OUTPUT_DIRECTORY}" CHROMALAB_RUNTIME_OUTPUT_DIR_CMAKE)

set(CHROMALAB_VULKAN_HOST_TOOLCHAIN
    "${CMAKE_BINARY_DIR}/chromalab-vulkan-shaders-host-toolchain.cmake")
configure_file(
    "${CMAKE_SOURCE_DIR}/cmake/vulkan-shaders-host-toolchain.cmake.in"
    "${CHROMALAB_VULKAN_HOST_TOOLCHAIN}"
    @ONLY
)

# Build flags — CPU-only for now, no OpenMP on Android
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN ON CACHE BOOL "" FORCE)
set(GGML_VULKAN_DISABLE_GLSLC_FEATURE_PROBES ON CACHE BOOL "" FORCE)
set(GGML_VULKAN_SHADERS_GEN_TOOLCHAIN "${CHROMALAB_VULKAN_HOST_TOOLCHAIN}" CACHE FILEPATH "" FORCE)
set(GGML_VULKAN_SHADERS_GEN_MAKE_PROGRAM "${CHROMALAB_HOST_NINJA_CMAKE}" CACHE FILEPATH "" FORCE)
set(GGML_OPENCL OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)

# Needed by mtmd's set_target_properties(VERSION ...)
set(LLAMA_INSTALL_VERSION "0.0.9101" CACHE STRING "" FORCE)

# Build llama.cpp core (ggml + llama)
add_subdirectory(${LLAMA_CPP_DIR} llama.cpp EXCLUDE_FROM_ALL)
if (TARGET ggml-vulkan)
    target_compile_options(ggml-vulkan PRIVATE
        -include "${CMAKE_SOURCE_DIR}/cmake/vulkan-features2-shim.h"
    )
    target_include_directories(ggml-vulkan PRIVATE
        "${CMAKE_SOURCE_DIR}/src/main/cpp/third_party/vulkan-hpp"
    )
    if (CMAKE_ANDROID_NDK)
        target_include_directories(ggml-vulkan PRIVATE
            "${CMAKE_ANDROID_NDK}/sources/third_party/shaderc/third_party/spirv-tools/external/spirv-headers/include"
        )
    endif()
endif()

# ---- Build mtmd library directly (skip CLI tools) ----
set(MTMD_DIR "${LLAMA_CPP_DIR}/tools/mtmd")

file(GLOB MTMD_MODEL_SOURCES "${MTMD_DIR}/models/*.cpp")

add_library(mtmd_lib STATIC
    ${MTMD_DIR}/mtmd.cpp
    ${MTMD_DIR}/mtmd-audio.cpp
    ${MTMD_DIR}/mtmd-image.cpp
    ${MTMD_DIR}/mtmd-helper.cpp
    ${MTMD_DIR}/clip.cpp
    ${MTMD_MODEL_SOURCES}
)

target_include_directories(mtmd_lib PUBLIC  ${MTMD_DIR})
target_include_directories(mtmd_lib PRIVATE ${LLAMA_CPP_DIR})
target_include_directories(mtmd_lib PRIVATE ${LLAMA_CPP_DIR}/vendor)
target_link_libraries(mtmd_lib PUBLIC ggml llama)
target_compile_features(mtmd_lib PRIVATE cxx_std_17)

if (NOT MSVC)
    target_compile_options(mtmd_lib PRIVATE -Wno-cast-qual)
endif()
if (ANDROID)
    target_compile_options(mtmd_lib PRIVATE -Wno-missing-prototypes)
endif()

# ---- JNI bridge library ----
add_library(llama_bridge SHARED
    src/main/cpp/llama_bridge.cpp
)

target_include_directories(llama_bridge PRIVATE
    ${LLAMA_CPP_DIR}/include
    ${LLAMA_CPP_DIR}/common
    ${MTMD_DIR}
)

target_link_libraries(llama_bridge
    android
    log
    llama
    ggml
    mtmd_lib
)
