#!/bin/bash

set -eu

NDK=$1
declare -A ARCHS=(
  [arm64]=aarch64-linux-android
  [arm]=armv7-linux-androideabi
  [x86_64]=x86_64-linux-android
  [x86]=i686-linux-android
)

rustup target add ${ARCHS[@]}

## Make standalone toolchains.
#for arch in "${!ARCHS[@]}"; do
#  target="${ARCHS[$arch]/armv7/arm}"
#  if [[ ! -d "${NDK}/toolchains/${target}/bin" ]]; then
#    "${NDK}/build/tools/make_standalone_toolchain.py" \
#      --arch "$arch" \
#      --install-dir "${NDK}/toolchains/${target}"
#  fi
#done

# Generate .cargo/config
if [[ ! -r .cargo/config ]]; then
  mkdir -p .cargo

  for target in "${ARCHS[@]}"; do
    cat <<-EOF >>.cargo/config
[target.${target}]
ar = "${NDK}/toolchains/${target/armv7/arm}/bin/${target/armv7/arm}-ar"
linker = "${NDK}/toolchains/${target/armv7/arm}/bin/${target/armv7/arm}-clang"
EOF
  done
fi

# Build libraries.
for target in "${ARCHS[@]}"; do
  export AR="${NDK}/toolchains/${target/armv7/arm}/bin/${target/armv7/arm}-ar"
  export CC="${NDK}/toolchains/${target/armv7/arm}/bin/${target/armv7/arm}-gcc"
  cargo build --target "${target}" --release
done
