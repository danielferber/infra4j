@echo off

call config\servico.config.bat

"%monitorexe%" //MR/%identificador%
