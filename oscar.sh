#!/bin/sh

#
# The following variable should be automatically
# assigned during install, if not, edit it to reflect
# your Java installation.
#

java_home=/usr/lib/jvm/jdk1.6.0_26/jre

#
# You don't need to edit the following line
#

exec ${java_home}/bin/java -jar lib/oscar.jar
