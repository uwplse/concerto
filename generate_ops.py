ops = {
    "ADD": "+",
    "SUB": "-",
    "LE": "<=",
    "LT": "<",
    "GT": ">",
    "GE": ">=",
    "DIV": "/",
    "MUL": "*",

    "EQ": "==",
    "NE": "!=",
}

tmpl = """
public static final BinOp {0} = new BinOp() {{
   public IValue apply(IValue v1, IValue v2) {{
     return IValue.lift(v1.asInt() {1} v2.asInt());
   }}
}};
"""

prim_ops = {
    "ADD": "plus",
    "SUB": "minus",
    "DIV": "div",
    "MUL": "mult",
}

compare_ops = set([
    "LE",
    "LT",
    "GT",
    "GE",
    "EQ",
    "NE",
])

tmpl_mut_ops = """
public static final BinOp {0} = new BinOp() {{
   public IValue apply(IValue v1, IValue v2, ExecutionState<?, ?> state) {{
     return IValue.lift(v1.asInt() {1} v2.asInt());
   }}

   public <AVal> IValue apply(AVal a1, AVal a2, Value expr, ValueMonad<?> monad, PrimitiveOperations<AVal> op) {{
     return new IValue(new EmbeddedValue(op.{2}(a1, a2), monad));
   }}
}};
"""


tmpl_cmp_ops = """
public static final BinOp {0} = new BinOp() {{
   public IValue apply(IValue v1, IValue v2, ExecutionState<?, ?> state) {{
     return IValue.lift(v1.asInt() {1} v2.asInt());
   }}

   public <AVal> IValue apply(AVal a1, AVal a2, Value expr, ValueMonad<?> monad, PrimitiveOperations<AVal> op) {{
     CompareResult res = op.cmp(a1, a2);
     return AbstractComparison.abstractComparison((ConditionExpr)expr, res, IValue.nondet(), IValue.lift(true), IValue.lift(false));
   }}
}};
"""



for (o_name, sym) in ops.iteritems():
    if o_name in compare_ops:
        print tmpl_cmp_ops.format(o_name, sym)
    else:
        print tmpl_mut_ops.format(o_name, sym, prim_ops[o_name])
