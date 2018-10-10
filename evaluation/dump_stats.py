import yaml, sys, subprocess, os

this_dir = os.path.realpath(os.path.dirname(sys.argv[0]))
gen_table = os.path.join(this_dir, "generate_table.py")

out = subprocess.check_output(["python", gen_table ] +  sys.argv[1:])

name_map = {
    "iflow": "IFlow",
    "array": "ABC",
}

def detex(s):
    proc = subprocess.Popen(["detex", "-s", "-"], stdin = subprocess.PIPE, stdout = subprocess.PIPE)
    (out, err) = proc.communicate(s)
    assert proc.returncode == 0
    return out.strip()

mapping = {
    "ptaminrefl": "Standard PTA Max Reflective Callees",
    "ptamaxrefl": "Standard PTA Min Reflective Callees",
    "ptaplainalloc": "Standard PTA Alloced Types",
    "ptacombinedalloc": "Combined PTA Alloced Types",
    "concreteinvoke": "Max Combined Reflective Callees",
    "ptacombinedruntime": "Combined PTA Runtime (s)",
    "ptaplainruntime": "Standard PTA Runtime (s)"
}

for m in ["plain", "combined"]:
    for (k, v) in name_map.iteritems():
        if m == "plain":
            t = "Standard"
        else:
            t = "Combined"
        mapping["%s%sresults" % (k, m)] = "%s %s Reports" % (t, v)
        mapping["%s%sruntime" % (k, m)] = "%s %s Runtime (s)" % (t, v)


for l in out.split("\n"):
    if l[:5] != "\\def\\":
        continue
    rest = l[5:]
    i = rest.find("{")
    if i == -1:
        continue
    key = rest[:i]
    if key not in mapping:
        continue
    value_raw = rest[i+1:-1]
    value = detex(value_raw)
    print mapping[key] + ": " + value
