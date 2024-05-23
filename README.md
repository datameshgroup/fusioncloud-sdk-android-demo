# fusioncloud-sdk-android-demo

### Overview

***

This android app demonstrates how to send login, payment, refund, and transaction status requests using the Fusion Cloud SDK.

### Getting Started

***

##### This demo app is built and run using:
* Android Studio
* (best viewed on) Pixel C simulator on landscape mode
 
### Building the FusionCloud demo app
***

Clone the FusionCloud demo app
* `git clone https://github.com/datameshgroup/fusioncloud-sdk-android-demo.git`
 

##### Configuration
In this demo app, the configuration stored inside the `Settings.java` file. The fields must be updated with the correct values before running the app. It is also configurable on the app itself.
```
public static String saleId = "<<Provided by DataMesh>>";
public static String poiId = "<<Provided by DataMesh>>";
public static String kek = "<<Provided by DataMesh>>";

public static String providerIdentification = "<<Provided by DataMesh>>";
public static String applicationName = "<<Provided by DataMesh>>";
public static String softwareVersion = ""<<Provided by DataMesh>>";
public static String certificationCode = "<<Provided by DataMesh>>";

```
Setting the TIMEOUT
_The current config for payment timeout is 60 seconds. After this times out, it will proceed to transaction status request which is 90 seconds. To change the timeout, update the ff values inside the `PaymentActiviry.java`_
```
//Timer settings; Update as needed.
    long loginTimeout = 60000;
    long paymentTimeout = 60000;
    long errorHandlingTimeout = 90000;
```

### Dependencies

***

This project uses the following dependencies):  

- **[Java Fusion SDK](https://github.com/datameshgroup/fusionsatellite-sdk-java):** contains all the models necessary to create request and response messages to the Fusion websocket server. 
__This library is implemented on the build.gradle, and is currently pointing to version 1.3.4_
- **[Java Fusion Cloud SDK](https://github.com/datameshgroup/fusioncloud-sdk-java):** contains a websocket client and security components needed to communicate with Unify.
_This library is included in the project as a jar file under libs folder, and is implemented on the build.gradle_

### Minimum Required JDK

***

- Java 1.8

> **Note:** Other versions may work as well, but have not been tested.
