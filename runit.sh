#!/bin/sh

CLASSPATH="/usr/lib/jvm/java/jre/lib/rt.jar:/usr/lib/jvm/java/jre/lib/tools.jar:/usr/lib/jvm/java/jre/lib/dt.jar:/usr/local/JIPBlocker/mysql-connector-java-5.1.5-bin.jar:."

export CLASSPATH

systemctl status firewalld
if [ $? -ne 0 ]
then
    systemctl start firewalld
fi

cd /usr/local/JIPBlocker
#cd /home/biff/workspace/JIPBlocker/src/
java JIPBlocker
