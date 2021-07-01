/* Copyright 2016-2021 Dimitrij Mijoski, Sander van Geloven
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
#include "finder.hxx"

#include <cassert>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <unicode/brkiter.h>
#include <unicode/ucnv.h>

#if defined(__MINGW32__) || defined(__unix__) || defined(__unix) ||            \
    (defined(__APPLE__) && defined(__MACH__)) || defined(__HAIKU__)
#include <getopt.h>
#include <unistd.h>
#endif
#ifdef _POSIX_VERSION
#include <langinfo.h>
#endif
#ifdef _WIN32
#include <io.h>
#define NOMINMAX
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#endif

// manually define if not supplied by the build system
#ifndef PROJECT_VERSION
#define PROJECT_VERSION "unknown.version"
#endif
#define PACKAGE_STRING "nuspell " PROJECT_VERSION

using namespace std;
using namespace nuspell;

enum Mode {
	DEFAULT_MODE /**< printing correct and misspelled words with
	                suggestions */
	,
	MISSPELLED_WORDS_MODE /**< printing only misspelled words */,
	MISSPELLED_LINES_MODE /**< printing only lines with misspelled word(s)*/
	,
	CORRECT_WORDS_MODE /**< printing only correct words */,
	CORRECT_LINES_MODE /**< printing only fully correct lines */,
	LINES_MODE, /**< intermediate mode used while parsing command line
	               arguments, otherwise unused */
	LIST_DICTIONARIES_MODE /**< printing available dictionaries */,
	HELP_MODE /**< printing help information */,
	VERSION_MODE /**< printing version information */,
	ERROR_MODE
};

struct Args_t {
	Mode mode = DEFAULT_MODE;
	bool whitespace_segmentation = false;
	string program_name = "nuspell";
	string dictionary;
	string encoding;
	vector<string> other_dicts;
	vector<string> files;

	Args_t() = default;
	Args_t(int argc, char* argv[]) { parse_args(argc, argv); }
	auto parse_args(int argc, char* argv[]) -> void;
};

/**
 * @brief Parses command line arguments.
 *
 * @param argc command-line argument count.
 * @param argv command-line argument vector.
 */
auto Args_t::parse_args(int argc, char* argv[]) -> void
{
	if (argc != 0 && argv[0] && argv[0][0] != '\0')
		program_name = argv[0];
// See POSIX Utility argument syntax
// http://pubs.opengroup.org/onlinepubs/9699919799/basedefs/V1_chap12.html
#if defined(_POSIX_VERSION) || defined(__MINGW32__)
	int c;
	// The program can run in various modes depending on the
	// command line options. mode is FSM state, this while loop is FSM.
	const char* shortopts = ":d:i:aDGLslhv";
	const struct option longopts[] = {
	    {"version", 0, nullptr, 'v'},
	    {"help", 0, nullptr, 'h'},
	    {nullptr, 0, nullptr, 0},
	};
	while ((c = getopt_long(argc, argv, shortopts, longopts, nullptr)) !=
	       -1) {
		switch (c) {
		case 'a':
			// ispell pipe mode, same as default mode
			if (mode != DEFAULT_MODE)
				mode = ERROR_MODE;
			break;
		case 'd':
			if (dictionary.empty())
				dictionary = optarg;
			else
				cerr << "WARNING: Detected not yet supported "
				        "other dictionary "
				     << optarg << '\n';
			other_dicts.emplace_back(optarg);

			break;
		case 'i':
			encoding = optarg;

			break;
		case 'D':
			if (mode == DEFAULT_MODE)
				mode = LIST_DICTIONARIES_MODE;
			else
				mode = ERROR_MODE;

			break;
		case 'G':
			if (mode == DEFAULT_MODE)
				mode = CORRECT_WORDS_MODE;
			else if (mode == LINES_MODE)
				mode = CORRECT_LINES_MODE;
			else
				mode = ERROR_MODE;

			break;
		case 'l':
			if (mode == DEFAULT_MODE)
				mode = MISSPELLED_WORDS_MODE;
			else if (mode == LINES_MODE)
				mode = MISSPELLED_LINES_MODE;
			else
				mode = ERROR_MODE;

			break;
		case 'L':
			if (mode == DEFAULT_MODE)
				mode = LINES_MODE;
			else if (mode == MISSPELLED_WORDS_MODE)
				mode = MISSPELLED_LINES_MODE;
			else if (mode == CORRECT_WORDS_MODE)
				mode = CORRECT_LINES_MODE;
			else
				mode = ERROR_MODE;

			break;
		case 's':
			whitespace_segmentation = true;

			break;
		case 'h':
			if (mode == DEFAULT_MODE)
				mode = HELP_MODE;
			else
				mode = ERROR_MODE;

			break;
		case 'v':
			if (mode == DEFAULT_MODE)
				mode = VERSION_MODE;
			else
				mode = ERROR_MODE;

			break;
		case ':':
			cerr << "Option -" << static_cast<char>(optopt)
			     << " requires an operand\n";
			mode = ERROR_MODE;

			break;
		case '?':
			cerr << "Unrecognized option: '-"
			     << static_cast<char>(optopt) << "'\n";
			mode = ERROR_MODE;

			break;
		}
	}
	files.insert(files.end(), argv + optind, argv + argc);
	if (mode == LINES_MODE) {
		// in v1 this defaults to MISSPELLED_LINES_MODE
		// we will make it error here
		mode = ERROR_MODE;
	}
#endif
}

