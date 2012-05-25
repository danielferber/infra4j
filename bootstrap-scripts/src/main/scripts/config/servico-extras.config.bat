@echo off

rem Configurações específicas do serviço Windows.
rem Só edite se souber o que está fazendo.

rem Por padrão, importa as configurações de set-configuração.bat
set "PR_Description=%descricao%"
set "PR_DisplayName=%identificador%"
set PR_Startup=manual	
rem set PR_JavaHome=
rem set PR_Jvm
set "PR_JvmOptions=%definicoes%"
set "PR_Classpath=%CLASSPATH%"
set "PR_StartPath=%diretorio%"

rem Initial memory pool size in MB. (Not used in exe mode.)		
rem set PR_JvmMs
rem Maximum memory pool size in MB. (Not used in exe mode.)
rem set PR_JvmMx
rem Thread stack size in KB. (Not used in exe mode.)
rem set PR_JvmSs


rem %SystemRoot%\System32\LogFiles\Apache Defines the path for logging. Creates the directory if necessary.
set "PR_LogPath=%diretorio%\log"
rem commons-daemon	Defines the service log filename prefix. The log file is created in the LogPath directory with .YEAR-MONTH-DAY.log suffix
set "PR_LogPrefix=servico-log.txt"
rem Info Defines the logging level and can be either Error, Info, Warn or Debug
set PR_LogLevel=Debug
rem 0 Set this non-zero (e.g. 1) to capture JVM jni debug messages in the procrun log file. Is not needed if stdout/stderr redirection is being used. Only applies to jvm mode.	
set PR_LogJniMessages=4
rem Redirected stdout filename. If named auto file is created inside LogPath with the name service-stdout.YEAR-MONTH-DAY.log.	
set "PR_StdOutput=%diretorio%\log\servico-stdout.txt"
rem Redirected stderr filename. If named auto file is created in the LogPath directory with the name service-stderr.YEAR-MONTH-DAY.log.	
set "PR_StdError=%diretorio%\log\servico-stderr.txt"
rem Defines the file name for storing the running process id. Actual file is created in the LogPath directory
set "PR_PidFile=%diretorio%\servico-pid.txt"
