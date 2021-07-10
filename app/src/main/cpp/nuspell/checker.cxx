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

#include "checker.hxx"
#include "utils.hxx"
#include <cassert>

using namespace std;

namespace nuspell {
inline namespace v5 {

template <class L>
class At_Scope_Exit {
	L& lambda;

      public:
	At_Scope_Exit(L& action) : lambda(action) {}
	~At_Scope_Exit() { lambda(); }
};

#define CONCAT_IMPL(x, y) x##y
#define MACRO_CONCAT(x, y) CONCAT_IMPL(x, y)

#define ASE_INTERNAL1(lname, aname, ...)                                       \
	auto lname = [&]() { __VA_ARGS__; };                                   \
	At_Scope_Exit<decltype(lname)> aname(lname)

#define ASE_INTERNAL2(ctr, ...)                                                \
	ASE_INTERNAL1(MACRO_CONCAT(Auto_func_, ctr),                           \
	              MACRO_CONCAT(Auto_instance_, ctr), __VA_ARGS__)

#define AT_SCOPE_EXIT(...) ASE_INTERNAL2(__COUNTER__, __VA_ARGS__)

auto Checker::spell_priv(string& s) const -> bool
{
	// do input conversion (iconv)
	input_substr_replacer.replace(s);

	// triming whitespace should be part of tokenization, not here

	if (s.empty())
		return true;
	bool abbreviation = s.back() == '.';
	if (abbreviation) {
		// trim trailing periods
		auto i = s.find_last_not_of('.');
		// if i == npos, i + 1 == 0, so no need for extra if.
		s.erase(i + 1);
		if (s.empty()) {
			// TODO add back removed periods
			return true;
		}
	}

	// accept number
	if (is_number(s))
		return true;

	erase_chars(s, ignored_chars);

	// handle break patterns
	auto copy = s;
	auto ret = spell_break(s);
	assert(s == copy);
	if (!ret && abbreviation) {
		s += '.';
		ret = spell_break(s);
	}
	return ret;
}

auto Checker::spell_break(std::string& s, size_t depth) const -> bool
{
	// check spelling accoring to case
	auto res = spell_casing(s);
	if (res) {
		// handle forbidden words
		if (res->contains(forbiddenword_flag)) {
			return false;
		}
		if (forbid_warn && res->contains(warn_flag)) {
			return false;
		}
		return true;
	}
	if (depth == 9)
		return false;

	// handle break pattern at start of a word
	for (auto& pat : break_table.start_word_breaks()) {
		if (begins_with(s, pat)) {
			auto substr = s.substr(pat.size());
			auto res = spell_break(substr, depth + 1);
			if (res)
				return res;
		}
	}

	// handle break pattern at end of a word
	for (auto& pat : break_table.end_word_breaks()) {
		if (ends_with(s, pat)) {
			auto substr = s.substr(0, s.size() - pat.size());
			auto res = spell_break(substr, depth + 1);
			if (res)
				return res;
		}
	}

	// handle break pattern in middle of a word
	for (auto& pat : break_table.middle_word_breaks()) {
		auto i = s.find(pat);
		if (i > 0 && i < s.size() - pat.size()) {
			auto part1 = s.substr(0, i);
			auto part2 = s.substr(i + pat.size());
			auto res1 = spell_break(part1, depth + 1);
			if (!res1)
				continue;
			auto res2 = spell_break(part2, depth + 1);
			if (res2)
				return res2;
		}
	}

	return false;
}

auto Checker::spell_casing(std::string& s) const -> const Flag_Set*
{
	auto casing_type = classify_casing(s);
	const Flag_Set* res = nullptr;

	switch (casing_type) {
	case Casing::SMALL:
	case Casing::CAMEL:
	case Casing::PASCAL:
		res = check_word(s);
		break;
	case Casing::ALL_CAPITAL:
		res = spell_casing_upper(s);
		break;
	case Casing::INIT_CAPITAL:
		res = spell_casing_title(s);
		break;
	}
	return res;
}

auto Checker::spell_casing_upper(std::string& s) const -> const Flag_Set*
{
	auto& loc = icu_locale;

	auto res = check_word(s, ALLOW_BAD_FORCEUCASE);
	if (res)
		return res;

	// handle prefixes separated by apostrophe for Catalan, French and
	// Italian, e.g. SANT'ELIA -> Sant'+Elia
	auto apos = s.find('\'');
	if (apos != s.npos && apos != s.size() - 1) {
		// apostophe is at beginning of word or dividing the word
		auto part1 = s.substr(0, apos + 1);
		auto part2 = s.substr(apos + 1);
		to_lower(part1, loc, part1);
		to_title(part2, loc, part2);
		auto t = part1 + part2;
		res = check_word(t, ALLOW_BAD_FORCEUCASE);
		if (res)
			return res;
		to_title(part1, loc, part1);
		t = part1 + part2;
		res = check_word(t, ALLOW_BAD_FORCEUCASE);
		if (res)
			return res;
	}
	auto s2 = string();

	// handle sharp s for German
	if (checksharps && s.find("SS") != s.npos) {
		to_lower(s, loc, s2);
		res = spell_sharps(s2);
		if (res)
			return res;

		to_title(s, loc, s2);
		res = spell_sharps(s2);
		if (res)
			return res;
	}
	to_title(s, loc, s2);
	res = check_word(s2, ALLOW_BAD_FORCEUCASE);
	if (res && !res->contains(keepcase_flag))
		return res;

	to_lower(s, loc, s2);
	res = check_word(s2, ALLOW_BAD_FORCEUCASE);
	if (res && !res->contains(keepcase_flag))
		return res;
	return nullptr;
}

auto Checker::spell_casing_title(std::string& s) const -> const Flag_Set*
{
	auto& loc = icu_locale;

	// check title case
	auto res = check_word(s, ALLOW_BAD_FORCEUCASE, SKIP_HIDDEN_HOMONYM);
	if (res)
		return res;

	auto s2 = string();
	to_lower(s, loc, s2);
	res = check_word(s2, ALLOW_BAD_FORCEUCASE);

	// with CHECKSHARPS, ß is allowed too in KEEPCASE words with title case
	if (res && res->contains(keepcase_flag) &&
	    !(checksharps && (s2.find("ß") != s.npos))) {
		res = nullptr;
	}
	return res;
}

/**
 * @internal
 * @brief Checks german word with double SS
 *
 * Checks recursively spelling starting on a word in title or lower case which
 * originate from a word in upper case containing the letters 'SS'. The
 * technique used is use recursion for checking all variations with repetitions
 * of minimal one replacement of 'ss' with sharp s 'ß'. Maximum recursion depth
 * is limited with a hardcoded value.
 *
 * @param base string to check spelling for where zero or more occurences of
 * 'ss' have been replaced by sharp s 'ß'.
 * @param pos position in the string to start next find and replacement.
 * @param n counter for the recursion depth.
 * @param rep counter for the number of replacements done.
 * @return The flags of the corresponding dictionary word.
 */
auto Checker::spell_sharps(std::string& base, size_t pos, size_t n,
                           size_t rep) const -> const Flag_Set*
{
	const size_t MAX_SHARPS = 5;
	pos = base.find("ss", pos);
	if (pos != base.npos && n < MAX_SHARPS) {
		base.replace(pos, 2, "ß");
		auto res = spell_sharps(base, pos + 1, n + 1, rep + 1);
		base.replace(pos, 2, "ss");
		if (res)
			return res;
		res = spell_sharps(base, pos + 2, n + 1, rep);
		if (res)
			return res;
	}
	else if (rep > 0) {
		return check_word(base, ALLOW_BAD_FORCEUCASE);
	}
	return nullptr;
}

auto Checker::check_word(std::string& s, Forceucase allow_bad_forceucase,
                         Hidden_Homonym skip_hidden_homonym) const
    -> const Flag_Set*
{

	auto ret1 = check_simple_word(s, skip_hidden_homonym);
	if (ret1)
		return ret1;
	auto ret2 = check_compound(s, allow_bad_forceucase);
	if (ret2)
		return &ret2->second;

	return nullptr;
}

auto Checker::check_simple_word(std::string& s,
                                Hidden_Homonym skip_hidden_homonym) const
    -> const Flag_Set*
{
	for (auto& we : Subrange(words.equal_range(s))) {
		auto& word_flags = we.second;
		if (word_flags.contains(need_affix_flag))
			continue;
		if (word_flags.contains(compound_onlyin_flag))
			continue;
		if (skip_hidden_homonym &&
		    word_flags.contains(HIDDEN_HOMONYM_FLAG))
			continue;
		return &word_flags;
	}
	{
		auto ret3 = strip_suffix_only(s, skip_hidden_homonym);
		if (ret3)
			return &ret3->second;
	}
	{
		auto ret2 = strip_prefix_only(s, skip_hidden_homonym);
		if (ret2)
			return &ret2->second;
	}
	{
		auto ret4 = strip_prefix_then_suffix_commutative(
		    s, skip_hidden_homonym);
		if (ret4)
			return &ret4->second;
	}
	if (!complex_prefixes) {
		auto ret6 = strip_suffix_then_suffix(s, skip_hidden_homonym);
		if (ret6)
			return &ret6->second;

		auto ret7 =
		    strip_prefix_then_2_suffixes(s, skip_hidden_homonym);
		if (ret7)
			return &ret7->second;

		auto ret8 = strip_suffix_prefix_suffix(s, skip_hidden_homonym);
		if (ret8)
			return &ret8->second;

		// this is slow and unused so comment
		// auto ret9 = strip_2_suffixes_then_prefix(s,
		// skip_hidden_homonym); if (ret9)
		//	return &ret9->second;
	}
	else {
		auto ret6 = strip_prefix_then_prefix(s, skip_hidden_homonym);
		if (ret6)
			return &ret6->second;
		auto ret7 =
		    strip_suffix_then_2_prefixes(s, skip_hidden_homonym);
		if (ret7)
			return &ret7->second;

		auto ret8 = strip_prefix_suffix_prefix(s, skip_hidden_homonym);
		if (ret8)
			return &ret8->second;

		// this is slow and unused so comment
		// auto ret9 = strip_2_prefixes_then_suffix(s,
		// skip_hidden_homonym); if (ret9)
		//	return &ret9->second;
	}
	return nullptr;
}

template <class AffixT>
class To_Root_Unroot_RAII {
      private:
	string& word;
	const AffixT& affix;

      public:
	To_Root_Unroot_RAII(string& w, const AffixT& a) : word(w), affix(a)
	{
		affix.to_root(word);
	}
	~To_Root_Unroot_RAII() { affix.to_derived(word); }
};

template <Affixing_Mode m>
auto Checker::is_valid_inside_compound(const Flag_Set& flags) const
{
	if (m == AT_COMPOUND_BEGIN && !flags.contains(compound_flag) &&
	    !flags.contains(compound_begin_flag))
		return false;
	if (m == AT_COMPOUND_MIDDLE && !flags.contains(compound_flag) &&
	    !flags.contains(compound_middle_flag))
		return false;
	if (m == AT_COMPOUND_END && !flags.contains(compound_flag) &&
	    !flags.contains(compound_last_flag))
		return false;
	return true;
}

/**
 * @internal
 * @brief strip_prefix_only
 * @param s derived word with affixes
 * @return if found, root word + prefix
 */
template <Affixing_Mode m>
auto Checker::strip_prefix_only(std::string& word,
                                Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<Prefix>
{
	auto& dic = words;

	for (auto it = prefixes.iterate_prefixes_of(word); it; ++it) {
		auto& e = *it;
		if (outer_affix_NOT_valid<m>(e))
			continue;
		if (is_circumfix(e))
			continue;
		To_Root_Unroot_RAII<Prefix> xxx(word, e);
		if (!e.check_condition(word))
			continue;
		for (auto& word_entry : Subrange(dic.equal_range(word))) {
			auto& word_flags = word_entry.second;
			if (!cross_valid_inner_outer(word_flags, e))
				continue;
			// badflag check
			if (m == FULL_WORD &&
			    word_flags.contains(compound_onlyin_flag))
				continue;
			if (skip_hidden_homonym &&
			    word_flags.contains(HIDDEN_HOMONYM_FLAG))
				continue;
			// needflag check
			if (!is_valid_inside_compound<m>(word_flags) &&
			    !is_valid_inside_compound<m>(e.cont_flags))
				continue;
			return {word_entry, e};
		}
	}
	return {};
}

/**
 * @internal
 * @brief strip_suffix_only
 * @param s derived word with affixes
 * @return if found, root word + suffix
 */
template <Affixing_Mode m>
auto Checker::strip_suffix_only(std::string& word,
                                Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<Suffix>
{
	auto& dic = words;
	for (auto it = suffixes.iterate_suffixes_of(word); it; ++it) {
		auto& e = *it;
		if (outer_affix_NOT_valid<m>(e))
			continue;
		if (e.appending.size() != 0 && m == AT_COMPOUND_END &&
		    e.cont_flags.contains(compound_onlyin_flag))
			continue;
		if (is_circumfix(e))
			continue;
		To_Root_Unroot_RAII<Suffix> xxx(word, e);
		if (!e.check_condition(word))
			continue;
		for (auto& word_entry : Subrange(dic.equal_range(word))) {
			auto& word_flags = word_entry.second;
			if (!cross_valid_inner_outer(word_flags, e))
				continue;
			// badflag check
			if (m == FULL_WORD &&
			    word_flags.contains(compound_onlyin_flag))
				continue;
			if (skip_hidden_homonym &&
			    word_flags.contains(HIDDEN_HOMONYM_FLAG))
				continue;
			// needflag check
			if (!is_valid_inside_compound<m>(word_flags) &&
			    !is_valid_inside_compound<m>(e.cont_flags))
				continue;
			return {word_entry, e};
		}
	}
	return {};
}

/**
 * @internal
 * @brief strip_prefix_then_suffix
 *
 * This accepts a derived word that was formed first by adding
 * suffix then prefix to the root. The stripping is in reverse.
 *
 * @param s derived word with affixes
 * @return if found, root word + suffix + prefix
 */
template <Affixing_Mode m>
auto Checker::strip_prefix_then_suffix(std::string& word,
                                       Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<Suffix, Prefix>
{
	for (auto it = prefixes.iterate_prefixes_of(word); it; ++it) {
		auto& pe = *it;
		if (pe.cross_product == false)
			continue;
		if (outer_affix_NOT_valid<m>(pe))
			continue;
		To_Root_Unroot_RAII<Prefix> xxx(word, pe);
		if (!pe.check_condition(word))
			continue;
		auto ret =
		    strip_pfx_then_sfx_2<m>(pe, word, skip_hidden_homonym);
		if (ret)
			return ret;
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_pfx_then_sfx_2(const Prefix& pe, std::string& word,
                                   Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<Suffix, Prefix>
{
	auto& dic = words;

	for (auto it = suffixes.iterate_suffixes_of(word); it; ++it) {
		auto& se = *it;
		if (se.cross_product == false)
			continue;
		if (affix_NOT_valid<m>(se))
			continue;
		if (is_circumfix(pe) != is_circumfix(se))
			continue;
		To_Root_Unroot_RAII<Suffix> xxx(word, se);
		if (!se.check_condition(word))
			continue;
		for (auto& word_entry : Subrange(dic.equal_range(word))) {
			auto& word_flags = word_entry.second;
			if (!cross_valid_inner_outer(se, pe) &&
			    !cross_valid_inner_outer(word_flags, pe))
				continue;
			if (!cross_valid_inner_outer(word_flags, se))
				continue;
			// badflag check
			if (m == FULL_WORD &&
			    word_flags.contains(compound_onlyin_flag))
				continue;
			if (skip_hidden_homonym &&
			    word_flags.contains(HIDDEN_HOMONYM_FLAG))
				continue;
			// needflag check
			if (!is_valid_inside_compound<m>(word_flags) &&
			    !is_valid_inside_compound<m>(se.cont_flags) &&
			    !is_valid_inside_compound<m>(pe.cont_flags))
				continue;
			return {word_entry, se, pe};
		}
	}

	return {};
}

/**
 * @internal
 * @brief strip_suffix_then_prefix
 *
 * This accepts a derived word that was formed first by adding
 * prefix then suffix to the root. The stripping is in reverse.
 *
 * @param s derived word with prefix and suffix
 * @return if found, root word + prefix + suffix
 */
template <Affixing_Mode m>
auto Checker::strip_suffix_then_prefix(std::string& word,
                                       Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<Prefix, Suffix>
{
	for (auto it = suffixes.iterate_suffixes_of(word); it; ++it) {
		auto& se = *it;
		if (se.cross_product == false)
			continue;
		if (outer_affix_NOT_valid<m>(se))
			continue;
		To_Root_Unroot_RAII<Suffix> xxx(word, se);
		if (!se.check_condition(word))
			continue;
		auto ret =
		    strip_sfx_then_pfx_2<m>(se, word, skip_hidden_homonym);
		if (ret)
			return ret;
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_sfx_then_pfx_2(const Suffix& se, std::string& word,
                                   Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<Prefix, Suffix>
{
	auto& dic = words;

	for (auto it = prefixes.iterate_prefixes_of(word); it; ++it) {
		auto& pe = *it;
		if (pe.cross_product == false)
			continue;
		if (affix_NOT_valid<m>(pe))
			continue;
		if (is_circumfix(pe) != is_circumfix(se))
			continue;
		To_Root_Unroot_RAII<Prefix> xxx(word, pe);
		if (!pe.check_condition(word))
			continue;
		for (auto& word_entry : Subrange(dic.equal_range(word))) {
			auto& word_flags = word_entry.second;
			if (!cross_valid_inner_outer(pe, se) &&
			    !cross_valid_inner_outer(word_flags, se))
				continue;
			if (!cross_valid_inner_outer(word_flags, pe))
				continue;
			// badflag check
			if (m == FULL_WORD &&
			    word_flags.contains(compound_onlyin_flag))
				continue;
			if (skip_hidden_homonym &&
			    word_flags.contains(HIDDEN_HOMONYM_FLAG))
				continue;
			// needflag check
			if (!is_valid_inside_compound<m>(word_flags) &&
			    !is_valid_inside_compound<m>(se.cont_flags) &&
			    !is_valid_inside_compound<m>(pe.cont_flags))
				continue;
			return {word_entry, pe, se};
		}
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_prefix_then_suffix_commutative(
    std::string& word, Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<Suffix, Prefix>
{
	for (auto it = prefixes.iterate_prefixes_of(word); it; ++it) {
		auto& pe = *it;
		if (pe.cross_product == false)
			continue;
		if (affix_NOT_valid<m>(pe))
			continue;
		To_Root_Unroot_RAII<Prefix> xxx(word, pe);
		if (!pe.check_condition(word))
			continue;
		auto ret =
		    strip_pfx_then_sfx_comm_2<m>(pe, word, skip_hidden_homonym);
		if (ret)
			return ret;
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_pfx_then_sfx_comm_2(
    const Prefix& pe, std::string& word,
    Hidden_Homonym skip_hidden_homonym) const -> Affixing_Result<Suffix, Prefix>
{
	auto& dic = words;
	auto has_needaffix_pe = pe.cont_flags.contains(need_affix_flag);
	auto is_circumfix_pe = is_circumfix(pe);

	for (auto it = suffixes.iterate_suffixes_of(word); it; ++it) {
		auto& se = *it;
		if (se.cross_product == false)
			continue;
		if (affix_NOT_valid<m>(se))
			continue;
		auto has_needaffix_se = se.cont_flags.contains(need_affix_flag);
		if (has_needaffix_pe && has_needaffix_se)
			continue;
		if (is_circumfix_pe != is_circumfix(se))
			continue;
		To_Root_Unroot_RAII<Suffix> xxx(word, se);
		if (!se.check_condition(word))
			continue;
		for (auto& word_entry : Subrange(dic.equal_range(word))) {
			auto& word_flags = word_entry.second;

			auto valid_cross_pe_outer =
			    !has_needaffix_pe &&
			    cross_valid_inner_outer(word_flags, se) &&
			    (cross_valid_inner_outer(se, pe) ||
			     cross_valid_inner_outer(word_flags, pe));

			auto valid_cross_se_outer =
			    !has_needaffix_se &&
			    cross_valid_inner_outer(word_flags, pe) &&
			    (cross_valid_inner_outer(pe, se) ||
			     cross_valid_inner_outer(word_flags, se));

			if (!valid_cross_pe_outer && !valid_cross_se_outer)
				continue;

			// badflag check
			if (m == FULL_WORD &&
			    word_flags.contains(compound_onlyin_flag))
				continue;
			if (skip_hidden_homonym &&
			    word_flags.contains(HIDDEN_HOMONYM_FLAG))
				continue;
			// needflag check
			if (!is_valid_inside_compound<m>(word_flags) &&
			    !is_valid_inside_compound<m>(se.cont_flags) &&
			    !is_valid_inside_compound<m>(pe.cont_flags))
				continue;
			return {word_entry, se, pe};
		}
	}

	return {};
}

template <Affixing_Mode m>
auto Checker::strip_suffix_then_suffix(std::string& word,
                                       Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<Suffix, Suffix>
{
	// The following check is purely for performance, it does not change
	// correctness.
	if (!suffixes.has_continuation_flags())
		return {};

	for (auto it = suffixes.iterate_suffixes_of(word); it; ++it) {
		auto& se1 = *it;

		// The following check is purely for performance, it does not
		// change correctness.
		if (!suffixes.has_continuation_flag(se1.flag))
			continue;
		if (outer_affix_NOT_valid<m>(se1))
			continue;
		if (is_circumfix(se1))
			continue;
		To_Root_Unroot_RAII<Suffix> xxx(word, se1);
		if (!se1.check_condition(word))
			continue;
		auto ret = strip_sfx_then_sfx_2<FULL_WORD>(se1, word,
		                                           skip_hidden_homonym);
		if (ret)
			return ret;
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_sfx_then_sfx_2(const Suffix& se1, std::string& word,
                                   Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<Suffix, Suffix>
{

	auto& dic = words;

	for (auto it = suffixes.iterate_suffixes_of(word); it; ++it) {
		auto& se2 = *it;
		if (!cross_valid_inner_outer(se2, se1))
			continue;
		if (affix_NOT_valid<m>(se2))
			continue;
		if (is_circumfix(se2))
			continue;
		To_Root_Unroot_RAII<Suffix> xxx(word, se2);
		if (!se2.check_condition(word))
			continue;
		for (auto& word_entry : Subrange(dic.equal_range(word))) {
			auto& word_flags = word_entry.second;
			if (!cross_valid_inner_outer(word_flags, se2))
				continue;
			// badflag check
			if (m == FULL_WORD &&
			    word_flags.contains(compound_onlyin_flag))
				continue;
			if (skip_hidden_homonym &&
			    word_flags.contains(HIDDEN_HOMONYM_FLAG))
				continue;
			// needflag check here if needed
			return {word_entry, se2, se1};
		}
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_prefix_then_prefix(std::string& word,
                                       Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<Prefix, Prefix>
{
	// The following check is purely for performance, it does not change
	// correctness.
	if (!prefixes.has_continuation_flags())
		return {};

	for (auto it = prefixes.iterate_prefixes_of(word); it; ++it) {
		auto& pe1 = *it;
		// The following check is purely for performance, it does not
		// change correctness.
		if (!prefixes.has_continuation_flag(pe1.flag))
			continue;
		if (outer_affix_NOT_valid<m>(pe1))
			continue;
		if (is_circumfix(pe1))
			continue;
		To_Root_Unroot_RAII<Prefix> xxx(word, pe1);
		if (!pe1.check_condition(word))
			continue;
		auto ret = strip_pfx_then_pfx_2<FULL_WORD>(pe1, word,
		                                           skip_hidden_homonym);
		if (ret)
			return ret;
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_pfx_then_pfx_2(const Prefix& pe1, std::string& word,
                                   Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<Prefix, Prefix>
{
	auto& dic = words;

	for (auto it = prefixes.iterate_prefixes_of(word); it; ++it) {
		auto& pe2 = *it;
		if (!cross_valid_inner_outer(pe2, pe1))
			continue;
		if (affix_NOT_valid<m>(pe2))
			continue;
		if (is_circumfix(pe2))
			continue;
		To_Root_Unroot_RAII<Prefix> xxx(word, pe2);
		if (!pe2.check_condition(word))
			continue;
		for (auto& word_entry : Subrange(dic.equal_range(word))) {
			auto& word_flags = word_entry.second;
			if (!cross_valid_inner_outer(word_flags, pe2))
				continue;
			// badflag check
			if (m == FULL_WORD &&
			    word_flags.contains(compound_onlyin_flag))
				continue;
			if (skip_hidden_homonym &&
			    word_flags.contains(HIDDEN_HOMONYM_FLAG))
				continue;
			// needflag check here if needed
			return {word_entry, pe2, pe1};
		}
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_prefix_then_2_suffixes(
    std::string& word, Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<>
{
	// The following check is purely for performance, it does not change
	// correctness.
	if (!suffixes.has_continuation_flags())
		return {};

	for (auto i1 = prefixes.iterate_prefixes_of(word); i1; ++i1) {
		auto& pe1 = *i1;
		if (pe1.cross_product == false)
			continue;
		if (outer_affix_NOT_valid<m>(pe1))
			continue;
		To_Root_Unroot_RAII<Prefix> xxx(word, pe1);
		if (!pe1.check_condition(word))
			continue;
		for (auto i2 = suffixes.iterate_suffixes_of(word); i2; ++i2) {
			auto& se1 = *i2;

			// The following check is purely for performance, it
			// does not change correctness.
			if (!suffixes.has_continuation_flag(se1.flag))
				continue;

			if (se1.cross_product == false)
				continue;
			if (affix_NOT_valid<m>(se1))
				continue;
			if (is_circumfix(pe1) != is_circumfix(se1))
				continue;
			To_Root_Unroot_RAII<Suffix> yyy(word, se1);
			if (!se1.check_condition(word))
				continue;
			auto ret = strip_pfx_2_sfx_3<FULL_WORD>(
			    pe1, se1, word, skip_hidden_homonym);
			if (ret)
				return ret;
		}
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_pfx_2_sfx_3(const Prefix& pe1, const Suffix& se1,
                                std::string& word,
                                Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<>
{
	auto& dic = words;

	for (auto it = suffixes.iterate_suffixes_of(word); it; ++it) {
		auto& se2 = *it;
		if (!cross_valid_inner_outer(se2, se1))
			continue;
		if (affix_NOT_valid<m>(se2))
			continue;
		if (is_circumfix(se2))
			continue;
		To_Root_Unroot_RAII<Suffix> xxx(word, se2);
		if (!se2.check_condition(word))
			continue;
		for (auto& word_entry : Subrange(dic.equal_range(word))) {
			auto& word_flags = word_entry.second;
			if (!cross_valid_inner_outer(se1, pe1) &&
			    !cross_valid_inner_outer(word_flags, pe1))
				continue;
			if (!cross_valid_inner_outer(word_flags, se2))
				continue;
			// badflag check
			if (m == FULL_WORD &&
			    word_flags.contains(compound_onlyin_flag))
				continue;
			if (skip_hidden_homonym &&
			    word_flags.contains(HIDDEN_HOMONYM_FLAG))
				continue;
			// needflag check here if needed
			return {word_entry};
		}
	}

	return {};
}

template <Affixing_Mode m>
auto Checker::strip_suffix_prefix_suffix(
    std::string& word, Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<>
{
	// The following check is purely for performance, it does not change
	// correctness.
	if (!suffixes.has_continuation_flags() &&
	    !prefixes.has_continuation_flags())
		return {};

	for (auto i1 = suffixes.iterate_suffixes_of(word); i1; ++i1) {
		auto& se1 = *i1;

		// The following check is purely for performance, it
		// does not change correctness.
		if (!suffixes.has_continuation_flag(se1.flag) &&
		    !prefixes.has_continuation_flag(se1.flag))
			continue;

		if (se1.cross_product == false)
			continue;
		if (outer_affix_NOT_valid<m>(se1))
			continue;
		To_Root_Unroot_RAII<Suffix> xxx(word, se1);
		if (!se1.check_condition(word))
			continue;
		for (auto i2 = prefixes.iterate_prefixes_of(word); i2; ++i2) {
			auto& pe1 = *i2;
			if (pe1.cross_product == false)
				continue;
			if (affix_NOT_valid<m>(pe1))
				continue;
			To_Root_Unroot_RAII<Prefix> yyy(word, pe1);
			if (!pe1.check_condition(word))
				continue;
			auto ret = strip_s_p_s_3<FULL_WORD>(
			    se1, pe1, word, skip_hidden_homonym);
			if (ret)
				return ret;
		}
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_s_p_s_3(const Suffix& se1, const Prefix& pe1,
                            std::string& word,
                            Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<>
{
	auto& dic = words;

	for (auto it = suffixes.iterate_suffixes_of(word); it; ++it) {
		auto& se2 = *it;
		if (se2.cross_product == false)
			continue;
		if (!cross_valid_inner_outer(se2, se1) &&
		    !cross_valid_inner_outer(pe1, se1))
			continue;
		if (affix_NOT_valid<m>(se2))
			continue;
		auto circ1ok = (is_circumfix(pe1) == is_circumfix(se1)) &&
		               !is_circumfix(se2);
		auto circ2ok = (is_circumfix(pe1) == is_circumfix(se2)) &&
		               !is_circumfix(se1);
		if (!circ1ok && !circ2ok)
			continue;
		To_Root_Unroot_RAII<Suffix> xxx(word, se2);
		if (!se2.check_condition(word))
			continue;
		for (auto& word_entry : Subrange(dic.equal_range(word))) {
			auto& word_flags = word_entry.second;
			if (!cross_valid_inner_outer(se2, pe1) &&
			    !cross_valid_inner_outer(word_flags, pe1))
				continue;
			if (!cross_valid_inner_outer(word_flags, se2))
				continue;
			// badflag check
			if (m == FULL_WORD &&
			    word_flags.contains(compound_onlyin_flag))
				continue;
			if (skip_hidden_homonym &&
			    word_flags.contains(HIDDEN_HOMONYM_FLAG))
				continue;
			// needflag check here if needed
			return {word_entry};
		}
	}

	return {};
}

template <Affixing_Mode m>
auto Checker::strip_2_suffixes_then_prefix(
    std::string& word, Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<>
{
	// The following check is purely for performance, it does not change
	// correctness.
	if (!suffixes.has_continuation_flags() &&
	    !prefixes.has_continuation_flags())
		return {};

	for (auto i1 = suffixes.iterate_suffixes_of(word); i1; ++i1) {
		auto& se1 = *i1;

		// The following check is purely for performance, it
		// does not change correctness.
		if (!suffixes.has_continuation_flag(se1.flag) &&
		    !prefixes.has_continuation_flag(se1.flag))
			continue;

		if (outer_affix_NOT_valid<m>(se1))
			continue;
		if (is_circumfix(se1))
			continue;
		To_Root_Unroot_RAII<Suffix> xxx(word, se1);
		if (!se1.check_condition(word))
			continue;
		for (auto i2 = suffixes.iterate_suffixes_of(word); i2; ++i2) {
			auto& se2 = *i2;
			if (se2.cross_product == false)
				continue;
			if (affix_NOT_valid<m>(se2))
				continue;
			To_Root_Unroot_RAII<Suffix> yyy(word, se2);
			if (!se2.check_condition(word))
				continue;
			auto ret = strip_2_sfx_pfx_3<FULL_WORD>(
			    se1, se2, word, skip_hidden_homonym);
			if (ret)
				return ret;
		}
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_2_sfx_pfx_3(const Suffix& se1, const Suffix& se2,
                                std::string& word,
                                Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<>
{
	auto& dic = words;

	for (auto it = prefixes.iterate_prefixes_of(word); it; ++it) {
		auto& pe1 = *it;
		if (pe1.cross_product == false)
			continue;
		if (!cross_valid_inner_outer(se2, se1) &&
		    !cross_valid_inner_outer(pe1, se1))
			continue;
		if (affix_NOT_valid<m>(pe1))
			continue;
		if (is_circumfix(se2) != is_circumfix(pe1))
			continue;
		To_Root_Unroot_RAII<Prefix> xxx(word, pe1);
		if (!pe1.check_condition(word))
			continue;
		for (auto& word_entry : Subrange(dic.equal_range(word))) {
			auto& word_flags = word_entry.second;
			if (!cross_valid_inner_outer(pe1, se2) &&
			    !cross_valid_inner_outer(word_flags, se2))
				continue;
			if (!cross_valid_inner_outer(word_flags, pe1))
				continue;
			// badflag check
			if (m == FULL_WORD &&
			    word_flags.contains(compound_onlyin_flag))
				continue;
			if (skip_hidden_homonym &&
			    word_flags.contains(HIDDEN_HOMONYM_FLAG))
				continue;
			// needflag check here if needed
			return {word_entry};
		}
	}

	return {};
}

template <Affixing_Mode m>
auto Checker::strip_suffix_then_2_prefixes(
    std::string& word, Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<>
{
	// The following check is purely for performance, it does not change
	// correctness.
	if (!prefixes.has_continuation_flags())
		return {};

	for (auto i1 = suffixes.iterate_suffixes_of(word); i1; ++i1) {
		auto& se1 = *i1;
		if (se1.cross_product == false)
			continue;
		if (outer_affix_NOT_valid<m>(se1))
			continue;
		To_Root_Unroot_RAII<Suffix> xxx(word, se1);
		if (!se1.check_condition(word))
			continue;
		for (auto i2 = prefixes.iterate_prefixes_of(word); i2; ++i2) {
			auto& pe1 = *i2;

			// The following check is purely for performance, it
			// does not change correctness.
			if (!prefixes.has_continuation_flag(pe1.flag))
				continue;

			if (pe1.cross_product == false)
				continue;
			if (affix_NOT_valid<m>(pe1))
				continue;
			if (is_circumfix(se1) != is_circumfix(pe1))
				continue;
			To_Root_Unroot_RAII<Prefix> yyy(word, pe1);
			if (!pe1.check_condition(word))
				continue;
			auto ret = strip_sfx_2_pfx_3<FULL_WORD>(
			    se1, pe1, word, skip_hidden_homonym);
			if (ret)
				return ret;
		}
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_sfx_2_pfx_3(const Suffix& se1, const Prefix& pe1,
                                std::string& word,
                                Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<>
{
	auto& dic = words;

	for (auto it = prefixes.iterate_prefixes_of(word); it; ++it) {
		auto& pe2 = *it;
		if (!cross_valid_inner_outer(pe2, pe1))
			continue;
		if (affix_NOT_valid<m>(pe2))
			continue;
		if (is_circumfix(pe2))
			continue;
		To_Root_Unroot_RAII<Prefix> xxx(word, pe2);
		if (!pe2.check_condition(word))
			continue;
		for (auto& word_entry : Subrange(dic.equal_range(word))) {
			auto& word_flags = word_entry.second;
			if (!cross_valid_inner_outer(pe1, se1) &&
			    !cross_valid_inner_outer(word_flags, se1))
				continue;
			if (!cross_valid_inner_outer(word_flags, pe2))
				continue;
			// badflag check
			if (m == FULL_WORD &&
			    word_flags.contains(compound_onlyin_flag))
				continue;
			if (skip_hidden_homonym &&
			    word_flags.contains(HIDDEN_HOMONYM_FLAG))
				continue;
			return {word_entry};
		}
	}

	return {};
}

template <Affixing_Mode m>
auto Checker::strip_prefix_suffix_prefix(
    std::string& word, Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<>
{
	// The following check is purely for performance, it does not change
	// correctness.
	if (!prefixes.has_continuation_flags() &&
	    !suffixes.has_continuation_flags())
		return {};

	for (auto i1 = prefixes.iterate_prefixes_of(word); i1; ++i1) {
		auto& pe1 = *i1;

		// The following check is purely for performance, it
		// does not change correctness.
		if (!prefixes.has_continuation_flag(pe1.flag) &&
		    !suffixes.has_continuation_flag(pe1.flag))
			continue;

		if (pe1.cross_product == false)
			continue;
		if (outer_affix_NOT_valid<m>(pe1))
			continue;
		To_Root_Unroot_RAII<Prefix> xxx(word, pe1);
		if (!pe1.check_condition(word))
			continue;
		for (auto i2 = suffixes.iterate_suffixes_of(word); i2; ++i2) {
			auto& se1 = *i2;
			if (se1.cross_product == false)
				continue;
			if (affix_NOT_valid<m>(se1))
				continue;
			To_Root_Unroot_RAII<Suffix> yyy(word, se1);
			if (!se1.check_condition(word))
				continue;
			auto ret = strip_p_s_p_3<FULL_WORD>(
			    pe1, se1, word, skip_hidden_homonym);
			if (ret)
				return ret;
		}
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_p_s_p_3(const Prefix& pe1, const Suffix& se1,
                            std::string& word,
                            Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<>
{
	auto& dic = words;

	for (auto it = prefixes.iterate_prefixes_of(word); it; ++it) {
		auto& pe2 = *it;
		if (pe2.cross_product == false)
			continue;
		if (!cross_valid_inner_outer(pe2, pe1) &&
		    !cross_valid_inner_outer(se1, pe1))
			continue;
		if (affix_NOT_valid<m>(pe2))
			continue;
		auto circ1ok = (is_circumfix(se1) == is_circumfix(pe1)) &&
		               !is_circumfix(pe2);
		auto circ2ok = (is_circumfix(se1) == is_circumfix(pe2)) &&
		               !is_circumfix(pe1);
		if (!circ1ok && !circ2ok)
			continue;
		To_Root_Unroot_RAII<Prefix> xxx(word, pe2);
		if (!pe2.check_condition(word))
			continue;
		for (auto& word_entry : Subrange(dic.equal_range(word))) {
			auto& word_flags = word_entry.second;
			if (!cross_valid_inner_outer(pe2, se1) &&
			    !cross_valid_inner_outer(word_flags, se1))
				continue;
			if (!cross_valid_inner_outer(word_flags, pe2))
				continue;
			// badflag check
			if (m == FULL_WORD &&
			    word_flags.contains(compound_onlyin_flag))
				continue;
			if (skip_hidden_homonym &&
			    word_flags.contains(HIDDEN_HOMONYM_FLAG))
				continue;
			return {word_entry};
		}
	}

	return {};
}

template <Affixing_Mode m>
auto Checker::strip_2_prefixes_then_suffix(
    std::string& word, Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<>
{
	// The following check is purely for performance, it does not change
	// correctness.
	if (!prefixes.has_continuation_flags() &&
	    !suffixes.has_continuation_flags())
		return {};

	for (auto i1 = prefixes.iterate_prefixes_of(word); i1; ++i1) {
		auto& pe1 = *i1;

		// The following check is purely for performance, it
		// does not change correctness.
		if (!prefixes.has_continuation_flag(pe1.flag) &&
		    !suffixes.has_continuation_flag(pe1.flag))
			continue;

		if (outer_affix_NOT_valid<m>(pe1))
			continue;
		if (is_circumfix(pe1))
			continue;
		To_Root_Unroot_RAII<Prefix> xxx(word, pe1);
		if (!pe1.check_condition(word))
			continue;
		for (auto i2 = prefixes.iterate_prefixes_of(word); i2; ++i2) {
			auto& pe2 = *i2;
			if (pe2.cross_product == false)
				continue;
			if (affix_NOT_valid<m>(pe2))
				continue;
			To_Root_Unroot_RAII<Prefix> yyy(word, pe2);
			if (!pe2.check_condition(word))
				continue;
			auto ret = strip_2_pfx_sfx_3<FULL_WORD>(
			    pe1, pe2, word, skip_hidden_homonym);
			if (ret)
				return ret;
		}
	}
	return {};
}

template <Affixing_Mode m>
auto Checker::strip_2_pfx_sfx_3(const Prefix& pe1, const Prefix& pe2,
                                std::string& word,
                                Hidden_Homonym skip_hidden_homonym) const
    -> Affixing_Result<>
{
	auto& dic = words;

	for (auto it = suffixes.iterate_suffixes_of(word); it; ++it) {
		auto& se1 = *it;
		if (se1.cross_product == false)
			continue;
		if (!cross_valid_inner_outer(pe2, pe1) &&
		    !cross_valid_inner_outer(se1, pe1))
			continue;
		if (affix_NOT_valid<m>(se1))
			continue;
		if (is_circumfix(pe2) != is_circumfix(se1))
			continue;
		To_Root_Unroot_RAII<Suffix> xxx(word, se1);
		if (!se1.check_condition(word))
			continue;
		for (auto& word_entry : Subrange(dic.equal_range(word))) {
			auto& word_flags = word_entry.second;
			if (!cross_valid_inner_outer(se1, pe2) &&
			    !cross_valid_inner_outer(word_flags, pe2))
				continue;
			if (!cross_valid_inner_outer(word_flags, se1))
				continue;
			// badflag check
			if (m == FULL_WORD &&
			    word_flags.contains(compound_onlyin_flag))
				continue;
			if (skip_hidden_homonym &&
			    word_flags.contains(HIDDEN_HOMONYM_FLAG))
				continue;
			return {word_entry};
		}
	}

	return {};
}

auto match_compound_pattern(const Compound_Pattern& p, string_view word,
                            size_t i, Compounding_Result first,
                            Compounding_Result second)
{
	if (i < p.begin_end_chars.idx())
		return false;
	if (word.compare(i - p.begin_end_chars.idx(),
	                 p.begin_end_chars.str().size(),
	                 p.begin_end_chars.str()) != 0)
		return false;
	if (p.first_word_flag != 0 &&
	    !first->second.contains(p.first_word_flag))
		return false;
	if (p.second_word_flag != 0 &&
	    !second->second.contains(p.second_word_flag))
		return false;
	if (p.match_first_only_unaffixed_or_zero_affixed &&
	    first.affixed_and_modified)
		return false;
	return true;
}

auto is_compound_forbidden_by_patterns(const vector<Compound_Pattern>& patterns,
                                       string_view word, size_t i,
                                       Compounding_Result first,
                                       Compounding_Result second)
{
	return any_of(begin(patterns), end(patterns), [&](auto& p) {
		return match_compound_pattern(p, word, i, first, second);
	});
}

auto Checker::check_compound(std::string& word,
                             Forceucase allow_bad_forceucase) const
    -> Compounding_Result
{
	auto part = string();

	if (compound_flag || compound_begin_flag || compound_middle_flag ||
	    compound_last_flag) {
		auto ret =
		    check_compound(word, 0, 0, part, allow_bad_forceucase);
		if (ret)
			return ret;
	}
	if (!compound_rules.empty()) {
		auto words_data = vector<const Flag_Set*>();
		return check_compound_with_rules(word, words_data, 0, part,
		                                 allow_bad_forceucase);
	}

	return {};
}

template <Affixing_Mode m>
auto Checker::check_compound(std::string& word, size_t start_pos,
                             size_t num_part, std::string& part,
                             Forceucase allow_bad_forceucase) const
    -> Compounding_Result
{
	size_t min_num_cp = 3;
	if (compound_min_length != 0)
		min_num_cp = compound_min_length;

	auto i = start_pos;
	for (size_t num_cp = 0; num_cp != min_num_cp; ++num_cp) {
		if (i == size(word))
			return {};
		valid_u8_advance_index(word, i);
	}
	auto last_i = size(word);
	for (size_t num_cp = 0; num_cp != min_num_cp; ++num_cp) {
		if (last_i < i)
			return {};
		valid_u8_reverse_index(word, last_i);
	}
	for (; i <= last_i; valid_u8_advance_index(word, i)) {
		auto part1_entry = check_compound_classic<m>(
		    word, start_pos, i, num_part, part, allow_bad_forceucase);

		if (part1_entry)
			return part1_entry;

		part1_entry = check_compound_with_pattern_replacements<m>(
		    word, start_pos, i, num_part, part, allow_bad_forceucase);

		if (part1_entry)
			return part1_entry;
	}
	return {};
}

auto are_three_code_points_equal(string_view word, size_t i) -> bool
{
	auto cp = valid_u8_next_cp(word, i);
	auto prev_cp = valid_u8_prev_cp(word, i);
	if (prev_cp.cp == cp.cp) {
		if (cp.end_i != size(word)) {
			auto next_cp = valid_u8_next_cp(word, cp.end_i);
			if (cp.cp == next_cp.cp)
				return true;
		}
		if (prev_cp.begin_i != 0) {
			auto prev2_cp = valid_u8_prev_cp(word, prev_cp.begin_i);
			if (prev2_cp.cp == cp.cp)
				return true;
		}
	}
	return false;
}

template <Affixing_Mode m>
auto Checker::check_compound_classic(std::string& word, size_t start_pos,
                                     size_t i, size_t num_part,
                                     std::string& part,
                                     Forceucase allow_bad_forceucase) const
    -> Compounding_Result
{
	auto old_num_part = num_part;
	part.assign(word, start_pos, i - start_pos);
	auto part1_entry = check_word_in_compound<m>(part);
	if (!part1_entry)
		return {};
	if (part1_entry->second.contains(forbiddenword_flag))
		return {};
	if (compound_check_triple) {
		if (are_three_code_points_equal(word, i))
			return {};
	}
	if (compound_check_case &&
	    has_uppercase_at_compound_word_boundary(word, i))
		return {};
	num_part += part1_entry.num_words_modifier;
	num_part += compound_root_flag &&
	            part1_entry->second.contains(compound_root_flag);

	part.assign(word, i, word.npos);
	auto part2_entry = check_word_in_compound<AT_COMPOUND_END>(part);
	if (!part2_entry)
		goto try_recursive;
	if (part2_entry->second.contains(forbiddenword_flag))
		goto try_recursive;
	if (is_compound_forbidden_by_patterns(compound_patterns, word, i,
	                                      part1_entry, part2_entry))
		goto try_recursive;
	if (compound_check_duplicate && part1_entry == part2_entry)
		goto try_recursive;
	if (compound_check_rep) {
		part.assign(word, start_pos);
		if (is_rep_similar(part))
			goto try_recursive;
	}
	if (compound_force_uppercase && !allow_bad_forceucase &&
	    part2_entry->second.contains(compound_force_uppercase))
		goto try_recursive;

	old_num_part = num_part;
	num_part += part2_entry.num_words_modifier;
	num_part += compound_root_flag &&
	            part2_entry->second.contains(compound_root_flag);
	if (compound_max_word_count != 0 &&
	    num_part + 1 >= compound_max_word_count) {
		if (compound_syllable_vowels.empty()) // is not Hungarian
			return {}; // end search here, num_part can only go up

		// else, language is Hungarian
		auto num_syllable = count_syllables(word);
		num_syllable += part2_entry.num_syllable_modifier;
		if (num_syllable > compound_syllable_max) {
			num_part = old_num_part;
			goto try_recursive;
		}
	}
	return part1_entry;

try_recursive:
	part2_entry = check_compound<AT_COMPOUND_MIDDLE>(
	    word, i, num_part + 1, part, allow_bad_forceucase);
	if (!part2_entry)
		goto try_simplified_triple;
	if (is_compound_forbidden_by_patterns(compound_patterns, word, i,
	                                      part1_entry, part2_entry))
		goto try_simplified_triple;
	// if (compound_check_duplicate && part1_entry == part2_entry)
	//	goto try_simplified_triple;
	if (compound_check_rep) {
		part.assign(word, start_pos);
		if (is_rep_similar(part))
			goto try_simplified_triple;
		auto& p2word = part2_entry->first;
		if (word.compare(i, p2word.size(), p2word) == 0) {
			// part.assign(word, start_pos,
			//            i - start_pos + p2word.size());
			// The erase() is equivaled as the assign above.
			part.erase(i - start_pos + p2word.size());
			if (is_rep_similar(part))
				goto try_simplified_triple;
		}
	}
	return part1_entry;

try_simplified_triple:
	if (!compound_simplified_triple)
		return {};
	auto prev_cp = valid_u8_prev_cp(word, i);
	if (prev_cp.begin_i == 0)
		return {};
	auto prev2_cp = valid_u8_prev_cp(word, prev_cp.begin_i);
	if (prev_cp.cp != prev2_cp.cp)
		return {};
	auto const enc_cp = U8_Encoded_CP(prev_cp.cp);
	word.insert(i, enc_cp);
	AT_SCOPE_EXIT(word.erase(i, size(enc_cp)));
	part.assign(word, i, word.npos);
	part2_entry = check_word_in_compound<AT_COMPOUND_END>(part);
	if (!part2_entry)
		goto try_simplified_triple_recursive;
	if (part2_entry->second.contains(forbiddenword_flag))
		goto try_simplified_triple_recursive;
	if (is_compound_forbidden_by_patterns(compound_patterns, word, i,
	                                      part1_entry, part2_entry))
		goto try_simplified_triple_recursive;
	if (compound_check_duplicate && part1_entry == part2_entry)
		goto try_simplified_triple_recursive;
	if (compound_check_rep) {
		part.assign(word, start_pos);

		// The added char above should not be checked for rep
		// similarity, instead check the original word.
		part.erase(i - start_pos, size(enc_cp));

		if (is_rep_similar(part))
			goto try_simplified_triple_recursive;
	}
	if (compound_force_uppercase && !allow_bad_forceucase &&
	    part2_entry->second.contains(compound_force_uppercase))
		goto try_simplified_triple_recursive;

	if (compound_max_word_count != 0 &&
	    num_part + 1 >= compound_max_word_count)
		return {};
	return part1_entry;

try_simplified_triple_recursive:
	part2_entry = check_compound<AT_COMPOUND_MIDDLE>(
	    word, i, num_part + 1, part, allow_bad_forceucase);
	if (!part2_entry)
		return {};
	if (is_compound_forbidden_by_patterns(compound_patterns, word, i,
	                                      part1_entry, part2_entry))
		return {};
	// if (compound_check_duplicate && part1_entry == part2_entry)
	//	return {};
	if (compound_check_rep) {
		part.assign(word, start_pos);
		part.erase(i - start_pos, size(enc_cp)); // for the added CP
		if (is_rep_similar(part))
			return {};
		auto& p2word = part2_entry->first;
		if (word.compare(i, p2word.size(), p2word) == 0) {
			part.assign(word, start_pos,
			            i - start_pos + p2word.size());
			part.erase(i - start_pos,
			           size(enc_cp)); // for the added CP
			if (is_rep_similar(part))
				return {};
		}
	}
	return part1_entry;
}

template <Affixing_Mode m>
auto Checker::check_compound_with_pattern_replacements(
    std::string& word, size_t start_pos, size_t i, size_t num_part,
    std::string& part, Forceucase allow_bad_forceucase) const
    -> Compounding_Result
{
	for (auto& p : compound_patterns) {
		if (p.replacement.empty())
			continue;
		if (word.compare(i, p.replacement.size(), p.replacement) != 0)
			continue;

		// at this point p.replacement is substring in word
		word.replace(i, p.replacement.size(), p.begin_end_chars.str());
		i += p.begin_end_chars.idx();
		AT_SCOPE_EXIT({
			i -= p.begin_end_chars.idx();
			word.replace(i, p.begin_end_chars.str().size(),
			             p.replacement);
		});

		part.assign(word, start_pos, i - start_pos);
		auto part1_entry = check_word_in_compound<m>(part);
		if (!part1_entry)
			continue;
		if (part1_entry->second.contains(forbiddenword_flag))
			continue;
		if (p.first_word_flag != 0 &&
		    !part1_entry->second.contains(p.first_word_flag))
			continue;
		if (compound_check_triple) {
			if (are_three_code_points_equal(word, i))
				continue;
		}

		part.assign(word, i, word.npos);
		auto part2_entry =
		    check_word_in_compound<AT_COMPOUND_END>(part);
		if (!part2_entry)
			goto try_recursive;
		if (part2_entry->second.contains(forbiddenword_flag))
			goto try_recursive;
		if (p.second_word_flag != 0 &&
		    !part2_entry->second.contains(p.second_word_flag))
			goto try_recursive;
		if (compound_check_duplicate && part1_entry == part2_entry)
			goto try_recursive;
		if (compound_check_rep) {
			part.assign(word, start_pos);
			part.replace(i - start_pos - p.begin_end_chars.idx(),
			             p.begin_end_chars.str().size(),
			             p.replacement);
			if (is_rep_similar(part))
				goto try_recursive;
		}
		if (compound_force_uppercase && !allow_bad_forceucase &&
		    part2_entry->second.contains(compound_force_uppercase))
			goto try_recursive;

		if (compound_max_word_count != 0 &&
		    num_part + 1 >= compound_max_word_count)
			return {};
		return part1_entry;

	try_recursive:
		part2_entry = check_compound<AT_COMPOUND_MIDDLE>(
		    word, i, num_part + 1, part, allow_bad_forceucase);
		if (!part2_entry)
			goto try_simplified_triple;
		if (p.second_word_flag != 0 &&
		    !part2_entry->second.contains(p.second_word_flag))
			goto try_simplified_triple;
		// if (compound_check_duplicate && part1_entry == part2_entry)
		//	goto try_simplified_triple;
		if (compound_check_rep) {
			part.assign(word, start_pos);
			part.replace(i - start_pos - p.begin_end_chars.idx(),
			             p.begin_end_chars.str().size(),
			             p.replacement);
			if (is_rep_similar(part))
				goto try_simplified_triple;
			auto& p2word = part2_entry->first;
			if (word.compare(i, p2word.size(), p2word) == 0) {
				part.assign(word, start_pos,
				            i - start_pos + p2word.size());
				if (is_rep_similar(part))
					goto try_simplified_triple;
			}
		}
		return part1_entry;

	try_simplified_triple:
		// TODO: check code points, not units
		if (!compound_simplified_triple)
			continue;
		auto prev_cp = valid_u8_prev_cp(word, i);
		if (prev_cp.begin_i == 0)
			continue;
		auto prev2_cp = valid_u8_prev_cp(word, prev_cp.begin_i);
		if (prev_cp.cp != prev2_cp.cp)
			continue;
		auto const enc_cp = U8_Encoded_CP(prev_cp.cp);
		word.insert(i, enc_cp);
		AT_SCOPE_EXIT(word.erase(i, size(enc_cp)));
		part.assign(word, i, word.npos);
		part2_entry = check_word_in_compound<AT_COMPOUND_END>(part);
		if (!part2_entry)
			goto try_simplified_triple_recursive;
		if (part2_entry->second.contains(forbiddenword_flag))
			goto try_simplified_triple_recursive;
		if (p.second_word_flag != 0 &&
		    !part2_entry->second.contains(p.second_word_flag))
			goto try_simplified_triple_recursive;
		if (compound_check_duplicate && part1_entry == part2_entry)
			goto try_simplified_triple_recursive;
		if (compound_check_rep) {
			part.assign(word, start_pos);
			part.erase(i - start_pos,
			           size(enc_cp)); // for the added char
			part.replace(i - start_pos - p.begin_end_chars.idx(),
			             p.begin_end_chars.str().size(),
			             p.replacement);
			if (is_rep_similar(part))
				goto try_simplified_triple_recursive;
		}
		if (compound_force_uppercase && !allow_bad_forceucase &&
		    part2_entry->second.contains(compound_force_uppercase))
			goto try_simplified_triple_recursive;

		if (compound_max_word_count != 0 &&
		    num_part + 1 >= compound_max_word_count)
			return {};
		return part1_entry;

	try_simplified_triple_recursive:
		part2_entry = check_compound<AT_COMPOUND_MIDDLE>(
		    word, i, num_part + 1, part, allow_bad_forceucase);
		if (!part2_entry)
			continue;
		if (p.second_word_flag != 0 &&
		    !part2_entry->second.contains(p.second_word_flag))
			continue;
		// if (compound_check_duplicate && part1_entry == part2_entry)
		//	continue;
		if (compound_check_rep) {
			part.assign(word, start_pos);
			part.erase(i - start_pos,
			           size(enc_cp)); // for the added char
			part.replace(i - start_pos - p.begin_end_chars.idx(),
			             p.begin_end_chars.str().size(),
			             p.replacement);
			if (is_rep_similar(part))
				continue;
			auto& p2word = part2_entry->first;
			if (word.compare(i, p2word.size(), p2word) == 0) {
				part.assign(word, start_pos,
				            i - start_pos + p2word.size());
				part.erase(i - start_pos,
				           size(enc_cp)); // for the added char
				if (is_rep_similar(part))
					continue;
			}
		}
		return part1_entry;
	}
	return {};
}

template <class AffixT>
auto is_modiying_affix(const AffixT& a)
{
	return !a.stripping.empty() || !a.appending.empty();
}

template <Affixing_Mode m>
auto Checker::check_word_in_compound(std::string& word) const
    -> Compounding_Result
{
	auto cpd_flag = char16_t();
	if (m == AT_COMPOUND_BEGIN)
		cpd_flag = compound_begin_flag;
	else if (m == AT_COMPOUND_MIDDLE)
		cpd_flag = compound_middle_flag;
	else if (m == AT_COMPOUND_END)
		cpd_flag = compound_last_flag;

	auto range = words.equal_range(word);
	for (auto& we : Subrange(range)) {
		auto& word_flags = we.second;
		if (word_flags.contains(need_affix_flag))
			continue;
		if (!word_flags.contains(compound_flag) &&
		    !word_flags.contains(cpd_flag))
			continue;
		if (word_flags.contains(HIDDEN_HOMONYM_FLAG))
			continue;
		auto num_syllable_mod = calc_syllable_modifier<m>(we);
		return {&we, 0, num_syllable_mod};
	}
	auto x2 = strip_suffix_only<m>(word, SKIP_HIDDEN_HOMONYM);
	if (x2) {
		auto num_syllable_mod = calc_syllable_modifier<m>(*x2, *x2.a);
		return {x2, 0, num_syllable_mod, is_modiying_affix(*x2.a)};
	}

	auto x1 = strip_prefix_only<m>(word, SKIP_HIDDEN_HOMONYM);
	if (x1) {
		auto num_words_mod = calc_num_words_modifier(*x1.a);
		return {x1, num_words_mod, 0, is_modiying_affix(*x1.a)};
	}

	auto x3 =
	    strip_prefix_then_suffix_commutative<m>(word, SKIP_HIDDEN_HOMONYM);
	if (x3) {
		auto num_words_mod = calc_num_words_modifier(*x3.b);
		auto num_syllable_mod = calc_syllable_modifier<m>(*x3, *x3.a);
		return {x3, num_words_mod, num_syllable_mod,
		        is_modiying_affix(*x3.a) || is_modiying_affix(*x3.b)};
	}
	return {};
}

auto Checker::calc_num_words_modifier(const Prefix& pfx) const -> unsigned char
{
	if (compound_syllable_vowels.empty())
		return 0;
	auto c = count_syllables(pfx.appending);
	return c > 1;
}

template <Affixing_Mode m>
auto Checker::calc_syllable_modifier(Word_List::const_reference we) const
    -> signed char
{
	auto subtract_syllable =
	    m == AT_COMPOUND_END && !compound_syllable_vowels.empty() &&
	    we.second.contains('I') && !we.second.contains('J');
	return 0 - subtract_syllable;
}

template <Affixing_Mode m>
auto Checker::calc_syllable_modifier(Word_List::const_reference we,
                                     const Suffix& sfx) const -> signed char
{
	if (m != AT_COMPOUND_END)
		return 0;
	if (compound_syllable_vowels.empty())
		return 0;
	auto& appnd = sfx.appending;
	signed char num_syllable_mod = 0 - count_syllables(appnd);
	auto sfx_extra = !appnd.empty() && appnd.back() == 'i';
	if (sfx_extra && appnd.size() > 1) {
		auto c = appnd[appnd.size() - 2];
		sfx_extra = c != 'y' && c != 't';
	}
	num_syllable_mod -= sfx_extra;

	if (compound_syllable_num) {
		switch (sfx.flag) {
		case 'c':
			num_syllable_mod += 2;
			break;

		case 'J':
			num_syllable_mod += 1;
			break;

		case 'I':
			num_syllable_mod += we.second.contains('J');
			break;
		}
	}
	return num_syllable_mod;
}

auto Checker::count_syllables(std::string_view word) const -> size_t
{
	return count_appereances_of(word, compound_syllable_vowels);
}

auto Checker::check_compound_with_rules(
    std::string& word, std::vector<const Flag_Set*>& words_data,
    size_t start_pos, std::string& part, Forceucase allow_bad_forceucase) const
    -> Compounding_Result
{
	size_t min_num_cp = 3;
	if (compound_min_length != 0)
		min_num_cp = compound_min_length;

	auto i = start_pos;
	for (size_t num_cp = 0; num_cp != min_num_cp; ++num_cp) {
		if (i == size(word))
			return {};
		valid_u8_advance_index(word, i);
	}
	auto last_i = size(word);
	for (size_t num_cp = 0; num_cp != min_num_cp; ++num_cp) {
		if (last_i < i)
			return {};
		valid_u8_reverse_index(word, last_i);
	}
	for (; i <= last_i; valid_u8_advance_index(word, i)) {
		part.assign(word, start_pos, i - start_pos);
		auto part1_entry = Word_List::const_pointer();
		auto range = words.equal_range(part);
		for (auto& we : Subrange(range)) {
			auto& word_flags = we.second;
			if (word_flags.contains(need_affix_flag))
				continue;
			if (!compound_rules.has_any_of_flags(word_flags))
				continue;
			part1_entry = &we;
			break;
		}
		if (!part1_entry)
			continue;
		words_data.push_back(&part1_entry->second);
		AT_SCOPE_EXIT(words_data.pop_back());

		part.assign(word, i, word.npos);
		auto part2_entry = Word_List::const_pointer();
		range = words.equal_range(part);
		for (auto& we : Subrange(range)) {
			auto& word_flags = we.second;
			if (word_flags.contains(need_affix_flag))
				continue;
			if (!compound_rules.has_any_of_flags(word_flags))
				continue;
			part2_entry = &we;
			break;
		}
		if (!part2_entry)
			goto try_recursive;

		{
			words_data.push_back(&part2_entry->second);
			AT_SCOPE_EXIT(words_data.pop_back());

			auto m = compound_rules.match_any_rule(words_data);
			if (!m)
				goto try_recursive;
			if (compound_force_uppercase && !allow_bad_forceucase &&
			    part2_entry->second.contains(
			        compound_force_uppercase))
				goto try_recursive;

			return {part1_entry};
		}
	try_recursive:
		part2_entry = check_compound_with_rules(
		    word, words_data, i, part, allow_bad_forceucase);
		if (part2_entry)
			return {part2_entry};
	}
	return {};
}
} // namespace v5
} // namespace nuspell
