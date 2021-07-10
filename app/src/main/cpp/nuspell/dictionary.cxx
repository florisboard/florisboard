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

#include "dictionary.hxx"
#include "utils.hxx"

#include <fstream>
#include <iostream>
#include <stdexcept>

using namespace std;

namespace nuspell {
inline namespace v5 {

Dictionary::Dictionary(std::istream& aff, std::istream& dic)
{
	if (!parse_aff_dic(aff, dic))
		throw Dictionary_Loading_Error("error parsing");
}

Dictionary::Dictionary() = default;

/**
 * @brief Create a dictionary from opened files as iostreams
 *
 * Prefer using load_from_path(). Use this if you have a specific use case,
 * like when .aff and .dic are in-memory buffers istringstream.
 *
 * @param aff The iostream of the .aff file
 * @param dic The iostream of the .dic file
 * @return Dictionary object
 * @throws Dictionary_Loading_Error on error
 */
auto Dictionary::load_from_aff_dic(std::istream& aff, std::istream& dic)
    -> Dictionary
{
	return Dictionary(aff, dic);
}

/**
 * @brief Create a dictionary from files
 * @param file_path_without_extension path *without* extensions (without .dic or
 * .aff)
 * @return Dictionary object
 * @throws Dictionary_Loading_Error on error
 */
auto Dictionary::load_from_path(const std::string& file_path_without_extension)
    -> Dictionary
{
	auto path = file_path_without_extension;
	path += ".aff";
	std::ifstream aff_file(path);
	if (aff_file.fail()) {
		auto err = "Aff file " + path + " not found";
		throw Dictionary_Loading_Error(err);
	}
	path.replace(path.size() - 3, 3, "dic");
	std::ifstream dic_file(path);
	if (dic_file.fail()) {
		auto err = "Dic file " + path + " not found";
		throw Dictionary_Loading_Error(err);
	}
	return load_from_aff_dic(aff_file, dic_file);
}

/**
 * @brief Checks if a given word is correct
 * @param word any word
 * @return true if correct, false otherwise
 */
auto Dictionary::spell(std::string_view word) const -> bool
{
	auto ok_enc = validate_utf8(word);
	if (unlikely(word.size() > 360))
		return false;
	if (unlikely(!ok_enc))
		return false;
	auto word_buf = string(word);
	return spell_priv(word_buf);
}

/**
 * @brief Suggests correct words for a given incorrect word
 * @param[in] word incorrect word
 * @param[out] out this object will be populated with the suggestions
 */
auto Dictionary::suggest(std::string_view word,
                         std::vector<std::string>& out) const -> void
{
	out.clear();
	auto ok_enc = validate_utf8(word);
	if (unlikely(word.size() > 360))
		return;
	if (unlikely(!ok_enc))
		return;
	suggest_priv(word, out);
}
} // namespace v5
} // namespace nuspell
