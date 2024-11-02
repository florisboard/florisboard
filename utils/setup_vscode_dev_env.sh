#!/bin/bash

WORKSPACE_ROOT_DIR="$(realpath "$(dirname "$0")/..")"
VSCODE_DIR="$WORKSPACE_ROOT_DIR/.vscode"
VSCODE_SETTINGS_JSON_PATH="$VSCODE_DIR/settings.json"

if [ "$WORKSPACE_ROOT_DIR" != "$(pwd)" ]; then
    echo "Not executing this script from workspace root dir!"
    exit 1
fi

if [ ! -d "$VSCODE_DIR" ]; then
    mkdir "$VSCODE_DIR"
fi

echo -en "{\n" > "$VSCODE_SETTINGS_JSON_PATH"

# <rust-analyzer>
rust_project_paths="$(find "$WORKSPACE_ROOT_DIR" -type f -name "Cargo.toml")"
echo -en "  \"rust-analyzer.linkedProjects\": [\n" >> "$VSCODE_SETTINGS_JSON_PATH"
for rust_project_path in $rust_project_paths; do
    echo -en "    \"$rust_project_path\",\n" >> "$VSCODE_SETTINGS_JSON_PATH"
done
echo -en "  ],\n" >> "$VSCODE_SETTINGS_JSON_PATH"
# </rust-analyzer>

echo -en "}\n" >> "$VSCODE_SETTINGS_JSON_PATH"
