#!/usr/bin/env sh

WORKSPACE_ROOT_DIR="$(realpath "$(dirname "$0")/..")"
VSCODE_SETTINGS_JSON_PATH="$WORKSPACE_ROOT_DIR/.vscode/settings.json"

if [ "$WORKSPACE_ROOT_DIR" != "$(pwd)" ]; then
    echo "Not executing this script from workspace root dir!"
    exit 1
fi

echo -n "{\n" > "$VSCODE_SETTINGS_JSON_PATH"

# <rust-analyzer>
rust_project_paths="$(find "$WORKSPACE_ROOT_DIR" -type f -name "Cargo.toml")"
echo -n "  \"rust-analyzer.linkedProjects\": [\n" >> "$VSCODE_SETTINGS_JSON_PATH"
for rust_project_path in $rust_project_paths; do
    echo -n "    \"$rust_project_path\",\n" >> "$VSCODE_SETTINGS_JSON_PATH"
done
echo -n "  ],\n" >> "$VSCODE_SETTINGS_JSON_PATH"
# </rust-analyzer>

echo -n "}\n" >> "$VSCODE_SETTINGS_JSON_PATH"
