git clone https://github.com/ggerganov/llama.cpp.git
if [ ! -d "llama.cpp" ]; then
  echo "Git clone llama.cpp failed."
  exit 1
fi

cp -r llamajava llama.cpp
cd llama.cpp
LATEST_TAG=$(git describe --tags `git rev-list --tags --max-count=1`)
echo "=> Llama.cpp latest tag: $LATEST_TAG"
git checkout $LATEST_TAG
echo "add_subdirectory(llamajava)">>CMakeLists.txt
echo "=> Checkout llama.cpp $LATEST_TAG finished."

mkdir -p build
cd build
cmake .. -DLLAMA_BLAS=ON -DLLAMA_BLAS_VENDOR=OpenBLAS -DBUILD_SHARED_LIBS=ON -DLLAMA_NATIVE=OFF
cmake --build . --config Release
echo "=> Build lib finished."