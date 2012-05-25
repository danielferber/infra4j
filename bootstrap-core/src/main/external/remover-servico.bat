@echo off

call config\servico.config.bat

"%servicoexe%" //DS/%identificador%
