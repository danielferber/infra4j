@echo off

call config\servico.config.bat

"%monitorexe%" //MS/%identificador%
