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

/**
 * @file
 * @brief Dictionary spelling.
 */

#ifndef NUSPELL_DICTIONARY_HXX
#define NUSPELL_DICTIONARY_HXX

#include "suggester.hxx"

namespace nuspell {
inline namespace v5 {

/**
 * @brief The only important public exception
 */
class Dictionary_Loading_Error : public std::runtime_error {
      public:
	using std::runtime_error::runtime_error;
};

/**
 * @brief The only important public class
 */
class NUSPELL_EXPORT Dictionary : private Suggester {
	Dictionary(std::istream& aff, std::istream& dic);

      public:
	Dictionary();
	auto static load_from_aff_dic(std::istream& aff, std::istream& dic)
	    -> Dictionary;
	auto static load_from_path(
	    const std::string& file_path_without_extension) -> Dictionary;
	auto spell(std::string_view word) const -> bool;
	auto suggest(std::string_view word, std::vector<std::string>& out) const
	    -> void;
};

} // namespace v5
} // namespace nuspell
#endif // NUSPELL_DICTIONARY_HXX
