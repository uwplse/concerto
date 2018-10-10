import yaml, sys, os, subprocess, tempfile, numpy, time

app_packages = "meta.framework:meta.application"
main_class = "meta.framework.FrameworkMain"
main_method = "main"
heap_size = "6g"

quiet_analysis = True

def get_basic_command(analysis_classpath):
    cmd = [
        "java", "-ea", "-Xmx" + heap_size, "-classpath", analysis_classpath, "edu.washington.cse.concerto.interpreter.meta.MetaInterpreter"
    ]
    return cmd

def run_analysis(base_command, analysis_name, app_dir, framework_conf, use_combined, record_results, time_budget):
    print "(" + time.strftime("%X") + ") Running", analysis_name, "using combined interpretation?", use_combined
    base_command.extend([
        "-y", framework_conf
    ])
    if quiet_analysis:
        base_command.append("-q")
    if not use_combined:
        base_command.extend(["-n", "-e", app_packages, "-t", str(time_budget)])
    with tempfile.NamedTemporaryFile() as results_file, \
         tempfile.NamedTemporaryFile() as cg_file, \
         tempfile.NamedTemporaryFile() as stats_file, \
         tempfile.NamedTemporaryFile() as dump_file:
        base_command.extend([
            "-g", cg_file.name,
            "-s", stats_file.name,
            "-c", results_file.name,
            "-v", dump_file.name,
            "-r"
        ])
        base_command.extend([analysis_name, app_dir, main_class, main_method])
#x        print " ".join(base_command)
        subprocess.check_call(base_command)
        with open(stats_file.name, 'r') as stats_stream, \
             open(cg_file.name, 'r') as cg_stream:
            cg_stats = yaml.load(cg_stream)
            runtime_stats = yaml.load(stats_stream)
        if record_results:
            with open(results_file.name, 'r') as result_stream:
                analysis_results = yaml.load(result_stream)
            with open(dump_file.name, 'r') as dump_stream:
                violations = yaml.load(dump_stream)
        else:
            violations = None
            analysis_results = None
    return (cg_stats, runtime_stats, analysis_results, violations)

def post_process(result_map):
    output_map = {}
    for (k,v) in result_map.iteritems():
        (cg, stats, results, violations) = v
        mm = len(filter(lambda z: len(z) >= 10, cg["callees"].itervalues()))
        output_map[k] = {}
        output_map[k]["call-graph"] = (cg["numNodes"], cg["numEdges"])
        output_map[k]["polymorphic"] = cg["numPoly"]
        output_map[k]["runtime"] = stats["runtime"]
        output_map[k]["megamorphic"] = mm
        if k == "pta":
            assert type(cg["calleeSizes"]) == list
            callees_std = numpy.std(cg["calleeSizes"])
            callees_mean = numpy.mean(cg["calleeSizes"])
            output_map[k]["results"] = (callees_mean.item(), callees_std.item())
        else:
            assert results is not None, k
            assert type(results) == int
            output_map[k]["results"] = results
            output_map[k]["violations"] = violations

    return output_map

def main(args):
    root_directory = os.path.realpath(os.path.dirname(args[0]) + "/..")
    build_dir = root_directory + "/build"
    analysis_classpath = build_dir + "/build-deps/*:" + build_dir + "/classes/java/main"

    app_classpath = build_dir + "/classes/java/test"
    framework_conf = root_directory + "/yawn/concerto_in.yml"

    budget = 60 * 60 # 1 hour in seconds

    analyses = [("pta", False), ("iflow", True), ("array", True)]
    eval_results = {}
    
    combined_results = {}
    for (a_name, record) in analyses:
        combined_results[a_name] = run_analysis(get_basic_command(analysis_classpath), a_name, app_classpath, framework_conf, True, record, budget)

    if len(args) > 2:
        with open(args[2] + "_combined.yml", "w") as f:
            f.write(yaml.dump(combined_results["pta"][0]))

    eval_results["combined"] = post_process(combined_results)

    plain_results = {}
    for (a_name, record) in analyses:
        plain_results[a_name] = run_analysis(get_basic_command(analysis_classpath), a_name, app_classpath, framework_conf, False, record, budget)

    if len(args) > 2:
        with open(args[2] + "_plain.yml", "w") as f:
            f.write(yaml.dump(plain_results["pta"][0]))
                              
    eval_results["plain"] = post_process(plain_results)
    if len(args) == 1:
        print yaml.dump(eval_results)
    else:
        with open(args[1], 'w') as output:
            output.write(yaml.dump(eval_results))

    
if __name__ == "__main__":
    main(sys.argv)
