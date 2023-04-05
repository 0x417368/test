/*
 * Copyright (c) 2023, Daniel Gultsch
 *
 * This file is part of Conversations.
 *
 * Conversations is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Conversations is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Conversations.  If not, see <https://www.gnu.org/licenses/>.
 */

package im.conversations.android.xml;

import im.conversations.android.xmpp.model.StreamElement;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TagWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagWriter.class);

    private OutputStreamWriter outputStream;
    private boolean finished = false;
    private final LinkedBlockingQueue<StreamElement> writeQueue = new LinkedBlockingQueue<>();
    private CountDownLatch stanzaWriterCountDownLatch = null;

    private final Thread asyncStanzaWriter =
            new Thread() {

                @Override
                public void run() {
                    stanzaWriterCountDownLatch = new CountDownLatch(1);
                    while (!isInterrupted()) {
                        if (finished && writeQueue.size() == 0) {
                            break;
                        }
                        try {
                            final StreamElement output = writeQueue.take();
                            outputStream.write(output.toString());
                            if (writeQueue.size() == 0) {
                                outputStream.flush();
                            }
                        } catch (Exception e) {
                            break;
                        }
                    }
                    stanzaWriterCountDownLatch.countDown();
                }
            };

    public TagWriter() {}

    public synchronized void setOutputStream(OutputStream out) throws IOException {
        if (out == null) {
            throw new IOException();
        }
        this.outputStream = new OutputStreamWriter(out);
    }

    public void beginDocument() throws IOException {
        if (outputStream == null) {
            throw new IOException("output stream was null");
        }
        outputStream.write("<?xml version='1.0'?>");
    }

    public void writeTag(final Tag tag) throws IOException {
        writeTag(tag, true);
    }

    public synchronized void writeTag(final Tag tag, final boolean flush) throws IOException {
        if (outputStream == null) {
            throw new IOException("output stream was null");
        }
        outputStream.write(tag.toString());
        if (flush) {
            outputStream.flush();
        }
    }

    public synchronized void writeElement(Element element) throws IOException {
        if (outputStream == null) {
            throw new IOException("output stream was null");
        }
        outputStream.write(element.toString());
        outputStream.flush();
    }

    public void writeStanzaAsync(final StreamElement stanza) {
        if (finished) {
            LOGGER.info("attempting to write stanza to finished TagWriter");
        } else {
            if (!asyncStanzaWriter.isAlive()) {
                try {
                    asyncStanzaWriter.start();
                } catch (IllegalThreadStateException e) {
                    // already started
                }
            }
            writeQueue.add(stanza);
        }
    }

    public void finish() {
        this.finished = true;
    }

    public boolean await(long timeout, TimeUnit timeunit) throws InterruptedException {
        if (stanzaWriterCountDownLatch == null) {
            return true;
        } else {
            return stanzaWriterCountDownLatch.await(timeout, timeunit);
        }
    }

    public boolean isActive() {
        return outputStream != null;
    }

    public synchronized void forceClose() {
        asyncStanzaWriter.interrupt();
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                // ignoring
            }
        }
        outputStream = null;
    }
}
