with import <nixpkgs> {};


stdenv.mkDerivation {
  name = "fray";
  buildInputs = with pkgs; [
    jdk21
  ];
}
