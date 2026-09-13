package net.styna.ae2securityterminal.api;

public class SecurityConnectionException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Connection failed due to different security realms.";

    public SecurityConnectionException() {
        super(DEFAULT_MESSAGE);
    }
}
