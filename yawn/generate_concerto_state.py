import sys, subprocess, yaml, os, tempfile


class ReflectionEnv:
    def __init__(self):
        self.class_name_table = []
        self.method_name_table = []

        self._method_lookup = {}
        self._class_lookup = {}

    def register_method(self, m):
        return self._register(m, self.method_name_table, self._method_lookup)
    def register_class(self, c):
        return self._register(c, self.class_name_table, self._class_lookup)
        
    def _register(self, item, table, lkp):
        if item in lkp:
            return lkp[item]
        else:
            ind = len(table)
            lkp[item] = ind
            table.append(item)
            return ind

    def update(self, state):
        state["classes"] = self.class_name_table
        state["methods"] = self.method_name_table

class LispTranslator:
    transpile_exec = os.path.join(os.path.dirname(sys.argv[0]), "filter-lang/transpile.native")
    def __init__(self, env, filter_source):
        with tempfile.NamedTemporaryFile(delete = False) as tf:
            print >> tf, filter_source
            tf.flush()
            transpiled = subprocess.check_output([self.transpile_exec, tf.name])
        self.filter_info = self.parse_transpiled(env, transpiled)
       
    def parse_transpiled(self, env, v):
        assert "===" in v
        (source, methods) = v.split("===")
        offset = len(env.method_name_table)
        source_tape = [ int(x) for x in source.split() ]
        for m in methods.split("\n"):
            if m == "":
                continue
            env.register_method(m)
        return LispFilterSource(offset, source_tape)
    
    def get_slot(self):
        return 0

    def get_prop_stream(self):
        return self.filter_info.to_stream()

class LispFilterSource:
    def __init__(self, offs, source):
        self.offs = offs
        self.source = source
    def to_stream(self):
        return [self.offs] + self.source


translation_table = {
    "filter-source": LispTranslator
}

class LiteralProp:
    def __init__(self, slot, prop_stream):
        self.slot = k
        self.prop_stream = prop_stream
    def get_slot(self):
        return self.slot
    def get_prop_stream(self):
        return self.prop_stream

class FilterSpec:
    def __init__(self, env, conf):
        self.klass_key = env.register_class(conf["class"])
        self.props = []
        if "props" in conf:
            for (k, v) in conf["props"].iteritems():
                if type(k) == int:
                    assert type(v) == list
                    self.props.append(LiteralProp(k, v))
                else:
                    assert k in translation_table
                    self.props.append(translation_table[k](env, v))
        if conf["target"] == "*":
            self.target_stream = None
        else:
            self.target_stream = conf["target"]

    def update(self, state):
        stream = state["det-stream"]
        stream.append(self.klass_key)
        stream.append(len(self.props))
        for p in self.props:
            stream.append(p.get_slot())
            prop_stream = p.get_prop_stream()
            stream.append(len(prop_stream))
            stream.extend(prop_stream)
        if self.target_stream is None:
            stream.append(-1)
        else:
            stream.append(len(self.target_stream))
            stream.extend(self.target_stream)

class BeanConf:
    def __init__(self, class_key, bean_conf):
        self.class_key = class_key
        self.bean_conf = bean_conf
        self.num_props = 0
        self.consts = {}
        self.streams = {}
        self.injections = {}
    def get_class_name(self):
        return self.bean_conf["class"]
    def add_const(self, method_key, const):
        self.num_props += 1
        self.consts[method_key] = const
    def add_stream(self, method_key, vals):
        self.num_props += 1
        self.streams[method_key] = vals
    def add_injection(self, method_key, ref_name):
        self.num_props += 1
        self.injections[method_key] = ref_name
    def set_key(self, ref_key):
        self.ref_key = ref_key

