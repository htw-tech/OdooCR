#!/bin/bash
# Fetch current date and time from the computer
current_time=$(date +"%m%d%H%M%Y.%S")

# Set date and time on the Android device
adb shell "su -c 'date $current_time'"

# Enable automatic time and time zone again
adb shell settings put global auto_time 1
adb shell settings put global auto_time_zone 1
