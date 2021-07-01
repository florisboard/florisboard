// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1997-2007, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#ifndef __IntlCalendarTest__
#define __IntlCalendarTest__
 
#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/calendar.h"
#include "unicode/smpdtfmt.h"
#include "caltztst.h"

class IntlCalendarTest: public CalendarTimeZoneTest {
public:
    // IntlTest override
    void runIndexedTest( int32_t index, UBool exec, const char* &name, char* par );
public:
    void TestTypes(void);

    void TestGregorian(void);

    void TestBuddhist(void);
    void TestBuddhistFormat(void);
    void TestBug21043Indian(void);
    void TestBug21044Hebrew(void);
    void TestBug21045Islamic(void);
    void TestBug21046IslamicUmalqura(void);

    void TestTaiwan(void);

    void TestJapanese(void);
    void TestJapaneseFormat(void);
    void TestJapanese3860(void);
    void TestForceGannenNumbering(void);
    
    void TestPersian(void);
    void TestPersianFormat(void);

    void TestConsistencyGregorian(void);
    void TestConsistencyCoptic(void);
    void TestConsistencyEthiopic(void);
    void TestConsistencyROC(void);
    void TestConsistencyChinese(void);
    void TestConsistencyDangi(void);
    void TestConsistencyBuddhist(void);
    void TestConsistencyEthiopicAmeteAlem(void);
    void TestConsistencyHebrew(void);
    void TestConsistencyIndian(void);
    void TestConsistencyIslamic(void);
    void TestConsistencyIslamicCivil(void);
    void TestConsistencyIslamicRGSA(void);
    void TestConsistencyIslamicTBLA(void);
    void TestConsistencyIslamicUmalqura(void);
    void TestConsistencyPersian(void);
    void TestConsistencyJapanese(void);

 protected:
    // Test a Gregorian-Like calendar
    void quasiGregorianTest(Calendar& cal, const Locale& gregoLocale, const int32_t *data);
    void simpleTest(const Locale& loc, const UnicodeString& expect, UDate expectDate, UErrorCode& status);
    void checkConsistency(const char* locale);

    int32_t daysToCheckInConsistency;
 
public: // package
    // internal routine for checking date
    static UnicodeString value(Calendar* calendar);
 
};


#endif /* #if !UCONFIG_NO_FORMATTING */
 
#endif // __IntlCalendarTest__