class BeanEnv:
    def __init__(self):
        self.mapping = {}
        self.nat_mapping = {}
        self.reserved_keys = set()
    def register(self, name, bean_conf, env):
        klass_key = env.register_class(bean_conf["class"])
        self.mapping[name] = BeanConf(klass_key, bean_conf)
        if "key" in bean_conf:
            nat_key = bean_conf["key"]
            self.mapping[name].set_key(nat_key)
            self.reserved_keys.add(nat_key)
            self.nat_mapping[name] = nat_key
    def resolve_keys(self):
        self._next_key = 0
        for (name, bean) in self.mapping.iteritems():
            if name not in self.nat_mapping:
                ref_key = self._find_next_key()
                self.nat_mapping[name] = ref_key
                self.mapping[name].set_key(ref_key)

    def resolve_references(self, env):
        for (name, bean) in self.mapping.iteritems():
            if "fields" in bean.bean_conf:
                for (f_name, val) in bean.bean_conf["fields"].iteritems():
                    setter_name = "void set" + f_name[0].upper() + f_name[1:]
                    if type(val) == list:
                        meth_sig = setter_name + "(int[])"
                        bean.add_stream(env.register_method(meth_sig), val)
                    elif type(val) == int:
                        meth_sig = setter_name + "(int)"
                        bean.add_const(env.register_method(meth_sig), val)
                    elif type(val) == str:
                        assert val in self.nat_mapping and val in self.mapping
                        arg_type = self._resolve_arg_type(f_name, bean.get_class_name(), self.mapping[val].get_class_name())
                        meth_sig = setter_name + "(" + arg_type + ")"
                        bean.add_injection(env.register_method(meth_sig), self.nat_mapping[val])
    def _find_next_key(self):
        while self._next_key in self.reserved_keys:
            self._next_key += 1
        to_ret = self._next_key
        self._next_key += 1
        return to_ret
            
    def _resolve_arg_type(self, field_name, host_type, bean_type):
        root_dir = os.path.realpath(os.path.dirname(sys.argv[0]) + "/..")
        class_dir = root_dir + "/build/classes/java"
        test_class_dir = class_dir + "/test"
        class_dir =  class_dir + "/main"
        dep_dir = root_dir + "/build/build-deps/*"
        cmd_line = [
            "java", "-classpath", class_dir + ":" + dep_dir, "edu.washington.cse.concerto.interpreter.util.ReferenceResolution", test_class_dir, field_name, host_type, bean_type
        ]
        t = subprocess.check_output(cmd_line).strip()
        return t

    def get_reference(self, name):
        return self.nat_mapping[name]

    def update(self, state):
        max_key = max(self.nat_mapping.itervalues())
        stream = state["det-stream"]
        stream.append(max_key + 1)
        stream.append(len(self.nat_mapping))
        for (name, bean) in self.mapping.iteritems():
            stream.append(bean.ref_key)
            stream.append(bean.class_key)
        for (name, bean) in self.mapping.iteritems():
            ref_key = bean.ref_key
            stream.append(ref_key)
            stream.append(bean.num_props)
            for (method_key, value) in bean.consts.iteritems():
                stream.extend([0, method_key, value])
            for (method_key, stream) in bean.streams.iteritems():
                stream_len = len(stream)
                stream.extend([1, method_key])
                stream.append(stream_len)
                stream.extend(stream)
            for (method_key, ref) in bean.injections.iteritems():
                stream.extend([2, method_key, ref])
        
def parse_beans(config, env):
    beans = BeanEnv()
    for (name,bean_conf) in config["beans"].iteritems():
        beans.register(name, bean_conf, env)
    beans.resolve_keys()
    beans.resolve_references(env)
    return beans

def main(config):
    env = ReflectionEnv()
    handler_table = []
    bean_env = parse_beans(config, env)
    for cls in config["handlers"]:
        handler_table.append(bean_env.get_reference(cls))
    filters = []
    for f_spec in config.get("filters", []):
        filters.append(FilterSpec(env, f_spec))
    state = {
        "app-types": [ "meta.application.*" ],
        "framework-types": [ "meta.framework.*" ]
    }
    env.update(state)
    state["det-stream"] = []
    bean_env.update(state)
    state["det-stream"].append(len(handler_table))
    state["det-stream"].extend(handler_table)
    state["det-stream"].append(len(filters))
    for f in filters:
        f.update(state)
    print yaml.dump(state)

with open(sys.argv[1], "r") as f:
    config = yaml.load(f)
main(config)
