package com.legate.digital_robot_core.chassis.driver.yangchen;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Slf4j
public class SerialLink implements AutoCloseable {
    private SerialPort port = null;
    private final ExecutorService ioPool = Executors.newSingleThreadExecutor();
    private Consumer<String> frameHandler = null;
    private volatile boolean running;
    private static final byte H1 = (byte)0xAA, H2 = (byte)0x54;

    public SerialLink(String comName, Consumer<String> onFrame) {
        try {
            this.frameHandler = onFrame;
            this.port = SerialPort.getCommPort(comName);
            port.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 50, 0);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    public void open() {
        try {
            if (!port.openPort()) throw new IllegalStateException("Open port failed: " + port.getSystemPortName());
            running = true;
            ioPool.submit(this::readLoop);
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }

    public synchronized void sendAscii(String dataStr) {
//        byte[] d = dataStr.getBytes(StandardCharsets.US_ASCII);
//        if (d.length > 255) throw new IllegalArgumentException("Data too long (>255)");
//        int L = d.length;
//        byte s = (byte) L;
//        for (byte b : d) s ^= b;
//
//        byte[] frame = new byte[2 + 1 + L + 1];
//        frame[0] = H1; frame[1] = H2;
//        frame[2] = (byte) L;
//        System.arraycopy(d, 0, frame, 3, L);
//        frame[frame.length - 1] = s;
//
//        port.writeBytes(frame, frame.length);
        System.out.println("sendAscii：" + dataStr);
    }

    private void readLoop() {
        try {
            var in = port.getInputStream();
            while (running) {
                // find header
                int b = in.read(); if (b < 0) continue;
                if ((b & 0xFF) != (H1 & 0xFF)) continue;
                int b2 = in.read(); if (b2 < 0 || (b2 & 0xFF) != (H2 & 0xFF)) continue;

                int L = in.read(); if (L < 0) continue;
                byte[] data = in.readNBytes(L);
                if (data.length != L) continue;
                int S = in.read(); if (S < 0) continue;

                byte s2 = (byte) L;
                for (byte x : data) s2 ^= x;
                if (s2 != (byte) S) continue;

                String payload = new String(data, StandardCharsets.US_ASCII).trim();
                frameHandler.accept(payload);
            }
        } catch (Exception ignored) {}
    }

    @Override public void close() {
        running = false;
        ioPool.shutdownNow();
        if (port.isOpen()) port.closePort();
    }
}