#!/bin/bash
# Script to find and setup keytool path
# Usage: ./get-keytool.sh

echo "🔍 Searching for keytool..."

# Common keytool locations
declare -a possible_paths=(
    "$HOME/Library/Application Support/Google/AndroidStudio*/jbr/Contents/Home/bin/keytool"
    "$HOME/android-studio/jbr/bin/keytool"
    "/usr/lib/jvm/java-17-openjdk-amd64/bin/keytool"
    "/usr/lib/jvm/java-11-openjdk-amd64/bin/keytool"
    "$JAVA_HOME/bin/keytool"
    "/usr/bin/keytool"
)

found_path=""

# Check common locations
for path in "${possible_paths[@]}"; do
    # Expand wildcards
    for expanded_path in $path; do
        if [ -f "$expanded_path" ]; then
            found_path="$expanded_path"
            break 2
        fi
    done
done

# If not found, search more broadly
if [ -z "$found_path" ]; then
    echo "Searching in common directories..."

    # macOS
    if [ "$(uname)" = "Darwin" ]; then
        found_path=$(find "$HOME/Library/Application Support" -name "keytool" -type f 2>/dev/null | head -n 1)
        if [ -z "$found_path" ]; then
            found_path=$(find /Applications -name "keytool" -type f 2>/dev/null | head -n 1)
        fi
    # Linux
    else
        found_path=$(find /usr/lib/jvm -name "keytool" -type f 2>/dev/null | head -n 1)
        if [ -z "$found_path" ]; then
            found_path=$(find "$HOME" -name "keytool" -type f 2>/dev/null | grep -E "(android-studio|jdk)" | head -n 1)
        fi
    fi
fi

if [ -n "$found_path" ]; then
    echo "✓ Found keytool at: $found_path"

    # Get the directory path
    bin_path=$(dirname "$found_path")

    # Check if already in PATH
    if [[ ":$PATH:" == *":$bin_path:"* ]]; then
        echo "✓ Path is already in your PATH environment variable"
    else
        echo ""
        echo "📝 This path is NOT in your PATH environment variable."
        echo ""
        echo "To add it, add this line to your shell configuration file:"

        # Detect shell
        shell_config=""
        if [ -n "$BASH_VERSION" ]; then
            shell_config="~/.bashrc"
        elif [ -n "$ZSH_VERSION" ]; then
            shell_config="~/.zshrc"
        else
            shell_config="~/.profile"
        fi

        echo ""
        echo "export PATH=\"$bin_path:\$PATH\""
        echo ""
        echo "Then reload your configuration:"
        echo "source $shell_config"
        echo ""
        echo "Or add temporarily for this session:"
        echo "export PATH=\"$bin_path:\$PATH\""
    fi

    echo ""
    echo "🔧 Test it by running:"
    echo "keytool -version"
    echo ""
    echo "📋 Example commands:"
    echo "# Check debug keystore:"
    echo "keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android"

else
    echo "✗ Could not find keytool"
    echo ""
    echo "Please ensure Java JDK or Android Studio is installed."
    echo "Visit: https://developer.android.com/studio"
    exit 1
fi
