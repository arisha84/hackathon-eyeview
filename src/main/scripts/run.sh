#!/bin/bash

MEM_MAX_RATIO=0.85
MEM_MIN_RATIO=0.25
MEM=`grep MemTotal /proc/meminfo | awk '{print $2}'`

#Account for Memcache
let "MEM=$MEM-512000"

MAXMEM=`awk -v mem=$MEM -v factor=$MEM_MAX_RATIO 'BEGIN{print int(mem/1000*factor)}'`
MINMEM=`awk -v mem=$MEM -v factor=$MEM_MIN_RATIO 'BEGIN{print int(mem/1000*factor)}'`

# See http://stackoverflow.com/questions/5719272/java-cms-being-ignored-and-getting-full-gc-instead and cassandra jvm configurations (cassandra/conf/cassandra-env.sh)
# Don't be too quick to assume that -XX:+UseCompressedOops is a good thing
OPTS=" -Xms${MINMEM}m -Xmx${MAXMEM}m -server -XX:-RelaxAccessControlCheck -XX:+UseGCOverheadLimit -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -XX:+UseStringCache -XX:+UseCompressedStrings -XX:+OptimizeStringConcat -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=1 -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -XX:MaxPermSize=256M "
GC_PRINT_OPTS=" -XX:ErrorFile=$EV_LOG/mad-service-jvm-error.log -Xloggc:$EV_LOG/mad-service-jvm-gc-activity.log -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -verbose:gc -XX:+PrintGCDateStamps "

CLASSPATH="$(echo *.jar | sed 's/ /:/g'):$(echo dependency/*.jar | sed 's/ /:/g')"
java $OPTS $GC_PRINT_OPTS -cp $CLASSPATH com.eyeview.cep.launcher.CEPLauncher
