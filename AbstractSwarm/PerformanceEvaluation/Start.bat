@echo off
setlocal EnableDelayedExpansion
cls
goto :main

REM -------------------------------------------------------------------------------------------
REM Funktion "welcome": Begruesst den Nutzer und laesst ihn das Programm starten
:main
setlocal
echo #########################################################
echo ##                                                     ##
echo ##                      Welcome                        ##
echo ##                                                     ##
echo #########################################################

echo.
echo # Note: Running a new simulation leads to a loss of current .log data!
echo.
echo # Press any button to start the simulation.
echo.

pause>nul

cd sim

Start sim_agenda.bat

goto :eof

endlocal
REM -------------------------------------------------------------------------------------------

REM -------------------------------------------------------------------------------------------
REM Funktion "eof": Beendet das Programm
:eof
exit
REM -------------------------------------------------------------------------------------------