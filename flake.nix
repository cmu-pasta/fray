{
  description = "Fray Flake Configuration";

  inputs = {
    flake-compat.url = "https://flakehub.com/f/edolstra/flake-compat/1.tar.gz";
    nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1.*.tar.gz";
    gradle2nix.url = "github:tadfisher/gradle2nix/v2";
  };
  outputs =
    { self
    , nixpkgs
    , gradle2nix
    , flake-compat
    ,
    }:
    let
      supportedSystems = [ "x86_64-linux" "aarch64-darwin" ];
      forEachSupportedSystem = f:
        nixpkgs.lib.genAttrs supportedSystems (system:
          f {
            pkgs = import nixpkgs {
              inherit system;
              overlays = [ self.overlays.default ];
            };
          });
    in
    {
      overlays.default = final: prev: {
        jdk = prev.jdk23;
        java = prev.jdk23;
      };

      packages = forEachSupportedSystem (
        { pkgs }:
        let
          commonPackages = with pkgs; [
            gcc
            cmake
            jdk23
            jdk11
          ];

          commonEnv = ''
            export CC="${pkgs.gcc}/bin/gcc"
            export CXX="${pkgs.gcc}/bin/g++"
            export JDK11="${pkgs.jdk11.home}"
            export JRE="${pkgs.jdk23.home}"
            export JAVA_HOME="${pkgs.jdk23.home}"
            ${pkgs.lib.optionalString pkgs.stdenv.isLinux ''
              export JETBRAINS_JDK_HOME="${pkgs.jetbrains.jdk.home}"
            ''}
          '';

          project =
            (gradle2nix.builders.${pkgs.system}.buildGradlePackage {
              pname = "fray";
              version = "0.6.1-SNAPSHOT";
              src = ./.;
              lockFile = ./gradle.lock;
              gradleBuildFlags = [
                "-Porg.gradle.java.installations.paths=${pkgs.jdk11.home},${pkgs.jdk23.home}"
                "build"
                "-x"
                "test"
                "-x"
                "check"
              ];
              buildInputs = commonPackages;
              preBuild = ''
                ${commonEnv}
                sed -i '/include("plugins/d' settings.gradle.kts
                sed -i '/include("integration-test")/d' settings.gradle.kts
              '';
            }).overrideAttrs (_: prev: {
              gradleFlags = pkgs.lib.lists.remove "--console=plain" prev.gradleFlags;
              installPhase = ''
                runHook preInstall
                mkdir -p $out/libs
                mkdir -p $out/bin
                cp core/build/libs/*.jar $out/libs
                cp instrumentation/agent/build/libs/*.jar $out/libs
                cp -r instrumentation/jdk/build/java-inst $out/
                cp -r jvmti/build/native-libs $out/
                gradle --no-daemon -Pfray.installDir=$out/ genRunner
                cp bin/fray $out/bin/
                runHook postInstall
              '';
            });
        in
        {
          default = project;
        }
      );

      devShells = forEachSupportedSystem ({ pkgs }: {
        default = pkgs.mkShell {
          packages = (with pkgs; [
            gcc
            cmake
            jdk23
            jdk11
          ]) ++ pkgs.lib.optionals (pkgs.stdenv.isLinux) [
            pkgs.jetbrains.jdk
          ];
          shellHook = ''
            export CC="${pkgs.gcc}/bin/gcc"
            export CXX="${pkgs.gcc}/bin/g++"
            export JDK11="${pkgs.jdk11.home}"
            export JRE="${pkgs.jdk23.home}"
            export JAVA_HOME="${pkgs.jdk23.home}"
            ${pkgs.lib.optionalString pkgs.stdenv.isLinux ''
              export JETBRAINS_JDK_HOME="${pkgs.jetbrains.jdk.home}"
            ''}
          '';
        };
      });
    };
}
