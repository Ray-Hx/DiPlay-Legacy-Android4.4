package com.shilapi.xcertplay.media

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioPayloadTest {
    @Test fun aacHeaderRetainsMpeg4LcFrequencyChannelsAndLength() {
        assertArrayEquals(
            byteArrayOf(0xff.toByte(), 0xf1.toByte(), 0x4c, 0x80.toByte(), 0x01, 0x7f, 0xfc.toByte(), 1, 2, 3, 4),
            MediaCodecSupport.adtsFrame(byteArrayOf(1, 2, 3, 4), 48_000, 2),
        )
        val header = ByteArray(7)
        MediaCodecSupport.writeAdtsHeader(header, 4, 44_100, 1)
        assertEquals(0x50, header[2].toInt() and 255)
        assertEquals(0x40, header[3].toInt() and 255)
    }

    @Test fun reusedAacHeaderOverwritesPreviousFrameLengthAndLeavesPayloadUntouched() {
        val target = ByteArray(12) { 0x55 }
        // Cross every ADTS length bit boundary, then shrink to reveal stale bits.
        for (payloadBytes in listOf(4, 248, 2048, 4096, 1)) {
            MediaCodecSupport.writeAdtsHeader(target, payloadBytes, 48_000, 2)
            val frameBytes = ((target[3].toInt() and 3) shl 11) or
                ((target[4].toInt() and 255) shl 3) or ((target[5].toInt() and 255) ushr 5)
            assertEquals(payloadBytes + 7, frameBytes)
            assertArrayEquals(ByteArray(5) { 0x55 }, target.copyOfRange(7, 12))
        }
    }

    @Test fun pcmConvertsOnlyPayloadAndDoesNotModifySharedRtpPacket() {
        val rtp = ByteArray(12) { 0x33 } + byteArrayOf(0x12, 0x34, 0xab.toByte(), 0xcd.toByte())
        val original = rtp.copyOf()
        val scratch = ByteArray(8) { 0x55 }
        Pcm16Payload.copyLittleEndian(rtp, 12, 4, scratch)
        assertArrayEquals(byteArrayOf(0x34, 0x12, 0xcd.toByte(), 0xab.toByte(), 0x55, 0x55, 0x55, 0x55), scratch)
        assertArrayEquals(original, rtp)
    }

    @Test fun shorterPcmPacketReusesScratchWithoutExposingPreviousSamples() {
        val scratch = ByteArray(6)
        Pcm16Payload.copyLittleEndian(byteArrayOf(1, 2, 3, 4, 5, 6), 0, 6, scratch)
        Pcm16Payload.copyLittleEndian(byteArrayOf(7, 8, 9), 0, 3, scratch)
        assertArrayEquals(byteArrayOf(8, 7, 9), scratch.copyOfRange(0, 3))
        Pcm16Payload.copyLittleEndian(byteArrayOf(), 0, 0, scratch)
        assertArrayEquals(byteArrayOf(8, 7, 9), scratch.copyOfRange(0, 3))
    }
}
