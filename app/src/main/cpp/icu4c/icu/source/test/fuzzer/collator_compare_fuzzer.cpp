// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include <cstring>

#include "fuzzer_utils.h"
#include "unicode/coll.h"
#include "unicode/localpointer.h"
#include "unicode/locid.h"

IcuEnvironment* env = new IcuEnvironment();

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  UErrorCode status = U_ZERO_ERROR;

  if (size < 2)
    return 0;

  std::unique_ptr<char16_t> compbuff1(new char16_t[size/4]);
  std::memcpy(compbuff1.get(), data, (size/4)*2);
  data = data + size/2;
  std::unique_ptr<char16_t> compbuff2(new char16_t[size/4]);
  std::memcpy(compbuff2.get(), data, (size/4)*2);

  icu::LocalPointer<icu::Collator> fuzzCollator(
      icu::Collator::createInstance(icu::Locale::getUS(), status), status);
  if (U_FAILURE(status))
    return 0;
  fuzzCollator->setStrength(icu::Collator::TERTIARY);

  fuzzCollator->compare(compbuff1.get(), size/4,
                        compbuff2.get(), size/4);

  return 0;
}
