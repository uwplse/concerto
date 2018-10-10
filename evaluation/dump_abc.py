import yaml, sys

with open(sys.argv[1], 'r') as f:
    res = yaml.load(f)

print ">> Combined Reports"
for l in res["combined"]["array"]["violations"]:
    print "*",l

print ">> Standard Reports"
for l in res["plain"]["array"]["violations"]:
    print "*",l
