export CLASSPATH=.:./lib/ACPAPI1.2.jar:./lib/jedis-2.4.2.jar:./lib/json-1.0.jar:./lib/log4j-1.2.17.jar:./lib/org.apache.commons.collections.jar:$CLASSPATH

rm -rf bin
mkdir bin

javac -d bin -sourcepath src @filelist.txt


cd bin

jar cf evaluator_rt.jar *

