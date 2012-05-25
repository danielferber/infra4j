@echo off

call config\servico.config.bat
call config\servico-extras.config.bat

"%servicoexe%" //US/%identificador%
