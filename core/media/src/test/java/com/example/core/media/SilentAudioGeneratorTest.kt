package com.example.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SilentAudioGeneratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testGenerateSilenceWavHasValidRiffHeaderAndCorrectLength() {
        val file = File(tempFolder.root, "test_silence.wav")
        val durationMs = 2000L // 2 seconds
        val sampleRate = 44100
        val channels = 2

        SilentAudioGenerator.createSilenceWavFile(
            outputFile = file,
            durationMs = durationMs,
            sampleRate = sampleRate,
            numChannels = channels
        )

        assertTrue(file.exists())

        // PCM 16-bit stereo = 4 bytes per sample (2 channels * 2 bytes)
        // 44100 samples/sec * 4 bytes/sample * 2 sec = 352,800 bytes of audio data
        // + 44 bytes header = 352,844 bytes total
        val expectedAudioBytes = ((sampleRate.toLong() * channels * 2 * durationMs) / 1000L)
        val expectedTotalBytes = expectedAudioBytes + 44
        assertEquals(expectedTotalBytes, file.length())

        val bytes = file.readBytes()

        // 1. Verify "RIFF"
        assertEquals('R'.code.toByte(), bytes[0])
        assertEquals('I'.code.toByte(), bytes[1])
        assertEquals('F'.code.toByte(), bytes[2])
        assertEquals('F'.code.toByte(), bytes[3])

        // 2. Verify "WAVE"
        assertEquals('W'.code.toByte(), bytes[8])
        assertEquals('A'.code.toByte(), bytes[9])
        assertEquals('V'.code.toByte(), bytes[10])
        assertEquals('E'.code.toByte(), bytes[11])

        // 3. Verify "fmt "
        assertEquals('f'.code.toByte(), bytes[12])
        assertEquals('m'.code.toByte(), bytes[13])
        assertEquals('t'.code.toByte(), bytes[14])
        assertEquals(' '.code.toByte(), bytes[15])

        // 4. Verify PCM format (1)
        val format = ByteBuffer.wrap(bytes, 20, 2).order(ByteOrder.LITTLE_ENDIAN).short
        assertEquals(1.toShort(), format)

        // 5. Verify Channels (2)
        val numChannels = ByteBuffer.wrap(bytes, 22, 2).order(ByteOrder.LITTLE_ENDIAN).short
        assertEquals(channels.toShort(), numChannels)

        // 6. Verify Sample Rate (44100)
        val rate = ByteBuffer.wrap(bytes, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(sampleRate, rate)

        // 7. Verify "data" marker
        assertEquals('d'.code.toByte(), bytes[36])
        assertEquals('a'.code.toByte(), bytes[37])
        assertEquals('t'.code.toByte(), bytes[38])
        assertEquals('a'.code.toByte(), bytes[39])

        // 8. Verify audio data samples are all silent (zero bytes)
        for (i in 44 until bytes.size) {
            assertEquals(0.toByte(), bytes[i])
        }
    }
}
