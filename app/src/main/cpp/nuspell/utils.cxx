/* Copyright 2016-2021 Dimitrij Mijoski
 *
 * This file is part of Nuspell.
 *
 * Nuspell is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Nuspell is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Nuspell.  If not, see <http://www.gnu.org/licenses/>.
 */

#include "utils.hxx"
#include "unicode.hxx"

#include <algorithm>
#include <limits>

#include <unicode/uchar.h>
#include <unicode/ucnv.h>
#include <unicode/unistr.h>
#include <unicode/ustring.h>

#if ' ' != 32 || '.' != 46 || 'A' != 65 || 'Z' != 90 || 'a' != 97 || 'z' != 122
#error "Basic execution character set is not ASCII"
#endif

using namespace std;

namespace nuspell {
inline namespace v5 {

template <class SepT>
static auto& split_on_any_of_low(std::string_view s, const SepT& sep,
                                 std::vector<std::string>& out)
{
	size_t i1 = 0;
	size_t i2;
	do {
		i2 = s.find_first_of(sep, i1);
		out.emplace_back(s.substr(i1, i2 - i1));
		i1 = i2 + 1; // we can only add +1 if separator is single char.

		// i2 gets s.npos after the last separator.
		// Length of i2-i1 will always go past the end. That is defined.
	} while (i2 != s.npos);
	return out;
}

/**
 * @internal
 * @brief Splits string on single char seperator.
 *
 * Consecutive separators are treated as separate and will emit empty strings.
 *
 * @param s string to split.
 * @param sep char that acts as separator to split on.
 * @param out vector where separated strings are appended.
 * @return @p out.
 */
auto split(std::string_view s, char sep, std::vector<std::string>& out)
    -> std::vector<std::string>&
{
	return split_on_any_of_low(s, sep, out);
}

/**
 * @internal
 * @brief Splits string on set of single char seperators.
 *
 * Consecutive separators are treated as separate and will emit empty strings.
 *
 * @param s string to split.
 * @param sep seperator(s) to split on.
 * @param out vector where separated strings are appended.
 * @return @p out.
 */
auto split_on_any_of(std::string_view s, const char* sep,
                     std::vector<std::string>& out) -> std::vector<std::string>&
{
	return split_on_any_of_low(s, sep, out);
}

auto utf32_to_utf8(std::u32string_view in, std::string& out) -> void
{
	out.clear();
	for (size_t i = 0; i != size(in); ++i) {
		auto cp = in[i];
		auto enc_cp = U8_Encoded_CP(cp);
		out += enc_cp;
	}
}
auto utf32_to_utf8(std::u32string_view in) -> std::string
{
	auto out = string();
	utf32_to_utf8(in, out);
	return out;
}

auto valid_utf8_to_32(std::string_view in, std::u32string& out) -> void
{
	out.clear();
	for (size_t i = 0; i != size(in);) {
		char32_t cp;
		valid_u8_advance_cp(in, i, cp);
		out.push_back(cp);
	}
}
auto valid_utf8_to_32(std::string_view in) -> std::u32string
{
	auto out = u32string();
	valid_utf8_to_32(in, out);
	return out;
}

auto utf8_to_16(std::string_view in) -> std::u16string
{
	auto out = u16string();
	utf8_to_16(in, out);
	return out;
}

bool utf8_to_16(std::string_view in, std::u16string& out)
{
	int32_t len;
	auto err = U_ZERO_ERROR;
	u_strFromUTF8(data(out), size(out), &len, data(in), size(in), &err);
	out.resize(len);
	if (err == U_BUFFER_OVERFLOW_ERROR) {
		err = U_ZERO_ERROR;
		u_strFromUTF8(data(out), size(out), &len, data(in), size(in),
		              &err);
	}
	if (U_SUCCESS(err))
		return true;
	out.clear();
	return false;
}

bool validate_utf8(string_view s)
{
	auto err = U_ZERO_ERROR;
	u_strFromUTF8(nullptr, 0, nullptr, data(s), size(s), &err);
	if (err == U_INVALID_CHAR_FOUND)
		return false;
	return err == U_BUFFER_OVERFLOW_ERROR || U_SUCCESS(err);
}

auto static is_ascii(char c) -> bool
{
	return static_cast<unsigned char>(c) <= 127;
}

auto is_all_ascii(std::string_view s) -> bool
{
	return all_of(begin(s), end(s), is_ascii);
}

auto static widen_latin1(char c) -> char16_t
{
	return static_cast<unsigned char>(c);
}

auto latin1_to_ucs2(std::string_view s) -> std::u16string
{
	u16string ret;
	latin1_to_ucs2(s, ret);
	return ret;
}
auto latin1_to_ucs2(std::string_view s, std::u16string& out) -> void
{
	out.resize(s.size());
	transform(begin(s), end(s), begin(out), widen_latin1);
}

auto static is_surrogate_pair(char16_t c) -> bool
{
	return 0xD800 <= c && c <= 0xDFFF;
}
auto is_all_bmp(std::u16string_view s) -> bool
{
	return none_of(begin(s), end(s), is_surrogate_pair);
}

auto to_upper_ascii(std::string& s) -> void
{
	auto& char_type = use_facet<ctype<char>>(locale::classic());
	char_type.toupper(begin_ptr(s), end_ptr(s));
}

auto static utf32_to_icu(u32string_view in) -> icu::UnicodeString
{
	static_assert(sizeof(UChar32) == sizeof(char32_t));
	return icu::UnicodeString::fromUTF32(
	    reinterpret_cast<const UChar32*>(in.data()), in.size());
}
auto static icu_to_utf32(const icu::UnicodeString& in, std::u32string& out)
    -> bool
{
	out.resize(in.length());
	auto err = U_ZERO_ERROR;
	auto len =
	    in.toUTF32(reinterpret_cast<UChar32*>(out.data()), out.size(), err);
	if (U_SUCCESS(err)) {
		out.erase(len);
		return true;
	}
	out.clear();
	return false;
}

auto to_upper(std::string_view in, const icu::Locale& loc) -> std::string
{
	auto out = std::string();
	to_upper(in, loc, out);
	return out;
}
auto to_title(std::string_view in, const icu::Locale& loc) -> std::string
{
	auto out = std::string();
	to_title(in, loc, out);
	return out;
}
auto to_lower(std::string_view in, const icu::Locale& loc) -> std::string
{
	auto out = std::string();
	to_lower(in, loc, out);
	return out;
}

auto to_upper(string_view in, const icu::Locale& loc, string& out) -> void
{
	auto sp = icu::StringPiece(data(in), size(in));
	auto us = icu::UnicodeString::fromUTF8(sp);
	us.toUpper(loc);
	out.clear();
	us.toUTF8String(out);
}
auto to_title(string_view in, const icu::Locale& loc, string& out) -> void
{
	auto sp = icu::StringPiece(data(in), size(in));
	auto us = icu::UnicodeString::fromUTF8(sp);
	us.toTitle(nullptr, loc);
	out.clear();
	us.toUTF8String(out);
}
auto to_lower(u32string_view in, const icu::Locale& loc, u32string& out) -> void
{
	auto us = utf32_to_icu(in);
	us.toLower(loc);
	icu_to_utf32(us, out);
}
auto to_lower(string_view in, const icu::Locale& loc, string& out) -> void
{
	auto sp = icu::StringPiece(data(in), size(in));
	auto us = icu::UnicodeString::fromUTF8(sp);
	us.toLower(loc);
	out.clear();
	us.toUTF8String(out);
}

auto to_lower_char_at(std::string& s, size_t i, const icu::Locale& loc) -> void
{
	auto cp = valid_u8_next_cp(s, i);
	auto us = icu::UnicodeString(UChar32(cp.cp));
	us.toLower(loc);
	auto u8_low = string();
	us.toUTF8String(u8_low);
	s.replace(i, cp.end_i - i, u8_low);
}
auto to_title_char_at(std::string& s, size_t i, const icu::Locale& loc) -> void
{
	auto cp = valid_u8_next_cp(s, i);
	auto us = icu::UnicodeString(UChar32(cp.cp));
	us.toTitle(nullptr, loc);
	auto u8_title = string();
	us.toUTF8String(u8_title);
	s.replace(i, cp.end_i - i, u8_title);
}

/**
 * @internal
 * @brief Determines casing (capitalization) type for a word.
 *
 * Casing is sometimes referred to as capitalization.
 *
 * @param s word.
 * @return The casing type.
 */
auto classify_casing(string_view s) -> Casing
{
	size_t upper = 0;
	size_t lower = 0;
	for (size_t i = 0; i != size(s);) {
		char32_t c;
		valid_u8_advance_cp(s, i, c);
		if (u_isupper(c))
			upper++;
		else if (u_islower(c))
			lower++;
		// else neutral
	}
	if (upper == 0)               // all lowercase, maybe with some neutral
		return Casing::SMALL; // most common case

	auto first_cp = valid_u8_next_cp(s, 0);
	auto first_capital = u_isupper(first_cp.cp);
	if (first_capital && upper == 1)
		return Casing::INIT_CAPITAL; // second most common

	if (lower == 0)
		return Casing::ALL_CAPITAL;

	if (first_capital)
		return Casing::PASCAL;
	else
		return Casing::CAMEL;
}

/**
 * @internal
 * @brief Check if word[i] or word[i-1] are uppercase
 *
 * Check if the two chars are alphabetic and at least one of them is in
 * uppercase.
 *
 * @return true if at least one is uppercase, false otherwise.
 */
auto has_uppercase_at_compound_word_boundary(string_view word, size_t i) -> bool
{
	auto cp = valid_u8_next_cp(word, i);
	auto cp_prev = valid_u8_prev_cp(word, i);
	if (u_isupper(cp.cp)) {
		if (u_isalpha(cp_prev.cp))
			return true;
	}
	else if (u_isupper(cp_prev.cp) && u_isalpha(cp.cp))
		return true;
	return false;
}

Encoding_Converter::Encoding_Converter(const char* enc)
{
	auto err = UErrorCode();
	cnv = ucnv_open(enc, &err);
}

Encoding_Converter::~Encoding_Converter()
{
	if (cnv)
		ucnv_close(cnv);
}

Encoding_Converter::Encoding_Converter(const Encoding_Converter& other)
{
	auto err = UErrorCode();
	cnv = ucnv_safeClone(other.cnv, nullptr, nullptr, &err);
}

auto Encoding_Converter::operator=(const Encoding_Converter& other)
    -> Encoding_Converter&
{
	this->~Encoding_Converter();
	auto err = UErrorCode();
	cnv = ucnv_safeClone(other.cnv, nullptr, nullptr, &err);
	return *this;
}

auto Encoding_Converter::to_utf8(string_view in, string& out) -> bool
{
	if (ucnv_getType(cnv) == UCNV_UTF8) {
		if (validate_utf8(in)) {
			out = in;
			return true;
		}
		else {
			out.clear();
			return false;
		}
	}
	auto err = U_ZERO_ERROR;
	auto len = ucnv_toAlgorithmic(UCNV_UTF8, cnv, out.data(), out.size(),
	                              in.data(), in.size(), &err);
	out.resize(len);
	if (err == U_BUFFER_OVERFLOW_ERROR) {
		err = U_ZERO_ERROR;
		ucnv_toAlgorithmic(UCNV_UTF8, cnv, out.data(), out.size(),
		                   in.data(), in.size(), &err);
	}
	return U_SUCCESS(err);
}

auto replace_ascii_char(string& s, char from, char to) -> void
{
	for (auto i = s.find(from); i != s.npos; i = s.find(from, i + 1)) {
		s[i] = to;
	}
}

auto erase_chars(string& s, string_view erase_chars) -> void
{
	if (erase_chars.empty())
		return;
	for (size_t i = 0, next_i = 0; i != size(s); i = next_i) {
		valid_u8_advance_index(s, next_i);
		auto enc_cp = string_view(&s[i], next_i - i);
		if (erase_chars.find(enc_cp) != erase_chars.npos) {
			s.erase(i, next_i - i);
			next_i = i;
		}
	}
	return;
}

/**
 * @internal
 * @brief Tests if word is a number.
 *
 * Allow numbers with dot ".", dash "-" or comma "," inbetween the digits, but
 * forbids double separators such as "..", "--" and ".,".
 */
auto is_number(string_view s) -> bool
{
	if (s.empty())
		return false;

	auto it = begin(s);
	if (s[0] == '-')
		++it;
	while (it != end(s)) {
		auto next = std::find_if(
		    it, end(s), [](auto c) { return c < '0' || c > '9'; });
		if (next == it)
			return false;
		if (next == end(s))
			return true;
		it = next;
		auto c = *it;
		if (c == '.' || c == ',' || c == '-')
			++it;
		else
			return false;
	}
	return false;
}

auto count_appereances_of(string_view haystack, string_view needles) -> size_t
{
	auto ret = size_t(0);
	for (size_t i = 0, next_i = 0; i != size(haystack); i = next_i) {
		valid_u8_advance_index(haystack, next_i);
		auto enc_cp = string_view(&haystack[i], next_i - i);
		ret += needles.find(enc_cp) != needles.npos;
	}
	return ret;
}

} // namespace v5
} // namespace nuspell
