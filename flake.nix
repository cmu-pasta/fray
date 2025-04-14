{
  description = "A Nix-flake-based Java development environment";

  inputs = {
    nixpkgs.url = "github:aoli-al/nixpkgs/jcef-macos";
    gradle2nix.url = "github:tadfisher/gradle2nix/v2";
  };
  outputs = { self, nixpkgs, gradle2nix }:
    let
      javaVersion = 23; # Change this value to update the whole stack

      supportedSystems = [ "x86_64-linux" "aarch64-darwin" ];
      forEachSupportedSystem = f: nixpkgs.lib.genAttrs supportedSystems (system: f {
        pkgs = import nixpkgs { inherit system; overlays = [ self.overlays.default ];  config.allowUnfree = true; };
      });
    in
    {
      overlays.default =
        final: prev:
        let
          jdk = prev."jdk${toString javaVersion}";
        in
        {
          inherit jdk;
          gradle = prev.gradle.override { java = jdk; };
        };

      packages = forEachSupportedSystem ({ pkgs }:
        let
          project = pkgs.gradle2nix.buildGradlePackage {
          pname = "fray";
          version = "0.4.0";

          src = ./.;

          nativeBuildInputs = with pkgs; [
            jdk
            gradle
          ];

          deps = ./deps.json;
        };
        in
        {
          default = project;
        }
      );

      devShells = forEachSupportedSystem ({ pkgs }: {
        default = pkgs.mkShell {
          packages = with pkgs; [
            gcc
            cmake
            gradle
            jdk
            jdk11
            jdk21
            jetbrains.jdk
          ];
          shellHook = ''
            export JDK11="${pkgs.jdk11.home}"
            export JDK21="${pkgs.jdk21.home}"
            export JRE="${pkgs.jdk.home}"
            export JAVA_HOME="${pkgs.jdk.home}"
            export JETBRAINS_JDK_HOME="${pkgs.jetbrains.jdk.home}"
          '';
        };
      });
    };
}
