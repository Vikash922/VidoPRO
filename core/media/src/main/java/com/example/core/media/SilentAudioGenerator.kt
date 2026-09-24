package com.example.core.media

import java.io.File

/**
 * Generates valid PCM WAV silence files for exact timeline gap durations (DEV-070, FIX-06).
 *
 * Used by Media3 Transformer export to represent gaps without risking audio drift,
 * desynchronization, or player stalls.
 */
object SilentAudioGenerator {

    /**
     * Creates a standard 16-bit PCM WAV file filled with silence (all zeros).
     *
     * @param outputFile destination file to write
     * @param durationMs duration of the silence in milliseconds
     * @param sampleRate audio sampling rate in Hz (default 44,100 Hz)
     * @param numChannels number of channels (default 2 for stereo)
     * @return the generated file
     */
    fun createSilenceWavFile(
        outputFile: File,
        durationMs: Long,
        sampleRate: Int = 44100,
        numChannels: Int = 2
    ): File {
        val safeDuration = durationMs.coerceAtLeast(1L)
        outputFile.parentFile?.mkdirs()

        val totalAudioLen = ((sampleRate.toLong() * numChannels * 2 * safeDuration) / 1000L)
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * numChannels * 2

        outputFile.outputStream().buffered().use { out ->
            val header = ByteArray(44)
            // "RIFF"
            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            // "WAVE"
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
            // "fmt "
            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0 // 16 for PCM chunk
            header[20] = 1; header[21] = 0 // format = 1 (PCM)
            header[22] = (numChannels and 0xff).toByte()
            header[23] = ((numChannels shr 8) and 0xff).toByte()
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            val blockAlign = numChannels * 2
            header[32] = (blockAlign and 0xff).toByte()
            header[33] = ((blockAlign shr 8) and 0xff).toByte()
            header[34] = 16; header[35] = 0 // 16 bits per sample
            // "data"
            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xff).toByte()
            header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
            header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
            header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

            out.write(header)

            val zeroBuffer = ByteArray(8192)
            var bytesRemaining = totalAudioLen
            while (bytesRemaining > 0) {
                val toWrite = minOf(bytesRemaining, zeroBuffer.size.toLong()).toInt()
                out.write(zeroBuffer, 0, toWrite)
                bytesRemaining -= toWrite
            }
        }
        return outputFile
    }
}
