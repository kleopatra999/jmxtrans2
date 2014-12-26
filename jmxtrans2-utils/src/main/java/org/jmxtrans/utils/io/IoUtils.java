/**
 * The MIT License
 * Copyright (c) 2014 JMXTrans Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jmxtrans.utils.io;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* Created by gehel on 12/17/14.
*/
public final class IoUtils {
    private static final Logger LOGGER = Logger.getLogger(IoUtils.class.getName());

    private static final int COPY_BUFFER_SIZE = 512;

    /**
     * Simple implementation without chunking if the source file is big.
     *
     * @param source
     * @param destination
     * @throws java.io.IOException
     */
    public static void doCopySmallFile(@Nonnull File source, @Nonnull File destination, boolean append) throws IOException {
        if (destination.exists() && destination.isDirectory()) {
            throw new IOException("Can not copy file, destination is a directory: " + destination.getAbsolutePath());
        } else if (!destination.exists()) {
            boolean renamed = source.renameTo(destination);
            if (renamed) return;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(destination, append);
            if (append) {
                fos.write("\n".getBytes("UTF-8"));
            }
            fos.write(Files.readAllBytes(Paths.get(source.getAbsolutePath())));
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    /**
     * Simple implementation without chunking if the source file is big.
     *
     * @param source
     * @param destination
     * @throws java.io.IOException
     */
    private static void doCopySmallFile(@Nonnull File source, @Nonnull File destination) throws IOException {
        if (destination.exists() && destination.isDirectory()) {
            throw new IOException("Can not copy file, destination is a directory: " + destination.getAbsolutePath());
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel input = null;
        FileChannel output = null;
        try {
            fis = new FileInputStream(source);
            fos = new FileOutputStream(destination, false);
            input = fis.getChannel();
            output = fos.getChannel();
            output.transferFrom(input, 0, input.size());
        } finally {
            closeQuietly(output);
            closeQuietly(input);
            closeQuietly(fis);
            closeQuietly(fos);
        }

        if (destination.length() != source.length()) {
            throw new IOException("Failed to copy content from '" +
                    source + "' (" + source.length() + "bytes) to '" + destination + "' (" + destination.length() + ")");
        }
    }

    public static void appendToFile(File source, File destination, long maxFileSize, int maxBackupIndex) throws IOException {
        boolean destinationExists = validateDestinationFile(source, destination, maxFileSize, maxBackupIndex);
        if (destinationExists) {
            doCopySmallFile(source, destination, true);
        } else {
            boolean renamed = source.renameTo(destination);
            if (!renamed) {
                doCopySmallFile(source, destination, false);
            }
        }
    }

    private static boolean validateDestinationFile(File source, File destination, long maxFileSize, int maxBackupIndex) throws IOException {
        if (!destination.exists() || destination.isDirectory()) return false;
        long totalLengthAfterAppending = destination.length() + source.length();
        if (totalLengthAfterAppending > maxFileSize) {
            rollFiles(destination, maxBackupIndex);
            return false; // File no longer exists because it was move to filename.1
        }

        return true;
    }

    private static void rollFiles(File destination, int maxBackupIndex) throws IOException {

        // if maxBackup index == 10 then we will have file
        // outputFile, outpuFile.1 outputFile.2 ... outputFile.10
        // we only care if 9 and lower exists to move them up a number
        for (int i = maxBackupIndex - 1; i >= 0; i--) {
            String path = destination.getAbsolutePath();
            path=(i==0)?path:path + "." + i;
            File f = new File(path);
            if (!f.exists()) continue;

            File fNext = new File(destination + "." + (i + 1));
            doCopySmallFile(f, fNext, false);
        }

        if (!destination.delete()) {
            LOGGER.log(Level.WARNING, "Could not delete file [" + destination.getAbsolutePath() + "].");
        }
    }

    public static void replaceFile(File source, File destination) throws IOException {
        if (destination.exists()) {
            // try to delete destination, it might fail, but doCopySmallFile() can deal with it
            if (!destination.delete()) {
                LOGGER.info("Could not delete " + destination.getAbsolutePath() + ", replacing only the content");
            }
        }
        doCopySmallFile(source, destination);
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

    public static OutputStream nullOutputStream() {
        return new NullOutputStream();
    }

    public static void closeQuietly(Closeable closeable) {
        if (closeable == null)
            return;
        try {
            closeable.close();
        } catch (IOException e) {
            // Not being able to close something is still a problem : potential data loss, not releasing
            // resources, ... So we should still log it.
            LOGGER.log(Level.WARNING, "Could not close object");
        }
    }

    public static void closeQuietly(Writer writer) {
        if (writer == null)
            return;
        try {
            writer.close();
        } catch (IOException e) {
            // Not being able to close something is still a problem : potential data loss, not releasing
            // resources, ... So we should still log it.
            LOGGER.log(Level.WARNING, "Could not close writer");
        }
    }

    /**
     * Needed for old JVMs where {@link java.io.InputStream} does not implement {@link java.io.Closeable}.
     */
    public static void closeQuietly(InputStream inputStream) {
        if (inputStream == null)
            return;
        try {
            inputStream.close();
        } catch (IOException e) {
            // Not being able to close something is still a problem : potential data loss, not releasing
            // resources, ... So we should still log it.
            LOGGER.log(Level.WARNING, "Could not close input stream");
        }
    }

}
