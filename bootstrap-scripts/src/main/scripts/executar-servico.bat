@echo off

call config\servico.config.bat

"%servicoexe%" //TS/%identificador%
