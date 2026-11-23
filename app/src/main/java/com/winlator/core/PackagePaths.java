package com.winlator.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * ATFCORE Windows add-on: the prebuilt Linux userland (glibc, Wine, Box64, drivers) carries absolute
 * paths under the upstream package directory (/data/data/com.winlator/...). The add-on has its own
 * package name of the same length, so those paths are rewritten byte for byte while archives are
 * extracted: binaries keep their layout and every path points into this app's data directory.
 */
public final class PackagePaths {
    public static final String UPSTREAM = "com.winlator";
    public static final String PACKAGE = "atfcore.wine";   // same length as UPSTREAM
    private static final byte[] FROM = UPSTREAM.getBytes(StandardCharsets.US_ASCII);
    private static final byte[] TO = PACKAGE.getBytes(StandardCharsets.US_ASCII);

    static { if (FROM.length != TO.length) throw new IllegalStateException("package names must have the same length"); }

    private PackagePaths() {}

    public static String rewrite(String path) { return path == null ? null : path.replace(UPSTREAM, PACKAGE); }

    public static void rewrite(byte[] data, int length) {
        for (int i = 0; i + FROM.length <= length; i++) {
            if (data[i] != FROM[0]) continue;
            int k = 1;
            while (k < FROM.length && data[i + k] == FROM[k]) k++;
            if (k == FROM.length) { System.arraycopy(TO, 0, data, i, TO.length); i += TO.length - 1; }
        }
    }

    /** Copies a stream rewriting the upstream package name; matches across buffer boundaries are kept. */
    public static boolean copy(InputStream in, OutputStream out) {
        final int keep = FROM.length - 1;
        byte[] buffer = new byte[StreamUtils.BUFFER_SIZE + keep];
        int pending = 0;
        try {
            int read;
            while ((read = in.read(buffer, pending, buffer.length - pending)) != -1) {
                int length = pending + read;
                rewrite(buffer, length);
                int flush = Math.max(0, length - keep);
                out.write(buffer, 0, flush);
                pending = length - flush;
                System.arraycopy(buffer, flush, buffer, 0, pending);
            }
            out.write(buffer, 0, pending);
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }
}
