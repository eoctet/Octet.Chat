set(TARGET llamajava)

add_library(${TARGET}
            SHARED
            llamajava.cpp
            ${PROJECT_SOURCE_DIR}/common/grammar-parser.h
            ${PROJECT_SOURCE_DIR}/common/grammar-parser.cpp
            ${PROJECT_SOURCE_DIR}/llama.cpp
            ${PROJECT_SOURCE_DIR}/llama.h
            ${PROJECT_SOURCE_DIR}/unicode.h
            ${PROJECT_SOURCE_DIR}/unicode.cpp
            ${PROJECT_SOURCE_DIR}/unicode-data.cpp
        )

target_include_directories(${TARGET} PRIVATE llamajava)
target_compile_features(${TARGET} PUBLIC cxx_std_11)
target_link_libraries(${TARGET} PRIVATE ggml ${LLAMA_EXTRA_LIBS})

set_target_properties(${TARGET} PROPERTIES POSITION_INDEPENDENT_CODE ON)
install(TARGETS ${TARGET} LIBRARY)
