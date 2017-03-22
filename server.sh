#!/bin/sh

PRG="$0"
PRGDIR=`dirname "$PRG"`
HAZELCAST_HOME=`cd "$PRGDIR/.." >/dev/null; pwd`/hazelcast
PID_FILE=$HAZELCAST_HOME/hazelcast_instance.pid
JAVA_OPTS="-Dhazelcast.config=hazelcast.xml"

if [ "x$MIN_HEAP_SIZE" != "x" ]; then
	JAVA_OPTS="$JAVA_OPTS -Xms${MIN_HEAP_SIZE}"
fi

if [ "x$MAX_HEAP_SIZE" != "x" ]; then
	JAVA_OPTS="$JAVA_OPTS -Xmx${MAX_HEAP_SIZE}"
fi

export CLASSPATH=$HAZELCAST_HOME/hazelcast-all-$VERSION.jar:$CLASSPATH/*

echo "########################################"
echo "# RUN_JAVA=$RUN_JAVA"
echo "# JAVA_OPTS=$JAVA_OPTS"
echo "# CLASSPATH=$CLASSPATH"
echo "# HAZELCAST_HOME=$HAZELCAST_HOME"
echo "# VERSION=$VERSION"
echo "# starting now...."
echo "########################################"

echo "Process id for hazelcast instance is written to location: " $PID_FILE
java -server $JAVA_OPTS com.hazelcast.core.server.StartServer &
echo $! > ${PID_FILE}
sleep infinity
