import os

BASE = os.path.join(os.path.dirname(os.path.realpath('__file__')), "..", "..")
BENCHMARKS = "avrora batik biojava cassandra eclipse fop graphchi h2 h2o jme jython kafka luindex lusearch pmd spring sunflow tomcat tradebeans tradesoap xalan zxing".split(" ")
GRADLE = os.path.join(BASE, "gradlew")