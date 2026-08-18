#!/usr/bin/env bash

set -e

KEY_NAME="rsa_key"
KEY_SIZE=4096
KEY_DIR="./keys"

echo "Detecting operating system..."

OS="$(uname -s)"

install_openssl() {
    echo "OpenSSL is not installed. Installing..."

    case "$OS" in
        Linux*)
            if command -v apt-get >/dev/null 2>&1; then
                sudo apt-get update
                sudo apt-get install -y openssl

            elif command -v dnf >/dev/null 2>&1; then
                sudo dnf install -y openssl

            elif command -v yum >/dev/null 2>&1; then
                sudo yum install -y openssl

            elif command -v pacman >/dev/null 2>&1; then
                sudo pacman -Sy --noconfirm openssl

            else
                echo "Unsupported Linux package manager."
                echo "Please install OpenSSL manually."
                exit 1
            fi
            ;;

        Darwin*)
            if command -v brew >/dev/null 2>&1; then
                brew install openssl
            else
                echo "Homebrew is not installed."
                echo "Please install Homebrew first."
                exit 1
            fi
            ;;

        MINGW*|MSYS*|CYGWIN*)
            if command -v winget >/dev/null 2>&1; then
                winget install --id ShiningLight.OpenSSL -e
            else
                echo "winget is not available."
                echo "Please install OpenSSL manually."
                exit 1
            fi
            ;;

        *)
            echo "Unsupported operating system: $OS"
            exit 1
            ;;
    esac
}

# Check OpenSSL
if ! command -v openssl >/dev/null 2>&1; then
    install_openssl
fi

# Verify installation
if ! command -v openssl >/dev/null 2>&1; then
    echo "Error: OpenSSL installation failed."
    exit 1
fi

echo "OpenSSL version:"
openssl version

# Create directory
mkdir -p "$KEY_DIR"

PRIVATE_KEY="$KEY_DIR/${KEY_NAME}.pem"
PUBLIC_KEY="$KEY_DIR/${KEY_NAME}.pub"

echo
echo "Generating RSA ${KEY_SIZE}-bit private key..."

openssl genrsa \
    -out "$PRIVATE_KEY" \
    "$KEY_SIZE"

echo "Generating RSA public key..."

openssl rsa \
    -in "$PRIVATE_KEY" \
    -pubout \
    -out "$PUBLIC_KEY"

# Protect private key on Unix systems
if [[ "$OS" != MINGW* && "$OS" != MSYS* && "$OS" != CYGWIN* ]]; then
    chmod 600 "$PRIVATE_KEY"
    chmod 644 "$PUBLIC_KEY"
fi

echo
echo "================================"
echo "RSA keys generated successfully"
echo "================================"
echo
echo "Private key: $PRIVATE_KEY"
echo "Public key:  $PUBLIC_KEY"