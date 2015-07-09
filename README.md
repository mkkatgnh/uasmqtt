uasmqtt
=======

Unmanned aerial systems connected together via MQTT

AircamQC - Android App which sends position and attitude.
           Optional the cam preview can be send.
           Trigger receiver to take pictures.
           Can also transmit/receive mavlink messages
           from a MQTT broker via bluetooth to a APM vehicle

uavMap - Processing 2.1.1 application to show
         position and orientation of Android App on a map (unfolding).
         Send trigger to take pictures

AircamQCViewer - Android App which shows cam preview.
                 Touching the screen send trigger to take picture.

TCPmavlink2uasmqtt - Small tool to connect APM Planner via TCP port
                     to a MQTT broker or a serial device. 
