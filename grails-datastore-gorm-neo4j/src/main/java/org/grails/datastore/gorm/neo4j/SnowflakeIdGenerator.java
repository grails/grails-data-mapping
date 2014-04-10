package org.grails.datastore.gorm.neo4j;

import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Random;

/**
 * Created by stefan on 10.04.14.
 */
public class SnowflakeIdGenerator implements IdGenerator {

    private final long datacenterIdBits = 10L;
    private final long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    private final long timestampBits = 41L;

    private final long datacenterIdShift = 64L - datacenterIdBits;
    private final long timestampLeftShift = 64L - datacenterIdBits - timestampBits;
    private final long sequenceMax = 4096;
    private final long twepoch = 1288834974657L;
    private final long datacenterId;

    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;

    public SnowflakeIdGenerator() {
        datacenterId = getDatacenterId();
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalStateException("datacenterId > maxDatacenterId");
        }
    }

    @Override
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalArgumentException("Clock moved backwards.  Refusing to generate id for " + (
                    lastTimestamp - timestamp) + " milliseconds.");
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) % sequenceMax;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = timestamp;
        return ((timestamp - twepoch) << timestampLeftShift) |
                (datacenterId << datacenterIdShift) |
                sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    protected long getDatacenterId() {
        try {

            byte[] hardwareAddress = null;
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();

            while ((hardwareAddress == null) && (nics.hasMoreElements())) {
               NetworkInterface nic = nics.nextElement();
               hardwareAddress = nic.getHardwareAddress();
            }

            if (hardwareAddress==null) {
                // no hardware address found, generate 6 random bytes
                hardwareAddress = new byte[6];
                new Random().nextBytes(hardwareAddress);
            }

            //System.out.println(DatatypeConverter.printHexBinary(mac));
            long id = ((0x000000FF & (long) hardwareAddress[hardwareAddress.length - 1]) | (0x0000FF00 & (((long) hardwareAddress[hardwareAddress.length - 2]) << 8))) >> 6;
            //System.out.println(id);
            return id;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}


