package xyz.outlinr.api.utils;

import java.util.UUID;
import java.time.Instant;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility for generating UUID version 7 (time-ordered UUIDs).
 * Implements RFC 9562.
 */
public final class UUIDv7Generator {

    private static long lastTimestamp = 0L;
    private static int sequence = 0;

    private UUIDv7Generator() {}

    public static UUID generate() {
        long now = Instant.now().toEpochMilli();
        long timestamp = now & 0xFFFFFFFFFFFFFL; // 48 bits

        int seq;
        synchronized (UUIDv7Generator.class) {
            if (timestamp <= lastTimestamp) {
                seq = (sequence + 1) & 0xFFF; // 12-bit sequence
                if (seq == 0) {
                    // Wait for next millisecond
                    while (Instant.now().toEpochMilli() <= lastTimestamp) {
                        Thread.onSpinWait();
                    }
                    timestamp = Instant.now().toEpochMilli() & 0xFFFFFFFFFFFFFL;
                } else {
                    sequence = seq;
                }
            } else {
                sequence = 0;
                seq = 0;
            }
            lastTimestamp = timestamp;
        }

        // Construct UUIDv7
        long msb = (timestamp & 0xFFFFFFFFL)                          // time_low (32 bits)
                 | ((timestamp >>> 32 & 0xFFFFL) << 32)               // time_mid (16 bits)
                 | ((0x7L & 0x7) << 12)                               // version 7 at bit 48-51
                 | ((long)(seq & 0xFFF) << 4);                        // sequence in bits 52-63

        long lsb = 0x8000_0000_0000_0000L                              // variant 10xx
                 | (ThreadLocalRandom.current().nextLong() & 0x3FFF_FFFF_FFFFL); // 62 random bits

        return new UUID(msb, lsb);
    }

    public static Instant getTimestamp(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long timeLow = msb & 0xFFFFFFFFL;
        long timeMid = (msb >>> 32) & 0xFFFFL;
        long timeHiAndVersion = (msb >>> 48) & 0x0FFFL;
        long timestamp = timeLow | (timeMid << 32) | (timeHiAndVersion << 48);
        return Instant.ofEpochMilli(timestamp);
    }
}