#!/bin/bash

THIS_DIR=$(cd $(dirname $0) && pwd)

for i in "array" "pta" "iflow"; do
	echo "Running $i"
	timeout 60m java -Xmx6g -ea -classpath "$THIS_DIR/build/build-deps/*:$THIS_DIR/build/classes/java/main:$THIS_DIR/build/classes/java/test" edu.washington.cse.concerto.interpreter.meta.MetaInterpreter -n $i "$THIS_DIR/build/classes/java/test" meta.framework.FrameworkMain main -e "meta.framework:meta.application"
	RET=$?
	if [ $RET -eq 0 -o $RET -eq 124 ]; then
		true
	else
		exit 1
	fi
	echo "Done with $i"
done

for i in "array" "pta" "iflow"; do
	echo "Running COMBINED $i"
	timeout 15m java -Xmx6g -ea -classpath "$THIS_DIR/build/build-deps/*:$THIS_DIR/build/classes/java/main:$THIS_DIR/build/classes/java/test" edu.washington.cse.concerto.interpreter.meta.MetaInterpreter -y $THIS_DIR/yawn/concerto_in.yml $i "$THIS_DIR/build/classes/java/test" meta.framework.FrameworkMain main
	RET=$?
	if [ $RET -eq 0 -o $RET -eq 124 ]; then
		true
	else
		exit 1
	fi
	echo "Done with $i"
done
