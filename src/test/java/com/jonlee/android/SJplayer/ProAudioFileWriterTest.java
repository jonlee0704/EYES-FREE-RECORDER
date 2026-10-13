package com.jonlee.android.SJplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Test;

/** JVM tests for the Pro 24-bit WAV writer. */
public class ProAudioFileWriterTest {

    private static long dataSizeInHeader(File f) throws Exception {
        byte[] header = new byte[44];
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            raf.readFully(header);
        }
        return ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(40) & 0xFFFFFFFFL;
    }

    @Test
    public void headerIsKeptCurrentWhileRecordingSoACrashLosesLittle() throws Exception {
        File f = File.createTempFile("pro", ".wav");
        f.deleteOnExit();
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        ProAudioFileWriter w = ProAudioFileWriter.createWav(raf.getFD(), 48000, 2);
        float[] block = new float[512];
        long written = 0;
        // About 2.2 MB of audio, then "crash": no close()
        for (int i = 0; i < 1500; i++) {
            w.write(block, 0, block.length);
            written += block.length * 3L;
        }
        long inHeader = dataSizeInHeader(f);
        assertTrue("header reports " + inHeader, inHeader > 0 && inHeader <= written);
        assertTrue("at most about 1 MB unaccounted", written - inHeader <= (1 << 20));
        assertEquals(44 + written, f.length());

        w.close();
        assertEquals(written, dataSizeInHeader(f));
        raf.close();
    }
}
