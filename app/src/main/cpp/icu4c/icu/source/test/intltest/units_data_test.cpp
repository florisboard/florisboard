// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "units_data.h"
#include "intltest.h"

using namespace ::icu::units;

class UnitsDataTest : public IntlTest {
  public:
    UnitsDataTest() {}

    void runIndexedTest(int32_t index, UBool exec, const char *&name, char *par = NULL);

    void testGetUnitCategory();
    void testGetAllConversionRates();
    void testGetPreferencesFor();
};

extern IntlTest *createUnitsDataTest() { return new UnitsDataTest(); }

void UnitsDataTest::runIndexedTest(int32_t index, UBool exec, const char *&name, char * /*par*/) {
    if (exec) { logln("TestSuite UnitsDataTest: "); }
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(testGetUnitCategory);
    TESTCASE_AUTO(testGetAllConversionRates);
    TESTCASE_AUTO(testGetPreferencesFor);
    TESTCASE_AUTO_END;
}

void UnitsDataTest::testGetUnitCategory() {
    struct TestCase {
        const char *unit;
        const char *expectedCategory;
    } testCases[]{
        {"kilogram-per-cubic-meter", "mass-density"},
        {"cubic-meter-per-meter", "consumption"},
        // TODO(CLDR-13787,hugovdm): currently we're treating
        // consumption-inverse as a separate category. Once consumption
        // preference handling has been clarified by CLDR-13787, this function
        // should be fixed.
        {"meter-per-cubic-meter", "consumption-inverse"},
    };

    IcuTestErrorCode status(*this, "testGetUnitCategory");
    for (const auto &t : testCases) {
        CharString category = getUnitCategory(t.unit, status);
        status.errIfFailureAndReset("getUnitCategory(%s)", t.unit);
        assertEquals("category", t.expectedCategory, category.data());
    }
}

void UnitsDataTest::testGetAllConversionRates() {
    IcuTestErrorCode status(*this, "testGetAllConversionRates");
    MaybeStackVector<ConversionRateInfo> conversionInfo;
    getAllConversionRates(conversionInfo, status);

    // Convenience output for debugging
    for (int i = 0; i < conversionInfo.length(); i++) {
        ConversionRateInfo *cri = conversionInfo[i];
        logln("* conversionInfo %d: source=\"%s\", baseUnit=\"%s\", factor=\"%s\", offset=\"%s\"", i,
              cri->sourceUnit.data(), cri->baseUnit.data(), cri->factor.data(), cri->offset.data());
        assertTrue("sourceUnit", cri->sourceUnit.length() > 0);
        assertTrue("baseUnit", cri->baseUnit.length() > 0);
        assertTrue("factor", cri->factor.length() > 0);
    }
}

class UnitPreferencesOpenedUp : public UnitPreferences {
  public:
    UnitPreferencesOpenedUp(UErrorCode &status) : UnitPreferences(status) {}
    const MaybeStackVector<UnitPreferenceMetadata> *getInternalMetadata() const { return &metadata_; }
    const MaybeStackVector<UnitPreference> *getInternalUnitPrefs() const { return &unitPrefs_; }
};

/**
 * This test is dependent upon CLDR Data: when the preferences change, the test
 * may fail: see the constants for expected Max/Min unit identifiers, for US and
 * World, and for Roads and default lengths.
 */
void UnitsDataTest::testGetPreferencesFor() {
    const char* USRoadMax = "mile";
    const char* USRoadMin = "foot";
    const char* USLenMax = "mile";
    const char* USLenMin = "inch";
    const char* WorldRoadMax = "kilometer";
    const char* WorldRoadMin = "meter";
    const char* WorldLenMax = "kilometer";
    const char* WorldLenMin = "centimeter";
    struct TestCase {
        const char *name;
        const char *category;
        const char *usage;
        const char *region;
        const char *expectedBiggest;
        const char *expectedSmallest;
    } testCases[]{
        {"US road", "length", "road", "US", USRoadMax, USRoadMin},
        {"001 road", "length", "road", "001", WorldRoadMax, WorldRoadMin},
        {"US lengths", "length", "default", "US", USLenMax, USLenMin},
        {"001 lengths", "length", "default", "001", WorldLenMax, WorldLenMin},
        {"XX road falls back to 001", "length", "road", "XX", WorldRoadMax, WorldRoadMin},
        {"XX default falls back to 001", "length", "default", "XX", WorldLenMax, WorldLenMin},
        {"Unknown usage US", "length", "foobar", "US", USLenMax, USLenMin},
        {"Unknown usage 001", "length", "foobar", "XX", WorldLenMax, WorldLenMin},
        {"Fallback", "length", "person-height-xyzzy", "DE", "meter-and-centimeter",
         "meter-and-centimeter"},
        {"Fallback twice", "length", "person-height-xyzzy-foo", "DE", "meter-and-centimeter",
         "meter-and-centimeter"},
        // Confirming results for some unitPreferencesTest.txt test cases
        {"001 area", "area", "default", "001", "square-kilometer", "square-centimeter"},
        {"GB area", "area", "default", "GB", "square-mile", "square-inch"},
        {"001 area geograph", "area", "geograph", "001", "square-kilometer", "square-kilometer"},
        {"GB area geograph", "area", "geograph", "GB", "square-mile", "square-mile"},
        {"CA person-height", "length", "person-height", "CA", "foot-and-inch", "inch"},
        {"AT person-height", "length", "person-height", "AT", "meter-and-centimeter",
         "meter-and-centimeter"},
    };
    IcuTestErrorCode status(*this, "testGetPreferencesFor");
    UnitPreferencesOpenedUp preferences(status);
    auto *metadata = preferences.getInternalMetadata();
    auto *unitPrefs = preferences.getInternalUnitPrefs();
    assertTrue(UnicodeString("Metadata count: ") + metadata->length() + " > 200",
               metadata->length() > 200);
    assertTrue(UnicodeString("Preferences count: ") + unitPrefs->length() + " > 250",
               unitPrefs->length() > 250);

    for (const auto &t : testCases) {
        logln(t.name);
        const UnitPreference *const *prefs;
        int32_t prefsCount;
        preferences.getPreferencesFor(t.category, t.usage, t.region, prefs, prefsCount, status);
        if (status.errIfFailureAndReset("getPreferencesFor(\"%s\", \"%s\", \"%s\", ...", t.category,
                                        t.usage, t.region)) {
            continue;
        }
        if (prefsCount > 0) {
            assertEquals(UnicodeString(t.name) + " - max unit", t.expectedBiggest,
                         prefs[0]->unit.data());
            assertEquals(UnicodeString(t.name) + " - min unit", t.expectedSmallest,
                         prefs[prefsCount - 1]->unit.data());
        } else {
            errln(UnicodeString(t.name) + ": failed to find preferences");
        }
        status.errIfFailureAndReset("testCase '%s'", t.name);
    }
}

#endif /* #if !UCONFIG_NO_FORMATTING */
