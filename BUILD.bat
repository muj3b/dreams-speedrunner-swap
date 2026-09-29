@echo off
echo Building SpeedrunnerSwap plugin...
call mvn clean verify
echo.
echo If build was successful, the plugin jar can be found at:
echo target\speedrunnerswap-4.3.6.jar
echo.
pause
