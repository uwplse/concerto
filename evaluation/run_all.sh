#!/bin/bash

THIS_DIR=$(cd $(dirname $0) && pwd)
python $THIS_DIR/run_analyses.py results.yml cg
python $THIS_DIR/dump_stats.py results.yml cg_plain.yml cg_combined.yml
