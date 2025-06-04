# TrinkAus

TrinkAus is a minimalist water reminder app designed to help you stay hydrated by reminding you to drink water every 2 hours.
It integrates with the HealthConnect API to track your water intake and seamlessly share this data with other health and fitness apps.

## Features

- Hydration Reminders: Receive reminders to drink water every 2 hours.
- HealthConnect Integration: Track your water intake and sync data with other health apps.
- Companion Wear OS App: Use the Wear OS app alongside the phone app for added convenience.

## Important Notes

- The Wear OS app requires the phone app to function, as it relies on the phone app to access the HealthConnect API.
- TrinkAus can be used as a standalone app or in conjunction with the Wear OS app for a more integrated experience.

## Installation

To install the Mobile app, download the APK from the releases section and install it on your Android device.

To install the Wear OS app, download the APK from the releases section and install it on your Wear OS device using [adb](https://developer.android.com/tools/adb).

1. Enable Developer Options on your Wear OS device.
2. Enable ADB debugging.
3. Connect your Wear OS device to your computer via USB.
4. Open a terminal and navigate to the directory where the Wear OS APK is located.
5. Run the following command to install the APK:
   ```bash
   adb -e install trinkaus-wear.apk
   ```