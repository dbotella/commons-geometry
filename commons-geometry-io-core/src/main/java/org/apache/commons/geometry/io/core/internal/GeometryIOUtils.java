/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.geometry.io.core.internal;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.stream.Stream;

/** Class containing utility methods for IO operations.
 */
public final class GeometryIOUtils {

    /** Path separator character used on Unix-like systems. */
    private static final char UNIX_PATH_SEP = '/';

    /** Path separator character used on Windows. */
    private static final char WINDOWS_PATH_SEP = '\\';

    /** Utility class; no instantiation. */
    private GeometryIOUtils() {}

    /** Get the file name of the given path or null if one does not exist
     * or is the empty string.
     * @param path path to get the file name of
     * @return file name of the given path
     */
    public static String getFileName(final Path path) {
        if (path != null) {
            return getFileName(path.toString());
        }

        return null;
    }

    /** Get the file name of the given url or null if one does not exist or is
     * the empty string.
     * @param url url to get the file name of
     * @return file name of the given url
     */
    public static String getFileName(final URL url) {
        if (url != null) {
            return getFileName(url.getPath());
        }

        return null;
    }

    /** Get the file name from the given path string, defined as
     * the substring following the last path separator character.
     * Null is returned if the argument is null or the file name is
     * the empty string.
     * @param path path to get the file name from
     * @return file name of the given path string or null if a
     *      non-empty file name does not exist
     */
    public static String getFileName(final String path) {
        if (path != null) {
            final int lastSep = Math.max(
                    path.lastIndexOf(UNIX_PATH_SEP),
                    path.lastIndexOf(WINDOWS_PATH_SEP));

            if (lastSep < path.length() - 1) {
                return path.substring(lastSep + 1);
            }
        }

        return null;
    }

    /** Get the part of the file name after the last dot.
     * @param fileName file name to get the extension for
     * @return the extension of the file name, the empty string if no extension is found, or
     *      null if the argument is null
     */
    public static String getFileExtension(final String fileName) {
        if (fileName != null) {
            final int idx = fileName.lastIndexOf('.');
            if (idx > -1) {
                return fileName.substring(idx + 1);
            }

            return "";
        }

        return null;
    }

    /** Create an unchecked exception from the given checked exception. The message of the
     * returned exception contains the original exception's type and message.
     * @param exc exception to wrap in an unchecked exception
     * @return the unchecked exception
     */
    public static UncheckedIOException createUnchecked(final IOException exc) {
        final String msg = exc.getClass().getSimpleName() + ": " + exc.getMessage();
        return new UncheckedIOException(msg, exc);
    }

    /** Pass a supplied {@link Closeable} instance to {@code function} and return the result.
     * The {@code Closeable} instance returned by the supplier is closed if function execution
     * fails, otherwise the instance is <em>not</em> closed.
     * @param <T> Return type
     * @param <C> Closeable type
     * @param function function called with the supplied Closeable instance
     * @param closeableSupplier supplier used to obtain a Closeable instance
     * @return result of calling {@code function} with a supplied Closeable instance
     * @throws IOException if an I/O error occurs
     */
    public static <T, C extends Closeable> T tryApplyCloseable(final IOFunction<C, T> function,
            final IOSupplier<? extends C> closeableSupplier) throws IOException {
        C closeable = null;
        try {
            closeable = closeableSupplier.get();
            return function.apply(closeable);
        } catch (IOException | RuntimeException exc) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (IOException suppressed) {
                    exc.addSuppressed(suppressed);
                }
            }

            throw exc;
        }
    }

    /** Create a stream associated with an input stream. The input stream is closed when the
     * stream is closed and also closed if stream creation fails. Any {@link IOException} thrown
     * when the input stream is closed after the return of this method are wrapped with {@link UncheckedIOException}.
     * @param <T> Stream element type
     * @param <I> Input stream type
     * @param streamFunction function accepting an input stream and returning a stream
     * @param inputStreamSupplier supplier used to obtain the input stream
     * @return stream associated with the input stream return by the supplier
     * @throws IOException if an I/O error occurs during input stream and stream creation
     */
    public static <T, I extends InputStream> Stream<T> createCloseableStream(
            final IOFunction<I, Stream<T>> streamFunction, final IOSupplier<? extends I> inputStreamSupplier)
                throws IOException {
        return tryApplyCloseable(
                in -> streamFunction.apply(in).onClose(closeAsUncheckedRunnable(in)),
                inputStreamSupplier);
    }

    /** Return a {@link Runnable} that calls {@link Closeable#getClass() close()} on the argument,
     * wrapping any {@link IOException} with {@link UncheckedIOException}.
     * @param closeable instance to be closed
     * @return runnable that calls {@code close()) on the argument
     */
    private static Runnable closeAsUncheckedRunnable(final Closeable closeable) {
        return () -> {
            try {
                closeable.close();
            } catch (IOException exc) {
                throw createUnchecked(exc);
            }
        };
    }
}
