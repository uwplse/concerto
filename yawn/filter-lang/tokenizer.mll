{
    (* short names for important modules *)
module L = Lexing 
module B = Buffer

let get      = L.lexeme
let sprintf  = Printf.sprintf

let position lexbuf =
    let p = lexbuf.L.lex_curr_p in
        sprintf "%s:%d:%d" 
        p.L.pos_fname p.L.pos_lnum (p.L.pos_cnum - p.L.pos_bol)

let set_filename (fname:string) (lexbuf:L.lexbuf)  =
    ( lexbuf.L.lex_curr_p <-  
        { lexbuf.L.lex_curr_p with L.pos_fname = fname }; lexbuf)

exception Error of string
let error lexbuf fmt = 
    Printf.kprintf (fun msg -> 
        raise (Error ((position lexbuf)^" "^msg))) fmt

}
let ws    = [' ' '\t']
let nl    = ['\n']

let digit = ['0'-'9']
let alpha = ['a'-'z' 'A'-'Z']
let id    = (alpha|'_') (alpha|digit|'_')*

rule token = parse
          | ws+       { token lexbuf  }
          | nl        { L.new_line lexbuf; token lexbuf }
          | digit+    { Tokens.INT(int_of_string @@ get lexbuf) }
          | '-' digit+ { Tokens.INT(int_of_string @@ get lexbuf) }
          | "call-int" { Tokens.CALL_INT }
          | "call-obj" { Tokens.CALL_OBJ }
          | "if" { Tokens.IF }
          | "lambda" { Tokens.LAMBDA }
          | "==" { Tokens.CMP `Eq }
          | "!=" { Tokens.CMP `Ne }
          | "<" { Tokens.CMP `Lt }
          | "<=" { Tokens.CMP `Le }
          | ">" { Tokens.CMP `Gt }
          | ">=" {Tokens.CMP `Ge }
          | "(" { Tokens.OPEN_PAREN }
          | ")" { Tokens.CLOSE_PAREN }
          | "[" { Tokens.OPEN_BRACKET }
          | "]" { Tokens.CLOSE_BRACKET }
          | "nil" { Tokens.NIL }
          | "let" { Tokens.LET }
		  | "int-array" { Tokens.INT_ARRAY }
          | id        { Tokens.IDENT (get lexbuf)}
          | '"'       { Tokens.STRING (string (B.create 100) lexbuf) } (* see below *)
          | eof       { Tokens.EOF           }
          | _         { error lexbuf 
                          "found '%s' - don't know how to handle" @@ get lexbuf }

and string buf = parse (* use buf to build up result *)
| [^'"' '\n' '\\']+  
            { B.add_string buf @@ get lexbuf
            ; string buf lexbuf 
            }
| '\n'      { B.add_string buf @@ get lexbuf
            ; L.new_line lexbuf
            ; string buf lexbuf
            }
| '\\' '"'  { B.add_char buf '"'
            ; string buf lexbuf
            }
| '\\'      { B.add_char buf '\\'
            ; string buf lexbuf
            }
| '"'       { B.contents buf } (* return *)
| eof       { error lexbuf "end of input inside of a string" }
| _         { error lexbuf 
                "found '%s' - don't know how to handle" @@ get lexbuf }

{

}
