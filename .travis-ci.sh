set -ev

opam init --yes --no-setup --comp 4.03.0
eval $(opam config env)
opam install --yes ppx_deriving
