@echo off & setlocal enabledelayedexpansion

:: make sure to maximize window on start
if not "%1" == "max" start /MAX cmd /c %0 max & exit /b

::--------------------------------------------------
:main

echo #   :main
echo.

echo #   Current dir: %cd%
echo.

echo #   Welcome!
echo #   This .bat will help you run a pre-defined simulation agenda
echo.

echo -----------------------------------------------------------------
echo.

echo #   Checking if pre-defined agenda ("sim_agenda.txt") exists at:
echo     "%cd%"
echo.

if exist "./sim_agenda.txt" (

	echo #   "sim_agenda.txt" found.
	echo.

	echo -----------------------------------------------------------------
	echo.

	goto :confirm_log_reset

)
else (
	
	echo #   "sim_agenda.txt" not found.
	echo #   Please create agenda file.
	echo #   Program will abort now.
	echo.

	echo -----------------------------------------------------------------
	echo.
	
	goto :eof

goto :eof

::--------------------------------------------------
:confirm_log_reset

echo #   :confirm_log_reset
echo.

echo #   Running a pre-defined agenda will require a reset of the .log files.
echo.

echo #   Do you wish to continue? (y/n):
echo.

set /p Input="#   Enter 'y' or 'n': "

if /i "%Input%"=="y" (
	
	cls

	cd ..\..\.

	for %%G in (events.log)     do (copy /Y nul "%%G" >nul) 
	for %%G in (timetables.log) do (copy /Y nul "%%G" >nul)

	cd .\PerformanceEvaluation\sim\.

	echo.

	echo #   .log files were reset.
	echo.
	
	echo -----------------------------------------------------------------
	echo.
	
	goto :instructions
)

if /i "%Input%"=="n" (
	
	cls

	echo.

	echo #   You did not agree with an initial reset of the .log files.
	echo #   Program will abort.
	echo.
	
	echo -----------------------------------------------------------------
	echo.

	goto :end
)

echo.

echo #   [Error: Invalid input] Only 'y':yes or 'n':no are valid inputs. Please try again.
echo.

echo ---------------------------
echo.

goto :confirm_log_reset

::--------------------------------------------------
:instructions

echo #   :instructions
echo.

echo #   To make sure the pre-defined simulation runs smoothly,
echo #   please check if the content of the agenda file matches
echo #   the following protocol:
echo.

echo #   (str, str, int, int)
echo.

echo #   Content should look like this:
echo.

echo #   Agent_1,Graph_1,Runs,Repetitions
echo #   Agent_1,Graph_2,Runs,Repetitions
echo #   ...
echo #   Agent_1,Graph_n,Runs,Repetitions
echo #   _batch_end
echo #   Agent_2,Graph_1,Runs,Repetitions
echo #   Agent_2,Graph_2,Runs,Repetitions
echo #   ...
echo #   Agent_2,Graph_n,Runs,Repetitions
echo #   _sim_end
echo.

echo ---------------------------
echo.

goto :print_content

::--------------------------------------------------
:print_content

echo #   :print_content
echo.

echo #   The current content of the simulation agenda file is:
echo.

type ".\sim_agenda.txt"
echo.

echo.

echo ---------------------------
echo.

goto :confirm_content

::--------------------------------------------------
:confirm_content

echo #   :confirm_content
echo.

echo #   Does the content match the requirements? (y/n):
echo.

set /p Input="#   Enter 'y' or 'n': "

if /i "%Input%"=="y" (
	
	cls

	echo.

	echo #   You confirmed that agenda content matches requirements.
	echo.
	
	echo -----------------------------------------------------------------
	echo.
	
	echo #   Next: Running the simulation agenda
	echo.

	set /a batch_cnt=0
	
	echo ---------------------------
	echo.

	goto :create_sim_folder
)

if /i "%Input%"=="n" (
	
	cls

	echo.

	echo #   You did not confirm that agenda content matches requirements.
	echo #   Program will abort.
	echo.
	
	echo ---------------------------
	echo.

	goto :end
)

echo.

echo #   [Error: Invalid input] Only 'y':yes or 'n':no are valid inputs. Please try again.
echo.

echo ---------------------------
echo.

goto :confirm_content

::--------------------------------------------------
:create_sim_folder

echo #   :create_sim_folder
echo.

echo #   Creating time stamp.
echo.

set dt=_%DATE:~6,4%_%DATE:~3,2%_%DATE:~0,2%__%TIME:~0,2%_%TIME:~3,2%_%TIME:~6,2%
set dt=%dt: =0%

echo #   Done.
echo #   Time stamp: %dt%
echo.

echo #   Creating new folder...

cd ..\Results\.
md %dt%

echo #   Done.
echo.

echo ---------------------------
echo.

goto :read_first_batch

::--------------------------------------------------
:read_first_batch

echo #   :read_first_batch
echo.

echo #   Creating empty copy of sim_history.txt in simulation folder as kind of a "black box" for simulation (as in aviation)
echo.

cd .\%dt%\.

copy /y NUL sim_history.txt >NUL

echo #   Done.
echo.

cd ..\..\sim\.

echo #   Start reading original sim_agenda.txt content
echo.

echo ---------------------------
echo.

set EXE=javaw.exe

set /a batch_cnt+=1

set /a sim_cnt=0

::--------------------------------------------------
:Iterate over the lines of content in sim_agenda.txt
for /f "tokens=* delims=" %%i in (.\sim_agenda.txt) do (
	
	set rline=%%i

	set line=%%~ni
	set line=!line: =!
	
	if "!line!" == "_batch_end" (
		
		call :batch_end
	)

	if "!line!" == "_sim_end" (
		
		call :last_batch
	)
	
	if not "!line!" == "_batch_end" if not "!line!" == "_sim_end" (
		
		set token_cnt=0
		set token_list=

		set /a sim_cnt+=1
		
		call :next_simulation
	)
)

goto :end

::--------------------------------------------------
:next_simulation

echo #   :next_simulation
echo.

set curr_order=%rline%

echo #   Current batch: %batch_cnt%
echo #   Order number:  %sim_cnt%
echo #   Current order: %curr_order%
echo #   Current dir: %cd%
echo.

echo ---------------------------
echo.

goto :process_line

::--------------------------------------------------
:process_line

for /f "tokens=1* delims=," %%a in ("%rline%") do (
	
	set token_list[!token_cnt!]=%%a

	set rline=%%b

	set /a token_cnt+=1
)

if not "%rline%" == "" (
	
	goto :process_line
)

if "%rline%" == "" (
	
	goto :run_simulation
)

goto :eof

::--------------------------------------------------
:run_simulation

echo #   :run_simulation
echo.

set agent=%token_list[0]%  
set graph=%token_list[1]%  
set runs=%token_list[2]%  
set reps=%token_list[3]%  

:: Remove (the two) trailing spaces
set agent=%agent: =%
set graph=%graph: =%
set runs=%runs: =%
set reps=%reps: =%

:: Changing directory to .\AbstractSwarm\PerformanceEvaluation\Results\%dt%\.
cd ..\Results\%dt%\.

echo #   Current config: %agent%,%graph%,%runs%,%reps%
echo #   Adding current configuration to sim_history.txt

:Write simulation information into "black box" sim_history.txt file in simulation folder
(echo %agent%,%graph%,%runs%,%reps%) >> sim_history.txt

echo #   Done.
echo.

:: Changing directory to .\AbstractSwarm\PerformanceEvaluation\.
cd ..\..\.

echo #   Starting PerformanceEvaluation.jar

Start javaw.exe -Xms8G -jar ./PerformanceEvaluation.jar %dt%

echo #   Done.
echo.

:: Changing directory to .\AbstractSwarm\.
cd ..\.

echo #   Simulating...
echo.

set /a rep_cnt=1

for /l %%a in (1, 1, %reps%) do (
		
	echo #   Order number: %sim_cnt%
	echo.
	
	echo #   Current configuration:
	echo #   Repetition: !rep_cnt! / %reps%
	echo #   Graph:      %graph%                                        
	echo #   Agent:      %agent%
	echo #   Runs:       %runs%

	set /a rep_cnt+=1
	Start /w AbstractSwarm Graphs/%graph% %agent% %runs%
	
	echo.
)

:: Killing PerformanceEvaluation.jar process
Taskkill /F /IM javaw.exe /T>nul

:: Changing directory to .\AbstractSwarm\PerformanceEvaluation\sim\.
cd .\PerformanceEvaluation\sim\.

echo #   -- Done. Current order (nr.: %sim_cnt%) was run.
echo.

echo ---------------------------
echo.

goto :eof

::--------------------------------------------------
:batch_end

echo #   :batch_end
echo.

echo #   Batch: %batch_cnt% --- Done.
echo.

echo #   Running evaluation.

cd ..\.

Start /w javaw.exe -jar .\PerformanceEvaluation.jar final %dt%

cd .\sim\.

echo #   -- Done. Evaluation was run.
echo.

echo ---------------------------
echo.

goto :next_batch

::--------------------------------------------------
:next_batch

echo #   :next_batch
echo.

echo #   Renaming 'sim_history.txt' to 'sim_history_batch_%batch_cnt%.txt'

cd ..\Results\%dt%\.

ren sim_history.txt sim_history_batch_%batch_cnt%.txt

copy /y NUL sim_history.txt >NUL

echo #   Done.
echo.

echo #   Reset the .log files (events.log, timetables.log)

cd ..\..\..\.

for %%G in (events.log)     do (copy /Y nul "%%G" >nul) 
for %%G in (timetables.log) do (copy /Y nul "%%G" >nul)

echo #   -- Done. .log files were reset.
echo.

cd .\PerformanceEvaluation\sim\.

set /a batch_cnt+=1
set /a sim_cnt=0 

echo #   Reading next batch (Batch number: %batch_cnt%)
echo.

echo ---------------------------
echo.

goto :eof

::--------------------------------------------------
:last_batch

echo #   Batch: %batch_cnt% --- Done.
echo.

echo #   Running evaluation.
echo.

cd ..\.

Start /w javaw.exe -jar .\PerformanceEvaluation.jar final %dt%

cd .\sim\.

echo #   -- Done. Evaluation was run.
echo.

echo #   Renaming 'sim_history.txt' to 'sim_history_batch_%batch_cnt%.txt'

cd ..\Results\%dt%\.

ren sim_history.txt sim_history_batch_%batch_cnt%.txt

echo #   Done.
echo.

cd ..\..\sim\.

echo #   All batches done. Simulation completed.
echo #   Thank you for using sim_agenda.bat
echo.

echo ---------------------------
echo.

goto :eof

::--------------------------------------------------
:end

echo #   Press any button to exit.

pause>nul

exit