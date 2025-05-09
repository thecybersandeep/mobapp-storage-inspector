@echo off
echo Building MobApp-Storage Inspector...

REM Build with Maven
call mvn clean package

REM Create output directory if it doesn't exist
if not exist output mkdir output

REM Copy the JAR to the output directory
copy target\storage-inspector-1.0-SNAPSHOT-fat.jar output\MobApp-Storage-Inspector.jar

echo Build complete! The application is available at: output\MobApp-Storage-Inspector.jar
echo Run it with: java --enable-native-access=ALL-UNNAMED -jar output\MobApp-Storage-Inspector.jar
pause