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

#include "finder.hxx"
#include "utils.hxx"

#include <algorithm>
#include <array>
#include <iostream>
#include <iterator>
#include <sstream>
#include <unordered_set>
#include <utility>

#if !defined(_WIN32) &&                                                        \
    (defined(__unix__) || defined(__unix) ||                                   \
     (defined(__APPLE__) && defined(__MACH__)) || defined(__HAIKU__))
#include <unistd.h>
#ifdef _POSIX_VERSION
#include <dirent.h>
#include "glob_ndk/glob_ndk.h"
#include <sys/stat.h>
#include <sys/types.h>
#endif

#elif defined(_WIN32)

#include <io.h>
#include <windows.h>

#ifdef __MINGW32__
#include <dirent.h>
//#include <glob.h> //not present in mingw-w64. present in vanilla mingw
#include <sys/stat.h>
#include <sys/types.h>
#endif //__MINGW32__

#endif

using namespace std;

namespace nuspell {
inline namespace v5 {
#ifdef _WIN32
const auto PATHSEP = ';';
const auto DIRSEP = '\\';
#else
const auto PATHSEP = ':';
const auto DIRSEP = '/';
#endif

/**
 * @brief Append the paths of the default directories to be searched for
 * dictionaries.
 * @param paths vector of directory paths to append to
 */
auto append_default_dir_paths(std::vector<string>& paths) -> void
{
	auto dicpath = getenv("DICPATH");
	if (dicpath && *dicpath)
		split(dicpath, PATHSEP, paths);

#ifdef _POSIX_VERSION
	auto home = getenv("HOME");
	auto xdg_data_home = getenv("XDG_DATA_HOME");
	if (xdg_data_home && *xdg_data_home)
		paths.push_back(xdg_data_home + string("/hunspell"));
	else if (home)
		paths.push_back(home + string("/.local/share/hunspell"));

	auto xdg_data_dirs = getenv("XDG_DATA_DIRS");
	if (xdg_data_dirs && *xdg_data_dirs) {
		auto data_dirs = string_view(xdg_data_dirs);

		auto i = paths.size();
		split(data_dirs, PATHSEP, paths);
		for (; i != paths.size(); ++i)
			paths[i] += "/hunspell";

		i = paths.size();
		split(data_dirs, PATHSEP, paths);
		for (; i != paths.size(); ++i)
			paths[i] += "/myspell";
	}
	else {
		paths.push_back("/usr/local/share/hunspell");
		paths.push_back("/usr/share/hunspell");
		paths.push_back("/usr/local/share/myspell");
		paths.push_back("/usr/share/myspell");
	}
#if defined(__APPLE__) && defined(__MACH__)
	auto osx = string("/Library/Spelling");
	if (home) {
		paths.push_back(home + osx);
	}
	paths.push_back(osx);
#endif
#endif
#ifdef _WIN32
	auto winpaths = {getenv("LOCALAPPDATA"), getenv("PROGRAMDATA")};
	for (auto& p : winpaths) {
		if (p) {
			paths.push_back(string(p) + "\\hunspell");
		}
	}
#endif
}

#ifdef _WIN32
class FileListerWindows {
	struct _finddata_t data = {};
	intptr_t handle = -1;
	bool goodbit = false;

      public:
	FileListerWindows() {}
	FileListerWindows(const char* pattern) { first(pattern); }
	FileListerWindows(const string& pattern) { first(pattern); }
	FileListerWindows(const FileListerWindows& d) = delete;
	void operator=(const FileListerWindows& d) = delete;
	~FileListerWindows() { close(); }

	auto first(const char* pattern) -> bool
	{
		close();
		handle = _findfirst(pattern, &data);
		goodbit = handle != -1;
		return goodbit;
	}
	auto first(const string& pattern) -> bool
	{
		return first(pattern.c_str());
	}

	auto name() const -> const char* { return data.name; }
	auto good() const -> bool { return goodbit; }
	auto next() -> bool
	{
		goodbit = _findnext(handle, &data) == 0;
		return goodbit;
	}
	auto close() -> void
	{
		if (handle == -1)
			return;
		_findclose(handle);
		handle = -1;
		goodbit = false;
	}
	auto list_all() -> vector<string>
	{
		vector<string> ret;
		for (; good(); next()) {
			ret.push_back(name());
		}
		return ret;
	}
};
#endif

#ifdef _POSIX_VERSION
class Globber {
      private:
	glob_t g = {};
	int ret = 1;

      public:
	Globber(const char* pattern) { ret = ::glob(pattern, 0, nullptr, &g); }
	Globber(const string& pattern) : Globber(pattern.c_str()) {}
	Globber(const Globber&) = delete;
	auto operator=(const Globber&) = delete;
	auto glob(const char* pattern) -> bool
	{
		globfree(&g);
		ret = ::glob(pattern, 0, nullptr, &g);
		return ret == 0;
	}
	auto glob(const string& pattern) -> bool
	{
		return glob(pattern.c_str());
	}
	auto begin() -> const char* const* { return g.gl_pathv; }
	auto end() -> const char* const* { return begin() + g.gl_pathc; }
	auto append_glob_paths_to(vector<string>& out) -> void
	{
		if (ret == 0)
			out.insert(out.end(), begin(), end());
	}
	~Globber() { globfree(&g); }
};
#elif defined(_WIN32)
class Globber {
	vector<string> data;

      public:
	Globber(const char* pattern) { glob(pattern); }
	Globber(const string& pattern) { glob(pattern); }
	auto glob(const char* pattern) -> bool { return glob(string(pattern)); }
	auto glob(const string& pattern) -> bool
	{
		data.clear();

		if (pattern.empty())
			return false;
		auto first_two = pattern.substr(0, 2);
		if (first_two == "\\\\" || first_two == "//" ||
		    first_two == "\\/" || first_two == "//")
			return false;

		auto q1 = vector<string>();
		auto q2 = q1;
		auto v = q1;

		split_on_any_of(pattern, "\\/", v);
		auto i = v.begin();
		if (i == v.end())
			return false;

		FileListerWindows fl;

		if (i->find(':') != i->npos) {
			// absolute path
			q1.push_back(*i++);
		}
		else if (pattern[0] == '\\' || pattern[0] == '/') {
			// relative to drive
			q1.push_back("");
		}
		else {
			// relative
			q1.push_back(".");
		}
		for (; i != v.end(); ++i) {
			if (i->empty())
				continue;
			for (auto& q1e : q1) {
				auto p = q1e + DIRSEP + *i;
				// cout << "P " << p << endl;
				fl.first(p.c_str());
				for (; fl.good(); fl.next()) {

					if (fl.name() == string(".") ||
					    fl.name() == string(".."))
						continue;
					auto n = q1e + DIRSEP + fl.name();
					q2.push_back(n);
					// cout << "Q2 " << n << endl;
				}
			}
			q1.clear();
			q1.swap(q2);
		}
		data.insert(data.end(), q1.begin(), q1.end());
		return true;
	}
	auto begin() -> vector<string>::iterator { return data.begin(); }
	auto end() -> vector<string>::iterator { return data.end(); }
	auto append_glob_paths_to(vector<string>& out) -> void
	{
		out.insert(out.end(), begin(), end());
	}
};
#else
// unimplemented
struct Globber {
	Globber(const char* pattern) {}
	Globber(const string& pattern) {}
	auto glob(const char* pattern) -> bool { return false; }
	auto glob(const string& pattern) -> bool { return false; }
	auto begin() -> char** { return nullptr; }
	auto end() -> char** { return nullptr; }
	auto append_glob_paths_to(vector<string>& out) -> void {}
};
#endif

/**
 * @brief Append the paths of the LibreOffice's directories to be searched for
 * dictionaries.
 *
 * @warning This function shall not be called from LibreOffice or modules that
 * may end up being used by LibreOffice. It is mainly intended to be used by
 * the CLI tool.
 *
 * @param paths vector of directory paths to append to
 */
auto append_libreoffice_dir_paths(std::vector<std::string>& paths) -> void
{
	auto lo_user_glob = string();
#ifdef _POSIX_VERSION
	// add LibreOffice Linux global paths
	auto prefixes = {"/usr/local/lib/libreoffice", "/usr/lib/libreoffice",
	                 "/opt/libreoffice*"};
	for (auto& prefix : prefixes) {
		Globber g(string(prefix) + "/share/extensions/dict-*");
		g.append_glob_paths_to(paths);
	}

	// add LibreOffice Linux local

	auto home = getenv("HOME");
	if (home == nullptr)
		return;
	lo_user_glob = home;
	lo_user_glob += "/.config/libreoffice/?/user/uno_packages/cache"
	                "/uno_packages/*/*.oxt/";
#elif defined(_WIN32)
	// add Libreoffice Windows global paths
	auto prefixes = {getenv("PROGRAMFILES"), getenv("PROGRAMFILES(x86)")};
	for (auto& prefix : prefixes) {
		if (prefix == nullptr)
			continue;
		Globber g(string(prefix) +
		          "\\LibreOffice ?\\share\\extensions\\dict-*");
		g.append_glob_paths_to(paths);
	}

	auto home = getenv("APPDATA");
	if (home == nullptr)
		return;
	lo_user_glob = home;
	lo_user_glob += "\\libreoffice\\?\\user\\uno_packages\\cache"
	                "\\uno_packages\\*\\*.oxt\\";
#else
	return;
#endif
	// finish adding LibreOffice user path dicts (Linux and Windows)
	Globber g(lo_user_glob + "dict*");
	g.append_glob_paths_to(paths);

	g.glob(lo_user_glob + "*.aff");
	auto path_str = string();
	for (auto& path : g) {
		path_str = path;
		path_str.erase(path_str.rfind(DIRSEP));
		paths.push_back(path_str);
	}
}

#if defined(_POSIX_VERSION) || defined(__MINGW32__)
class Directory {
	DIR* dp = nullptr;
	struct dirent* ent_p = nullptr;

      public:
	Directory() = default;
	Directory(const Directory& d) = delete;
	void operator=(const Directory& d) = delete;
	auto open(const string& dirname) -> bool
	{
		close();
		dp = opendir(dirname.c_str());
		return dp;
	}
	auto next() -> bool { return (ent_p = readdir(dp)); }
	auto entry_name() const -> const char* { return ent_p->d_name; }
	auto close() -> void
	{
		if (dp) {
			(void)closedir(dp);
			dp = nullptr;
		}
	}
	~Directory() { close(); }
};
#elif defined(_WIN32)
class Directory {
	FileListerWindows fl;
	bool first = true;

      public:
	Directory() {}
	Directory(const Directory& d) = delete;
	void operator=(const Directory& d) = delete;
	auto open(const string& dirname) -> bool
	{
		fl.first(dirname + "\\*");
		first = true;
		return fl.good();
	}
	auto next() -> bool
	{
		if (first)
			first = false;
		else
			fl.next();
		return fl.good();
	}
	auto entry_name() const -> const char* { return fl.name(); }
	auto close() -> void { fl.close(); }
};
#else
struct Directory {
	Directory() {}
	Directory(const Directory& d) = delete;
	void operator=(const Directory& d) = delete;
	auto open(const string& dirname) -> bool { return false; }
	auto next() -> bool { return false; }
	auto entry_name() const -> const char* { return nullptr; }
	auto close() -> void {}
};
#endif

/**
 * @brief Search a directory for dictionaries.
 *
 * This function searches the directory for files that represent a dictionary
 * and for each one found it appends the pair of dictionary name and filepath to
 * dictionary, both without the filename extension (.aff or .dic).
 *
 * For example for the files /dict/dir/en_US.dic and /dict/dir/en_US.aff the
 * following pair will be appended ("en_US", "/dict/dir/en_US").
 *
 * @todo At some point this API should be made to be more strongly typed.
 * Instead of using that pair of strings to represent the dictionary files, a
 * new class should be created with three public functions, getters, that would
 * return the name, the path to the .aff file (with filename extension to avoid
 * confusions) and the path to the .dic file. The C++ 17 std::filesystem::path
 * should probably be used. It is unspecified to the public what this class
 * holds privately, but it should probably hold only one path to the aff file.
 * For the directory paths, it is simple, just use the type
 * std::filesystem::path. When this API is created, the same function names
 * should be used, added as overloads. The old API should be marked as
 * deprecated. This should be done when we start requiring GCC 9 which supports
 * C++ 17 filesystem out of the box. GCC 8 has this too, but it is somewhat
 * experimental and requires manually linking to additional static library.
 *
 * @param dir_path path to directory
 * @param dict_list vector to append the found dictionaries to
 */
auto search_dir_for_dicts(const string& dir_path,
                          vector<pair<string, string>>& dict_list) -> void
{
	Directory d;
	if (d.open(dir_path) == false)
		return;

	unordered_set<string> dics;
	string file_name;
	while (d.next()) {
		file_name = d.entry_name();
		auto sz = file_name.size();
		if (sz < 4)
			continue;

		if (file_name.compare(sz - 4, 4, ".dic") == 0) {
			dics.insert(file_name);
			file_name.replace(sz - 4, 4, ".aff");
		}
		else if (file_name.compare(sz - 4, 4, ".aff") == 0) {
			dics.insert(file_name);
			file_name.replace(sz - 4, 4, ".dic");
		}
		else {
			continue;
		}
		if (dics.count(file_name)) {
			file_name.erase(sz - 4);
			auto full_path = dir_path + DIRSEP + file_name;
			dict_list.emplace_back(move(file_name),
			                       move(full_path));
		}
	}
}

/**
 * @brief Search the directories for dictionaries.
 *
 * @see search_dir_for_dicts()
 *
 * @param dir_paths list of paths to directories
 * @param dict_list vector to append the found dictionaries to
 */
auto search_dirs_for_dicts(const std::vector<string>& dir_paths,
                           std::vector<std::pair<string, string>>& dict_list)
    -> void
{
	for (auto& p : dir_paths)
		search_dir_for_dicts(p, dict_list);
}

/**
 * @brief Search the default directories for dictionaries.
 *
 * @see append_default_dir_paths()
 * @see search_dirs_for_dicts()
 *
 * @param dict_list vector to append the found dictionaries to
 */
auto search_default_dirs_for_dicts(
    std::vector<std::pair<std::string, std::string>>& dict_list) -> void
{
	auto dir_paths = vector<string>();
	append_default_dir_paths(dir_paths);
	search_dirs_for_dicts(dir_paths, dict_list);
}

/**
 * @brief Find dictionary path given the name.
 *
 * Find the first dictionary whose name matches @p dict_name.
 *
 * @param dict_list vector of pairs with name and paths
 * @param dict_name dictionary name
 * @return iterator of @p dict_list that points to the found dictionary or end
 * if not found.
 */
auto find_dictionary(
    const std::vector<std::pair<std::string, std::string>>& dict_list,
    const std::string& dict_name)
    -> std::vector<std::pair<std::string, std::string>>::const_iterator
{
	return find_if(begin(dict_list), end(dict_list),
	               [&](auto& e) { return e.first == dict_name; });
}

Dict_Finder_For_CLI_Tool::Dict_Finder_For_CLI_Tool()
{
	append_default_dir_paths(dir_paths);
	append_libreoffice_dir_paths(dir_paths);
	dir_paths.push_back(".");
	search_dirs_for_dicts(dir_paths, dict_multimap);
	stable_sort(begin(dict_multimap), end(dict_multimap),
	            [](auto& a, auto& b) { return a.first < b.first; });
}

/**
 * @internal
 * @brief Gets the dictionary path.
 *
 * If path is given (contains slash) it returns the input argument,
 * otherwise searches the found dictionaries by their name and returns their
 * path.
 *
 * @param dict name or path of dictionary without the trailing .aff/.dic.
 * @return the path to dictionary or empty if does not exists.
 */
auto Dict_Finder_For_CLI_Tool::get_dictionary_path(
    const std::string& dict) const -> std::string
{
#ifdef _WIN32
	const auto SEPARATORS = "\\/";
#else
	const auto SEPARATORS = '/';
#endif
	// first check if it is a path
	if (dict.find_first_of(SEPARATORS) != dict.npos) {
		// a path
		return dict;
	}
	else {
		// search list
		auto x = find_dictionary(dict_multimap, dict);
		if (x != end(dict_multimap))
			return x->second;
	}
	return {};
}
} // namespace v5
} // namespace nuspell
