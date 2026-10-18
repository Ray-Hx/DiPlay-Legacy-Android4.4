package com.shilapi.xcertplay.network;

import java.io.IOException;
import java.net.MulticastSocket;
import java.net.SocketAddress;

/** KitKat binds MulticastSocket(address) before setting SO_REUSEADDR. */
public final class KitKatMdnsSocket extends MulticastSocket {
    public KitKatMdnsSocket(SocketAddress address) throws IOException {
        super((SocketAddress) null);
        try {
            setReuseAddress(true);
            if (address != null) {
                bind(address);
            }
        } catch (IOException | RuntimeException error) {
            close();
            throw error;
        }
    }
}
