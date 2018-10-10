type ast =
  | Nil
  | If of if_form
  | Ident of string
  | Apply of ast * ast
  | Lambda of string * ast
  | Call of arg_call_form
  | NullCall of basic_call_form
  | IntConst of int
  | Let of (string * ast) list * ast
  | IntArray of ast list
  | StringConst of string [@@deriving show]
and if_form = {
  lop: ast;
  cmp: [ `Lt | `Le | `Eq | `Ne | `Ge | `Gt ];
  rop: ast;
  then_branch: ast;
  else_branch: ast;
} [@@deriving show]
and basic_call_form = {
  call_type: [`Int | `Obj ];
  receiver: ast;
  method_key: ast;
} [@@deriving show]
and arg_call_form = {
  base_call: basic_call_form;
  param: ast;
} [@@deriving show]

let fail ?ctxt t =
  let msg = match ctxt with
    | None -> "Unexpected token: "
    | Some ctxt_ -> "Unexpected token (" ^ ctxt_ ^ ") "
  in
  failwith @@ msg ^ (Tokens.show_token t)

let parse file_name =
  let in_chan = if file_name = "-" then stdin else open_in file_name in
  let lexbuf = Tokenizer.set_filename file_name @@ Lexing.from_channel in_chan in
  let read_buf = ref [] in
  let put_back x = read_buf := x::(!read_buf) in
  let tok () =
    match !read_buf with
    | [] -> Tokenizer.token lexbuf
    | h::t -> read_buf := t; h
  in
  let consume t = match tok () with
    | o when o = t -> ()
    | o -> fail ~ctxt:("expected " ^ (Tokens.show_token t)) o
  in
  let rec parse_expr ?next () =
    let start = match next with | Some t -> t | None -> tok () in
    match start with
    | Tokens.INT x -> IntConst x
    | Tokens.STRING str -> StringConst str
    | Tokens.OPEN_PAREN -> begin
      let next = tok () in
      let parsed = match next with
        | Tokens.LAMBDA -> begin
            match tok () with
            | Tokens.IDENT param -> Lambda (param, (parse_expr ()))
            | t -> fail t
          end
        | (Tokens.CALL_INT | Tokens.CALL_OBJ) as call_tok ->
          let receiver = parse_expr () in
          let method_key = parse_expr () in
          let base_call = {
            call_type = if call_tok = Tokens.CALL_OBJ then `Obj else `Int;
            method_key; receiver; 
          } in
          let t = tok () in begin
            match t with
            | Tokens.CLOSE_PAREN -> put_back t; NullCall base_call
            | _ ->
              let param = parse_expr ~next:t () in
              Call { base_call; param }
          end
        | Tokens.IF ->
          let () = consume Tokens.OPEN_PAREN in
          let lop = parse_expr () in
          let cmp = match tok () with
            | Tokens.CMP cmp -> cmp
            | t -> fail t
          in
          let rop = parse_expr () in
          let () = consume Tokens.CLOSE_PAREN in
          let then_b = parse_expr () in
          let else_b = parse_expr () in
          If { lop; cmp; rop; then_branch = then_b; else_branch = else_b }
        | Tokens.LET ->
          let () = consume Tokens.OPEN_BRACKET in
          let rec match_loop accum = match tok () with
            | Tokens.OPEN_PAREN ->
              let ident = match tok () with
                | Tokens.IDENT p -> p
                | t -> fail ~ctxt:"identifier expected" t
              in
              let binding = parse_expr () in
              let () = consume Tokens.CLOSE_PAREN in
              match_loop @@ (ident, binding)::accum
            | Tokens.CLOSE_BRACKET ->
              let bind_body = parse_expr () in
              Let (List.rev accum, bind_body)
            | t -> fail ~ctxt:"Expected (" t
          in
          match_loop []
        | Tokens.INT_ARRAY ->
          let rec consume_loop accum = 
            let t = tok () in
            match t with
            | Tokens.CLOSE_PAREN -> IntArray (List.rev accum)
            | _ ->
              consume_loop @@ (parse_expr ~next:t ())::accum
          in
          consume_loop []
        | other ->
          let callee = parse_expr ~next:other () in
          let arg = parse_expr () in
          Apply (callee, arg)
      in
      let () = consume Tokens.CLOSE_PAREN in parsed
      end
    | Tokens.IDENT id -> Ident id
    | Tokens.NIL -> Nil
    | t -> fail ~ctxt:"expecting expr" t
  in
  parse_expr ()

