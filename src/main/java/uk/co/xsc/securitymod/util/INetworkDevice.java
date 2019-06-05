package uk.co.xsc.securitymod.util;

public interface INetworkDevice {

    boolean receives();

    boolean transmits();

    default void onReceive() {}

}
