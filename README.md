# presentationsPointer
Android application able to perform click on your lapton using bluetooth and a simple server java.

For Linux only: install bluez and libbluetooth-dev (sudo apt-get install bluez libbluetooth-dev)

To run the server, copy the RemoteBluetoothServer.jar file in a folder and type java –jar RemoteBluetoothServer.jar (sudo on Linux). 

On Linux, if you get an "javax.bluetooth.ServiceRegistrationException: Can not open SDP session" exception, follow the instructions provided in this post: 
https://stackoverflow.com/questions/30946821/bluecove-with-bluez-chucks-can-not-open-sdp-session-2-no-such-file-or-direct
