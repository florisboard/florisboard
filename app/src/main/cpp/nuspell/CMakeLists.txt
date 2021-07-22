add_library(nuspell
aff_data.cxx     aff_data.hxx
checker.cxx      checker.hxx
suggester.cxx    suggester.hxx
dictionary.cxx   dictionary.hxx
                 unicode.hxx
utils.cxx        utils.hxx
                 structures.hxx)

add_library(Nuspell::nuspell ALIAS nuspell)
