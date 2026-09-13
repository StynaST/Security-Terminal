package net.styna.ae2securityterminal.api;

import java.util.EnumSet;
import java.util.Map;

public interface ISecurityProvider {

    /**
     * used to represent the security key for the network, should be based on a unique timestamp.
     */
    long getSecurityKey();

    /**
     * Push permission data into security cache.
     */
    void readPermissions(Map<Integer, EnumSet<SecurityPermissions>> playerPerms);

    /**
     * @return is security on or off?
     */
    boolean isSecurityEnabled();

    /**
     * @return player ID for who placed the security provider.
     */
    int getOwner();
}
