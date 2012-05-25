@echo off

call config\servico.config.bat

"%servicoexe%" //SS/%identificador%
