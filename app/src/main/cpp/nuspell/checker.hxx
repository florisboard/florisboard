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

#ifndef NUSPELL_CHECKER_HXX
#define NUSPELL_CHECKER_HXX

#include "aff_data.hxx"

namespace nuspell {
inline namespace v5 {

enum Affixing_Mode {
	FULL_WORD,
	AT_COMPOUND_BEGIN,
	AT_COMPOUND_END,
	AT_COMPOUND_MIDDLE
};

struct Affixing_Result_Base {
	Word_List::const_pointer root_word = {};

	operator Word_List::const_pointer() const { return root_word; }
	auto& operator*() const { return *root_word; }
	auto operator->() const { return root_word; }
};

template <class T1 = void, class T2 = void>
struct Affixing_Result : Affixing_Result_Base {
	const T1* a = {};
	const T2* b = {};

	Affixing_Result() = default;
	Affixing_Result(Word_List::const_reference r, const T1& a, const T2& b)
	    : Affixing_Result_Base{&r}, a{&a}, b{&b}
	{
	}
};
template <class T1>
struct Affixing_Result<T1, void> : Affixing_Result_Base {
	const T1* a = {};

	Affixing_Result() = default;
	Affixing_Result(Word_List::const_reference r, const T1& a)
	    : Affixing_Result_Base{&r}, a{&a}
	{
	}
};

template <>
struct Affixing_Result<void, void> : Affixing_Result_Base {
	Affixing_Result() = default;
	Affixing_Result(Word_List::const_reference r) : Affixing_Result_Base{&r}
	{
	}
};

struct Compounding_Result {
	Word_List::const_pointer word_entry = {};
	unsigned char num_words_modifier = {};
	signed char num_syllable_modifier = {};
	bool affixed_and_modified = {}; /**< non-zero affix */
	operator Word_List::const_pointer() const { return word_entry; }
	auto& operator*() const { return *word_entry; }
	auto operator->() const { return word_entry; }
};

struct Checker : public Aff_Data {
	enum Forceucase : bool {
		FORBID_BAD_FORCEUCASE = false,
		ALLOW_BAD_FORCEUCASE = true
	};

	enum Hidden_Homonym : bool {
		ACCEPT_HIDDEN_HOMONYM = false,
		SKIP_HIDDEN_HOMONYM = true
	};
	Checker()
	    : Aff_Data() // we explicity do value init so content is zeroed
	{
	}
	auto spell_priv(std::string& s) const -> bool;
	auto spell_break(std::string& s, size_t depth = 0) const -> bool;
	auto spell_casing(std::string& s) const -> const Flag_Set*;
	auto spell_casing_upper(std::string& s) const -> const Flag_Set*;
	auto spell_casing_title(std::string& s) const -> const Flag_Set*;
	auto spell_sharps(std::string& base, size_t n_pos = 0, size_t n = 0,
	                  size_t rep = 0) const -> const Flag_Set*;

	auto check_word(std::string& s, Forceucase allow_bad_forceucase = {},
	                Hidden_Homonym skip_hidden_homonym = {}) const
	    -> const Flag_Set*;
	auto check_simple_word(std::string& word,
	                       Hidden_Homonym skip_hidden_homonym = {}) const
	    -> const Flag_Set*;

	template <Affixing_Mode m>
	auto affix_NOT_valid(const Prefix& a) const;
	template <Affixing_Mode m>
	auto affix_NOT_valid(const Suffix& a) const;
	template <Affixing_Mode m, class AffixT>
	auto outer_affix_NOT_valid(const AffixT& a) const;
	template <class AffixT>
	auto is_circumfix(const AffixT& a) const;
	template <Affixing_Mode m>
	auto is_valid_inside_compound(const Flag_Set& flags) const;

	template <Affixing_Mode m = FULL_WORD>
	auto strip_prefix_only(std::string& s,
	                       Hidden_Homonym skip_hidden_homonym = {}) const
	    -> Affixing_Result<Prefix>;

	template <Affixing_Mode m = FULL_WORD>
	auto strip_suffix_only(std::string& s,
	                       Hidden_Homonym skip_hidden_homonym = {}) const
	    -> Affixing_Result<Suffix>;

	template <Affixing_Mode m = FULL_WORD>
	auto
	strip_prefix_then_suffix(std::string& s,
	                         Hidden_Homonym skip_hidden_homonym = {}) const
	    -> Affixing_Result<Suffix, Prefix>;

	template <Affixing_Mode m>
	auto strip_pfx_then_sfx_2(const Prefix& pe, std::string& s,
	                          Hidden_Homonym skip_hidden_homonym) const
	    -> Affixing_Result<Suffix, Prefix>;

	template <Affixing_Mode m = FULL_WORD>
	auto
	strip_suffix_then_prefix(std::string& s,
	                         Hidden_Homonym skip_hidden_homonym = {}) const
	    -> Affixing_Result<Prefix, Suffix>;

	template <Affixing_Mode m>
	auto strip_sfx_then_pfx_2(const Suffix& se, std::string& s,
	                          Hidden_Homonym skip_hidden_homonym) const
	    -> Affixing_Result<Prefix, Suffix>;

	template <Affixing_Mode m = FULL_WORD>
	auto strip_prefix_then_suffix_commutative(
	    std::string& word, Hidden_Homonym skip_hidden_homonym = {}) const
	    -> Affixing_Result<Suffix, Prefix>;

	template <Affixing_Mode m = FULL_WORD>
	auto strip_pfx_then_sfx_comm_2(const Prefix& pe, std::string& word,
	                               Hidden_Homonym skip_hidden_homonym) const
	    -> Affixing_Result<Suffix, Prefix>;

	template <Affixing_Mode m = FULL_WORD>
	auto
	strip_suffix_then_suffix(std::string& s,
	                         Hidden_Homonym skip_hidden_homonym = {}) const
	    -> Affixing_Result<Suffix, Suffix>;

	template <Affixing_Mode m>
	auto strip_sfx_then_sfx_2(const Suffix& se1, std::string& s,
	                          Hidden_Homonym skip_hidden_homonym) const
	    -> Affixing_Result<Suffix, Suffix>;

	template <Affixing_Mode m = FULL_WORD>
	auto
	strip_prefix_then_prefix(std::string& s,
	                         Hidden_Homonym skip_hidden_homonym = {}) const
	    -> Affixing_Result<Prefix, Prefix>;

	template <Affixing_Mode m>
	auto strip_pfx_then_pfx_2(const Prefix& pe1, std::string& s,
	                          Hidden_Homonym skip_hidden_homonym) const
	    -> Affixing_Result<Prefix, Prefix>;

	template <Affixing_Mode m = FULL_WORD>
	auto strip_prefix_then_2_suffixes(
	    std::string& s, Hidden_Homonym skip_hidden_homonym = {}) const
	    -> Affixing_Result<>;

	template <Affixing_Mode m>
	auto strip_pfx_2_sfx_3(const Prefix& pe1, const Suffix& se1,
	                       std::string& s,
	                       Hidden_Homonym skip_hidden_homonym) const
	    -> Affixing_Result<>;

	template <Affixing_Mode m = FULL_WORD>
	auto strip_suffix_prefix_suffix(
	    std::string& s, Hidden_Homonym skip_hidden_homonym = {}) const
	    -> Affixing_Result<>;

	template <Affixing_Mode m>
	auto strip_s_p_s_3(const Suffix& se1, const Prefix& pe1,
	                   std::string& word,
	                   Hidden_Homonym skip_hidden_homonym) const
	    -> Affixing_Result<>;

	template <Affixing_Mode m = FULL_WORD>
	auto strip_2_suffixes_then_prefix(
	    std::string& s, Hidden_Homonym skip_hidden_homonym = {}) const
	    -> Affixing_Result<>;

	template <Affixing_Mode m>
	auto strip_2_sfx_pfx_3(const Suffix& se1, const Suffix& se2,
	                       std::string& word,
	                       Hidden_Homonym skip_hidden_homonym) const
	    -> Affixing_Result<>;

	template <Affixing_Mode m = FULL_WORD>
	auto strip_suffix_then_2_prefixes(
	    std::string& s, Hidden_Homonym skip_hidden_homonym = {}) const
	    -> Affixing_Result<>;

	template <Affixing_Mode m>
	auto strip_sfx_2_pfx_3(const Suffix& se1, const Prefix& pe1,
	                       std::string& s,
	                       Hidden_Homonym skip_hidden_homonym) const
	    -> Affixing_Result<>;

	template <Affixing_Mode m = FULL_WORD>
	auto strip_prefix_suffix_prefix(
	    std::string& word, Hidden_Homonym skip_hidden_homonym = {}) const
	    -> Affixing_Result<>;

	template <Affixing_Mode m>
	auto strip_p_s_p_3(const Prefix& pe1, const Suffix& se1,
	                   std::string& word,
	                   Hidden_Homonym skip_hidden_homonym) const
	    -> Affixing_Result<>;

	template <Affixing_Mode m = FULL_WORD>
	auto strip_2_prefixes_then_suffix(
	    std::string& word, Hidden_Homonym skip_hidden_homonym = {}) const
	    -> Affixing_Result<>;

	template <Affixing_Mode m>
	auto strip_2_pfx_sfx_3(const Prefix& pe1, const Prefix& pe2,
	                       std::string& word,
	                       Hidden_Homonym skip_hidden_homonym) const
	    -> Affixing_Result<>;

	auto check_compound(std::string& word,
	                    Forceucase allow_bad_forceucase) const
	    -> Compounding_Result;

	template <Affixing_Mode m = AT_COMPOUND_BEGIN>
	auto check_compound(std::string& word, size_t start_pos,
	                    size_t num_part, std::string& part,
	                    Forceucase allow_bad_forceucase) const
	    -> Compounding_Result;

	template <Affixing_Mode m = AT_COMPOUND_BEGIN>
	auto check_compound_classic(std::string& word, size_t start_pos,
	                            size_t i, size_t num_part,
	                            std::string& part,
	                            Forceucase allow_bad_forceucase) const
	    -> Compounding_Result;

	template <Affixing_Mode m = AT_COMPOUND_BEGIN>
	auto check_compound_with_pattern_replacements(
	    std::string& word, size_t start_pos, size_t i, size_t num_part,
	    std::string& part, Forceucase allow_bad_forceucase) const
	    -> Compounding_Result;

	template <Affixing_Mode m>
	auto check_word_in_compound(std::string& s) const -> Compounding_Result;

	auto calc_num_words_modifier(const Prefix& pfx) const -> unsigned char;

	template <Affixing_Mode m>
	auto calc_syllable_modifier(Word_List::const_reference we) const
	    -> signed char;

	template <Affixing_Mode m>
	auto calc_syllable_modifier(Word_List::const_reference we,
	                            const Suffix& sfx) const -> signed char;

	auto count_syllables(std::string_view word) const -> size_t;

	auto check_compound_with_rules(std::string& word,
	                               std::vector<const Flag_Set*>& words_data,
	                               size_t start_pos, std::string& part,
	                               Forceucase allow_bad_forceucase) const

	    -> Compounding_Result;
	auto is_rep_similar(std::string& word) const -> bool;
};

template <Affixing_Mode m>
auto Checker::affix_NOT_valid(const Prefix& e) const
{
	if (m == FULL_WORD && e.cont_flags.contains(compound_onlyin_flag))
		return true;
	if (m == AT_COMPOUND_END &&
	    !e.cont_flags.contains(compound_permit_flag))
		return true;
	if (m != FULL_WORD && e.cont_flags.contains(compound_forbid_flag))
		return true;
	return false;
}
template <Affixing_Mode m>
auto Checker::affix_NOT_valid(const Suffix& e) const
{
	if (m == FULL_WORD && e.cont_flags.contains(compound_onlyin_flag))
		return true;
	if (m == AT_COMPOUND_BEGIN &&
	    !e.cont_flags.contains(compound_permit_flag))
		return true;
	if (m != FULL_WORD && e.cont_flags.contains(compound_forbid_flag))
		return true;
	return false;
}
template <Affixing_Mode m, class AffixT>
auto Checker::outer_affix_NOT_valid(const AffixT& e) const
{
	if (affix_NOT_valid<m>(e))
		return true;
	if (e.cont_flags.contains(need_affix_flag))
		return true;
	return false;
}
template <class AffixT>
auto Checker::is_circumfix(const AffixT& a) const
{
	return a.cont_flags.contains(circumfix_flag);
}

template <class AffixInner, class AffixOuter>
auto cross_valid_inner_outer(const AffixInner& inner, const AffixOuter& outer)
{
	return inner.cont_flags.contains(outer.flag);
}

template <class Affix>
auto cross_valid_inner_outer(const Flag_Set& word_flags, const Affix& afx)
{
	return word_flags.contains(afx.flag);
}

} // namespace v5
} // namespace nuspell
#endif // NUSPELL_CHECKER_HXX
