#!/bin/bash
#
BUILD_DIR=$(
  cd "$(dirname "$0")"
  pwd
)
LLAMA_CPP_DIR="$BUILD_DIR/llama.cpp"
LLAMA_LIBS_DIR="$BUILD_DIR/build"

#Clear old llama.cpp dir
rm -rf "$LLAMA_CPP_DIR"

#git clone new version
git clone -b $1 --single-branch https://github.com/ggerganov/llama.cpp.git
if [ ! -d "$LLAMA_CPP_DIR" ]; then
  echo "Git clone failed..."
  exit 1
fi

#Clear old libs
rm -rf "$LLAMA_LIBS_DIR"
mkdir -p "$LLAMA_LIBS_DIR"

#
cp "$BUILD_DIR/llamajava/jni.h" "$LLAMA_CPP_DIR"
cp "$BUILD_DIR/llamajava/jni_md.h" "$LLAMA_CPP_DIR"
cp "$BUILD_DIR/llamajava/llamajava.h" "$LLAMA_CPP_DIR"
cp "$BUILD_DIR/llamajava/llamajava.cpp" "$LLAMA_CPP_DIR"
if [ ! -f "$LLAMA_CPP_DIR/Makefile_backup" ]; then
    cp "$LLAMA_CPP_DIR/Makefile" "$LLAMA_CPP_DIR/Makefile_backup"
fi

BUILD_LLAMA_JAVA_CMD="llamajava.o: llamajava.cpp llamajava.h jni.h jni_md.h \n\t\$(CXX) \$(CXXFLAGS) -c $< -o \$@\n"

OS_VER=$(uname -s)

if [ "$OS_VER" == "Darwin" ]; then
  #Build MacOS dylib
  BUILD_LLAMA_JAVA_DYLIB_CMD="$BUILD_LLAMA_JAVA_CMD\nlibllama.dylib: llama.o ggml.o llamajava.o \$(OBJS) \n\t\$(CXX) \$(CXXFLAGS) -shared -fPIC -o \$@ $^ \$(LDFLAGS)\n"
  BUILD_LLAMA_CMD="$BUILD_LLAMA_JAVA_DYLIB_CMD\nclean:"
  #echo $BUILD_LLAMA_CMD
  sed -i "" -e "s/clean:/$BUILD_LLAMA_CMD/g" "$LLAMA_CPP_DIR/Makefile"

  cd "$LLAMA_CPP_DIR"
  make clean
  make libllama.dylib
  if [ -f "libllama.dylib" ]; then
    mv libllama.dylib "$LLAMA_LIBS_DIR"
#    rm -rf "$LLAMA_CPP_DIR"
  fi
elif [ "$OS_VER" == "Linux" ]; then
  #Build linux so
  LIB_LLAMA_CMD="libllama.so: llama.o ggml.o \$(OBJS)"
  BUILD_LLAMA_JAVA_SO_CMD="$BUILD_LLAMA_JAVA_CMD\nlibllama.so: llama.o ggml.o llamajava.o \$(OBJS)"
  #echo $BUILD_LLAMA_JAVA_SO_CMD
  sed -i "s/$LIB_LLAMA_CMD/$BUILD_LLAMA_JAVA_SO_CMD/g" "$LLAMA_CPP_DIR/Makefile"

  cd "$LLAMA_CPP_DIR"
  make clean
  make libllama.so
  if [ -f "libllama.so" ]; then
    mv libllama.so "$LLAMA_LIBS_DIR"
#    rm -rf "$LLAMA_CPP_DIR"
  fi
fi