/**
 * @brief Prints help information to standard output.
 *
 * @param program_name pass argv[0] here.
 */
auto print_help(const string& program_name) -> void
{
	auto& p = program_name;
	auto& o = cout;
	o << "Usage:\n"
	     "\n";
	o << p << " [-s] [-d dict_NAME] [-i enc] [file_name]...\n";
	o << p << " -l|-G [-L] [-s] [-d dict_NAME] [-i enc] [file_name]...\n";
	o << p << " -D|-h|--help|-v|--version\n";
	o << "\n"
	     "Check spelling of each FILE. Without FILE, check standard "
	     "input.\n"
	     "\n"
	     "  -d di_CT      use di_CT dictionary. Only one dictionary at a\n"
	     "                time is currently supported\n"
	     "  -D            print search paths and available dictionaries\n"
	     "                and exit\n"
	     "  -i enc        input/output encoding, default is active locale\n"
	     "  -l            print only misspelled words or lines\n"
	     "  -G            print only correct words or lines\n"
	     "  -L            lines mode\n"
	     "  -s            use simple whitespace text segmentation to\n"
	     "                extract words instead of the default Unicode\n"
	     "                text segmentation. It is not recommended to use\n"
	     "                this.\n"
	     "  -h, --help    print this help and exit\n"
	     "  -v, --version print version number and exit\n"
	     "\n";
	o << "Example: " << p << " -d en_US file.txt\n";
	o << "\n"
	     "Bug reports: <https://github.com/nuspell/nuspell/issues>\n"
	     "Full documentation: "
	     "<https://github.com/nuspell/nuspell/wiki>\n"
	     "Home page: <http://nuspell.github.io/>\n";
}

/**
 * @brief Prints the version number to standard output.
 */
auto print_version() -> void
{
	cout << PACKAGE_STRING
	    "\n"
	    "Copyright (C) 2016-2021 Dimitrij Mijoski and Sander van Geloven\n"
	    "License LGPLv3+: GNU LGPL version 3 or later "
	    "<http://gnu.org/licenses/lgpl.html>.\n"
	    "This is free software: you are free to change and "
	    "redistribute it.\n"
	    "There is NO WARRANTY, to the extent permitted by law.\n"
	    "\n"
	    "Written by Dimitrij Mijoski and Sander van Geloven.\n";
}

/**
 * @brief Lists dictionary paths and available dictionaries.
 *
 * @param f a finder for search paths and located dictionary.
 */
