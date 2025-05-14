{
  description = "A Nix-flake-based Java development environment";

  inputs = {
    nixpkgs.url = "https://flakehub.com/f/NixOS/nixpkgs/0.1.*.tar.gz";
    gradle2nix.url = "github:tadfisher/gradle2nix/v2";
  };
  outputs =
    { self
    , nixpkgs
    , gradle2nix
    ,
    }:
    let
      javaVersion = 23; # Change this value to update the whole stack

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
      overlays.default = final: prev:
        let
          jdk = prev."jdk${toString javaVersion}";
        in
        {
          inherit jdk;
          gradle = prev.gradle.override { java = jdk; };
          gradle2nix = gradle2nix.builders.x86_64-linux;
        };

      packages = forEachSupportedSystem (
        { pkgs }:
        let
          project = (pkgs.gradle2nix.buildGradlePackage {
            pname = "fray";
            version = "0.4.4-SNAPSHOT";

            src = ./.;

            nativeBuildInputs = with pkgs; [
              jdk
              gradle
            ];

            lockFile = ./gradle.lock;
            gradleBuildFlags = [ "build" ];
          }).overrideAttrs {
            installPhase = ''
              runHook preInstall
              mkdir -p $out/lib
              cp --verbose core/build/libs/*.jar $out/lib
              runHook postInstall
            '';
          };
        in
        {
          default = project;
        }
      );

      devShells = forEachSupportedSystem ({ pkgs }: {
        default = pkgs.mkShell {
          packages = with pkgs;
            [
              gcc
              cmake
              gradle
              jdk
              jdk11
              jdk21
            ]
            ++ lib.optionals (pkgs.stdenv.isLinux) [
              jetbrains.jdk
            ];
          shellHook = ''
            export CC="${pkgs.gcc}/bin/gcc"
            export CXX="${pkgs.gcc}/bin/g++"
            export JDK11="${pkgs.jdk11.home}"
            export JDK21="${pkgs.jdk21.home}"
            export JRE="${pkgs.jdk.home}"
            export JAVA_HOME="${pkgs.jdk.home}"
            ${pkgs.lib.optionalString pkgs.stdenv.isLinux ''
              export JETBRAINS_JDK_HOME="${pkgs.jetbrains.jdk.home}"
            ''}
          '';
        };
      });
    };
}
