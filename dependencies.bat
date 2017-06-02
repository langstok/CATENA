@echo off
call mvn install:install-file -Dfile=.\lib\ws4j-1.0.1.jar -DgroupId=edu.cmu.lti.ws4j -DartifactId=ws4j -Dversion=1.0.1 -Dpackaging=jar

pause

