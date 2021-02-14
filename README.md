# General

Java frontend with object recognition and local db to test for changes in dimension of printed electronics. This was meant to be coupled with a movable xy-table connected via a com-port. This project was part of my bachelor thesis in 2019.

### Libraries needed for build

- Java Version 11.0.3
- [JavaFX 11.0.2](https://gluonhq.com/products/javafx/) for the frontend
- [OpenCV](https://sourceforge.net/projects/opencvlibrary/) for the object recognition
- Scene Builder (Version 2)
- rxtx library (comm ports 64 bit) (http://fizzed.com/oss/rxtx-for-java) -> download, refernce in project settings, put dlls in the javasdk or opencv build under opencv > build > java x64


### Packages installed with maven
run package manager (maven) to update dependencies

### USB-Driver
To coeenct to the xy-table you need this driver.
https://www.silabs.com/products/development-tools/software/usb-to-uart-bridge-vcp-drivers
