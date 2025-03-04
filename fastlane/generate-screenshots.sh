#!/bin/zsh

cd staging/images
# Do some image magick to make the screenshots look pretty
mkdir out

FIRST_RATIO=0.25
SECOND_RATIO=0.6

SPLIT_IMAGE_1=settings-ui-keyboard-dark-red.png
SPLIT_IMAGE_2=settings-ui-keyboard-amoled-blue.png
SPLIT_IMAGE_3=settings-ui-keyboard-light-green.png
OUT_FILE=out/settings-ui-out.png


echo "Generating split image 1"
magick $SPLIT_IMAGE_1 \
 \( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$FIRST_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-SECOND_RATIO)))],0" \) \
 -alpha off -compose copyopacity -composite $SPLIT_IMAGE_2 +swap -compose over -composite \
 \( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$SECOND_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-FIRST_RATIO)))],0" \) \
 -alpha off -compose copyopacity -composite $SPLIT_IMAGE_3 +swap -compose over -composite $OUT_FILE
echo "Done ..."
SPLIT_IMAGE_1=keyboard-dark.png
SPLIT_IMAGE_2=keyboard-amoled.png
SPLIT_IMAGE_3=keyboard-light.png
OUT_FILE=out/keyboard-out.png

echo "Generating split image 2"
magick $SPLIT_IMAGE_1 \
 \( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$FIRST_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-SECOND_RATIO)))],0" \) \
 -alpha off -compose copyopacity -composite $SPLIT_IMAGE_2 +swap -compose over -composite \
 \( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$SECOND_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-FIRST_RATIO)))],0" \) \
 -alpha off -compose copyopacity -composite $SPLIT_IMAGE_3 +swap -compose over -composite $OUT_FILE
echo "Done ..."

SPLIT_IMAGE_1=keyboard-bl-dark.png
SPLIT_IMAGE_2=keyboard-bl-amoled.png
SPLIT_IMAGE_3=keyboard-bl-light.png
OUT_FILE=out/keyboard-bl-out.png

echo "Generating split image 3"
magick $SPLIT_IMAGE_1 \
\( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$FIRST_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-SECOND_RATIO)))],0" \) \
-alpha off -compose copyopacity -composite $SPLIT_IMAGE_2 +swap -compose over -composite \
\( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$SECOND_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-FIRST_RATIO)))],0" \) \
-alpha off -compose copyopacity -composite $SPLIT_IMAGE_3 +swap -compose over -composite $OUT_FILE
echo "Done ..."


SPLIT_IMAGE_1=keyboard-emoji-dark.png
SPLIT_IMAGE_2=keyboard-emoji-amoled.png
SPLIT_IMAGE_3=keyboard-emoji-light.png
OUT_FILE=out/keyboard-emoji-out.png

echo "Generating split image 4"
magick $SPLIT_IMAGE_1 \
\( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$FIRST_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-SECOND_RATIO)))],0" \) \
-alpha off -compose copyopacity -composite $SPLIT_IMAGE_2 +swap -compose over -composite \
\( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$SECOND_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-FIRST_RATIO)))],0" \) \
-alpha off -compose copyopacity -composite $SPLIT_IMAGE_3 +swap -compose over -composite $OUT_FILE
echo "Done ..."


SPLIT_IMAGE_1=keyboard-clipboard-blue.png
SPLIT_IMAGE_2=keyboard-clipboard-red.png
SPLIT_IMAGE_3=keyboard-clipboard-green.png
OUT_FILE=out/keyboard-clipboard-out.png

echo "Generating split image 5"
magick $SPLIT_IMAGE_1 \
\( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$FIRST_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-SECOND_RATIO)))],0" \) \
-alpha off -compose copyopacity -composite $SPLIT_IMAGE_2 +swap -compose over -composite \
\( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$SECOND_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-FIRST_RATIO)))],0" \) \
-alpha off -compose copyopacity -composite $SPLIT_IMAGE_3 +swap -compose over -composite $OUT_FILE
echo "Done ..."

SPLIT_IMAGE_1=keyboard-smartbar-blue.png
SPLIT_IMAGE_2=keyboard-smartbar-red.png
SPLIT_IMAGE_3=keyboard-smartbar-green.png
OUT_FILE=out/keyboard-smartbar-out.png

