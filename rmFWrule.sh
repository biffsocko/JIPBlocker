#!/bin/sh

/usr/bin/firewall-cmd --zone=public --remove-rich-rule="rule family=ipv4 source address=$1 reject"

