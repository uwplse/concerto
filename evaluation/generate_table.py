import yaml, sys, subprocess
from pyparsing import *
from math import ceil

non_paren = CharsNotIn("()")
paren = Forward()
paren <<= (Literal("(") + ZeroOrMore(paren) + Literal(")")) ^ non_paren
paren_seq = delimitedList(Group(paren))

context_grammar = Literal("Seq(").suppress() + paren_seq + (Literal(")),") | Literal("),")).suppress() + Group(ZeroOrMore(paren))

def parse_concrete_context(v):
    k = v.find("Seq(")
    if k == -1:
        return None
    res = context_grammar.parseString(v[k:])
    ctxt = "".join(res[-2])
    unit = "".join(res[-1])
    return (ctxt, unit)

GRAPH_INIT_SIG = "<meta.framework.ObjectGraph: void init()>"
ALLOC_TYPE_UNIT = "staticinvoke <intr.Intrinsics: java.lang.Object allocateType(int)>"

def get_reflective_invoke_stats(plain_cg_file, combined_cg_file):
    with open(plain_cg_file, 'r') as f:
        plain_cg = yaml.load(f)
    callee_counts = []
    for (k, v) in plain_cg["nodes"].iteritems():
        split = v.find(">,")
        ctxt = v[1:split + 1]
        unit = v[split + 2:-1]
        if ("CallObj" in ctxt or "CallInt" in ctxt) and unit.startswith('staticinvoke'):
            callee_counts.append(len(plain_cg["callees"][k]))
        if GRAPH_INIT_SIG == ctxt and unit.startswith(ALLOC_TYPE_UNIT):
            plain_alloc_counts = len(plain_cg["callees"][k])
    with open(combined_cg_file, 'r') as f:
        combined_cg = yaml.load(f)
    for (k, v) in combined_cg["nodes"].iteritems():
        parse = parse_concrete_context(v)
        if parse is None:
            continue
        (ctxt, unit) = parse
        if GRAPH_INIT_SIG in ctxt and unit.startswith(ALLOC_TYPE_UNIT):
            combined_alloc_counts = len(combined_cg["callees"][k])
    return (min(callee_counts), max(callee_counts), plain_alloc_counts, combined_alloc_counts, combined_cg["max-concrete-invoke"])

def dump_results(mode, data):
    for analysis in ["array", "iflow", "pta"]:
        analysis_stats = data[analysis]
        
        if analysis_stats["runtime"] == -1:
            runtime_string = r'{\footnotesize t/o}'
        else:
            runtime_string = "%.1f s" % (analysis_stats["runtime"] / 1000.0)
        print r'\def\%s%sruntime{%s}' % (analysis, mode, runtime_string)
        if analysis != "pta":
            print r'\def\%s%sresults{%d}' % (analysis, mode, analysis_stats["results"])

def main(args):
    with open(args[1], 'r') as r:
        eval_data = yaml.load(r)
    for mode in ["plain", "combined"]:
        dump_results(mode, eval_data[mode])
    for analysis in ["array", "iflow", "pta"]:
        cg_stats = eval_data["plain"][analysis]["call-graph"]
        print r'\def\%splainnodes{%d}' % (analysis, cg_stats[0])

    (min_callee, max_callee, plain_alloc, combined_alloc, max_concrete_invoke) = get_reflective_invoke_stats(args[2], args[3])
    print r'\def\ptaslowdown{%d}' % (eval_data["plain"]["pta"]["runtime"] / eval_data["combined"]["pta"]["runtime"])
    print r'\def\ptaminrefl{%d}' % min_callee
    print r'\def\ptamaxrefl{%d}' % max_callee
    print r'\def\ptaplainalloc{%d}' % plain_alloc
    print r'\def\ptacombinedalloc{%d}' % combined_alloc
    print r'\def\concreteinvoke{%d}' % max_concrete_invoke

if __name__ == "__main__":
    main(sys.argv)
