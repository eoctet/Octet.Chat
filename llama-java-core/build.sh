#!/bin/bash

mkdir -p build
cd build
cmake ..
cmake --build . --config Release
echo "=> Build lib finished."