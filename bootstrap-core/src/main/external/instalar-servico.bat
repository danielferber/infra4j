@echo off

call config\servico.config.bat

"%servicoexe%" //IS/%identificador% --Jvm=auto --StartMode=jvm --StopMode=jvm --StartClass=%bootstrapclass% --StopClass=%bootstrapclass% --StartParams=start --StopParams=stop --StartMethod=main --StopMethod=main

call atualizar-servico.bat
