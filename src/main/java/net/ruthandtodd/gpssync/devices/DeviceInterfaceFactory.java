package net.ruthandtodd.gpssync.devices;


import java.util.Map;

public class DeviceInterfaceFactory {

    public enum Channel{
        USB, ANT
    }

    Map<Channel, DeviceInterface> map;

    public DeviceInterface getInterfaceForChannel(Channel channel){

        return map.get(channel);

    }

}
