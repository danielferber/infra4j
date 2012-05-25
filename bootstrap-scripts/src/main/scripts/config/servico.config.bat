@echo off

rem Especifique neste arquivo configurações gerais para executar a aplicação java.
rem Configurações específicas encontram-se no arquivo set-configuração-extras.bat.

rem O identificador é um texto usado para referenciar este serviço nos comandos de instalar, remover, inciar e parar serviços.
set "identificador=${bootstrap.identificador}"

rem A descrição é o texto mostrados como nome do serviço no gerenciador de serviços do Windows.
set "descricao=${bootstrap.descricao}"

rem Diretório onde será executada a aplicação Java.
set "diretorio=%cd%"

rem Lista de variáveis de ambiente. 
rem É uma forma de passar parâmetros para a aplicação.
set "ambiente="

rem Lista de definições de propriedades de sistema. 
rem É outra forma de passar parâmetros para a aplicação. 
rem Usado também para configurar bibliotecas da aplicação.
set "definicoes="

rem Nome classe que contém os pontos de entrada.
set bootstrapclass=br.pro.danielferber.bootstrap.Bootstrap

rem Caminho dos dois executáveis auxiliares que integram a aplicação Java como serviço do Windows
set "servicoexe=%cd%\bin\service.exe"
set "monitorexe=%cd%\bin\monitor.exe"

rem Lista de diretórios e arquivos .jar que contém o binário da aplicação.
rem Use como referência a listagem gerada durante o build do Maven.
set "CLASSPATH=${classpath}"