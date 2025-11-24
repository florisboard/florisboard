#!/bin/bash
#
# ZIPRAF_OMEGA Pre-Commit Hook
# =============================
#
# This hook validates changes to the ZIPRAF_OMEGA module before committing.
# It ensures compliance with normative requirements and validates integrity.
#
# Author: Rafael Melo Reis (original specification)
# License: Apache 2.0 (with authorship preservation)
#
# Installation:
#   cp pre-commit-hook.sh .git/hooks/pre-commit
#   chmod +x .git/hooks/pre-commit

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔══════════════════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║           ZIPRAF_OMEGA Pre-Commit Validation                                  ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if ZIPRAF_OMEGA files are being committed
if git diff --cached --name-only | grep -q "lib/zipraf-omega/"; then
    echo -e "${YELLOW}→ ZIPRAF_OMEGA files detected in commit${NC}"
    echo ""
    
    # Check if Python is available
    if ! command -v python3 &> /dev/null; then
        echo -e "${RED}✗ Python 3 not found${NC}"
        echo -e "${YELLOW}  Skipping ZIPRAF_OMEGA validation${NC}"
        exit 0
    fi
    
    # Check if activation script exists
    if [ ! -f "lib/zipraf-omega/ativar.py" ]; then
        echo -e "${YELLOW}⚠ Activation script not found${NC}"
        echo -e "${YELLOW}  Skipping ZIPRAF_OMEGA validation${NC}"
        exit 0
    fi
    
    echo -e "${BLUE}→ Running ZIPRAF_OMEGA activation validation...${NC}"
    echo ""
    
    # Run activation script
    if python3 lib/zipraf-omega/ativar.py > /tmp/zipraf_validation.log 2>&1; then
        echo -e "${GREEN}✓ ZIPRAF_OMEGA validation PASSED${NC}"
        echo ""
        
        # Show summary from validation
        if grep -q "standards compliant" /tmp/zipraf_validation.log; then
            COMPLIANT=$(grep "standards compliant" /tmp/zipraf_validation.log)
            echo -e "${GREEN}  ${COMPLIANT}${NC}"
        fi
        
        if grep -q "Cycle.*completed" /tmp/zipraf_validation.log; then
            CYCLES=$(grep -c "Cycle.*completed" /tmp/zipraf_validation.log)
            echo -e "${GREEN}  ✓ ${CYCLES} loop cycles executed${NC}"
        fi
        
        echo ""
        echo -e "${BLUE}→ Checking authorship preservation...${NC}"
        
        # Check that Rafael Melo Reis authorship is preserved
        CHANGED_FILES=$(git diff --cached --name-only lib/zipraf-omega/)
        AUTHORSHIP_OK=true
        
        for file in $CHANGED_FILES; do
            if [ -f "$file" ]; then
                if ! grep -q "Rafael Melo Reis" "$file" 2>/dev/null; then
                    echo -e "${RED}✗ Warning: Authorship not found in $file${NC}"
                    AUTHORSHIP_OK=false
                fi
            fi
        done
        
        if [ "$AUTHORSHIP_OK" = true ]; then
            echo -e "${GREEN}✓ Authorship preservation verified${NC}"
        else
            echo -e "${YELLOW}⚠ Some files missing authorship attribution${NC}"
            echo -e "${YELLOW}  Please add 'Author: Rafael Melo Reis' to file headers${NC}"
        fi
        
        echo ""
        echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}✅ Pre-commit validation PASSED${NC}"
        echo -e "${GREEN}═══════════════════════════════════════════════════════════════════════════════${NC}"
        
        # Clean up
        rm -f /tmp/zipraf_validation.log
        
        exit 0
    else
        echo -e "${RED}✗ ZIPRAF_OMEGA validation FAILED${NC}"
        echo ""
        echo -e "${RED}Error details:${NC}"
        cat /tmp/zipraf_validation.log
        echo ""
        echo -e "${RED}═══════════════════════════════════════════════════════════════════════════════${NC}"
        echo -e "${RED}❌ Pre-commit validation FAILED${NC}"
        echo -e "${RED}Please fix validation errors before committing${NC}"
        echo -e "${RED}═══════════════════════════════════════════════════════════════════════════════${NC}"
        
        # Clean up
        rm -f /tmp/zipraf_validation.log
        
        exit 1
    fi
else
    echo -e "${BLUE}→ No ZIPRAF_OMEGA files in commit${NC}"
    echo -e "${GREEN}✓ Skipping ZIPRAF_OMEGA validation${NC}"
    exit 0
fi
