#!/bin/sh

echo "Installing git hooks..."

HOOK_DIR=".git/hooks"
HOOK_FILE="$HOOK_DIR/pre-commit"

mkdir -p "$HOOK_DIR"

cat > "$HOOK_FILE" << 'EOF'
#!/bin/sh

echo "Running pre-commit checks..."

# format check
./gradlew spotlessCheck

if [ $? -ne 0 ]; then
  echo "❌ Formatting failed. Run: ./gradlew spotlessApply"
  exit 1
fi

# compile
./gradlew compileJava

if [ $? -ne 0 ]; then
  echo "❌ Compilation failed"
  exit 1
fi

# unit test
./gradlew test

if [ $? -ne 0 ]; then
  echo "❌ Tests failed"
  exit 1
fi

echo "✅ Pre-commit checks passed"
EOF

chmod +x "$HOOK_FILE"

echo "Git hooks installed!"
