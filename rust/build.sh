#!/bin/bash

set -eu

MIN_SDK=$1
NDK=$2
declare -A ARCHS=(
  [arm64]=aarch64-linux-android
  [arm]=armv7-linux-androideabi
  [x86_64]=x86_64-linux-android
  [x86]=i686-linux-android
)

rustup target add ${ARCHS[@]}

# Generate .cargo/config
if [[ ! -r .cargo/config ]]; then
  mkdir -p .cargo

  for target in "${ARCHS[@]}"; do
    cat <<-EOF >>.cargo/config
[target.${target}]
ar = "${NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/${target/armv7/arm}-ar"
linker = "${NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/${target/armv7/armv7a}${MIN_SDK}-clang"
EOF
  done
fi

# Build libraries.
for target in "${ARCHS[@]}"; do
  STRIP="${NDK}/toolchains/llvm/prebuilt/linux-x86_64/bin/${target/armv7/arm}-strip"
  cargo build --target "${target}" --release
  "$STRIP" "target/${target}/release/libandroid_ffi.so"
done
