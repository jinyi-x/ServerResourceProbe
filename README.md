# ServerResourceProbe
A server resource monitor written by jinyi.  

## Developing Plan

&nbsp;&nbsp;Currently can output Server Information on consle. In the future may add GUI and a IoT Device to show server information at any time;

## Usage
Server:

```shell
java -jar ResProbe.jar&    
```

Notice: Server WILL output the token of client on stdio stream

Client:

```shell
java -jar ResProbeClient.jar [PORT] [Server Address]
```