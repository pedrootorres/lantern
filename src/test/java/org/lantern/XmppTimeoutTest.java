package org.lantern;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.security.auth.login.CredentialException;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.junit.Test;
import org.lantern.state.Mode;
import org.lantern.state.Model;

public class XmppTimeoutTest {
    @Test
    public void test() throws CredentialException, IOException,
            NotInClosedBetaException, InterruptedException {
        TestUtils.load(true);
        final Model model = TestUtils.getModel();
        final org.lantern.state.Settings settings = model.getSettings();
        // settings.setProxies(new HashSet<String>());

        settings.setMode(Mode.get);

        final XmppHandler handler = TestUtils.getXmppHandler();
        // The handler could have already been created and connected, so
        // make sure we disconnect.
        handler.disconnect();
        handler.connect();

        assertTrue(handler.isLoggedIn());

        Packet packet = new Presence(Type.available);

        //Disconnect from the Internet here.
        System.out.println("DISCONNECT NOW");
        Thread.sleep(20000); //wait to give user time to disconnect

        handler.sendPacket(packet);

        System.out.println("sent packet");
        while (true) {
            //just loop here forever
            Thread.sleep(1000);

        }
    }
}
