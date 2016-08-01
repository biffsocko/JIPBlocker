#!/bin/sh

/usr/bin/firewall-cmd --zone=public --add-rich-rule="rule family=ipv4 source address=$1 reject"

