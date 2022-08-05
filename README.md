# fusioncloud-sdk-android-demo

### Overview

***

This android app demonstrates how to send login, payment, refund, and transaction status requests using the Fusion Cloud SDK.

### Getting Started

***

##### This demo app is built and run using:
* Android Studio
 
### Building the FusionCloud demo app
***

Clone the FusionCloud demo app
* `git clone https://github.com/datameshgroup/fusioncloud-sdk-android-demo.git`
 

##### Configuration
In this demo app, the initialization of the configuration classes is done inside the `Settings.java` file. The fields must be updated with the correct values before running the app.
```
public static String ENV = "DEV"|"PROD";

public static String SALE_ID = "<<Provided by DataMesh>>";
public static String POI_ID = "<<Provided by DataMesh>>";
public static String serverDomain = "<<Provided by DataMesh>>"; 
public static String socketProtocol = "<<Provided by DataMesh>>";

public static String kekValue = "<<Provided by DataMesh>>";
public static String keyIdentifier = ""<<Provided by DataMesh>>";
public static String keyVersion = "<<Provided by DataMesh>>";

public static String providerIdentification = "<<Provided by DataMesh>>";
public static String applicationName = "<<Provided by DataMesh>>";
public static String softwareVersion = "<<Your POS version>>";
```
### Dependencies

***

This project uses the following dependencies):  

- **[Java Fusion SDK](https://github.com/datameshgroup/fusionsatellite-sdk-java):** contains all the models necessary to create request and response messages to the Fusion websocket server
_This library is included in the project as a jar file under libs folder, and is implemented on the build.gradle_
- **[Java Fusion Cloud SDK](https://github.com/datameshgroup/fusioncloud-sdk-java):** contains a websocket client and security components needed to communicate with Unify.
_This library is implemented on the build.gradle, and is currently pointing to version 1.2.2_

### Minimum Required JDK

***

- Java 1.8

> **Note:** Other versions may work as well, but have not been tested.
