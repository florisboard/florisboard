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

#include "aff_data.hxx"
#include "utils.hxx"

#include <iostream>
#include <sstream>
#include <unordered_map>

using namespace std;

/**
 * @brief Library main namespace
 */
namespace nuspell {

/**
 * @brief Library main namespace with version number attached
 *
 * This inline namespace is used for ABI versioning. It is the same as the major
 * verison. Look up on the Internet to see what is it for (ABI versioning
 * mostly). Client code should never mention this inline namespace.
 */
inline namespace v5 {

auto Encoding::normalize_name() -> void
{
	to_upper_ascii(name);
	if (name == "UTF8")
		name = "UTF-8";
	else if (name.compare(0, 10, "MICROSOFT-") == 0)
		name.erase(0, 10);
}

namespace {

void reset_failbit_istream(std::istream& in)
{
	in.clear(in.rdstate() & ~in.failbit);
}

enum class Parsing_Error_Code {
	NO_FLAGS_AFTER_SLASH_WARNING = -2,
	NONUTF8_FLAGS_ABOVE_127_WARNING = -1,
	NO_ERROR = 0,
	MISSING_FLAGS,
	UNPAIRED_LONG_FLAG,
	INVALID_NUMERIC_FLAG,
	// FLAGS_ARE_UTF8_BUT_FILE_NOT,
	INVALID_UTF8,
	FLAG_ABOVE_65535,
	INVALID_NUMERIC_ALIAS,
	AFX_CONDITION_INVALID_FORMAT,
	COMPOUND_RULE_INVALID_FORMAT
};

auto decode_flags(const string& s, Flag_Type t, const Encoding& enc,
                  u16string& out) -> Parsing_Error_Code
{
	using Err = Parsing_Error_Code;
	using Ft = Flag_Type;
	auto warn = Err();
	out.clear();
	if (s.empty())
		return Err::MISSING_FLAGS;
	switch (t) {
	case Ft::SINGLE_CHAR:
		if (enc.is_utf8() && !is_all_ascii(s)) {
			warn = Err::NONUTF8_FLAGS_ABOVE_127_WARNING;
			// This warning will be triggered in Hungarian.
			// Version 1 passed this, it just read a single byte
			// even if the stream utf-8. Hungarian dictionary
			// exploited this bug/feature, resulting it's file to be
			// mixed utf-8 and latin2. In v2 this will eventually
			// work, with a warning.
		}
		latin1_to_ucs2(s, out);
		break;
	case Ft::DOUBLE_CHAR: {
		if (enc.is_utf8() && !is_all_ascii(s))
			warn = Err::NONUTF8_FLAGS_ABOVE_127_WARNING;

		if (s.size() % 2 == 1)
			return Err::UNPAIRED_LONG_FLAG;

		auto i = s.begin();
		auto e = s.end();
		for (; i != e; i += 2) {
			auto c1 = *i;
			auto c2 = *(i + 1);
			out.push_back((c1 << 8) | c2);
		}
		break;
	}
	case Ft::NUMBER: {
		auto p = s.c_str();
		char* p2 = nullptr;
		errno = 0;
		for (;;) {
			auto flag = strtoul(p, &p2, 10);
			if (p2 == p)
				return Err::INVALID_NUMERIC_FLAG;
			if (flag == numeric_limits<decltype(flag)>::max() &&
			    errno == ERANGE) {
				errno = 0;
				return Err::FLAG_ABOVE_65535;
			}
			if (flag > 0xFFFF)
				return Err::FLAG_ABOVE_65535;
			out.push_back(flag);

			if (p2 == end_ptr(s) || *p2 != ',')
				break;

			p = p2 + 1;
		}
		break;
	}
	case Ft::UTF8: {
		// if (!enc.is_utf8())
		//	return Err::FLAGS_ARE_UTF8_BUT_FILE_NOT;

		auto ok = utf8_to_16(s, out);
		if (!ok) {
			out.clear();
			return Err::INVALID_UTF8;
		}

		if (!is_all_bmp(out)) {
			out.clear();
			return Err::FLAG_ABOVE_65535;
		}
		break;
	}
	}
	return warn;
}

auto decode_flags_possible_alias(const string& s, Flag_Type t,
                                 const Encoding& enc,
                                 const vector<Flag_Set>& flag_aliases,
                                 u16string& out) -> Parsing_Error_Code
{
	if (flag_aliases.empty())
		return decode_flags(s, t, enc, out);

	char* p;
	errno = 0;
	out.clear();
	auto i = strtoul(s.c_str(), &p, 10);
	if (p == s.c_str())
		return Parsing_Error_Code::INVALID_NUMERIC_ALIAS;

	if (i == numeric_limits<decltype(i)>::max() && errno == ERANGE)
		return Parsing_Error_Code::INVALID_NUMERIC_ALIAS;

	if (0 < i && i <= flag_aliases.size()) {
		out = flag_aliases[i - 1];
		return {};
	}
	return Parsing_Error_Code::INVALID_NUMERIC_ALIAS;
}

auto report_parsing_error(Parsing_Error_Code err, size_t line_num)
{
	using Err = Parsing_Error_Code;
	switch (err) {
	case Err::NO_FLAGS_AFTER_SLASH_WARNING:
		cerr << "Nuspell warning: no flags after slash in line "
		     << line_num << '\n';
		break;
	case Err::NONUTF8_FLAGS_ABOVE_127_WARNING:
		cerr << "Nuspell warning: bytes above 127 in flags in UTF-8 "
		        "file are treated as lone bytes for backward "
		        "compatibility. That means if in the flags you have "
		        "ONE character above ASCII, it may be interpreted as "
		        "2, 3, or 4 flags. Please update dictionary and affix "
		        "files to use FLAG UTF-8 and make the file valid "
		        "UTF-8 if it is not already. Warning in line "
		     << line_num << '\n';
		break;
	case Err::NO_ERROR:
		break;
	case Err::MISSING_FLAGS:
		cerr << "Nuspell error: missing flags in line " << line_num
		     << '\n';
		break;
	case Err::UNPAIRED_LONG_FLAG:
		cerr << "Nuspell error: the number of chars in string of long "
		        "flags is odd, should be even. Error in line "
		     << line_num << '\n';
		break;
	case Err::INVALID_NUMERIC_FLAG:
		cerr << "Nuspell error: invalid numerical flag in line"
		     << line_num << '\n';
		break;
	// case Err::FLAGS_ARE_UTF8_BUT_FILE_NOT:
	//	cerr << "Nuspell error: flags are UTF-8 but file is not\n";
	//	break;
	case Err::INVALID_UTF8:
		cerr << "Nuspell error: Invalid UTF-8 in flags in line "
		     << line_num << '\n';
		break;
	case Err::FLAG_ABOVE_65535:
		cerr << "Nuspell error: Flag above 65535 in line " << line_num
		     << '\n';
		break;
	case Err::INVALID_NUMERIC_ALIAS:
		cerr << "Nuspell error: Flag alias is invalid in line"
		     << line_num << '\n';
		break;
	case Err::AFX_CONDITION_INVALID_FORMAT:
		cerr << "Nuspell error: Affix condition is invalid in line "
		     << line_num << '\n';
		break;
	case Err::COMPOUND_RULE_INVALID_FORMAT:
		cerr << "Nuspell error: Compound rule is in invalid format in "
		        "line "
		     << line_num << '\n';
		break;
	}
}

auto decode_compound_rule(const string& s, Flag_Type t, const Encoding& enc,
                          u16string& out) -> Parsing_Error_Code
{
	using Ft = Flag_Type;
	using Err = Parsing_Error_Code;
	switch (t) {
	case Ft::SINGLE_CHAR:
	case Ft::UTF8:
		return decode_flags(s, t, enc, out);
		break;
	case Ft::DOUBLE_CHAR:
		out.clear();
		if (s.empty())
			return Err::MISSING_FLAGS;
		for (size_t i = 0;;) {
			if (s.size() - i < 4)
				return Err::COMPOUND_RULE_INVALID_FORMAT;
			if (s[i] != '(' || s[i + 3] != ')')
				return Err::COMPOUND_RULE_INVALID_FORMAT;
			auto c1 = s[i + 1];
			auto c2 = s[i + 2];
			out.push_back((c1 << 8) | c2);
			i += 4;
			if (i == s.size())
				break;
			if (s[i] == '?' || s[i] == '*') {
				out.push_back(s[i]);
				i += 1;
			}
		}
		break;
	case Ft::NUMBER:
		out.clear();
		if (s.empty())
			return Err::MISSING_FLAGS;
		errno = 0;
		for (auto p = s.c_str(); *p != 0;) {
			if (*p != '(')
				return Err::COMPOUND_RULE_INVALID_FORMAT;
			++p;
			char* p2;
			auto flag = strtoul(p, &p2, 10);
			if (p2 == p)
				return Err::INVALID_NUMERIC_FLAG;
			if (flag == numeric_limits<decltype(flag)>::max() &&
			    errno == ERANGE) {
				errno = 0;
				return Err::FLAG_ABOVE_65535;
			}
			if (flag > 0xFFFF)
				return Err::FLAG_ABOVE_65535;
			p = p2;
			if (*p != ')')
				return Err::COMPOUND_RULE_INVALID_FORMAT;
			out.push_back(flag);
			++p;
			if (*p == '?' || *p == '*') {
				out.push_back(*p);
				++p;
			}
		}
		break;
	}
	return {};
}

auto strip_utf8_bom(std::istream& in) -> void
{
	if (!in.good())
		return;
	auto bom = string(3, '\0');
	in.read(bom.data(), 3);
	if (in && bom == "\xEF\xBB\xBF")
		return;
	if (in.bad())
		return;
	reset_failbit_istream(in);
	for (auto i = size_t(in.gcount()); i-- != 0;)
		in.putback(bom[i]);
}

struct Compound_Rule_Ref_Wrapper {
	std::u16string& rule;
};

auto wrap_compound_rule(std::u16string& r) -> Compound_Rule_Ref_Wrapper
{
	return {r};
}

class Aff_Line_IO_Manip;

template <class T>
struct Ref_Wrapper_1 {
	Aff_Line_IO_Manip& manip;
	T& data;
};

struct Ref_Wrapper_1_Cpd_Rule {
	Aff_Line_IO_Manip& manip;
	Compound_Rule_Ref_Wrapper data;
};

template <class T, class U>
struct Ref_Wrapper_2 {
	Aff_Line_IO_Manip& manip;
	T& data1;
	U& data2;
};

class Aff_Line_IO_Manip {
	std::string str_buf;
	std::u16string flag_buffer;

	const Aff_Data* aff_data = nullptr;
	Encoding_Converter cvt;

      public:
	Parsing_Error_Code err = {};

	Aff_Line_IO_Manip() = default;
	Aff_Line_IO_Manip(Aff_Data& a)
	    : aff_data(&a), cvt(a.encoding.value_or_default())
	{
	}

	auto& parse(istream& in, Encoding& enc)
	{
		in >> str_buf;
		if (in.fail())
			return in;
		enc = str_buf;
		cvt = Encoding_Converter(enc.value_or_default());
		if (!cvt.valid())
			in.setstate(in.failbit);
		return in;
	}

	auto& parse(istream& in, std::string& str)
	{
		in >> str_buf;
		if (in.fail()) // str_buf is unmodified on fail
			return in;
		auto ok = cvt.to_utf8(str_buf, str);
		if (!ok)
			in.setstate(in.failbit);
		return in;
	}

	auto& parse(istream& in, Flag_Type& flag_type)
	{
		using Ft = Flag_Type;
		flag_type = {};
		in >> str_buf;
		if (in.fail())
			return in;
		to_upper_ascii(str_buf);
		if (str_buf == "LONG")
			flag_type = Ft::DOUBLE_CHAR;
		else if (str_buf == "NUM")
			flag_type = Ft::NUMBER;
		else if (str_buf == "UTF-8")
			flag_type = Ft::UTF8;
		else
			in.setstate(in.failbit);
		return in;
	}

	auto& parse(istream& in, icu::Locale& loc)
	{
		in >> str_buf;
		if (in.fail())
			return in;
		loc = icu::Locale(str_buf.c_str());
		if (loc.isBogus())
			in.setstate(in.failbit);
		return in;
	}

      private:
	auto& parse_flags(istream& in, std::u16string& flags)
	{
		in >> str_buf;
		if (in.fail())
			return in;
		err = decode_flags(str_buf, aff_data->flag_type,
		                   aff_data->encoding, flags);
		if (static_cast<int>(err) > 0)
			in.setstate(in.failbit);
		return in;
	}

      public:
	auto& parse(istream& in, char16_t& flag)
	{
		flag = 0;
		parse_flags(in, flag_buffer);
		if (in)
			flag = flag_buffer[0];
		return in;
	}

	auto& parse(istream& in, Flag_Set& flags)
	{
		parse_flags(in, flag_buffer);
		if (in)
			flags = flag_buffer;
		return in;
	}

	auto& parse_word_slash_flags(istream& in, string& word, Flag_Set& flags)
	{
		using Err = Parsing_Error_Code;
		in >> str_buf;
		if (in.fail())
			return in;
		// err = {};
		auto slash_pos = str_buf.find('/');
		if (slash_pos != str_buf.npos) {
			auto flag_str =
			    str_buf.substr(slash_pos + 1); // temporary
			str_buf.erase(slash_pos);
			err = decode_flags_possible_alias(
			    flag_str, aff_data->flag_type, aff_data->encoding,
			    aff_data->flag_aliases, flag_buffer);
			if (err == Err::MISSING_FLAGS)
				err = Err::NO_FLAGS_AFTER_SLASH_WARNING;
			flags = flag_buffer;
		}
		auto ok = cvt.to_utf8(str_buf, word);
		if (!ok)
			in.setstate(in.failbit);
		if (static_cast<int>(err) > 0)
			in.setstate(in.failbit);
		return in;
	}

	auto parse_word_slash_single_flag(istream& in, string& word,
	                                  char16_t& flag) -> istream&
	{
		in >> str_buf;
		if (in.fail())
			return in;
		// err = {};
		auto slash_pos = str_buf.find('/');
		if (slash_pos != str_buf.npos) {
			auto flag_str =
			    str_buf.substr(slash_pos + 1); // temporary
			str_buf.erase(slash_pos);
			err = decode_flags(flag_str, aff_data->flag_type,
			                   aff_data->encoding, flag_buffer);
			if (!flag_buffer.empty())
				flag = flag_buffer[0];
		}
		auto ok = cvt.to_utf8(str_buf, word);
		if (!ok)
			in.setstate(in.failbit);
		if (static_cast<int>(err) > 0)
			in.setstate(in.failbit);
		return in;
	}

	auto& parse_compound_rule(istream& in, u16string& out)
	{
		in >> str_buf;
		if (in.fail())
			return in;
		err = decode_compound_rule(str_buf, aff_data->flag_type,
		                           aff_data->encoding, out);
		if (static_cast<int>(err) > 0)
			in.setstate(in.failbit);
		return in;
	}

	template <class T>
	auto operator()(T& x) -> Ref_Wrapper_1<T>
	{
		return {*this, x};
	}

	auto operator()(Compound_Rule_Ref_Wrapper x) -> Ref_Wrapper_1_Cpd_Rule
	{
		return {*this, x};
	}

	template <class T, class U>
	auto operator()(T& x, U& y) -> Ref_Wrapper_2<T, U>
	{
		return {*this, x, y};
	}
};

template <class T, class = decltype(declval<Aff_Line_IO_Manip&>().parse(
                       declval<istream&>(), declval<T&>()))>
auto& operator>>(istream& in, Ref_Wrapper_1<T> x)
{
	return x.manip.parse(in, x.data);
}

auto& operator>>(istream& in, Ref_Wrapper_1<pair<string, string>> x)
{
	return in >> x.manip(x.data.first) >> x.manip(x.data.second);
}

auto& operator>>(istream& in, Ref_Wrapper_1<Condition> x)
{
	auto str = string();
	in >> x.manip(str);
	if (in.fail())
		return in;
	try {
		x.data = std::move(str);
	}
	catch (const Condition_Exception& ex) {
		x.manip.err = Parsing_Error_Code::AFX_CONDITION_INVALID_FORMAT;
		in.setstate(in.failbit);
	}
	return in;
}

auto& operator>>(istream& in, Ref_Wrapper_1_Cpd_Rule x)
{
	return x.manip.parse_compound_rule(in, x.data.rule);
}

auto& operator>>(istream& in, Ref_Wrapper_2<string, Flag_Set> x)
{
	return x.manip.parse_word_slash_flags(in, x.data1, x.data2);
}

auto& operator>>(istream& in, Ref_Wrapper_2<string, char16_t> x)
{
	return x.manip.parse_word_slash_single_flag(in, x.data1, x.data2);
}

auto& operator>>(istream& in, Ref_Wrapper_1<Compound_Pattern> x)
{
	auto [manip, p] = x;
	auto first_word_end = string();
	auto second_word_begin = string();
	p.match_first_only_unaffixed_or_zero_affixed = false;
	in >> manip(first_word_end, p.first_word_flag);
	in >> manip(second_word_begin, p.second_word_flag);
	if (in.fail())
		return in;
	if (first_word_end == "0") {
		first_word_end.clear();
		p.match_first_only_unaffixed_or_zero_affixed = true;
	}
	p.begin_end_chars = {first_word_end, second_word_begin};
	auto old_mask = in.exceptions();
	in.exceptions(in.goodbit);  // disable exceptions
	in >> manip(p.replacement); // optional
	if (in.fail() && in.eof() && !in.bad()) {
		reset_failbit_istream(in);
		p.replacement.clear();
	}
	in.exceptions(old_mask);
	return in;
}

template <class T, class Func = identity>
auto parse_vector_of_T(istream& in, Aff_Line_IO_Manip& p, const string& command,
                       unordered_map<string, size_t>& counts, vector<T>& vec,
                       Func modifier_wrapper = Func()) -> void
{
	auto dat = counts.find(command);
	if (dat == counts.end()) {
		// first line
		auto& cnt = counts[command]; // cnt == 0
		size_t a;
		in >> a;
		if (in)
			cnt = a;
		else
			cerr << "Nuspell error: a vector command (series of "
			        "of similar commands) has no count. Ignoring "
			        "all of them.\n";
	}
	else if (dat->second != 0) {
		dat->second--;
		vec.emplace_back();
		in >> p(modifier_wrapper(vec.back()));
		if (in.fail())
			cerr << "Nuspell error: single entry of a vector "
			        "command (series of "
			        "of similar commands) is invalid.\n";
	}
	else {
		cerr << "Nuspell warning: extra entries of " << command << "\n";
		// cerr << "Nuspell warning in line " << line_num << endl;
	}
}

template <class AffixT>
auto parse_affix(istream& in, Aff_Line_IO_Manip& p, string& command,
                 vector<AffixT>& vec,
                 unordered_map<string, pair<bool, size_t>>& cmd_affix) -> void
{
	char16_t f;
	in >> p(f);
	if (in.fail())
		return;
	command.append(reinterpret_cast<char*>(&f), sizeof(f));
	auto dat = cmd_affix.find(command);
	// note: the current affix parser does not allow the same flag
	// to be used once with cross product and again witohut
	// one flag is tied to one cross product value
	if (dat == cmd_affix.end()) {
		char cross_char; // 'Y' or 'N'
		size_t cnt;
		auto& cross_and_cnt = cmd_affix[command]; // == false, 0
		in >> cross_char >> cnt;
		if (in.fail())
			return;
		if (cross_char != 'Y' && cross_char != 'N') {
			in.setstate(in.failbit);
			return;
		}
		bool cross = cross_char == 'Y';
		cross_and_cnt = {cross, cnt};
	}
	else if (dat->second.second) {
		dat->second.second--;
		vec.emplace_back();
		auto& elem = vec.back();
		elem.flag = f;
		elem.cross_product = dat->second.first;
		in >> p(elem.stripping);
		if (elem.stripping == "0")
			elem.stripping.clear();
		in >> p(elem.appending, elem.cont_flags);
		if (elem.appending == "0")
			elem.appending.clear();
		if (in.fail())
			return;
		auto old_mask = in.exceptions();
		in.exceptions(in.goodbit);
		in >> p(elem.condition); // optional
		if (in.fail() && in.eof() && !in.bad()) {
			elem.condition = ".";
			reset_failbit_istream(in);
		}
		in.exceptions(old_mask);

		// in >> elem.morphological_fields;
	}
	else {
		cerr << "Nuspell warning: extra entries of "
		     << command.substr(0, 3) << "\n";
	}
}

} // namespace

auto Aff_Data::parse_aff(istream& in) -> bool
{
	auto prefixes = vector<Prefix>();
	auto suffixes = vector<Suffix>();
	auto break_patterns = vector<string>();
	auto break_exists = false;
	auto input_conversion = vector<pair<string, string>>();
	auto output_conversion = vector<pair<string, string>>();
	// auto morphological_aliases = vector<vector<string>>();
	auto rules = vector<u16string>();
	auto replacements = vector<pair<string, string>>();
	auto map_related_chars = vector<string>();
	auto phonetic_replacements = vector<pair<string, string>>();

	max_compound_suggestions = 3;
	max_ngram_suggestions = 4;
	max_diff_factor = 5;
	flag_type = Flag_Type::SINGLE_CHAR;

	unordered_map<string, string*> command_strings = {
	    {"IGNORE", &ignored_chars},

	    {"KEY", &keyboard_closeness},
	    {"TRY", &try_chars}};

	unordered_map<string, bool*> command_bools = {
	    {"COMPLEXPREFIXES", &complex_prefixes},

	    {"ONLYMAXDIFF", &only_max_diff},
	    {"NOSPLITSUGS", &no_split_suggestions},
	    {"SUGSWITHDOTS", &suggest_with_dots},
	    {"FORBIDWARN", &forbid_warn},

	    {"COMPOUNDMORESUFFIXES", &compound_more_suffixes},
	    {"CHECKCOMPOUNDDUP", &compound_check_duplicate},
	    {"CHECKCOMPOUNDREP", &compound_check_rep},
	    {"CHECKCOMPOUNDCASE", &compound_check_case},
	    {"CHECKCOMPOUNDTRIPLE", &compound_check_triple},
	    {"SIMPLIFIEDTRIPLE", &compound_simplified_triple},
	    {"SYLLABLENUM", &compound_syllable_num},

	    {"FULLSTRIP", &fullstrip},
	    {"CHECKSHARPS", &checksharps}};

	unordered_map<string, unsigned short*> command_shorts = {
	    {"MAXCPDSUGS", &max_compound_suggestions},
	    {"MAXNGRAMSUGS", &max_ngram_suggestions},
	    {"MAXDIFF", &max_diff_factor},

	    {"COMPOUNDMIN", &compound_min_length},
	    {"COMPOUNDWORDMAX", &compound_max_word_count}};

	unordered_map<string, vector<pair<string, string>>*> command_vec_pair =
	    {{"REP", &replacements},
	     {"PHONE", &phonetic_replacements},
	     {"ICONV", &input_conversion},
	     {"OCONV", &output_conversion}};

	unordered_map<string, char16_t*> command_flag = {
	    {"NOSUGGEST", &nosuggest_flag},
	    {"WARN", &warn_flag},

	    {"COMPOUNDFLAG", &compound_flag},
	    {"COMPOUNDBEGIN", &compound_begin_flag},
	    {"COMPOUNDEND", &compound_last_flag},
	    {"COMPOUNDMIDDLE", &compound_middle_flag},
	    {"ONLYINCOMPOUND", &compound_onlyin_flag},
	    {"COMPOUNDPERMITFLAG", &compound_permit_flag},
	    {"COMPOUNDFORBIDFLAG", &compound_forbid_flag},
	    {"COMPOUNDROOT", &compound_root_flag},
	    {"FORCEUCASE", &compound_force_uppercase},

	    {"CIRCUMFIX", &circumfix_flag},
	    {"FORBIDDENWORD", &forbiddenword_flag},
	    {"KEEPCASE", &keepcase_flag},
	    {"NEEDAFFIX", &need_affix_flag},
	    {"SUBSTANDARD", &substandard_flag}};

	// keeps count for each array-command
	auto cmd_with_vec_cnt = unordered_map<string, size_t>();
	auto cmd_affix = unordered_map<string, pair<bool, size_t>>();
	auto line = string();
	auto command = string();
	auto line_num = size_t(0);
	auto ss = istringstream();
	auto p = Aff_Line_IO_Manip(*this);
	Setlocale_To_C_In_Scope setlocale_to_C;
	auto error_happened = false;
	// while parsing, the streams must have plain ascii locale without
	// any special number separator otherwise istream >> int might fail
	// due to thousands separator.
	// "C" locale can be used assuming it is US-ASCII
	in.imbue(locale::classic());
	ss.imbue(locale::classic());
	strip_utf8_bom(in);
	while (getline(in, line)) {
		line_num++;
		ss.str(line);
		ss.clear();
		p.err = {};
		ss >> ws;
		if (ss.eof() || ss.peek() == '#') {
			continue; // skip comment or empty lines
		}
		ss >> command;
		to_upper_ascii(command);
		if (command == "SFX") {
			parse_affix(ss, p, command, suffixes, cmd_affix);
		}
		else if (command == "PFX") {
			parse_affix(ss, p, command, prefixes, cmd_affix);
		}
		else if (command_strings.count(command)) {
			auto& str = *command_strings[command];
			if (str.empty())
				ss >> p(str);
			else
				cerr << "Nuspell warning: "
				        "setting "
				     << command << " more than once, ignoring\n"
				     << "Nuspell warning in line " << line_num
				     << endl;
		}
		else if (command_bools.count(command)) {
			*command_bools[command] = true;
		}
		else if (command_shorts.count(command)) {
			auto ptr = command_shorts[command];
			ss >> *ptr;
			if (ptr == &compound_min_length && *ptr == 0)
				compound_min_length = 1;
			if (ptr == &max_diff_factor && max_diff_factor > 10)
				max_diff_factor = 5;
		}
		else if (command_flag.count(command)) {
			ss >> p(*command_flag[command]);
		}
		else if (command == "MAP") {
			parse_vector_of_T(ss, p, command, cmd_with_vec_cnt,
			                  map_related_chars);
		}
		else if (command_vec_pair.count(command)) {
			auto& vec = *command_vec_pair[command];
			parse_vector_of_T(ss, p, command, cmd_with_vec_cnt,
			                  vec);
		}
		else if (command == "SET") {
			if (encoding.empty()) {
				ss >> p(encoding);
			}
			else {
				cerr << "Nuspell warning: "
				        "setting "
				     << command << " more than once, ignoring\n"
				     << "Nuspell warning in line " << line_num
				     << endl;
			}
		}
		else if (command == "FLAG") {
			ss >> p(flag_type);
		}
		else if (command == "LANG") {
			ss >> p(icu_locale);
		}
		else if (command == "AF") {
			parse_vector_of_T(ss, p, command, cmd_with_vec_cnt,
			                  flag_aliases);
		}
		else if (command == "AM") {
			// parse_vector_of_T(ss, command, cmd_with_vec_cnt,
			//                  morphological_aliases);
		}
		else if (command == "BREAK") {
			parse_vector_of_T(ss, p, command, cmd_with_vec_cnt,
			                  break_patterns);
			break_exists = true;
		}
		else if (command == "CHECKCOMPOUNDPATTERN") {
			parse_vector_of_T(ss, p, command, cmd_with_vec_cnt,
			                  compound_patterns);
		}
		else if (command == "COMPOUNDRULE") {
			parse_vector_of_T(ss, p, command, cmd_with_vec_cnt,
			                  rules, wrap_compound_rule);
		}
		else if (command == "COMPOUNDSYLLABLE") {
			ss >> compound_syllable_max;
			ss >> p(compound_syllable_vowels);
		}
		else if (command == "WORDCHARS") {
			ss >> wordchars;
		}
		if (ss.fail()) {
			error_happened = true;
			cerr
			    << "Nuspell error: could not parse affix file line "
			    << line_num << ": " << line << endl;
			report_parsing_error(p.err, line_num);
		}
		else if (p.err != Parsing_Error_Code::NO_ERROR) {
			cerr
			    << "Nuspell warning: while parsing affix file line "
			    << line_num << ": " << line << endl;
			report_parsing_error(p.err, line_num);
		}
	}
	// default BREAK definition
	if (!break_exists) {
		break_patterns = {"-", "^-", "-$"};
	}
	for (auto& r : replacements) {
		auto& s = r.second;
		replace_ascii_char(s, '_', ' ');
	}

	// now fill data structures from temporary data
	compound_rules = std::move(rules);
	similarities.assign(begin(map_related_chars), end(map_related_chars));
	break_table = std::move(break_patterns);
	input_substr_replacer = std::move(input_conversion);
	output_substr_replacer = std::move(output_conversion);
	this->replacements = std::move(replacements);
	// phonetic_table = std::move(phonetic_replacements);
	for (auto& x : prefixes) {
		erase_chars(x.appending, ignored_chars);
	}
	for (auto& x : suffixes) {
		erase_chars(x.appending, ignored_chars);
	}
	this->prefixes = std::move(prefixes);
	this->suffixes = std::move(suffixes);

	cerr.flush();
	return in.eof() && !error_happened; // true for success
}

/**
 * @internal
 * @brief Scans @p line for morphological field [a-z][a-z]:
 *
 * Scans the line for space, two lowercase alphabetic chars and colon.
 *
 * @param line
 * @returns the end of the word before the morph field, or npos
 */
auto dic_find_end_of_word_heuristics(const string& line)
{
	if (line.size() < 4)
		return line.npos;
	size_t a = 0;
	for (;;) {
		a = line.find(' ', a);
		if (a == line.npos)
			break;
		auto b = line.find_first_not_of(' ', a);
		if (b == line.npos)
			break;
		if (b > line.size() - 3)
			break;
		if (line[b] >= 'a' && line[b] <= 'z' && line[b + 1] >= 'a' &&
		    line[b + 1] <= 'z' && line[b + 2] == ':')
			return a;
		a = b;
	}
	return line.npos;
}

auto Aff_Data::parse_dic(istream& in) -> bool
{
	size_t line_number = 1;
	size_t approximate_size;
	string line;
	string word;
	string flags_str;
	u16string flags;
	string u8word;
	auto enc_conv = Encoding_Converter(encoding.value_or_default());

	// locale must be without thousands separator.
	auto& ctype = use_facet<std::ctype<char>>(locale::classic());
	in.imbue(locale::classic());
	Setlocale_To_C_In_Scope setlocale_to_C;

	strip_utf8_bom(in);
	if (in >> approximate_size)
		words.reserve(approximate_size);
	else
		return false;
	getline(in, line);

	while (getline(in, line)) {
		line_number++;
		word.clear();
		flags_str.clear();
		flags.clear();
		if (!line.empty() && line.back() == '\r')
			line.pop_back();

		size_t slash_pos = 0;
		size_t tab_pos = 0;
		for (;;) {
			slash_pos = line.find('/', slash_pos);
			if (slash_pos == line.npos)
				break;
			if (slash_pos == 0)
				break;
			if (line[slash_pos - 1] != '\\')
				break;

			line.erase(slash_pos - 1, 1);
		}
		if (slash_pos != line.npos && slash_pos != 0) {
			// slash found, word until slash
			word.assign(line, 0, slash_pos);
			auto ptr = ctype.scan_is(ctype.space, &line[slash_pos],
			                         end_ptr(line));
			auto end_flags_pos = ptr - begin_ptr(line);
			flags_str.assign(line, slash_pos + 1,
			                 end_flags_pos - (slash_pos + 1));
			auto err = decode_flags_possible_alias(
			    flags_str, flag_type, encoding, flag_aliases,
			    flags);
			report_parsing_error(err, line_number);
			if (static_cast<int>(err) > 0)
				continue;
		}
		else if ((tab_pos = line.find('\t')) != line.npos) {
			// Tab found, word until tab. No flags.
			// After tab follow morphological fields
			word.assign(line, 0, tab_pos);
		}
		else {
			auto end = dic_find_end_of_word_heuristics(line);
			word.assign(line, 0, end);
		}
		if (word.empty())
			continue;
		auto ok = enc_conv.to_utf8(word, u8word);
		if (!ok)
			continue;
		erase_chars(u8word, ignored_chars);
		auto casing = classify_casing(u8word);
		auto inserted = words.emplace(u8word, flags);
		switch (casing) {
		case Casing::ALL_CAPITAL:
			if (flags.empty())
				break;
			[[fallthrough]];
		case Casing::PASCAL:
		case Casing::CAMEL: {
			// This if is needed for the test allcaps2.dic.
			// Maybe it can be solved better by not checking the
			// forbiddenword_flag, but by keeping the hidden
			// homonym last in the multimap among the same-key
			// entries.
			if (inserted->second.contains(forbiddenword_flag))
				break;
			to_title(u8word, icu_locale, u8word);
			flags += HIDDEN_HOMONYM_FLAG;
			words.emplace(u8word, flags);
			break;
		}
		default:
			break;
		}
	}
	return in.eof(); // success if we reached eof
}
} // namespace v5
} // namespace nuspell