module NamelessRepr = Map.Make(String)
module ConstPool = Map.Make(String)

type rename_context = {
  next: int;
  mapping: int NamelessRepr.t;
  const_pool: int ConstPool.t;
  const_counter: int;
}

let has_mapping { next; mapping; _ } var =
  NamelessRepr.mem var mapping

let map_var { mapping; _  } var =
  NamelessRepr.find var mapping

let new_var ({ next; mapping; _ } as repr) var =
  (next, { repr with next = succ next; mapping = NamelessRepr.add var next mapping })

let intern ({ const_pool; const_counter; _ } as repr) const =
  if ConstPool.mem const const_pool then
    (ConstPool.find const const_pool, repr)
  else
    (const_counter, { repr with
                      const_pool = ConstPool.add const const_counter const_pool;
                      const_counter = succ const_counter })

let empty_ctxt = {
  next = 0;
  mapping = NamelessRepr.empty;
  const_pool = NamelessRepr.empty;
  const_counter = 0;
}

let repr_of_cmp = function
  | `Lt -> 0
  | `Le -> 1
  | `Eq -> 2
  | `Ne -> 3
  | `Ge -> 4
  | `Gt -> 5

let rec transpile emit repr ast =
  let call_to_code call_type = (match call_type with `Int -> 0 | `Obj -> 1 ) in
  let (>>=) a b = transpile emit a b in
  let (>>) a b = emit b; a in
  let ret = match ast with
    | Nil -> emit 0; repr
    | If { lop; cmp; rop; then_branch; else_branch } -> begin
        emit 1;
        (transpile emit repr lop)
        >> (repr_of_cmp cmp) >>= rop >>= then_branch >>= else_branch
      end
    | Ident id -> emit 2; emit @@ map_var repr id; repr
    | Apply (l, v) -> emit 3; transpile emit repr l >>= v
    | Lambda (id, x) ->
      let old_mapping = repr.mapping in
      let (ident, repr') = new_var repr id in
      emit 4; emit ident; { (transpile emit repr' x) with mapping = old_mapping }
    | Call { base_call = { call_type; receiver; method_key }; param } ->
      emit 5; emit @@ call_to_code call_type;
      (transpile emit repr receiver) >>= method_key >>= param
    | IntConst c -> emit 6; emit c; repr
    | Let (bindings,body) ->
      emit 7; emit @@ List.length bindings;
      let old_mapping = repr.mapping in
      let out_repr = List.fold_left (fun acc (id, binding) ->
          let (new_var, acc') = new_var acc id in
          emit new_var; transpile emit acc' binding
        ) repr bindings in
      { (transpile emit out_repr body) with mapping = old_mapping }
    | StringConst c -> emit 8; let (interned, repr') = intern repr c in
      emit interned; repr'
    | IntArray ia ->
      let num_items = List.length ia in
      emit 9; emit num_items; List.fold_left (transpile emit) repr ia
    | NullCall { call_type; receiver; method_key } ->
      emit 10; emit @@ call_to_code call_type;
      (transpile emit repr receiver) >>= method_key
        
  in
  ret

let () =
  let parsed = parse Sys.argv.(1) in
  let intrinsics = [ "request"; "dispatcher"; "response" ] in
  let start_context = List.fold_left (fun acc k -> snd @@ new_var acc k) empty_ctxt intrinsics in
  let out_repr = transpile (Printf.printf "%d ") start_context parsed in
  Printf.printf "%d" out_repr.next;
  print_endline "\n===";
  ConstPool.bindings out_repr.const_pool
  |> List.sort (fun (_,v1) (_,v2) -> v1 - v2)
  |> List.iter (fun kv -> fst kv |> Printf.printf "%s\n")

  