echo "Generating split image 6"
magick $SPLIT_IMAGE_1 \
\( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$FIRST_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-SECOND_RATIO)))],0" \) \
-alpha off -compose copyopacity -composite $SPLIT_IMAGE_2 +swap -compose over -composite \
\( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$SECOND_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-FIRST_RATIO)))],0" \) \
-alpha off -compose copyopacity -composite $SPLIT_IMAGE_3 +swap -compose over -composite $OUT_FILE
echo "Done ..."

SPLIT_IMAGE_1=keyboard-fp-dark.png
SPLIT_IMAGE_2=keyboard-fp-amoled.png
SPLIT_IMAGE_3=keyboard-fp-light.png
OUT_FILE=out/keyboard-fp-out.png

echo "Generating split image 7"
magick $SPLIT_IMAGE_1 \
\( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$FIRST_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-SECOND_RATIO)))],0" \) \
-alpha off -compose copyopacity -composite $SPLIT_IMAGE_2 +swap -compose over -composite \
\( +clone +level-colors white -fill black -draw "polygon %[fx:int(w*$SECOND_RATIO)],%[fx:h] %[fx:w],%[fx:h] %[fx:w],0 %[fx:int(w*$((1-FIRST_RATIO)))],0" \) \
-alpha off -compose copyopacity -composite $SPLIT_IMAGE_3 +swap -compose over -composite $OUT_FILE
echo "Done ..."


echo "Copying icons to out folder"
cp ../../metadata/androidbeta/en-US/images/icon.png out/icon-preview.png
cp ../../metadata/android/en-US/images/icon.png out/icon-stable.png
cd out

PREVIEW_PATH=../../../metadata/androidbeta/en-US/images
STABLE_PATH=../../../metadata/android/en-US/images

SCREENSHOT_PATH_PREVIEW=$PREVIEW_PATH/phoneScreenshots
SCREENSHOT_PATH_STABLE=$STABLE_PATH/phoneScreenshots

# Create showcase images with screenshots and text
SHADOW_OPACITY=50
SHADOW_RADIUS=50
BORDER_RADIUS=50
SCREENSHOT_SCALE=70 # in percent
SHOWCASE_TEXT_FONT="../../fonts/Roboto-Light.ttf"
SHOWCASE_TEXT=(
  "settings-ui-out.png:Highly configurable\nkeyboard"
  "keyboard-bl-out.png:Customize the look\nof your keyboard with themes"
  "keyboard-out.png:Feature rich theme editor"
  "keyboard-emoji-out.png:Emoji support"
  "keyboard-clipboard-out.png:Configurable clipboard history"
  "keyboard-smartbar-out.png:Customizable smartbar"
)

for t in $SHOWCASE_TEXT; do
  echo "Generating $t"
  magick -size 1080x1920 xc:white `# Create background`\
    \( \
      \( \
        \( \
          ${t%:*} `# Src image`\
          \( +clone +level-colors black -fill white -draw "roundrectangle 0,0,%[fx:w],%[fx:h],${BORDER_RADIUS},${BORDER_RADIUS}" \) `# Create an alpha mask for rounded corners`\
          -alpha off -compose copy-opacity -composite `# Apply alpha mask`\
        \) `# Create src image with rounded corners`\
        \( +clone -background black -shadow ${SHADOW_OPACITY}x${SHADOW_RADIUS} \) `# Create drop shadow`\
        +swap -background none -compose src-over -layers merge +repage `# Combine shadow with image`\
      \) \
      -resize 65% `# Scale down`\
    \) \
    -gravity South -geometry +0-20 -composite `# Place the screenshot with shadow over the background`\
    \( -gravity Center -size $((1080*0.8))x$((1920*0.2)) -fill black -font ${SHOWCASE_TEXT_FONT} -pointsize 48 label:"${t#*:}" \) `# Create text`\
    -gravity North -composite `# Place the screenshot with shadow over the background`\
    ${t%:*}
  echo "Done ..."
done

