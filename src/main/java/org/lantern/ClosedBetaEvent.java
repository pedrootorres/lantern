package org.lantern;

/**
 * Event thrown when the server tells gives us the status of whether or not
 * we're in the closed beta.
 */
public class ClosedBetaEvent {

    private final boolean inClosedBeta;

    public ClosedBetaEvent(final boolean inClosedBeta) {
        this.inClosedBeta = inClosedBeta;
    }

    public boolean isInClosedBeta() {
        return inClosedBeta;
    }
}