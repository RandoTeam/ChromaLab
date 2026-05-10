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

# ---- llama.cpp source tree ----
set(LLAMA_CPP_DIR "${CMAKE_SOURCE_DIR}/libs/llama.cpp")

# Build flags — CPU-only for now, no OpenMP on Android
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
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
