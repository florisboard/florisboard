with import <nixpkgs> {};

let
  cmakeVersion = "3.18.1";
  buildToolsVersion = "31.0.0" ;

  # Use cmakeVersion when you define androidComposition
  env = (pkgs.androidenv.override {
    licenseAccepted=true;
  });

  androidComposition = env.composeAndroidPackages {
    platformVersions = [ "31" ];
    toolsVersion = "26.1.1";
    buildToolsVersions = [ buildToolsVersion ];
    ndkVersions = ["22.1.7171670"];
    cmakeVersions = [ cmakeVersion ];
    includeNDK = true;
  };
in
pkgs.mkShell rec {
  ANDROID_SDK_ROOT = "${androidComposition.androidsdk}/libexec/android-sdk";
  ANDROID_NDK_ROOT = "${ANDROID_SDK_ROOT}/ndk-bundle";

  GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${ANDROID_SDK_ROOT}/build-tools/${buildToolsVersion }/aapt2";

  # Use the same cmakeVersion here
  shellHook = ''
    export PATH="$(echo "$ANDROID_SDK_ROOT/cmake/${cmakeVersion}".*/bin):$PATH"
  '';
}
