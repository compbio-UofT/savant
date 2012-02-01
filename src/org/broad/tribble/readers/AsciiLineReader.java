/*
 * Copyright (c) 2007-2009 by The Broad Institute, Inc. and the Massachusetts Institute of Technology.
 * All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL), Version 2.1 which
 * is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR WARRANTIES OF
 * ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT
 * OR OTHER DEFECTS, WHETHER OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR
 * RESPECTIVE TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES OF
 * ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES, ECONOMIC
 * DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER THE BROAD OR MIT SHALL
 * BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT SHALL KNOW OF THE POSSIBILITY OF THE
 * FOREGOING.
 */
package org.broad.tribble.readers;

import java.io.*;

/**
 * @author jrobinso
 */
public class AsciiLineReader implements LineReader {
    private static final byte LINEFEED = (byte) ('\n' & 0xff);
    private static final byte CARRIAGE_RETURN = (byte) ('\r' & 0xff);


    InputStream is;
    byte[] buffer;
    int nextChar;
    int nChars;
    char[] lineBuffer;
    long lineNumber = 0;
    long position;

    public AsciiLineReader(InputStream is) {
        this(is, 512000);
    }

    public AsciiLineReader(InputStream is, int bufferSize) {
        this.is = is;
        buffer = new byte[bufferSize];
        nextChar = nChars = 0;

        // Allocate this only once, even though it is essentially a local variable of
        // readLine.  This makes a huge difference in performance
        lineBuffer = new char[10000];
    }

    public long getPosition() {
        return position;
    }

    public void skip(long nBytes) throws IOException {
        is.skip(nBytes);
    }

    /**
     * Read a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), or a carriage return
     * followed immediately by a linefeed.
     *
     * @return A String containing the contents of the line or null if the
     *         end of the stream has been reached
     */
    public String readLine() throws IOException {
        int linePosition = 0;

        while (true) {
            if (nChars == -1) {
                return null;
            }

            // Refill buffer if neccessary
            if (nextChar == nChars) {
                fill();
                if (nextChar == nChars || nChars == -1) {
                    // eof reached.  Return the last line, or null if this is a new line
                    if (linePosition > 0) {
                        lineNumber++;
                        position += linePosition;
                        return new String(lineBuffer, 0, linePosition);
                    } else {
                        return null;
                    }
                }
            }


            char c = (char) (buffer[nextChar++] & 0xFF);
            if (c == LINEFEED || c == CARRIAGE_RETURN) {

                // + 1 for the terminator
                position += linePosition + 1;

                if (c == CARRIAGE_RETURN && peek() == LINEFEED) {
                    nextChar++; // <= skip the trailing \n in case of \r\n termination
                    position++;
                }
                lineNumber++;

                return new String(lineBuffer, 0, linePosition);
            } else {
                // Expand line buffer size if neccessary.  Reserve at least 2 characters
                // for potential line-terminators in return string

                if (linePosition > (lineBuffer.length - 3)) {
                    char[] temp = new char[lineBuffer.length + 1000];
                    System.arraycopy(lineBuffer, 0, temp, 0, lineBuffer.length);
                    lineBuffer = temp;
                }

                lineBuffer[linePosition++] = c;
            }
        }
    }

    /**
     * Peek ahead one character, filling from the underlying stream if neccessary.
     *
     * @return
     * @throws java.io.IOException
     */
    private char peek() throws IOException {
        // Refill buffer if neccessary
        if (nextChar == nChars) {
            fill();
            if (nextChar == nChars) {
                // eof reached.
                return 0;
            }
        }
        return (char) buffer[nextChar];

    }

    private void fill() throws IOException {
        nChars = is.read(buffer);
        nextChar = 0;
    }

    public void close() {
        try {
            is.close();
            lineNumber = 0;

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public long getCurrentLineNumber() {
        return lineNumber;
    }

    public static void main(String[] args) throws Exception {
        File testFile = new File("test/data/HindForGISTIC.hg16.cn");
        long t0, lineCount, dt;
        double rate;

        for (int i = 0; i < 3; i++) {
            BufferedReader reader2 = new BufferedReader(new FileReader(testFile));
            t0 = System.currentTimeMillis();
            lineCount = 0;
            while (reader2.readLine() != null) {
                lineCount++;
            }
            dt = System.currentTimeMillis() - t0;
            rate = ((double) lineCount) / dt;
            System.out.println("BR: " + lineCount + " lines read.  Rate = " + rate + " lines per second.   DT = " + dt);
            reader2.close();

            AsciiLineReader reader = new AsciiLineReader(new FileInputStream(testFile));
            t0 = System.currentTimeMillis();
            lineCount = 0;
            while (reader.readLine() != null) {
                lineCount++;
            }
            dt = System.currentTimeMillis() - t0;
            rate = ((double) lineCount) / dt;
            System.out.println("AR: " + lineCount + " lines read.  Rate = " + rate + " lines per second.     DT = " + dt);
            reader.close();
        }


    }
}

