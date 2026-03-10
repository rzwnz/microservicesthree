package com.sthree.file.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sthree.file.exception.FileServiceException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * ClamAV-based virus scanning service.
 *
 * Connects to a ClamAV daemon via TCP socket (clamd protocol)
 * to scan file content before storage. The service is optional —
 * when disabled, all files pass scanning automatically.
 *
 * @author rzwnz
 * @version 1.0.0
 */
@Slf4j
@Service
public class VirusScanService {

    private static final String INSTREAM_CMD = "zINSTREAM\0";
    private static final int CHUNK_SIZE = 2048;

    @Value("${virus-scan.enabled:false}")
    private boolean enabled;

    @Value("${virus-scan.clamav-host:localhost}")
    private String clamavHost;

    @Value("${virus-scan.clamav-port:3310}")
    private int clamavPort;

    /**
     * Scan the given input stream for viruses.
     *
     * @param inputStream the file content to scan
     * @param fileName    the original file name (for logging)
     * @return the scan result
     */
    public ScanResult scan(InputStream inputStream, String fileName) {
        if (!enabled) {
            log.debug("Virus scanning disabled — skipping scan for: {}", fileName);
            return ScanResult.clean();
        }

        try (Socket socket = new Socket(clamavHost, clamavPort)) {
            socket.setSoTimeout(30_000);

            OutputStream out = new BufferedOutputStream(socket.getOutputStream());
            out.write(INSTREAM_CMD.getBytes(StandardCharsets.US_ASCII));

            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // ClamAV INSTREAM protocol: 4-byte big-endian chunk length, then data
                out.write(intToBytes(bytesRead));
                out.write(buffer, 0, bytesRead);
            }
            // Send zero-length terminator
            out.write(intToBytes(0));
            out.flush();

            // Read response
            byte[] response = socket.getInputStream().readAllBytes();
            String responseStr = new String(response, StandardCharsets.US_ASCII).trim();

            log.debug("ClamAV response for {}: {}", fileName, responseStr);

            if (responseStr.contains("OK") && !responseStr.contains("FOUND")) {
                return ScanResult.clean();
            } else if (responseStr.contains("FOUND")) {
                String threat = responseStr.replaceAll(".*stream: (.+) FOUND.*", "$1");
                log.warn("Virus detected in {}: {}", fileName, threat);
                return ScanResult.infected(threat);
            } else {
                log.error("Unexpected ClamAV response for {}: {}", fileName, responseStr);
                return ScanResult.error(responseStr);
            }
        } catch (IOException e) {
            log.error("ClamAV connection failed for {}: {}", fileName, e.getMessage());
            // Fail-open: if ClamAV is unreachable, allow the upload but log a warning
            return ScanResult.error("ClamAV unreachable: " + e.getMessage());
        }
    }

    /**
     * Scan file bytes for viruses. Convenience wrapper.
     *
     * @param content  the file bytes
     * @param fileName the original file name
     * @throws FileServiceException if the file is infected
     */
    public void scanOrReject(byte[] content, String fileName) {
        ScanResult result = scan(new java.io.ByteArrayInputStream(content), fileName);
        if (result.isInfected()) {
            throw new FileServiceException(
                    "Virus detected in file: " + result.getThreatName(),
                    FileServiceException.ErrorCode.VALIDATION_ERROR,
                    400);
        }
    }

    private static byte[] intToBytes(int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }

    /**
     * Result of a ClamAV virus scan.
     */
    public record ScanResult(Status status, String threatName, String errorMessage) {

        public enum Status { CLEAN, INFECTED, ERROR }

        public static ScanResult clean() { return new ScanResult(Status.CLEAN, null, null); }
        public static ScanResult infected(String threat) { return new ScanResult(Status.INFECTED, threat, null); }
        public static ScanResult error(String message) { return new ScanResult(Status.ERROR, null, message); }

        public boolean isClean() { return status == Status.CLEAN; }
        public boolean isInfected() { return status == Status.INFECTED; }
        public String getThreatName() { return threatName; }
    }
}
