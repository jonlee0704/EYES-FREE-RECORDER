package com.jonlee.android.SJplayer;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * Encodes Pro Recording Mode output to a file: 24-bit PCM WAV for Manual gain modes,
 * or AAC in an MPEG-4 container (MediaCodec + MediaMuxer) otherwise.
 * Input is interleaved float PCM in -1..1. Not thread-safe; used from the recording thread.
 */
public abstract class ProAudioFileWriter {

    public abstract void write(float[] interleaved, int offset, int length) throws IOException;

    public abstract void close() throws IOException;

    /** True when the file is close to the largest size its format allows. */
    public boolean isNearSizeLimit() {
        return false;
    }

    public static ProAudioFileWriter createWav(FileDescriptor fd, int sampleRate, int channels) throws IOException {
        return new Wav24Writer(fd, sampleRate, channels);
    }

    public static ProAudioFileWriter createAac(FileDescriptor fd, int sampleRate, int channels, int bitRate) throws IOException {
        return new AacWriter(fd, sampleRate, channels, bitRate);
    }

    /**
     * 24-bit little-endian PCM WAV. The header sizes are patched every few seconds and on close,
     * so a recording cut off by a crash or a killed process still opens with nearly all its audio.
     * The fd must be seekable.
     */
    static class Wav24Writer extends ProAudioFileWriter {
        private static final int HEADER_SIZE = 44;
        /** RIFF sizes are 32-bit; stop a little below 4 GiB. */
        static final long MAX_DATA_BYTES = 0xFFFFFFFFL - HEADER_SIZE - (64L << 20);
        private static final long HEADER_UPDATE_INTERVAL_BYTES = 1L << 20;   // about 3.6 s of 24-bit stereo
        private long bytesAtLastHeaderUpdate = 0;
        private final FileOutputStream stream;
        private final FileChannel channel;
        private final int sampleRate;
        private final int channels;
        private ByteBuffer buffer = ByteBuffer.allocate(0);
        private long dataBytes = 0;

        Wav24Writer(FileDescriptor fd, int sampleRate, int channels) throws IOException {
            this.stream = new FileOutputStream(fd);
            this.channel = stream.getChannel();
            this.sampleRate = sampleRate;
            this.channels = channels;
            channel.position(0);
            channel.write(header(0));
        }

        @Override
        public void write(float[] interleaved, int offset, int length) throws IOException {
            int needed = length * 3;
            if (buffer.capacity() < needed) {
                buffer = ByteBuffer.allocate(needed).order(ByteOrder.LITTLE_ENDIAN);
            }
            buffer.clear();
            for (int i = offset; i < offset + length; i++) {
                float s = Math.max(-1f, Math.min(1f, interleaved[i]));
                int v = Math.round(s * 8388607f);
                buffer.put((byte) v).put((byte) (v >> 8)).put((byte) (v >> 16));
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            dataBytes += needed;
            if (dataBytes - bytesAtLastHeaderUpdate >= HEADER_UPDATE_INTERVAL_BYTES) {
                writeHeaderInPlace();
                bytesAtLastHeaderUpdate = dataBytes;
            }
        }

        @Override
        public boolean isNearSizeLimit() {
            return dataBytes >= MAX_DATA_BYTES;
        }

        private void writeHeaderInPlace() throws IOException {
            long end = channel.position();
            channel.position(0);
            ByteBuffer h = header(dataBytes);
            while (h.hasRemaining()) channel.write(h);
            channel.position(end);
        }

        @Override
        public void close() throws IOException {
            try {
                writeHeaderInPlace();
            } finally {
                stream.close();
            }
        }

        private ByteBuffer header(long dataSize) {
            int blockAlign = channels * 3;
            ByteBuffer h = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
            h.put(new byte[]{'R', 'I', 'F', 'F'});
            h.putInt((int) Math.min(0xFFFFFFFFL, 36 + dataSize));
            h.put(new byte[]{'W', 'A', 'V', 'E'});
            h.put(new byte[]{'f', 'm', 't', ' '});
            h.putInt(16);
            h.putShort((short) 1);                 // PCM
            h.putShort((short) channels);
            h.putInt(sampleRate);
            h.putInt(sampleRate * blockAlign);
            h.putShort((short) blockAlign);
            h.putShort((short) 24);
            h.put(new byte[]{'d', 'a', 't', 'a'});
            h.putInt((int) Math.min(0xFFFFFFFFL, dataSize));
            h.flip();
            return h;
        }
    }

    /** AAC-LC in MPEG-4 (.m4a). */
    static class AacWriter extends ProAudioFileWriter {
        private static final String TAG = "ProAacWriter";
        private static final long TIMEOUT_US = 10_000;
        private final MediaCodec codec;
        private final MediaMuxer muxer;
        private final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        private final int sampleRate;
        private final int channels;
        private int trackIndex = -1;
        private boolean muxerStarted = false;
        private long framesQueued = 0;

        AacWriter(FileDescriptor fd, int sampleRate, int channels, int bitRate) throws IOException {
            this.sampleRate = sampleRate;
            this.channels = channels;
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();
            muxer = new MediaMuxer(fd, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        }

        @Override
        public void write(float[] interleaved, int offset, int length) throws IOException {
            int pos = offset;
            int end = offset + length;
            while (pos < end) {
                int inIndex = codec.dequeueInputBuffer(TIMEOUT_US);
                if (inIndex < 0) {
                    drain(false);
                    continue;
                }
                ByteBuffer in = codec.getInputBuffer(inIndex);
                if (in == null) continue;
                in.clear();
                in.order(ByteOrder.LITTLE_ENDIAN);
                int samples = Math.min(end - pos, in.remaining() / 2);
                samples -= samples % channels;
                if (samples <= 0) samples = Math.min(end - pos, channels);
                for (int i = 0; i < samples; i++) {
                    float s = Math.max(-1f, Math.min(1f, interleaved[pos + i]));
                    in.putShort((short) Math.round(s * 32767f));
                }
                long ptsUs = framesQueued * 1_000_000L / sampleRate;
                codec.queueInputBuffer(inIndex, 0, samples * 2, ptsUs, 0);
                framesQueued += samples / channels;
                pos += samples;
                drain(false);
            }
        }

        @Override
        public void close() throws IOException {
            try {
                int inIndex = codec.dequeueInputBuffer(TIMEOUT_US * 10);
                if (inIndex >= 0) {
                    long ptsUs = framesQueued * 1_000_000L / sampleRate;
                    codec.queueInputBuffer(inIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
                drain(true);
            } finally {
                try { codec.stop(); } catch (Exception ignored) {}
                codec.release();
                try {
                    if (muxerStarted) muxer.stop();
                } catch (Exception e) {
                    Log.w(TAG, "Muxer stop failed: " + e.getMessage());
                }
                muxer.release();
            }
        }

        private void drain(boolean endOfStream) {
            int idleLoops = 0;
            while (true) {
                int outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US);
                if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream || ++idleLoops > 100) return;
                } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    trackIndex = muxer.addTrack(codec.getOutputFormat());
                    muxer.start();
                    muxerStarted = true;
                } else if (outIndex >= 0) {
                    ByteBuffer out = codec.getOutputBuffer(outIndex);
                    boolean isConfig = (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0;
                    if (out != null && info.size > 0 && muxerStarted && !isConfig) {
                        out.position(info.offset);
                        out.limit(info.offset + info.size);
                        muxer.writeSampleData(trackIndex, out, info);
                    }
                    codec.releaseOutputBuffer(outIndex, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return;
                }
            }
        }
    }
}
