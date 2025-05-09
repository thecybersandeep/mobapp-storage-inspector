#!/bin/bash

echo "Building MobApp-Storage Inspector..."

# Build with Maven
mvn clean package

# move the JAR 
mv target/storage-inspector-1.0-SNAPSHOT-fat.jar target/MobApp-Storage-Inspector.jar

echo "Build complete! The application is available at: target/MobApp-Storage-Inspector.jar"
echo "Run it with: java --enable-native-access=ALL-UNNAMED -jar output/MobApp-Storage-Inspector.jar"