auto list_dictionaries(const Dict_Finder_For_CLI_Tool& f) -> void
{
	if (f.get_dir_paths().empty()) {
		cout << "No search paths available" << '\n';
	}
	else {
		cout << "Search paths:" << '\n';
		for (auto& p : f.get_dir_paths()) {
			cout << p << '\n';
		}
	}

	// Even if no search paths are available, still report on available
	// dictionaries.
	if (f.get_dictionaries().empty()) {
		cout << "No dictionaries available\n";
	}
	else {
		cout << "Available dictionaries:\n";
		for (auto& d : f.get_dictionaries()) {
			cout << left << setw(15) << d.first << ' ' << d.second
			     << '\n';
		}
	}
}

auto to_utf8(string_view source, string& dest, UConverter* ucnv,
             UErrorCode& uerr)
{
	dest.resize(dest.capacity());
	auto len = ucnv_toAlgorithmic(UCNV_UTF8, ucnv, dest.data(), dest.size(),
	                              source.data(), source.size(), &uerr);
	dest.resize(len);
	if (uerr == U_BUFFER_OVERFLOW_ERROR) {
		uerr = U_ZERO_ERROR;
		ucnv_toAlgorithmic(UCNV_UTF8, ucnv, dest.data(), dest.size(),
		                   source.data(), source.size(), &uerr);
	}
}

auto from_utf8(string_view source, string& dest, UConverter* ucnv,
               UErrorCode& uerr)
{
	dest.resize(dest.capacity());
	auto len =
	    ucnv_fromAlgorithmic(ucnv, UCNV_UTF8, dest.data(), dest.size(),
	                         source.data(), source.size(), &uerr);
	dest.resize(len);
	if (uerr == U_BUFFER_OVERFLOW_ERROR) {
		uerr = U_ZERO_ERROR;
		ucnv_fromAlgorithmic(ucnv, UCNV_UTF8, dest.data(), dest.size(),
		                     source.data(), source.size(), &uerr);
	}
}

auto to_unicode_string(string_view source, icu::UnicodeString& dest,
                       UConverter* ucnv, UErrorCode& uerr)
{
	auto buf = dest.getBuffer(-1);
	auto len = ucnv_toUChars(ucnv, buf, dest.getCapacity(), source.data(),
	                         source.size(), &uerr);
	if (uerr == U_BUFFER_OVERFLOW_ERROR) {
		uerr = U_ZERO_ERROR;
		dest.releaseBuffer(0);
		buf = dest.getBuffer(len);
		if (!buf)
			throw bad_alloc();
		len = ucnv_toUChars(ucnv, buf, dest.getCapacity(),
		                    source.data(), source.size(), &uerr);
	}
	dest.releaseBuffer(len);
}

auto process_word(Mode mode, const Dictionary& dic, string_view word,
                  size_t pos_word, vector<string_view>& wrong_words,
                  vector<string>& suggestions, ostream& out)
{
	auto correct = dic.spell(word);
	switch (mode) {
	case DEFAULT_MODE: {
		if (correct) {
			out << "*\n";
			break;
		}
		dic.suggest(word, suggestions);
		if (suggestions.empty()) {
			out << "# " << word << ' ' << pos_word << '\n';
			break;
		}
		out << "& " << word << ' ' << suggestions.size() << ' '
		    << pos_word << ": ";
		out << suggestions[0];
		for_each(begin(suggestions) + 1, end(suggestions),
		         [&](auto& sug) { out << ", " << sug; });
		out << '\n';
		break;
	}
	case MISSPELLED_WORDS_MODE:
		if (!correct)
			out << word << '\n';
		break;
	case CORRECT_WORDS_MODE:
		if (correct)
			out << word << '\n';
		break;
	case MISSPELLED_LINES_MODE:
	case CORRECT_LINES_MODE:
		if (!correct)
			wrong_words.push_back(word);
		break;
	default:
		break;
	}
}

