@echo off & setlocal EnableDelayedExpansion

:: ----------------------------------------------------------------------
::This file reads all available agents and graphs at pre-defined locations and generates a text file
::that can be used to efficiently create ones own sim_agenda.txt simulation configuration (copy + paste)
::instead of searching for available agents and graphs manually (this file is not required for general evaluation)

:: ----------------------------------------------------------------------
:: Deleting old text file

echo # Deleting old all_options.txt

IF EXIST all_options.txt (DEL all_options.txt)

echo # Done.
echo.

:: ----------------------------------------------------------------------


:: ----------------------------------------------------------------------
:: Write headline in text file

echo # Writing headline in new all_options.txt

(

	echo # Uebersicht ueber alle verfuegbaren Graphen und Agenten:
	echo.

) >> all_options.txt

echo # Done
echo.

:: ----------------------------------------------------------------------
:: Writing headline for graphs in text file

echo # Writing headline for graphs in all_options.txt

(	
	
	echo # Graphen:
	echo.

) >> all_options.txt

echo # Done
echo.

:: ----------------------------------------------------------------------
:: Read every graph in Graphs folder

echo # Reading graphs at: ../../Graphs

set graph_cnt = 0

for %%a in (../../Graphs/*.xml) do (
	
	set /a graph_cnt += 1
	echo # curr_graph: %%a
	echo # curr_index: !graph_cnt!
	set graph_arr[!graph_cnt!]=%%a
)

echo # Done
echo.

:: ----------------------------------------------------------------------

:: ----------------------------------------------------------------------
:: Writing graphs to text file

echo # Writing graphs to all_options.txt

set /a max_graph_index = %graph_cnt%

for /l %%b in (1, 1, %graph_cnt%) do (
	
	echo %%b: !graph_arr[%%b]! >> all_options.txt
)

echo # Done
echo.

:: ----------------------------------------------------------------------

:: ----------------------------------------------------------------------
:: Writing extra blank line to separate graphs and agents in text file

echo # Writing extra blank line to text file

echo. >> all_options.txt

echo # Done
echo.

:: ----------------------------------------------------------------------

:: ----------------------------------------------------------------------
:: Read every agent in Agents folder

echo # Reading agents at ../../Agents/

set agent_cnt = 0

for /d %%c in (../../Agents/*) do (
	
	set /a agent_cnt += 1
	echo # curr_agent: %%~nc
	echo # curr_index: !agent_cnt!
	set agent_arr[!agent_cnt!]=%%~nc
)

echo # Done
echo.

:: ----------------------------------------------------------------------

:: ----------------------------------------------------------------------
:: Writing headline for agents in text file

echo # Writing headline for agents in all_options.txt

(	
	
	echo # Agenten:
	echo.

) >> all_options.txt

echo # Done
echo.

:: ----------------------------------------------------------------------

:: ----------------------------------------------------------------------
:: Writing agents to text file

echo # Writing agents to all_options.txt

set /a max_agent_index = %agent_cnt% - 1

for /l %%d in (1, 1, %agent_cnt%) do (
	
	echo %%d: !agent_arr[%%d]!
	echo %%d: !agent_arr[%%d]! >> all_options.txt
)

echo # Done
echo.

:: ----------------------------------------------------------------------
:: Writing sim_examples

if EXIST sim_examples.txt (DEL sim_examples.txt)

for /l %%e in (1, 1, %agent_cnt%) do (

	for /l %%f in (1, 1, %graph_cnt%) do (
	
		echo !agent_arr[%%e]!,!graph_arr[%%f]!,100,20 >> sim_examples.txt
	)
	
	if not %%e == %agent_cnt% (echo _batch_end >> sim_examples.txt)
)

echo _sim_end >> sim_examples.txt

endlocal