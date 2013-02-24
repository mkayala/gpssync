package net.ruthandtodd.gpssync.devices;


import com.google.common.collect.ImmutableMap;
import net.ruthandtodd.gpssync.GpssyncConfig;

import java.util.Map;

public class DeviceInterfaceFactory {

    public static enum Channel{
        USB, ANT, FIT
    }

    private DeviceInterfaceFactory(){

    }

    private static final DeviceInterfaceFactory instance = new DeviceInterfaceFactory();

    public static DeviceInterfaceFactory getFactory(){
        return instance;
    }

    Map<Channel, DeviceInterface> map =
            ImmutableMap.<Channel, DeviceInterface>of(Channel.ANT, new GantGpxGetter(),
            Channel.FIT, new FitGpxGetter());

    public DeviceInterface getInterfaceForChannel(Channel channel){
        return map.get(channel);
    }

    public  DeviceInterface getPreferredInterface(){
        return  getInterfaceForChannel(GpssyncConfig.getConfig().getPreferredChannel());
    }



}