auto process_word_other_encoding(Mode mode, const Dictionary& dic,
                                 string_view word, string_view u8word,
                                 size_t pos_word,
                                 vector<string_view>& wrong_words,
                                 vector<string>& suggestions, ostream& out,
                                 UConverter* ucnv, UErrorCode& uerr)
{
	auto correct = dic.spell(u8word);
	switch (mode) {
	case DEFAULT_MODE: {
		if (correct) {
			out << "*\n";
			break;
		}
		dic.suggest(u8word, suggestions);
		if (suggestions.empty()) {
			out << "# " << word << ' ' << pos_word << '\n';
			break;
		}
		out << "& " << word << ' ' << suggestions.size() << ' '
		    << pos_word << ": ";
		auto sug_in_encoding = string();
		from_utf8(suggestions[0], sug_in_encoding, ucnv, uerr);
		out << sug_in_encoding;
		for_each(begin(suggestions) + 1, end(suggestions),
		         [&](const string& u8sug) {
			         out << ", ";
			         from_utf8(u8sug, sug_in_encoding, ucnv, uerr);
			         out << sug_in_encoding;
		         });
		out << '\n';
		break;
	}
	case MISSPELLED_WORDS_MODE:
		if (!correct)
			out << word << '\n';
		break;
	case CORRECT_WORDS_MODE:
		if (correct)
			out << word << '\n';
		break;
	case MISSPELLED_LINES_MODE:
	case CORRECT_LINES_MODE:
		if (!correct)
			wrong_words.push_back(word);
		break;
	default:
		break;
	}
}

auto finish_line(Mode mode, const string& line,
                 const vector<string_view>& wrong_words, ostream& out)
{
	switch (mode) {
	case DEFAULT_MODE:
		out << '\n';
		break;
	case MISSPELLED_LINES_MODE:
		if (!wrong_words.empty())
			out << line << '\n';
		break;
	case CORRECT_LINES_MODE:
		if (wrong_words.empty())
			out << line << '\n';
		break;
	default:
		break;
	}
}

auto whitespace_segmentation_loop(istream& in, ostream& out,
                                  const Dictionary& dic, Mode mode,
                                  UConverter* ucnv, UErrorCode& uerr)
{
	auto line = string();
	auto suggestions = vector<string>();
	auto wrong_words = vector<string_view>();
	auto loc = in.getloc();
	auto& facet = use_facet<ctype<char>>(loc);
	auto isspace = [&](char c) { return facet.is(facet.space, c); };
	auto u8word = string();
	auto is_utf8 = ucnv_getType(ucnv) == UCNV_UTF8;

	while (getline(in, line)) {
		wrong_words.clear();
		for (auto a = begin(line); a != end(line);) {
			a = find_if_not(a, end(line), isspace);
			if (a == end(line))
				break;
			auto b = find_if(a, end(line), isspace);
			auto word = string_view(&*a, distance(a, b));
			auto pos_word = distance(begin(line), a);
			if (is_utf8) {
				process_word(mode, dic, word, pos_word,
				             wrong_words, suggestions, out);
			}
			else {
				to_utf8(word, u8word, ucnv, uerr);
				process_word_other_encoding(
				    mode, dic, word, u8word, pos_word,
				    wrong_words, suggestions, out, ucnv, uerr);
			}
			a = b;
		}
		finish_line(mode, line, wrong_words, out);
	}
}

auto is_word_break(int32_t typ)
{
	return (UBRK_WORD_NUMBER <= typ && typ < UBRK_WORD_NUMBER_LIMIT) ||
	       (UBRK_WORD_LETTER <= typ && typ < UBRK_WORD_LETTER_LIMIT) ||
	       (UBRK_WORD_KANA <= typ && typ < UBRK_WORD_KANA_LIMIT) ||
	       (UBRK_WORD_IDEO <= typ && typ < UBRK_WORD_IDEO_LIMIT);
}

auto segment_line_utf8(Mode mode, const Dictionary& dic, const string& line,
                       UText* utext, icu::BreakIterator* ubrkiter,
                       UErrorCode& uerr, vector<string>& suggestions,
                       vector<string_view>& wrong_words, ostream& out)
{
	utext_openUTF8(utext, line.data(), line.size(), &uerr);
	ubrkiter->setText(utext, uerr);
	for (auto i = ubrkiter->first(), prev = 0; i != ubrkiter->DONE;
	     prev = i, i = ubrkiter->next()) {
		auto typ = ubrkiter->getRuleStatus();
		if (is_word_break(typ)) {
			auto word = string_view(line).substr(prev, i - prev);
			process_word(mode, dic, word, prev, wrong_words,
			             suggestions, out);
		}
	}
	finish_line(mode, line, wrong_words, out);
	assert(U_SUCCESS(uerr));
}

