{
  description = "florisboard build environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
  }: let
    supportedSystems = with flake-utils.lib.system; [x86_64-linux x86_64-darwin aarch64-darwin];
    forAllSystems = f: nixpkgs.lib.genAttrs supportedSystems (system: f system);
    android = {
      versions = {
        tools = "26.1.1";
        platformTools = "33.0.3";
        buildTools = "31.0.0";
        ndk = "22.1.7171670";
        cmake = "3.18.1";
        emulator = "31.3.9";
      };
      platforms = ["32"];
    };
  in
    flake-utils.lib.eachSystem supportedSystems
    (
      system: let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            android_sdk.accept_license = true; # accept all of the sdk licenses
            allowUnfree = true; # needed to get android stuff to compile
          };
        };
        # Make the android enviornment we specify
        android-composition = pkgs.androidenv.composeAndroidPackages {
          toolsVersion = android.versions.tools;
          platformToolsVersion = android.versions.platformTools;
          buildToolsVersions = [android.versions.buildTools];
          platformVersions = android.platforms;
          cmakeVersions = [android.versions.cmake];
          includeNDK = true;
          ndkVersions = [android.versions.ndk];
          includeEmulator = true;
          emulatorVersion = android.versions.emulator;
        };
        android-sdk =
          (pkgs.androidenv.composeAndroidPackages {
            toolsVersion = android.versions.tools;
            platformToolsVersion = android.versions.platformTools;
            buildToolsVersions = [android.versions.buildTools];
            platformVersions = android.platforms;
            cmakeVersions = [android.versions.cmake];
            includeNDK = true;
            ndkVersions = [android.versions.ndk];
          })
          .androidsdk;
      in rec {
        devShells.default = pkgs.mkShell rec {
          ANDROID_SDK_ROOT = "${android-sdk}/libexec/android-sdk";
          ANDROID_NDK_ROOT = "${ANDROID_SDK_ROOT}/ndk-bundle";
          GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${ANDROID_SDK_ROOT}/build-tools/${android.versions.buildTools}/aapt2";
          JAVA_HOME = "${pkgs.jdk8.home}";
          nativeBulidInputs = with pkgs; [
            android-sdk
            jdk8
            clang
            kotlin-language-server
          ];

          # Use the same cmakeVersion here
          shellHook = ''
            export PATH="$(echo "$ANDROID_SDK_ROOT/cmake/${android.versions.cmake}".*/bin):$PATH"
          '';
        };
      }
    );
}
