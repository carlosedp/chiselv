#!/bin/bash

# Check latest Firtool version

# FIRTOOL_VERSION=$(curl -sL https://api.github.com/repos/llvm/circt/releases/latest | grep tag_name | cut -d '"' -f 4)
FIRTOOL_VERSION=firtool-1.75.0

OS=$(uname -s)

if [ "$OS" = "Linux" ]; then
  FIRTOOL_URL=https://github.com/llvm/circt/releases/download/$FIRTOOL_VERSION/firrtl-bin-linux-x64.tar.gz
elif [ "$OS" = "Darwin" ]; then
  FIRTOOL_URL=https://github.com/llvm/circt/releases/download/$FIRTOOL_VERSION/firrtl-bin-macos-x64.tar.gz
fi

# Download firtool and create a symbolic link to it
curl -sL "$FIRTOOL_URL" | tar -xz
ln -sf "$FIRTOOL_VERSION"/bin/firtool firtool

echo "Firtool $FIRTOOL_VERSION has been downloaded and linked to current directory."