auto segment_line_generic(Mode mode, const Dictionary& dic, const string& line,
                          icu::UnicodeString& uline, UConverter* ucnv,
                          icu::BreakIterator* ubrkiter, UErrorCode& uerr,
                          string& u8word, vector<string>& suggestions,
                          vector<string_view>& wrong_words, ostream& out)
{
	to_unicode_string(line, uline, ucnv, uerr);
	ubrkiter->setText(uline);
	size_t orig_prev = 0, orig_i = 0;
	auto src = line.c_str();
	auto src_end = src + line.size();

	ucnv_resetToUnicode(ucnv);
	for (auto i = ubrkiter->first(), prev = 0; i != ubrkiter->DONE;
	     prev = i, i = ubrkiter->next(), orig_prev = orig_i) {

		for (auto j = prev; j != i; ++j) {
			auto cp = ucnv_getNextUChar(ucnv, &src, src_end, &uerr);

			// U_IS_SURROGATE(uline[j]) or
			// U_IS_LEAD(uline[j]) can work too
			j += !U_IS_BMP(cp);
		}
		orig_i = distance(line.c_str(), src);

		auto typ = ubrkiter->getRuleStatus();
		if (is_word_break(typ)) {
			auto uword = uline.tempSubStringBetween(prev, i);
			u8word.clear();
			uword.toUTF8String(u8word);
			auto word = string_view(line).substr(
			    orig_prev, orig_i - orig_prev);
			process_word_other_encoding(
			    mode, dic, word, u8word, orig_prev, wrong_words,
			    suggestions, out, ucnv, uerr);
		}
	}
	finish_line(mode, line, wrong_words, out);
	assert(U_SUCCESS(uerr));
}

auto unicode_segentation_loop(istream& in, ostream& out, const Dictionary& dic,
                              Mode mode, UConverter* ucnv, UErrorCode& uerr)
{
	auto line = string();
	auto suggestions = vector<string>();
	auto wrong_words = vector<string_view>();

	// TODO: try to use Locale constructed from dictionary name.
	auto ubrkiter = unique_ptr<icu::BreakIterator>(
	    icu::BreakIterator::createWordInstance(icu::Locale(), uerr));
	auto utext = icu::LocalUTextPointer(
	    utext_openUTF8(nullptr, line.data(), line.size(), &uerr));
	auto uline = icu::UnicodeString();
	auto u8word = string();
	auto is_utf8 = ucnv_getType(ucnv) == UCNV_UTF8;

	while (getline(in, line)) {
		wrong_words.clear();
		if (is_utf8)
			segment_line_utf8(mode, dic, line, utext.getAlias(),
			                  ubrkiter.get(), uerr, suggestions,
			                  wrong_words, out);
		else
			segment_line_generic(mode, dic, line, uline, ucnv,
			                     ubrkiter.get(), uerr, u8word,
			                     suggestions, wrong_words, out);
	}
}

