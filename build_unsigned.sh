#!/data/data/com.termux/files/usr/bin/bash
# Build FlorisBoard APKs without custom signing (release + beta unsigned)
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

cd "$SCRIPT_DIR"

# Clean previous builds
./gradlew clean

# Build unsigned release and beta APKs
./gradlew :app:assembleRelease :app:assembleBeta

echo "Build finished."
echo "Unsigned APKs are in:"
echo "  app/build/outputs/apk/release/"
echo "  app/build/outputs/apk/beta/"
