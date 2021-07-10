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

#include "suggester.hxx"
#include "utils.hxx"
#include <unicode/uchar.h>

using namespace std;

namespace nuspell {
inline namespace v5 {

auto static insert_sug_first(const string& word, List_Strings& out)
{
	out.insert(begin(out), word);
}

auto& operator|=(Suggester::High_Quality_Sugs& lhs,
                 Suggester::High_Quality_Sugs rhs)
{
	lhs = Suggester::High_Quality_Sugs(lhs || rhs);
	return lhs;
}

auto Suggester::suggest_priv(string_view input_word, List_Strings& out) const
    -> void
{
	if (empty(input_word))
		return;
	auto word = string(input_word);
	input_substr_replacer.replace(word);
	auto abbreviation = word.back() == '.';
	if (abbreviation) {
		// trim trailing periods
		auto i = word.find_last_not_of('.');
		// if i == npos, i + 1 == 0, so no need for extra if.
		word.erase(i + 1);
		if (word.empty())
			return;
	}
	auto buffer = string();
	auto casing = classify_casing(word);
	auto hq_sugs = High_Quality_Sugs();
	switch (casing) {
	case Casing::SMALL:
		if (compound_force_uppercase &&
		    check_compound(word, ALLOW_BAD_FORCEUCASE)) {
			to_title(word, icu_locale, buffer);
			out.push_back(buffer);
			return;
		}
		hq_sugs |= suggest_low(word, out);
		break;
	case Casing::INIT_CAPITAL:
		hq_sugs |= suggest_low(word, out);
		to_lower(word, icu_locale, buffer);
		hq_sugs |= suggest_low(buffer, out);
		break;
	case Casing::CAMEL:
	case Casing::PASCAL: {
		hq_sugs |= suggest_low(word, out);
		auto dot_idx = word.find('.');
		if (dot_idx != word.npos) {
			auto after_dot = string_view(word).substr(dot_idx + 1);
			auto casing_after_dot = classify_casing(after_dot);
			if (casing_after_dot == Casing::INIT_CAPITAL) {
				word.insert(dot_idx + 1, 1, ' ');
				insert_sug_first(word, out);
				word.erase(dot_idx + 1, 1);
			}
		}
		if (casing == Casing::PASCAL) {
			buffer = word;
			to_lower_char_at(buffer, 0, icu_locale);
			if (spell_priv(buffer))
				insert_sug_first(buffer, out);
			hq_sugs |= suggest_low(buffer, out);
		}
		to_lower(word, icu_locale, buffer);
		if (spell_priv(buffer))
			insert_sug_first(buffer, out);
		hq_sugs |= suggest_low(buffer, out);
		if (casing == Casing::PASCAL) {
			to_title(word, icu_locale, buffer);
			if (spell_priv(buffer))
				insert_sug_first(buffer, out);
			hq_sugs |= suggest_low(buffer, out);
		}
		for (auto it = begin(out); it != end(out); ++it) {
			auto& sug = *it;
			auto space_idx = sug.find(' ');
			if (space_idx == sug.npos)
				continue;
			auto i = space_idx + 1;
			auto len = sug.size() - i;
			if (len > word.size())
				continue;
			if (sug.compare(i, len, word, word.size() - len) == 0)
				continue;
			to_title_char_at(sug, i, icu_locale);
			rotate(begin(out), it, it + 1);
		}
		break;
	}
	case Casing::ALL_CAPITAL:
		to_lower(word, icu_locale, buffer);
		if (keepcase_flag != 0 && spell_priv(buffer))
			insert_sug_first(buffer, out);
		hq_sugs |= suggest_low(buffer, out);
		to_title(word, icu_locale, buffer);
		hq_sugs |= suggest_low(buffer, out);
		for (auto& sug : out)
			to_upper(sug, icu_locale, sug);
		break;
	}

	if (!hq_sugs && max_ngram_suggestions != 0) {
		if (casing == Casing::SMALL)
			buffer = word;
		else
			to_lower(word, icu_locale, buffer);
		auto old_size = out.size();
		ngram_suggest(buffer, out);
		if (casing == Casing::ALL_CAPITAL) {
			for (auto i = old_size; i != out.size(); ++i)
				to_upper(out[i], icu_locale, out[i]);
		}
	}

	auto has_dash = word.find('-') != word.npos;
	auto has_dash_sug =
	    has_dash && any_of(begin(out), end(out), [](const string& s) {
		    return s.find('-') != s.npos;
	    });
	if (has_dash && !has_dash_sug) {
		auto sugs_tmp = List_Strings();
		auto i = size_t();
		for (;;) {
			auto j = word.find('-', i);
			buffer.assign(word, i, j - i);
			if (!spell_priv(buffer)) {
				suggest_priv(buffer, sugs_tmp);
				for (auto& t : sugs_tmp) {
					buffer = word;
					buffer.replace(i, j - i, t);
					auto flg = check_word(buffer);
					if (!flg ||
					    !flg->contains(forbiddenword_flag))
						out.push_back(buffer);
				}
			}
			if (j == word.npos)
				break;
			i = j + 1;
		}
	}

	if (casing == Casing::INIT_CAPITAL || casing == Casing::PASCAL) {
		for (auto& sug : out)
			to_title_char_at(sug, 0, icu_locale);
	}

	// Suggest with dots can go here but nobody uses it so no point in
	// implementing it.

	if ((casing == Casing::INIT_CAPITAL || casing == Casing::ALL_CAPITAL) &&
	    (keepcase_flag != 0 || forbiddenword_flag != 0)) {
		auto is_ok = [&](string& s) {
			if (s.find(' ') != s.npos)
				return true;
			if (spell_priv(s))
				return true;
			to_lower(s, icu_locale, s);
			if (spell_priv(s))
				return true;
			to_title(s, icu_locale, s);
			return spell_priv(s);
		};
		auto it = begin(out);
		auto last = end(out);
		// Bellow is remove_if(it, last, is_not_ok);
		// We don't use remove_if because is_ok modifies
		// the argument.
		for (; it != last; ++it)
			if (!is_ok(*it))
				break;
		if (it != last) {
			for (auto it2 = it + 1; it2 != last; ++it2)
				if (is_ok(*it2))
					*it++ = move(*it2);
			out.erase(it, last);
		}
	}
	{
		auto it = begin(out);
		auto last = end(out);
		for (; it != last; ++it)
			last = remove(it + 1, last, *it);
		out.erase(last, end(out));
	}
	for (auto& sug : out)
		output_substr_replacer.replace(sug);
}

auto Suggester::suggest_low(std::string& word, List_Strings& out) const
    -> High_Quality_Sugs
{
	auto ret = ALL_LOW_QUALITY_SUGS;
	auto old_size = out.size();
	uppercase_suggest(word, out);
	rep_suggest(word, out);
	map_suggest(word, out);
	ret = High_Quality_Sugs(old_size != out.size());
	adjacent_swap_suggest(word, out);
	distant_swap_suggest(word, out);
	keyboard_suggest(word, out);
	extra_char_suggest(word, out);
	forgotten_char_suggest(word, out);
	move_char_suggest(word, out);
	bad_char_suggest(word, out);
	doubled_two_chars_suggest(word, out);
	two_words_suggest(word, out);
	return ret;
}

auto Suggester::add_sug_if_correct(std::string& word, List_Strings& out) const
    -> bool
{
	auto res = check_word(word, FORBID_BAD_FORCEUCASE, SKIP_HIDDEN_HOMONYM);
	if (!res)
		return false;
	if (res->contains(forbiddenword_flag))
		return false;
	if (forbid_warn && res->contains(warn_flag))
		return false;
	out.push_back(word);
	return true;
}

auto Suggester::uppercase_suggest(const std::string& word,
                                  List_Strings& out) const -> void
{
	auto upp = to_upper(word, icu_locale);
	add_sug_if_correct(upp, out);
}

auto Suggester::rep_suggest(std::string& word, List_Strings& out) const

    -> void
{
	auto& reps = replacements;
	for (auto& r : reps.whole_word_replacements()) {
		auto& from = r.first;
		auto& to = r.second;
		if (word == from) {
			word = to;
			try_rep_suggestion(word, out);
			word = from;
		}
	}
	for (auto& r : reps.start_word_replacements()) {
		auto& from = r.first;
		auto& to = r.second;
		if (begins_with(word, from)) {
			word.replace(0, from.size(), to);
			try_rep_suggestion(word, out);
			word.replace(0, to.size(), from);
		}
	}
	for (auto& r : reps.end_word_replacements()) {
		auto& from = r.first;
		auto& to = r.second;
		if (ends_with(word, from)) {
			auto pos = word.size() - from.size();
			word.replace(pos, word.npos, to);
			try_rep_suggestion(word, out);
			word.replace(pos, word.npos, from);
		}
	}
	for (auto& r : reps.any_place_replacements()) {
		auto& from = r.first;
		auto& to = r.second;
		for (auto i = word.find(from); i != word.npos;
		     i = word.find(from, i + 1)) {
			word.replace(i, from.size(), to);
			try_rep_suggestion(word, out);
			word.replace(i, to.size(), from);
		}
	}
}

auto Suggester::try_rep_suggestion(std::string& word, List_Strings& out) const
    -> void
{
	if (add_sug_if_correct(word, out))
		return;

	auto i = size_t(0);
	auto j = word.find(' ');
	if (j == word.npos)
		return;
	auto part = string();
	for (; j != word.npos; i = j + 1, j = word.find(' ', i)) {
		part.assign(word, i, j - i);
		if (!check_word(part, FORBID_BAD_FORCEUCASE,
		                SKIP_HIDDEN_HOMONYM))
			return;
	}
	out.push_back(word);
}

auto Checker::is_rep_similar(std::string& word) const -> bool
{
	auto& reps = replacements;
	for (auto& r : reps.whole_word_replacements()) {
		auto& from = r.first;
		auto& to = r.second;
		if (word == from) {
			word = to;
			auto ret = check_simple_word(word, SKIP_HIDDEN_HOMONYM);
			word = from;
			if (ret)
				return true;
		}
	}
	for (auto& r : reps.start_word_replacements()) {
		auto& from = r.first;
		auto& to = r.second;
		if (begins_with(word, from)) {
			word.replace(0, from.size(), to);
			auto ret = check_simple_word(word, SKIP_HIDDEN_HOMONYM);
			word.replace(0, to.size(), from);
			if (ret)
				return true;
		}
	}
	for (auto& r : reps.end_word_replacements()) {
		auto& from = r.first;
		auto& to = r.second;
		if (ends_with(word, from)) {
			auto pos = word.size() - from.size();
			word.replace(pos, word.npos, to);
			auto ret = check_simple_word(word, SKIP_HIDDEN_HOMONYM);
			word.replace(pos, word.npos, from);
			if (ret)
				return true;
		}
	}
	for (auto& r : reps.any_place_replacements()) {
		auto& from = r.first;
		auto& to = r.second;
		for (auto i = word.find(from); i != word.npos;
		     i = word.find(from, i + 1)) {
			word.replace(i, from.size(), to);
			auto ret = check_simple_word(word, SKIP_HIDDEN_HOMONYM);
			word.replace(i, to.size(), from);
			if (ret)
				return true;
		}
	}
	return false;
}

auto Suggester::max_attempts_for_long_alogs(string_view word) const -> size_t
{
	auto ret = 10'000'000 / size(word);
	if (compound_flag || compound_begin_flag || compound_last_flag ||
	    compound_middle_flag)
		ret /= size(word);
	return ret;
}

auto Suggester::map_suggest(std::string& word, List_Strings& out) const -> void
{
	auto remaining_attempts = max_attempts_for_long_alogs(word);
	map_suggest(word, out, 0, remaining_attempts);
}

auto Suggester::map_suggest(std::string& word, List_Strings& out, size_t i,
                            size_t& remaining_attempts) const -> void
{
	for (size_t next_i = i; i != size(word); i = next_i) {
		valid_u8_advance_index(word, next_i);
		auto word_cp = U8_Encoded_CP(word, {i, next_i});
		for (auto& e : similarities) {
			auto j = e.chars.find(word_cp);
			if (j == word.npos)
				goto try_find_strings;
			for (size_t k = 0, next_k = 0; k != size(e.chars);
			     k = next_k) {
				valid_u8_advance_index(e.chars, next_k);
				if (k == j)
					continue;
				if (remaining_attempts == 0)
					return;
				--remaining_attempts;
				auto rep_cp =
				    string_view(&e.chars[k], next_k - k);
				word.replace(i, size(word_cp), rep_cp);
				add_sug_if_correct(word, out);
				map_suggest(word, out, i + size(rep_cp),
				            remaining_attempts);
				word.replace(i, size(rep_cp), word_cp);
			}
			for (auto& r : e.strings) {
				if (remaining_attempts == 0)
					return;
				--remaining_attempts;
				word.replace(i, size(word_cp), r);
				add_sug_if_correct(word, out);
				map_suggest(word, out, i + size(r),
				            remaining_attempts);
				word.replace(i, size(r), word_cp);
			}
		try_find_strings:
			for (auto& f : e.strings) {
				if (word.compare(i, size(f), f) != 0)
					continue;
				for (size_t k = 0, next_k = 0;
				     k != size(e.chars); k = next_k) {
					if (remaining_attempts == 0)
						return;
					--remaining_attempts;
					valid_u8_advance_index(e.chars, next_k);
					auto rep_cp = string_view(&e.chars[k],
					                          next_k - k);
					word.replace(i, size(f), rep_cp);
					add_sug_if_correct(word, out);
					map_suggest(word, out, i + size(rep_cp),
					            remaining_attempts);
					word.replace(i, size(rep_cp), f);
				}
				for (auto& r : e.strings) {
					if (f == r)
						continue;
					if (remaining_attempts == 0)
						return;
					--remaining_attempts;
					word.replace(i, size(f), r);
					add_sug_if_correct(word, out);
					map_suggest(word, out, i + size(r),
					            remaining_attempts);
					word.replace(i, size(r), f);
				}
			}
		}
	}
}

auto Suggester::adjacent_swap_suggest(std::string& word,
                                      List_Strings& out) const -> void
{
	if (word.empty())
		return;

	auto i1 = size_t(0);
	auto i2 = valid_u8_next_index(word, i1);
	for (size_t i3 = i2; i3 != size(word); i1 = i2, i2 = i3) {
		valid_u8_advance_index(word, i3);
		i2 = u8_swap_adjacent_cp(word, i1, i2, i3);
		add_sug_if_correct(word, out);
		i2 = u8_swap_adjacent_cp(word, i1, i2, i3);
	}
	i1 = 0;
	i2 = valid_u8_next_index(word, i1);
	if (i2 == size(word))
		return;
	auto i3 = valid_u8_next_index(word, i2);
	if (i3 == size(word))
		return;
	auto i4 = valid_u8_next_index(word, i3);
	if (i4 == size(word))
		return;
	auto i5 = valid_u8_next_index(word, i4);
	if (i5 == size(word)) {
		// word has 4 CPs
		i2 = u8_swap_adjacent_cp(word, i1, i2, i3);
		i4 = u8_swap_adjacent_cp(word, i3, i4, i5);
		add_sug_if_correct(word, out);
		i2 = u8_swap_adjacent_cp(word, i1, i2, i3);
		i4 = u8_swap_adjacent_cp(word, i3, i4, i5);
		return;
	}
	auto i6 = valid_u8_next_index(word, i5);
	if (i6 == size(word)) {
		// word has 5 CPs
		i2 = u8_swap_adjacent_cp(word, i1, i2, i3);
		i5 = u8_swap_adjacent_cp(word, i4, i5, i6);
		add_sug_if_correct(word, out);
		i2 = u8_swap_adjacent_cp(word, i1, i2, i3); // revert first two
		i3 = u8_swap_adjacent_cp(word, i2, i3, i4);
		add_sug_if_correct(word, out);
		i3 = u8_swap_adjacent_cp(word, i2, i3, i4);
		i5 = u8_swap_adjacent_cp(word, i4, i5, i6);
	}
}

auto Suggester::distant_swap_suggest(std::string& word, List_Strings& out) const
    -> void
{
	if (empty(word))
		return;
	auto remaining_attempts = max_attempts_for_long_alogs(word);
	auto i1 = size_t(0);
	auto i2 = valid_u8_next_index(word, i1);
	for (auto i3 = i2; i3 != size(word); i1 = i2, i2 = i3) {
		valid_u8_advance_index(word, i3);
		for (size_t j = i3, j2 = i3; j != size(word); j = j2) {
			valid_u8_advance_index(word, j2);
			if (remaining_attempts == 0)
				return;
			--remaining_attempts;
			auto [new_i2, new_j] =
			    u8_swap_cp(word, {i1, i2}, {j, j2});
			add_sug_if_correct(word, out);
			u8_swap_cp(word, {i1, new_i2}, {new_j, j2});
		}
	}
}

auto Suggester::keyboard_suggest(std::string& word, List_Strings& out) const
    -> void
{
	auto& kb = keyboard_closeness;
	for (size_t j = 0, next_j = 0; j != size(word); j = next_j) {
		char32_t c;
		valid_u8_advance_cp(word, next_j, c);
		auto enc_cp = U8_Encoded_CP(word, {j, next_j});
		auto upp_c = char32_t(u_toupper(c));
		if (upp_c != c) {
			auto enc_upp_c = U8_Encoded_CP(upp_c);
			word.replace(j, size(enc_cp), enc_upp_c);
			add_sug_if_correct(word, out);
			word.replace(j, size(enc_upp_c), enc_cp);
		}
		for (auto i = kb.find(enc_cp); i != kb.npos;
		     i = kb.find(enc_cp, i + size(enc_cp))) {
			if (i != 0 && kb[i - 1] != '|') {
				auto prev_i = valid_u8_prev_index(kb, i);
				auto kb_c = U8_Encoded_CP(kb, {prev_i, i});
				word.replace(j, size(enc_cp), kb_c);
				add_sug_if_correct(word, out);
				word.replace(j, size(kb_c), enc_cp);
			}
			auto next_i = i + size(enc_cp);
			if (next_i != size(kb) && kb[next_i] != '|') {
				auto next2_i = valid_u8_next_index(kb, next_i);
				auto kb_c =
				    U8_Encoded_CP(kb, {next_i, next2_i});
				word.replace(j, size(enc_cp), kb_c);
				add_sug_if_correct(word, out);
				word.replace(j, size(kb_c), enc_cp);
			}
		}
	}
}

auto Suggester::extra_char_suggest(std::string& word, List_Strings& out) const
    -> void
{
	for (size_t i = 0, next_i = 0; i != size(word); i = next_i) {
		valid_u8_advance_index(word, next_i);
		auto cp = U8_Encoded_CP(word, {i, next_i});
		word.erase(i, size(cp));
		add_sug_if_correct(word, out);
		word.insert(i, cp);
	}
}

auto Suggester::forgotten_char_suggest(std::string& word,
                                       List_Strings& out) const -> void
{
	auto remaining_attempts = max_attempts_for_long_alogs(word);
	for (size_t t = 0, next_t = 0; t != size(try_chars); t = next_t) {
		valid_u8_advance_index(try_chars, next_t);
		auto cp = string_view(&try_chars[t], next_t - t);
		for (size_t i = 0;; valid_u8_advance_index(word, i)) {
			if (remaining_attempts == 0)
				return;
			--remaining_attempts;
			word.insert(i, cp);
			add_sug_if_correct(word, out);
			word.erase(i, size(cp));
			if (i == size(word))
				break;
		}
	}
}

auto Suggester::move_char_suggest(std::string& word, List_Strings& out) const
    -> void
{
	if (empty(word))
		return;
	auto remaining_attempts = max_attempts_for_long_alogs(word);
	auto i1 = size_t(0);
	auto i2 = valid_u8_next_index(word, i1);
	for (auto i3 = i2; i3 != size(word); i1 = i2, i2 = i3) {
		valid_u8_advance_index(word, i3);
		auto new_i2 = u8_swap_adjacent_cp(word, i1, i2, i3);
		for (auto j1 = new_i2, j2 = i3, j3 = i3; j3 != size(word);
		     j1 = j2, j2 = j3) {
			valid_u8_advance_index(word, j3);
			if (remaining_attempts == 0) {
				// revert word to initial value
				rotate(begin(word) + i1, begin(word) + j1,
				       begin(word) + j2);
				return;
			}
			--remaining_attempts;
			j2 = u8_swap_adjacent_cp(word, j1, j2, j3);
			add_sug_if_correct(word, out);
		}
		// revert word to initial value
		rotate(begin(word) + i1, end(word) - (i2 - i1), end(word));
	}

	auto i3 = size(word);
	i2 = valid_u8_prev_index(word, i3);
	for (i1 = i2; i1 != 0; i3 = i2, i2 = i1) {
		valid_u8_reverse_index(word, i1);
		auto new_i2 = u8_swap_adjacent_cp(word, i1, i2, i3);
		for (auto j3 = new_i2, j2 = i1, j1 = i1; j1 != 0;
		     j3 = j2, j2 = j1) {
			valid_u8_reverse_index(word, j1);
			if (remaining_attempts == 0) {
				// revert word
				rotate(begin(word) + j2, begin(word) + j3,
				       begin(word) + i3);
				return;
			}
			--remaining_attempts;
			j2 = u8_swap_adjacent_cp(word, j1, j2, j3);
			add_sug_if_correct(word, out);
		}
		// revert word
		rotate(begin(word), begin(word) + (i3 - i2), begin(word) + i3);
	}
}

auto Suggester::bad_char_suggest(std::string& word, List_Strings& out) const
    -> void
{
	auto remaining_attempts = max_attempts_for_long_alogs(word);
	for (size_t t = 0, next_t = 0; t != size(try_chars); t = next_t) {
		char32_t t_cp;
		valid_u8_advance_cp(try_chars, next_t, t_cp);
		auto t_enc_cp = string_view(&try_chars[t], next_t - t);
		for (size_t i = 0, next_i = 0; i != size(word); i = next_i) {
			char32_t w_cp;
			valid_u8_advance_cp(word, next_i, w_cp);
			auto w_enc_cp = U8_Encoded_CP(word, {i, next_i});
			if (t_cp == w_cp)
				continue;
			if (remaining_attempts == 0)
				return;
			--remaining_attempts;
			word.replace(i, size(w_enc_cp), t_enc_cp);
			add_sug_if_correct(word, out);
			word.replace(i, size(t_enc_cp), w_enc_cp);
		}
	}
}

auto Suggester::doubled_two_chars_suggest(std::string& word,
                                          List_Strings& out) const -> void
{
	char32_t cp[5];
	size_t i[5];
	size_t j = 0;
	size_t num_cp = 0;
	for (; j != size(word) && num_cp != 4; ++num_cp) {
		i[num_cp] = j;
		valid_u8_advance_cp(word, j, cp[num_cp]);
	}
	if (num_cp != 4) // Not really needed. Makes static analysis happy.
		return;
	while (j != size(word)) {
		i[4] = j;
		valid_u8_advance_cp(word, j, cp[4]);
		if (cp[0] == cp[2] && cp[1] == cp[3] && cp[0] == cp[4]) {
			word.erase(i[3], j - i[3]);
			add_sug_if_correct(word, out);
			word.insert(i[3], word, i[1], i[3] - i[1]);
		}
		copy(begin(i) + 1, end(i), begin(i));
		copy(begin(cp) + 1, end(cp), begin(cp));
	}
}

auto Suggester::two_words_suggest(const std::string& word,
                                  List_Strings& out) const -> void
{
	if (empty(word))
		return;

	auto w1_num_cp = size_t(0);
	auto word1 = string();
	auto word2 = string();
	for (size_t i = 0, next_i = 0;; i = next_i, ++w1_num_cp) {
		valid_u8_advance_index(word, next_i);
		if (next_i == size(word))
			break;
		word1.append(word, i, next_i - i);
		// TODO: maybe switch to check_word()
		auto w1 = check_simple_word(word1, SKIP_HIDDEN_HOMONYM);
		if (!w1)
			continue;
		word2.assign(word, next_i);
		auto w2 = check_simple_word(word2, SKIP_HIDDEN_HOMONYM);
		if (!w2)
			continue;
		word1 += ' ';
		word1 += word2;
		if (find(begin(out), end(out), word1) == end(out))
			out.push_back(word1);
		auto w2_more_than_1_cp =
		    valid_u8_next_index(word2, 0) != size(word2);
		if (w1_num_cp > 1 && w2_more_than_1_cp && !empty(try_chars) &&
		    (try_chars.find('a') != try_chars.npos ||
		     try_chars.find('-') != try_chars.npos)) {
			word1[next_i] = '-';
			if (find(begin(out), end(out), word1) == end(out))
				out.push_back(word1);
		}
		word1.erase(next_i);
	}
}

namespace {
auto ngram_similarity_low_level(size_t n, u32string_view a, u32string_view b)
    -> ptrdiff_t
{
	auto score = ptrdiff_t(0);
	n = min(n, a.size());
	for (size_t k = 1; k != n + 1; ++k) {
		auto k_score = ptrdiff_t(0);
		for (size_t i = 0; i != a.size() - k + 1; ++i) {
			auto kgram = a.substr(i, k);
			auto find = b.find(kgram);
			if (find != b.npos)
				++k_score;
		}
		score += k_score;
		if (k_score < 2)
			break;
	}
	return score;
}
auto ngram_similarity_weighted_low_level(size_t n, u32string_view a,
                                         u32string_view b) -> ptrdiff_t
{
	auto score = ptrdiff_t(0);
	n = min(n, a.size());
	for (size_t k = 1; k != n + 1; ++k) {
		auto k_score = ptrdiff_t(0);
		for (size_t i = 0; i != a.size() - k + 1; ++i) {
			auto kgram = a.substr(i, k);
			auto find = b.find(kgram);
			if (find != b.npos) {
				++k_score;
			}
			else {
				--k_score;
				if (i == 0 || i == a.size() - k)
					--k_score;
			}
		}
		score += k_score;
	}
	return score;
}

auto ngram_similarity_longer_worse(size_t n, u32string_view a, u32string_view b)
    -> ptrdiff_t
{
	if (b.empty())
		return 0;
	auto score = ngram_similarity_low_level(n, a, b);
	auto d = ptrdiff_t(b.size() - a.size()) - 2;
	if (d > 0)
		score -= d;
	return score;
}
auto ngram_similarity_any_mismatch(size_t n, u32string_view a, u32string_view b)
    -> ptrdiff_t
{
	if (b.empty())
		return 0;
	auto score = ngram_similarity_low_level(n, a, b);
	auto d = abs(ptrdiff_t(b.size() - a.size())) - 2;
	if (d > 0)
		score -= d;
	return score;
}
auto ngram_similarity_any_mismatch_weighted(size_t n, u32string_view a,
                                            u32string_view b) -> ptrdiff_t
{
	if (b.empty())
		return 0;
	auto score = ngram_similarity_weighted_low_level(n, a, b);
	auto d = abs(ptrdiff_t(b.size() - a.size())) - 2;
	if (d > 0)
		score -= d;
	return score;
}

auto left_common_substring_length(u32string_view a, u32string_view b)
    -> ptrdiff_t
{
	if (a.empty() || b.empty())
		return 0;
	if (a[0] != b[0] && UChar32(a[0]) != u_tolower(b[0]))
		return 0;
	auto it = std::mismatch(begin(a) + 1, end(a), begin(b) + 1, end(b));
	return it.first - begin(a);
}
auto longest_common_subsequence_length(u32string_view a, u32string_view b,
                                       vector<size_t>& state_buffer)
    -> ptrdiff_t
{
	state_buffer.assign(b.size(), 0);
	auto row1_prev = size_t(0);
	for (size_t i = 0; i != a.size(); ++i) {
		row1_prev = size_t(0);
		auto row2_prev = size_t(0);
		for (size_t j = 0; j != b.size(); ++j) {
			auto row1_current = state_buffer[j];
			auto& row2_current = state_buffer[j];
			if (a[i] == b[j])
				row2_current = row1_prev + 1;
			else
				row2_current = max(row1_current, row2_prev);
			row1_prev = row1_current;
			row2_prev = row2_current;
		}
		row1_prev = row2_prev;
	}
	return ptrdiff_t(row1_prev);
}
struct Count_Eq_Chars_At_Same_Pos_Result {
	ptrdiff_t num;
	bool is_swap;
};
auto count_eq_chars_at_same_pos(u32string_view a, u32string_view b)
    -> Count_Eq_Chars_At_Same_Pos_Result
{
	auto n = min(a.size(), b.size());
	auto count = size_t();
	for (size_t i = 0; i != n; ++i) {
		if (a[i] == b[i])
			++count;
	}
	auto is_swap = false;
	if (a.size() == b.size() && n - count == 2) {
		auto miss1 = mismatch(begin(a), end(a), begin(b));
		auto miss2 =
		    mismatch(miss1.first + 1, end(a), miss1.second + 1);
		is_swap = *miss1.first == *miss2.second &&
		          *miss1.second == *miss2.first;
	}
	return {ptrdiff_t(count), is_swap};
}
struct Word_Entry_And_Score {
	Word_List::const_pointer word_entry = {};
	ptrdiff_t score = {};
	[[maybe_unused]] auto operator<(const Word_Entry_And_Score& rhs) const
	{
		return score > rhs.score; // Greater than
	}
};
struct Word_And_Score {
	u32string word = {};
	ptrdiff_t score = {};
	[[maybe_unused]] auto operator<(const Word_And_Score& rhs) const
	{
		return score > rhs.score; // Greater than
	}
};
} // namespace

auto Suggester::ngram_suggest(const std::string& word_u8,
                              List_Strings& out) const -> void
{
	auto const wrong_word = valid_utf8_to_32(word_u8);
	auto wide_buf = u32string();
	auto roots = vector<Word_Entry_And_Score>();
	auto dict_word = u32string();
	for (size_t bucket = 0; bucket != words.bucket_count(); ++bucket) {
		for (auto& word_entry : words.bucket_data(bucket)) {
			auto& [dict_word_u8, flags] = word_entry;
			if (flags.contains(forbiddenword_flag) ||
			    flags.contains(HIDDEN_HOMONYM_FLAG) ||
			    flags.contains(nosuggest_flag) ||
			    flags.contains(compound_onlyin_flag))
				continue;
			valid_utf8_to_32(dict_word_u8, dict_word);
			auto score =
			    left_common_substring_length(wrong_word, dict_word);
			auto& lower_dict_word = wide_buf;
			to_lower(dict_word, icu_locale, lower_dict_word);
			score += ngram_similarity_longer_worse(3, wrong_word,
			                                       lower_dict_word);
			if (roots.size() != 100) {
				roots.push_back({&word_entry, score});
				push_heap(begin(roots), end(roots));
			}
			else if (score > roots.front().score) {
				pop_heap(begin(roots), end(roots));
				roots.back() = {&word_entry, score};
				push_heap(begin(roots), end(roots));
			}
		}
	}

	auto threshold = ptrdiff_t();
	for (auto k : {1u, 2u, 3u}) {
		auto& mangled_wrong_word = wide_buf;
		mangled_wrong_word = wrong_word;
		for (size_t i = k; i < mangled_wrong_word.size(); i += 4)
			mangled_wrong_word[i] = '*';
		threshold += ngram_similarity_any_mismatch(
		    wrong_word.size(), wrong_word, mangled_wrong_word);
	}
	threshold /= 3;

	auto expanded_list = List_Strings();
	auto expanded_cross_afx = vector<bool>();
	auto expanded_word = u32string();
	auto guess_words = vector<Word_And_Score>();
	for (auto& root : roots) {
		expand_root_word_for_ngram(*root.word_entry, word_u8,
		                           expanded_list, expanded_cross_afx);
		for (auto& expanded_word_u8 : expanded_list) {
			valid_utf8_to_32(expanded_word_u8, expanded_word);
			auto score = left_common_substring_length(
			    wrong_word, expanded_word);
			auto& lower_expanded_word = wide_buf;
			to_lower(expanded_word, icu_locale,
			         lower_expanded_word);
			score += ngram_similarity_any_mismatch(
			    wrong_word.size(), wrong_word, lower_expanded_word);
			if (score < threshold)
				continue;

			if (guess_words.size() != 200) {
				guess_words.push_back(
				    {move(expanded_word), score});
				push_heap(begin(guess_words), end(guess_words));
			}
			else if (score > guess_words.front().score) {
				pop_heap(begin(guess_words), end(guess_words));
				guess_words.back() = {move(expanded_word),
				                      score};
				push_heap(begin(guess_words), end(guess_words));
			}
		}
	}
	sort_heap(begin(guess_words), end(guess_words)); // is this needed?

	auto lcs_state = vector<size_t>();
	for (auto& [guess_word, score] : guess_words) {
		auto& lower_guess_word = wide_buf;
		to_lower(guess_word, icu_locale, lower_guess_word);
		auto lcs = longest_common_subsequence_length(
		    wrong_word, lower_guess_word, lcs_state);

		if (wrong_word.size() == lower_guess_word.size() &&
		    wrong_word.size() == size_t(lcs)) {
			score += 2000;
			break;
		}

		auto ngram2 = ngram_similarity_any_mismatch_weighted(
		    2, wrong_word, lower_guess_word);
		ngram2 += ngram_similarity_any_mismatch_weighted(
		    2, lower_guess_word, wrong_word);
		auto ngram4 = ngram_similarity_any_mismatch(4, wrong_word,
		                                            lower_guess_word);
		auto left_common =
		    left_common_substring_length(wrong_word, lower_guess_word);
		auto num_eq_chars_same_pos =
		    count_eq_chars_at_same_pos(wrong_word, lower_guess_word);

		score = 2 * lcs;
		score -=
		    abs(ptrdiff_t(wrong_word.size() - lower_guess_word.size()));
		score += left_common + ngram2 + ngram4;
		if (num_eq_chars_same_pos.num != 0)
			score += 1;
		if (num_eq_chars_same_pos.is_swap)
			score += 10;
		if (5 * ngram2 <
		    ptrdiff_t(wrong_word.size() + lower_guess_word.size()) *
		        (10 - max_diff_factor))
			score -= 1000;
	}

	sort(begin(guess_words), end(guess_words));

	auto more_selective =
	    !guess_words.empty() && guess_words.front().score > 1000;
	auto old_num_sugs = out.size();
	auto max_sug =
	    min(MAX_SUGGESTIONS, old_num_sugs + max_ngram_suggestions);
	for (auto& [guess_word, score] : guess_words) {
		if (out.size() == max_sug)
			break;
		if (more_selective && score <= 1000)
			break;
		if (score < -100 &&
		    (old_num_sugs != out.size() || only_max_diff))
			break;
		auto guess_word_u8 = utf32_to_utf8(guess_word);
		if (any_of(begin(out), end(out),
		           [&g = guess_word_u8](auto& sug) {
			           return g.find(sug) != g.npos;
		           })) {
			if (score < -100)
				break;
			else
				continue;
		}
		out.push_back(move(guess_word_u8));
	}
}

auto Suggester::expand_root_word_for_ngram(
    Word_List::const_reference root_entry, std::string_view wrong,
    List_Strings& expanded_list, std::vector<bool>& cross_affix) const -> void
{
	expanded_list.clear();
	cross_affix.clear();
	auto& [root, flags] = root_entry;
	if (!flags.contains(need_affix_flag)) {
		expanded_list.push_back(root);
		cross_affix.push_back(false);
	}
	if (flags.empty())
		return;
	for (auto& suffix : suffixes) {
		if (!cross_valid_inner_outer(flags, suffix))
			continue;
		if (outer_affix_NOT_valid<FULL_WORD>(suffix))
			continue;
		if (is_circumfix(suffix))
			continue;
		// TODO Suffixes marked with needaffix or circumfix should not
		// be just skipped as we can later add prefix. This is not
		// handled in hunspell, too.
		if (!ends_with(root, suffix.stripping))
			continue;
		if (!suffix.check_condition(root))
			continue;

		if (!suffix.appending.empty() &&
		    !ends_with(wrong, suffix.appending))
			continue;

		auto expanded = suffix.to_derived_copy(root);
		expanded_list.push_back(move(expanded));
		cross_affix.push_back(suffix.cross_product);
	}

	for (size_t i = 0, n = expanded_list.size(); i != n; ++i) {
		if (!cross_affix[i])
			continue;

		for (auto& prefix : prefixes) {
			auto& root_sfx = expanded_list[i];
			if (!cross_valid_inner_outer(flags, prefix))
				continue;
			if (outer_affix_NOT_valid<FULL_WORD>(prefix))
				continue;
			if (is_circumfix(prefix))
				continue;
			if (!begins_with(root_sfx, prefix.stripping))
				continue;
			if (!prefix.check_condition(root_sfx))
				continue;

			if (!prefix.appending.empty() &&
			    !begins_with(wrong, prefix.appending))
				continue;

			auto expanded = prefix.to_derived_copy(root_sfx);
			expanded_list.push_back(move(expanded));
		}
	}

	for (auto& prefix : prefixes) {
		if (!cross_valid_inner_outer(flags, prefix))
			continue;
		if (outer_affix_NOT_valid<FULL_WORD>(prefix))
			continue;
		if (is_circumfix(prefix))
			continue;
		if (!begins_with(root, prefix.stripping))
			continue;
		if (!prefix.check_condition(root))
			continue;

		if (!prefix.appending.empty() &&
		    !begins_with(wrong, prefix.appending))
			continue;

		auto expanded = prefix.to_derived_copy(root);
		expanded_list.push_back(move(expanded));
	}
}
} // namespace v5
} // namespace nuspell