int main(int argc, char* argv[])
{
	// May speed up I/O. After this, don't use C printf, scanf etc.
	ios_base::sync_with_stdio(false);

	auto args = Args_t(argc, argv);
	switch (args.mode) {
	case HELP_MODE:
		print_help(args.program_name);
		return 0;
	case VERSION_MODE:
		print_version();
		return 0;
	case ERROR_MODE:
		cerr << "Invalid (combination of) arguments, try '"
		     << args.program_name << " --help' for more information\n";
		return 1;
	default:
		break;
	}
	auto f = Dict_Finder_For_CLI_Tool();
	if (args.mode == LIST_DICTIONARIES_MODE) {
		list_dictionaries(f);
		return 0;
	}
	char* loc_str = nullptr;
#ifdef _WIN32
	loc_str = setlocale(LC_CTYPE, nullptr); // will return "C"

	/* On Windows, the console is a buggy thing. If the default C locale is
	active, then the encoding of the strings gotten from C or C++ stdio
	(fgets, scanf, cin) is GetConsoleCP(). Stdout accessed via standard
	functions (printf, cout) expects encoding of GetConsoleOutputCP() which
	is the same as GetConsoleCP() unless manually changed. By default both
	are the active OEM encoding, unless changed with the command chcp, or by
	calling the Set functions.

	If we call setlocale(LC_CTYPE, ""), or let's say setlocale(LC_CTYPE,
	".1251"), then stdin will still return in the encoding GetConsoleCP(),
	but stdout functions like printf now will expect a different encoding,
	the one set via setlocale. Because of this mess don't change locale with
	setlocale on Windows.

	When stdin or stout are redirected from/to file or another terminal like
	the one in MSYS2, they are read/written as-is. Then we will assume UTF-8
	encoding. */
#else
	loc_str = setlocale(LC_CTYPE, "");
	if (!loc_str) {
		clog << "WARNING: Invalid locale string, fall back to \"C\".\n";
		loc_str = setlocale(LC_CTYPE, nullptr); // will return "C"
	}
#endif
	auto loc_str_sv = string_view(loc_str);
	if (args.encoding.empty()) {
#if _POSIX_VERSION
		auto enc_str = nl_langinfo(CODESET);
		args.encoding = enc_str;
#elif _WIN32
		if (_isatty(_fileno(stdin)) || _isatty(_fileno(stdout)))
			args.encoding = "cp" + to_string(GetConsoleCP());
		else
			args.encoding = "UTF-8";
#endif
	}
	clog << "INFO: Locale LC_CTYPE=" << loc_str_sv
	     << ", Used encoding=" << args.encoding << '\n';
	if (args.dictionary.empty()) {
		// infer dictionary from locale
		auto idx = min(loc_str_sv.find('.'), loc_str_sv.find('@'));
		args.dictionary = loc_str_sv.substr(0, idx);
	}
	if (args.dictionary.empty()) {
		cerr << "No dictionary provided and can not infer from OS "
		        "locale\n";
	}
	auto filename = f.get_dictionary_path(args.dictionary);
	if (filename.empty()) {
		cerr << "Dictionary " << args.dictionary << " not found\n";
		return 1;
	}
	clog << "INFO: Pointed dictionary " << filename << ".{dic,aff}\n";
	auto dic = Dictionary();
	try {
		dic = Dictionary::load_from_path(filename);
	}
	catch (const Dictionary_Loading_Error& e) {
		cerr << e.what() << '\n';
		return 1;
	}
	// ICU reports all types of errors, logic errors and runtime errors
	// using this enum. We should not check for logic errors, they should
	// not happend. Optionally, only assert that they are not there can be
	// used. We should check for runtime errors.
	// The encoding conversion is a common case where runtime error can
	// happen, but by default ICU uses Unicode replacement character on
	// errors and reprots success. This can be changed, but there is no need
	// for that.
	auto uerr = U_ZERO_ERROR;
	auto enc_cstr = args.encoding.c_str();
	if (args.encoding.empty()) {
		enc_cstr = nullptr;
		clog << "WARNING: using default ICU encoding converter for IO"
		     << endl;
	}
	auto ucnv = icu::LocalUConverterPointer(ucnv_open(enc_cstr, &uerr));
	if (U_FAILURE(uerr)) {
		cerr << "ERROR: Invalid encoding " << args.encoding << ".\n";
		return 1;
	}
	auto loop_function = unicode_segentation_loop;
	if (args.whitespace_segmentation)
		loop_function = whitespace_segmentation_loop;
	if (args.files.empty()) {
		loop_function(cin, cout, dic, args.mode, ucnv.getAlias(), uerr);
	}
	else {
		for (auto& file_name : args.files) {
			ifstream in(file_name);
			if (!in.is_open()) {
				cerr << "Can't open " << file_name << '\n';
				return 1;
			}
			loop_function(in, cout, dic, args.mode, ucnv.getAlias(),
			              uerr);
		}
	}
	return 0;
}
