@echo off

call config\servico.config.bat

"%monitorexe%" //ES/%identificador%
