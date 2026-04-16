#!/usr/bin/env bash

FASTLANE_DIR=$(dirname "$(realpath "$0")")
OBTAINIUM_DIR="$FASTLANE_DIR/obtainium"
README_FILE="$FASTLANE_DIR/../README.md"

echo "obtainium"
obtainium_section="obtainium_links"
obtainium_links="<!-- BEGIN SECTION: $obtainium_section -->\n"
obtainium_links+="<!-- auto-generated link templates, do NOT edit by hand -->\n"
obtainium_links+="<!-- see fastlane/$(basename "$0") -->\n"
for file in "$OBTAINIUM_DIR"/*.json; do
  track_name=$(basename "$file" .json)
  track_urlenc_json=$(jq -c . "$file" | jq -sRr @uri)
  echo "  collect info for track '$track_name'"
  markdown_link="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://app/$track_urlenc_json"
  obtainium_links+="[obtainium_$track_name]: $markdown_link\n"
done
obtainium_links+="<!-- END SECTION: $obtainium_section -->"

echo "update README.md"
if grep -q "<!-- BEGIN SECTION: $obtainium_section -->" "$README_FILE"; then
  echo "  update existing section"
  sed -i "/<!-- BEGIN SECTION: $obtainium_section -->/,/<!-- END SECTION: $obtainium_section -->/c\\$obtainium_links" "$README_FILE"
else
  echo "  add new section"
  echo -e "\n$obtainium_links" >> "$README_FILE"
fi
