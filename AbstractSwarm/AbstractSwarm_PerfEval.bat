@echo off
Start javaw.exe -jar ./PerformanceEvaluation/PerformanceEvaluation.jar
Start /w AbstractSwarm.exe
Taskkill /F /IM javaw.exe /T