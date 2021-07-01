#ifndef NUSPELL_EXPORT_H
#define NUSPELL_EXPORT_H

#ifdef NUSPELL_STATIC_DEFINE
#  define NUSPELL_EXPORT
#elif defined(_WIN32) || defined(__CYGWIN__)
#  ifdef nuspell_EXPORTS // Define this only when building Nuspell as DLL on Windows, not when using the DLL.
#    define NUSPELL_EXPORT __declspec(dllexport)
#  else
#    define NUSPELL_EXPORT __declspec(dllimport)
#  endif
#elif __GNUC__ >= 4
#  define NUSPELL_EXPORT __attribute__((visibility("default")))
#else
#  define NUSPELL_EXPORT
#endif

#endif /* NUSPELL_EXPORT_H */
