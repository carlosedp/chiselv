#!/bin/bash

FIRTOOL_VERSION=1.50.0

OS=$(uname -s)

if [ "$OS" = "Linux" ]; then
  FIRTOOL_URL=https://github.com/llvm/circt/releases/download/firtool-$FIRTOOL_VERSION/firrtl-bin-linux-x64.tar.gz
elif [ "$OS" = "Darwin" ]; then
  FIRTOOL_URL=https://github.com/llvm/circt/releases/download/firtool-$FIRTOOL_VERSION/firrtl-bin-macos-x64.tar.gz
fi

# Download firtool and create a symbolic link to it
curl -sL "$FIRTOOL_URL" | tar -xz
ln -sf firtool-$FIRTOOL_VERSION/bin/firtool firtool
