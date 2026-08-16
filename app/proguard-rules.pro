# Paho MQTT client uses reflection for its internal ping sender / logging.
-keep class org.eclipse.paho.client.mqttv3.** { *; }
-dontwarn org.eclipse.paho.**
