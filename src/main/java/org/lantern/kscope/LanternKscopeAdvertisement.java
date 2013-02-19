package org.lantern.kscope;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Advertisement for a Lantern node to be distributed using the Kaleidoscope
 * limited advertisement protocol.
 */
public class LanternKscopeAdvertisement {

    private final String jid;
    
    private final String address;
    
    private final int port;

    private final int ttl;

    // TODO: add cookie here (and proxy, obvs.)
    //private final String cookie;

    @Override
    public String toString() {
        return this.jid + " <" + this.address + ":" 
            + this.port + ">, ttl=" + this.ttl;
    }

    public LanternKscopeAdvertisement() {
        this("", "", 0, 0);
    }
    
    public LanternKscopeAdvertisement(final String jid) {
        this(jid, "", 0, 0);
    }

    public LanternKscopeAdvertisement(final String jid, final InetAddress addr, 
        final int port) {
        this(jid, addr.getHostAddress(), port);
    }
    
    public LanternKscopeAdvertisement(final String jid, final String addr, 
        final int port) {
        this(jid, addr, port, 0);
    }

    public LanternKscopeAdvertisement(final String jid, final InetAddress addr,
            final int port, final int ttl) {
        this(jid, addr.getHostAddress(), port, ttl);
    }
    
    public LanternKscopeAdvertisement(final String jid, final String addr,
            final int port, final int ttl) {
        this.jid = jid;
        this.address = addr;
        this.port = port;
        this.ttl = ttl;
    }

    public String getJid() {
        return jid;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public int getTtl() {
        return ttl;
    }

    public boolean hasMappedEndpoint() {
        try {
            InetAddress.getAllByName(address);
            return this.port > 1;
        } catch (final UnknownHostException e) {
            return false;
        }
    }
}
