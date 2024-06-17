# build with:
# podman manifest create REPO/crossbuild-riscv64
# podman build  --platform linux/x86_64,linux/arm64 -t REPO/crossbuild-riscv64 -f riscv64-crossbuild.dockerfile .
# podman push REPO/crossbuild-riscv64
# or
# docker buildx build --platform linux/x86_64,linux/arm64 -t REPO/crossbuild-riscv64 -f riscv64-crossbuild.dockerfile .
FROM ubuntu

ENV DEBIAN_FRONTEND noninteractive

RUN apt-get update && \
  apt-get install -y --no-install-recommends \
  build-essential \
  autoconf \
  curl \
  wget \
  git \
  python3 \
  python3-pip \
  bzip2 \
  crossbuild-essential-riscv64 \
  macutils \
  bsdextrautils \
  ca-certificates && \
  rm -rf /var/lib/apt/lists/*

WORKDIR /build

RUN riscv64-linux-gnu-gcc --version

CMD ["/bin/bash"]
