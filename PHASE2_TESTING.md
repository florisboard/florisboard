# Phase 2 Testing Guide

## Quick Start

### Step 1: Build and Install
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Enable Language Detection Features

1. Open FlorisBoard Settings
2. Navigate to **Advanced** → **Language Detection** (new section)
3. Configure preferences:
   - ☑ **Enable Language Detection** (enabled by default)
   - ☐ **Auto-Switch Layout** (disabled by default - enable for testing)
   - ☐ **Show Visual Indicator** (disabled by default - enable to see detection)
   - **Detection Sensitivity**: 30% (adjust as needed)

## Test Scenarios

### Test 1: Visual Indicator
**Goal**: Verify language detection is working

1. Enable "Show Visual Indicator" in settings
2. Open any text field
3. Type Telugu: "నమస్కారం"
   - **Expected**: Label shows "Mode: TELUGU"
4. Type English: "hello world"
   - **Expected**: Label shows "Mode: ENGLISH"
5. Type Teluglish: "naku kavali"
   - **Expected**: Label shows "Mode: TELUGLISH"

### Test 2: Auto-Switch Layout
**Goal**: Verify automatic layout switching

**Prerequisites**:
- Telugu subtype configured
- English subtype configured

**Steps**:
1. Enable "Auto-Switch Layout" in settings
2. Start with English layout active
3. Type Teluglish: "naku"
   - **Expected**: Keyboard stays on English/Teluglish layout
4. Type Telugu: "న"
   - **Expected**: Keyboard switches to Telugu layout
5. Type English: "hello"
   - **Expected**: Keyboard switches to English layout

### Test 3: Sensitivity Adjustment
**Goal**: Test different detection thresholds

1. Set sensitivity to **10%** (very aggressive)
   - Type: "naku"
   - **Expected**: Detected as TELUGLISH (low threshold)

2. Set sensitivity to **90%** (very conservative)
   - Type: "naku"
   - **Expected**: Might be detected as ENGLISH (high threshold)
   - Type: "naku kavali cheppu"
   - **Expected**: Detected as TELUGLISH (more matches)

3. Set sensitivity to **30%** (default)
   - Type: "naku kavali"
   - **Expected**: Detected as TELUGLISH (balanced)

### Test 4: Disable Detection
**Goal**: Verify detection can be turned off

1. Disable "Enable Language Detection"
2. Type any text
   - **Expected**: No language detection occurs
   - **Expected**: Visual indicator shows "Mode: UNKNOWN" (if enabled)
   - **Expected**: No auto-switching happens

## Teluglish Test Words

Use these words to test Teluglish detection:

### High Confidence (should always detect)
- "naku kavali" (2/2 matches = 100%)
- "nenu chesanu" (2/2 matches = 100%)
- "meeru unnaru" (2/2 matches = 100%)

### Medium Confidence (depends on threshold)
- "naku hello" (1/2 matches = 50%)
- "kavali please" (1/2 matches = 50%)

### Low Confidence (may not detect at default 30%)
- "hello naku" (1/2 matches = 50%, but "hello" is English)
- "test naku test" (1/3 matches = 33%)

## Telugu Unicode Test

Type these Telugu words to test Unicode detection:

- నమస్కారం (namaskaram - hello)
- ధన్యవాదాలు (dhanyavadalu - thank you)
- మీరు ఎలా ఉన్నారు (meeru ela unnaru - how are you)

## Performance Testing

### Cache Efficiency
1. Type the same word multiple times
2. Check that detection is fast (cached results)

### Rapid Language Switching
1. Type: "hello నమస్కారం naku world"
2. Verify detection updates correctly for each word

## Debugging Tips

### Enable Developer Tools
1. Settings → Advanced → Developer Tools
2. Enable "Show Input State Overlay"
3. This shows additional debugging information

### Check Logcat
```bash
adb logcat | grep -i "language"
```

Look for language detection logs (if added in future)

## Common Issues

### Issue: Visual indicator not showing
**Solution**: Make sure "Show Visual Indicator" is enabled in Language Detection settings

### Issue: Auto-switch not working
**Solutions**:
1. Verify "Auto-Switch Layout" is enabled
2. Check that Telugu and English subtypes are configured
3. Ensure subtypes have correct locale codes (te, en)

### Issue: Teluglish not detected
**Solutions**:
1. Lower the detection sensitivity (try 10-20%)
2. Use more Teluglish words in the sentence
3. Check that words are in the Teluglish dictionary (see LanguageDetector.kt)

### Issue: Too many false positives
**Solutions**:
1. Increase detection sensitivity (try 40-50%)
2. This makes detection more conservative

## Expected Behavior Summary

| Input Type | Detection Result | Auto-Switch Target |
|------------|------------------|-------------------|
| Telugu Unicode (న, మ, etc.) | TELUGU | Telugu layout |
| Teluglish words (naku, kavali) | TELUGLISH | English layout |
| English words (hello, world) | ENGLISH | English layout |
| Empty/whitespace | UNKNOWN | No switch |
| Mixed (hello నమస్కారం) | Depends on current word | Switches per word |

## Advanced Testing

### Test Custom Sensitivity Values
Try these sensitivity values and document behavior:
- 0% (detect everything as Teluglish)
- 10% (very aggressive)
- 30% (default, balanced)
- 50% (conservative)
- 90% (very conservative)
- 100% (only perfect matches)

### Test Edge Cases
- Single character: "a", "న"
- Numbers: "123", "456"
- Punctuation: "...", "!!!"
- Mixed: "hello123", "naku!!!"
- Very long words: "supercalifragilisticexpialidocious"

## Reporting Issues

When reporting issues, include:
1. Detection sensitivity setting
2. Auto-switch enabled/disabled
3. Input text
4. Expected vs actual detection result
5. Logcat output (if available)

## Next Steps

After testing Phase 2, consider:
1. **Phase 3**: Language-specific word suggestions
2. **Phase 4**: Learning user's Teluglish vocabulary
3. **Phase 5**: Context-aware detection (sentence-level)
