package com.shilapi.xcertplay.network

import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import javax.jmdns.JmDNS

class KitKatMdnsSocketTest {
    @Test fun twoReusableSocketsCanShareTheSameBoundPort() {
        KitKatMdnsSocket(InetSocketAddress(0)).use { first ->
            KitKatMdnsSocket(InetSocketAddress(first.localPort)).use { second ->
                assertTrue(first.reuseAddress)
                assertTrue(second.reuseAddress)
                assertEquals(first.localPort, second.localPort)
            }
        }
    }

    @Test fun unboundConstructionStaysUnboundUntilExplicitBind() {
        KitKatMdnsSocket(null).use { socket ->
            assertFalse(socket.isBound)
            assertTrue(socket.reuseAddress)
            socket.bind(InetSocketAddress(0))
            assertTrue(socket.isBound)
        }
    }

    @Test fun pinnedJmDnsActuallyUsesThePatchedSocketAllocation() {
        JmDNS.create(InetAddress.getLoopbackAddress(), "diplay-socket-test").use { dns ->
            val socket = dns.javaClass.getMethod("getSocket").invoke(dns)
            assertTrue(socket is MulticastSocket)
            assertEquals(KitKatMdnsSocket::class.java, socket.javaClass)
        }
    }
}
