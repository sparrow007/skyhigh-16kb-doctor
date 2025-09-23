#!/bin/bash

# SkyHigh 16KB Doctor - Run App with Fresh Report
# This script ensures the app always has the latest report data

set -e  # Exit on any error

echo "Starting SkyHigh 16KB Doctor build with fresh report..."

# Step 1: Build the APK first (needed for scanning)
echo "Step 1: Building APK..."
./gradlew :app:assembleDebug

# Step 2: Clean scan cache and generate report from the built APK
echo "Step 2: Cleaning scan cache and generating SkyHigh Doctor report..."
rm -rf app/build/skyhigh/reports/scan/*
./gradlew :app:skyhighDoctor --rerun-tasks

# Step 3: Copy report to assets (this happens automatically via finalizedBy)
echo "Step 3: Report copied to assets automatically"

# Step 4: Rebuild APK with the updated assets containing fresh report
echo "Step 4: Rebuilding APK with fresh report in assets..."
./gradlew :app:assembleDebug

# Step 5: Install the APK with fresh report
echo "Step 5: Installing app with fresh report..."
./gradlew :app:installDebug

# Step 6: Launch the app automatically
echo "Step 6: Launching the app..."
adb shell am start -n com.sparrow.skyhigh_16kb_doctor/.MainActivity

echo "Success! App installed and launched with fresh SkyHigh Doctor report."
echo "The app should now be running on your device with the latest report data."