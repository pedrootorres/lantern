package org.lantern.kscope;

import java.util.Map;
import java.util.HashMap;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.kaleidoscope.TrustGraphAdvertisement;
import org.kaleidoscope.TrustGraphNodeId;
import org.kaleidoscope.BasicTrustGraphNodeId;

import org.lantern.LanternUtils;

/**
 * Advertisement for a Lantern node to be distributed using the Kaleidoscope
 * limited advertisement protocol.
 */
public class LanternKscopeAdvertisement implements TrustGraphAdvertisement {

    private final String sender;

    private final String jid;
    
    private final String address;
    
    private final int port;

    private final int ttl;

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
    
    public LanternKscopeAdvertisement(final String adJid, final String addr,
            final int port, final int ttl) {
        this.jid = adJid;
        this.address = addr;
        this.port = port;
        this.ttl = ttl;
        this.sender = adJid;
    }

    public LanternKscopeAdvertisement(final String senderJid,
            final String adJid, final String addr, final int port,
            final int ttl) {
        this.jid = adJid;
        this.address = addr;
        this.port = port;
        this.ttl = ttl;
        this.sender = senderJid;
    }

    public TrustGraphNodeId getSender() {
        return new BasicTrustGraphNodeId(this.sender);
    }

    public String getSenderJid() {
        return this.sender;
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

    public int getInboundTTL() {
        return ttl;
    }

    public String getPayload() {
        Map<String, Object> payloadData = new HashMap<String, Object>();
        payloadData.put("from", getSenderJid());
        payloadData.put("address", getAddress());
        payloadData.put("port", getPort());
        payloadData.put("ttl", getInboundTTL());
        String payload = LanternUtils.jsonify(payloadData);
        return payload;
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
