import yaml, subprocess, os, sys, tempfile

this_dir = os.path.realpath(os.path.dirname(sys.argv[0]))

with open(sys.argv[1], 'r') as f:
    full_config = yaml.load(f)

del full_config["filters"]

with tempfile.NamedTemporaryFile() as f:
    yaml.dump(full_config, f)
    subprocess.check_call(["python", this_dir + "/generate_concerto_state.py", f.name])
