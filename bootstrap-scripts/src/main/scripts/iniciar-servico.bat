@echo off

call config\servico.config.bat

"%servicoexe%" //RS/%identificador%
