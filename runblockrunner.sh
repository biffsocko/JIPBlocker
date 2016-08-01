#!/bin/sh

CLASSPATH="/usr/lib/jvm/java/jre/lib/rt.jar:/usr/lib/jvm/java/jre/lib/tools.jar"
CLASSPATH="$CLASSPATH:/usr/lib/jvm/java/jre/lib/dt.jar:/usr/local/JIPBlocker/mysql-connector-java-5.1.5-bin.jar:/usr/local/JIPBlocker:."

export CLASSPATH

#cd /home/biff/workspace/JIPBlocker/src/
cd /usr/local/JIPBlocker
nohup java JIPBlockerRunner &
