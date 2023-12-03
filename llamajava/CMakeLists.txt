set(TARGET llamajava)

add_library(${TARGET}
            SHARED
            llamajava.cpp
            llamajava.h
            jni.h
            jni_md.h
            ${PROJECT_SOURCE_DIR}/common/grammar-parser.h
            ${PROJECT_SOURCE_DIR}/common/grammar-parser.cpp
            ${PROJECT_SOURCE_DIR}/llama.cpp
            ${PROJECT_SOURCE_DIR}/llama.h
            )

target_include_directories(${TARGET} PUBLIC . ${PROJECT_SOURCE_DIR})
target_compile_features(${TARGET} PUBLIC cxx_std_11)
target_link_libraries(${TARGET} PRIVATE ggml ${LLAMA_EXTRA_LIBS})

if (BUILD_SHARED_LIBS)
    set_target_properties(${TARGET} PROPERTIES POSITION_INDEPENDENT_CODE ON)
    if (LLAMA_METAL)
        set_target_properties(${TARGET} PROPERTIES RESOURCE "${CMAKE_CURRENT_SOURCE_DIR}/ggml-metal.metal")
    endif()
    install(TARGETS ${TARGET} LIBRARY)
endif()
