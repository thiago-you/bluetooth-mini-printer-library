# Bluetooth Mini Printer Library
This library is an updated and compiled implementation from the one [Joonik Team](https://github.com/Joonik/BlueToothDEMO) has ported with a lot of effort. The original app was designed for eclipse and comes bundled with the printer.

This new implementation is designed to Android Studio and are using the library "androidx" and work better on newer versions of Android, with a simple interface and UTF-8/ISO-8859-1 charset compatibility (latin).

### DEMO
You can use the activities under demo folder to quickly test the library and have a example to your own implementation. An demo implementation can be found into [demo repository](https://github.com/thiagoyou/bluetooth-mini-printer-demo) 

### Improvements
    - Simple interface
    - UTF-8/ISO-8859-1 charset compatibility
    - Print QR Code
    - Print images from camera and from gallery
    - Compatibility with newer versions of Android

### Compatible with (tested)
    - https://www.amazon.com/AGPtEK%C2%AE-Bluetooth-Thermal-Software-Rechargeable/dp/B00XKMQLFI/
    - https://www.amazon.com/AGPtEK%C2%AE-Bluetooth-Thermal-Software-Rechargeable/dp/B00XKMQLFI/

### Requirements
    Min SDK Version >= 23
    Target SDK >= 28
    
### Import Library
Follow these steps to import the library into your project:

    - Download the library
    - Go to you project under "File" -> "New" -> "Import Module"
    - Choose the library, if it shows name error check "import" option and rename lib
    - In build.gradle, import library as "implementation project(':library-name')"
    - Sync build.gradle and build your project