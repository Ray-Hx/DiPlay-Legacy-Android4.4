package com.shilapi.xcertplay.media

/** Converts the network's big-endian samples into reusable AudioTrack scratch space. */
internal object Pcm16Payload {
    fun copyLittleEndian(source: ByteArray, offset: Int, length: Int, destination: ByteArray) {
        require(offset >= 0 && length >= 0 && offset <= source.size - length)
        require(length <= destination.size)
        var index = 0
        while (index + 1 < length) {
            destination[index] = source[offset + index + 1]
            destination[index + 1] = source[offset + index]
            index += 2
        }
        // Preserve the previous converter's handling of an incomplete final sample.
        if (index < length) destination[index] = source[offset + index]
    }
}
