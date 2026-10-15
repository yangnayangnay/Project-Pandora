@echo off
title Pandora Backend Server
echo Starting Pandora Backend...
cd /d D:\AndroidStudio\Project_Pandora
D:\maven\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run -f pandora-backend/pom.xml
pause