ICON_SHOWCASE_TEXT=(
  "icon-preview.png:FlorisBoard Preview\nGet all (stable, beta and rc) updates"
  "icon-stable.png:FlorisBoard Stable\nGet all stable updates"
)
for t in $ICON_SHOWCASE_TEXT; do
  echo "Generating $t"
  magick -size 1080x1920 xc:white `# Create background`\
    \( \
      \( \
        \( \
          ${t%:*} `# Src image`\
          \( +clone +level-colors black -fill white -draw "roundrectangle 0,0,%[fx:w],%[fx:h],${BORDER_RADIUS},${BORDER_RADIUS}" \) `# Create an alpha mask for rounded corners`\
          -alpha off -compose copy-opacity -composite `# Apply alpha mask`\
        \) `# Create src image with rounded corners`\
        \( +clone -background black -shadow ${SHADOW_OPACITY}x${SHADOW_RADIUS} \) `# Create drop shadow`\
        +swap -background none -compose src-over -layers merge +repage `# Combine shadow with image`\
      \) \
      -resize 100% `# Scale down`\
    \) \
    -gravity Center -geometry +0-300 -composite `# Place the screenshot with shadow over the background`\
     \( \
        \( \
          \( \
              keyboard-fp-out.png `# Src image`\
              \( +clone +level-colors black -fill white -draw "roundrectangle 0,0,%[fx:w],%[fx:h],${BORDER_RADIUS},${BORDER_RADIUS}" \) `# Create an alpha mask for rounded corners`\
              -alpha off -compose copy-opacity -composite `# Apply alpha mask`\
            \) `# Create src image with rounded corners`\
            \( +clone -background black -shadow ${SHADOW_OPACITY}x${SHADOW_RADIUS} \) `# Create drop shadow`\
            +swap -background none -compose src-over -layers merge +repage `# Combine shadow with image`\
          \) \
      -resize 90% `# Scale down`\
     \) \
    -gravity South -composite `# Place the screenshot with shadow over the background`\
    \( -gravity Center -size $((1080*0.8))x$((1920*0.2)) -fill black -font ${SHOWCASE_TEXT_FONT} -pointsize 48 label:"${t#*:}" \) `# Create text`\
    -gravity North -composite `# Place the screenshot with shadow over the background`\
    ${t%:*}
  echo "Done ..."
done

echo "Moving icons to fastlane folder"
mv icon-preview.png $SCREENSHOT_PATH_PREVIEW/1.png
mv icon-stable.png $SCREENSHOT_PATH_STABLE/1.png
echo "Done ..."

magick -size 1024x500 xc:white `# Create background`\
  \( \
    \( \
      \( \
        keyboard-fp-out.png `# Src image`\
          \( +clone +level-colors black -fill white -draw "roundrectangle 0,0,%[fx:w],%[fx:h],${BORDER_RADIUS},${BORDER_RADIUS}" \) `# Create an alpha mask for rounded corners`\
            -alpha off -compose copy-opacity -composite `# Apply alpha mask`\
          \) `# Create src image with rounded corners`\
          \( +clone -background black -shadow ${SHADOW_OPACITY}x${SHADOW_RADIUS} \) `# Create drop shadow`\
          +swap -background none -compose src-over -layers merge +repage `# Combine shadow with image`\
        \) \
    -resize 50% `# Scale down`\
    \) \
    -gravity Center -composite `# Place the screenshot with shadow over the background`\
    keyboard-fp-out.png
  echo "Done ..."

echo "Moving featureGraphics to fastlane folder"
cp keyboard-fp-out.png $PREVIEW_PATH/featureGraphic.png
cp keyboard-fp-out.png $STABLE_PATH/featureGraphic.png
echo "Done ..."

screenshots=(
  "settings-ui-out.png"
  "keyboard-bl-out.png"
  "keyboard-out.png"
  "keyboard-emoji-out.png"
  "keyboard-clipboard-out.png"
  "keyboard-smartbar-out.png"
)


INDEX=2
for i in $screenshots; do
  echo "Copy $i.png to fastlane folder"
  cp $i $SCREENSHOT_PATH_PREVIEW/$INDEX.png
  cp $i $SCREENSHOT_PATH_STABLE/$INDEX.png
  echo "Done ..."
  let INDEX=${INDEX}+1
done

echo "Cleanup..."
cd ..
rm -r out
echo "Done"
