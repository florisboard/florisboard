add_library(nuspell
aff_data.cxx     aff_data.hxx
checker.cxx      checker.hxx
suggester.cxx    suggester.hxx
dictionary.cxx   dictionary.hxx
finder.cxx       finder.hxx
                 unicode.hxx
utils.cxx        utils.hxx
                 structures.hxx)

add_library(Nuspell::nuspell ALIAS nuspell)

#include(GenerateExportHeader)
#generate_export_header(nuspell)

#set(nuspell_headers aff_data.hxx checker.hxx suggester.hxx dictionary.hxx
#    finder.hxx structures.hxx unicode.hxx
#    ${CMAKE_CURRENT_BINARY_DIR}/nuspell_export.h)
#[[set_target_properties(nuspell PROPERTIES
    PUBLIC_HEADER "${nuspell_headers}"
    VERSION ${PROJECT_VERSION}
    SOVERSION ${PROJECT_VERSION_MAJOR}f
    CXX_VISIBILITY_PRESET hidden)]]

#target_compile_features(nuspell PUBLIC cxx_std_17)

#[[target_include_directories(nuspell
    PUBLIC $<BUILD_INTERFACE:${CMAKE_CURRENT_BINARY_DIR}>
    INTERFACE $<BUILD_INTERFACE:${PROJECT_SOURCE_DIR}/src>
              $<INSTALL_INTERFACE:${CMAKE_INSTALL_INCLUDEDIR}>)

target_link_libraries(nuspell PUBLIC ICU::uc ICU::data)

add_executable(nuspell-bin main.cxx)
set_target_properties(nuspell-bin PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY ../tools
    RUNTIME_OUTPUT_NAME nuspell)
target_compile_definitions(nuspell-bin PRIVATE
    PROJECT_VERSION=\"${PROJECT_VERSION}\")
target_link_libraries(nuspell-bin nuspell)
if (BUILD_SHARED_LIBS AND WIN32)
    # This should be PRE_LINK (or PRE_BUILD), so Vcpkg's POST_BUILD
    # step (see VCPKG_APPLOCAL_DEPS) that copies dll can pick up nuspell.dll
    # inside the folder ../tools.
    add_custom_command(TARGET nuspell-bin PRE_LINK
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
            $<TARGET_FILE:nuspell> $<TARGET_FILE_DIR:nuspell-bin>)
endif()

if (NOT subproject)
    install(TARGETS nuspell
        EXPORT NuspellTargets
        ARCHIVE DESTINATION ${CMAKE_INSTALL_LIBDIR}
        LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
        RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
        PUBLIC_HEADER DESTINATION ${CMAKE_INSTALL_INCLUDEDIR}/nuspell)
    install(EXPORT NuspellTargets
        DESTINATION ${CMAKE_INSTALL_LIBDIR}/cmake/nuspell
        NAMESPACE Nuspell::)
    install(TARGETS nuspell-bin DESTINATION ${CMAKE_INSTALL_BINDIR})
endif()]]
