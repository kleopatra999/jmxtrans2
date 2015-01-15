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
package org.jmxtrans.utils.net;

import javax.annotation.Nonnull;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Convenience class for writing bytes to a {@linkplain java.net.Socket}.
 *
 * @author <a href="mailto:cleclerc@xebia.fr">Cyrille Le Clerc</a>
 */
public class SocketOutputStream extends FilterOutputStream {

    @Nonnull
    private final Socket socket;

    public SocketOutputStream(@Nonnull Socket socket) throws IOException {
        super(socket.getOutputStream());
        this.socket = socket;
    }

    /**
     * Return the underlying {@linkplain java.net.Socket}
     */
    @Nonnull
    public Socket getSocket() {
        return socket;
    }

    @Override
    @Nonnull
    public String toString() {
        return "SocketOutputStream{" +
                "socket=" + socket +
                '}';
    }

}