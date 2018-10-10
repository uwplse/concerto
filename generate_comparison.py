class Cmp:
    pass

class LT(Cmp):
    def instance(self):
        return "LtExpr"
    def flip(self):
        return GT()
    def inverse(self):
        return GE()
    def propagate(self):
        return "propagateLT"

class LE(Cmp):
    def instance(self):
        return "LeExpr"
    def flip(self):
        return GE()
    def inverse(self):
        return GT()
    def propagate(self):
        return "propagateLE"

class EQ(Cmp):
    def instance(self):
        return "EqExpr"
    def flip(self):
        return self
    def inverse(self):
        return NE()
    def propagate(self):
        return "propagateEQ"

class NE(Cmp):
    def instance(self):
        return "NeExpr"
    def flip(self):
        return self
    def inverse(self):
        return EQ()
    def propagate(self):
        return "propagateNE"

class GE(Cmp):
    def instance(self):
        return "GeExpr"
    def flip(self):
        return LE()
    def inverse(self):
        return LT()
    def propagate(self):
        return "propagateGE"

class GT(Cmp):
    def instance(self):
        return "GtExpr"
    def flip(self):
        return LT()
    def inverse(self):
        return LE()
    def propagate(self):
        return "propagateGT"

to_do = [
    LT(),
    LE(),
    EQ(),
    NE(),
    GT(),
    GE()
]

tmpl = """
}} else if(expr instanceof {0}) {{
  if(isTrueBranch) {{
    propagated = new Pair<>(
      primOperations.{1}(leftOp, rightOp),
      primOperations.{2}(rightOp, leftOp)
    );
  }} else {{
    propagated = new Pair<>(
      primOperations.{3}(leftOp, rightOp),
      primOperations.{4}(rightOp, leftOp)
    );
  }}
"""

for td in to_do:
    print tmpl.format(td.instance(), td.propagate(), td.flip().propagate(),
                      td.inverse().propagate(), td.inverse().flip().propagate())
