cmake_minimum_required(VERSION 3.14) # for add_link_options and implicit target directories.
project("llama-java-core" C CXX)
include(CheckIncludeFileCXX)
include(FetchContent)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED true)

set(BUILD_SHARED_LIBS_DEFAULT OFF)
set(BUILD_SHARED_LIBS OFF)
set(GGML_NATIVE OFF)
set(GGML_BLAS ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY "${PROJECT_BINARY_DIR}")
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "${PROJECT_BINARY_DIR}")
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${PROJECT_BINARY_DIR}")

if (CMAKE_SYSTEM_NAME MATCHES "Windows")
    set(JNI_INCLUDES llamajava/win)
else()
    set(JNI_INCLUDES llamajava/linux)
endif()

if (CMAKE_SYSTEM_NAME MATCHES "Darwin")
    find_library(FOUNDATION_LIBRARY Foundation REQUIRED)
    find_library(METAL_FRAMEWORK Metal REQUIRED)
    find_library(METALKIT_FRAMEWORK MetalKit REQUIRED)

    set(GGML_METAL ON)
    set(GGML_METAL_EMBED_LIBRARY OFF)
    set(LLAMA_ACCELERATE ON)
endif()

if (GGML_BLAS)
    if (CMAKE_SYSTEM_NAME MATCHES "Linux" OR CMAKE_SYSTEM_NAME MATCHES "Windows")
        set(GGML_BLAS_VENDOR "OpenBLAS")
        find_package(BLAS REQUIRED)
    endif()
endif()

if (GGML_CUDA)
    find_package(CUDAToolkit REQUIRED)
endif()

FetchContent_Declare(
	llama.cpp
	GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
	GIT_TAG        b3261
)
FetchContent_MakeAvailable(llama.cpp)

add_library(llamajava SHARED llamajava/llamajava.cpp)

target_include_directories(llamajava PUBLIC ${JNI_INCLUDES})
target_link_libraries(llamajava PRIVATE common llama)
set_target_properties(llamajava
    PROPERTIES
        INTERFACE_COMPILE_FEATURES cxx_std_11
        POSITION_INDEPENDENT_CODE ON)
