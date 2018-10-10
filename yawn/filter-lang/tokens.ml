type token =
  | INT of int
  | STRING of string
  | IF
  | CALL_INT
  | CALL_OBJ
  | LAMBDA
  | IDENT of string
  | CMP of [ `Lt | `Le | `Eq | `Ne | `Ge | `Gt ]
  | EOF
  | OPEN_PAREN
  | CLOSE_PAREN
  | OPEN_BRACKET
  | CLOSE_BRACKET
  | INT_ARRAY
  | LET
  | NIL
  [@@deriving show]
 
