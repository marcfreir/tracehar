# TraceHAR 📱

An Android application for collecting Human Activity Recognition (HAR) sensor data for machine learning research and dataset creation.

## 📋 Overview

This app collects real-time sensor data from Android devices to create datasets for training machine learning models in human activity recognition tasks. It captures data from multiple sensors simultaneously and saves it in CSV format for easy analysis and model training.

## ✨ Features

- **Multi-sensor data collection**: Accelerometer, Gyroscope, Linear Acceleration, and Magnetometer
- **Real-time data recording** with customizable sampling rates
- **Organized data storage** in CSV format with timestamps
- **Activity labeling** for supervised learning datasets
- **Automatic file management** with timestamp-based naming
- **Real-time sample counting** and recording status
- **Permission handling** for storage access

## 🔧 Technical Specifications

### Sensors Used
- **Accelerometer** (`TYPE_ACCELEROMETER`): Measures device acceleration including gravity
- **Gyroscope** (`TYPE_GYROSCOPE`): Measures rotation rates around device axes
- **Linear Acceleration** (`TYPE_LINEAR_ACCELERATION`): Acceleration without gravity component
- **Magnetometer** (`TYPE_MAGNETIC_FIELD`): Measures ambient magnetic field

### Data Format
Data is saved as CSV files with the following structure:

```csv
timestamp,activity,acc_x,acc_y,acc_z,gyro_x,gyro_y,gyro_z,linear_acc_x,linear_acc_y,linear_acc_z,mag_x,mag_y,mag_z
1703123456789,walking,-0.123,9.456,2.345,0.012,-0.034,0.056,0.234,-0.567,0.890,23.45,-12.34,45.67
```

### Sampling Configuration
- **Default sampling rate**: `SENSOR_DELAY_GAME` (~50Hz)
- **Data synchronization**: Triggered by accelerometer readings
- **File format**: CSV with headers for easy processing

## 🚀 Getting Started

### Prerequisites
- Android device with API level 21+ (Android 7.0+)
- Device with accelerometer and gyroscope sensors

> Issue? Contact me: marcfreir@outlook.com
