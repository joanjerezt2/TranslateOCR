package org.apertium.lttoolbox.process;

import org.apertium.utils.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class InputFile  extends FilterInputStream {

    private InputStream inputSource;
    public int[] ubuffer = new int[3];
    public byte[] cbuffer = new byte[4];
    public int bufferSize = 0;
    private static final int U_EOF = -1;

    public InputFile(InputStream bufferedIn) {
        super(bufferedIn);
    }

    public byte get() throws IOException {
        if (bufferSize == 0) {
            internalRead();
        }
        return (byte) ubuffer[--bufferSize];
    }

    public int peek() throws IOException {
        if (bufferSize == 0) {
            internalRead();
        }
        return ubuffer[bufferSize - 1];
    }

    private void internalRead() throws IOException {
        // Return if the buffer already contains data.
        if (bufferSize != 0) {
            return;
        }
        // Read the first byte.
        int first = this.read();
        // Handle EOF.
        if (first == -1) {
            ubuffer[bufferSize++] = U_EOF;
            return;
        }
        // Handle the null byte.
        if (first == '\0') {
            ubuffer[bufferSize++] = '\0';
            return;
        }

        // Store the first byte.
        cbuffer[0] = (byte) first;

        // Determine the UTF-8 sequence length.
        int length;
        if ((first & 0xF0) == 0xF0) {
            length = 4;
        } else if ((first & 0xE0) == 0xE0) {
            length = 3;
        } else if ((first & 0xC0) == 0xC0) {
            length = 2;
        } else {
            length = 1;
        }

        // Read the remaining bytes.
        for (int i = 1; i < length; i++) {
            int next = this.read();
            if (next == -1) {
                throw new IOException("Could not read " + (length - 1) + " expected bytes from stream");
            }
            cbuffer[i] = (byte) next;
        }

        // Decode UTF-8 into a Unicode code point.
        int codePoint;
        if (length == 1) {
            codePoint = first;
        } else {
            codePoint = first & (0x7F >> length);
            for (int i = 1; i < length; i++) {
                int next = cbuffer[i] & 0xFF;
                if ((next & 0xC0) != 0x80) {
                    throw new IOException("Invalid UTF-8 continuation byte");
                }
                codePoint = (codePoint << 6) | (next & 0x3F);
            }
        }

        // Clear the output buffer.
        Arrays.fill(ubuffer, (byte) 0);
        // Store the decoded Unicode code point.
        ubuffer[0] = codePoint;
        bufferSize = 1;
    }

    public boolean eof() {
        try {
            // En Java no hay feof nativo, simulamos mirando si hay bytes disponibles
            // o intentando leer de forma no destructiva si el stream lo soporta.
            this.mark(1);
            int next = this.read();
            this.reset();
            return next == -1;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String finishWBlank() throws IOException {
        StringBuilder ret = new StringBuilder();
        ret.appendCodePoint('[');
        ret.appendCodePoint('[');
        int c;
        while (!eof()) {
            c = get();
            if (c == '\0') {
                break;
            }
            ret.appendCodePoint(c);
            if (c == '\\') {
                ret.appendCodePoint(get());
            } else if (c == ']' && peek() == ']') {
                ret.appendCodePoint(get());
                break;
            }
        }
        return ret.toString();
    }

    public String readBlock(char start, char end) throws IOException {
        StringBuilder ret = new StringBuilder();
        ret.appendCodePoint(start);
        char c = (char) 0;
        while (c != end) {
            c = (char) get();
            if (c == '\0') {
                break;
            }
            ret.appendCodePoint(c);
            if (c == '\\') {
                ret.appendCodePoint(get());
            }
        }
        return ret.toString();

    }

    public String readBlank() throws IOException {
        return readBlank(false);
    }

    public String readBlank(boolean readwblank) throws IOException {
        StringBuilder ret = new StringBuilder();
        while (!eof()) {
            byte c = get();
            if ((char) c == '^' || (char) c == '\0' || c == -1) {
                unget(c);
                break;
            }
            else if ((char) c == '[') {
                byte c2 = get();
                if (c2 == '[') {
                    if (readwblank) {
                        ret.append(finishWBlank());
                    } else {
                        unget(c2);
                        unget(c);
                        break;
                    }
                } else {
                    unget(c2);
                    ret.append(readBlock((char) c, ']'));
                }
            }
            else {
                ret.appendCodePoint(c);
                if (c == '\\') {
                    if (eof() || peek() == '\0') {
                        System.err.println("Unexpected trailing backslash");
                        System.exit(1);
                    }
                    ret.appendCodePoint(get());
                }
            }
        }
        return ret.toString();
    }

    public void unget(byte c) {
        ubuffer[bufferSize++] = c;
    }

    public boolean open(String fname) throws IOException {
        close();
        Reader infile;
        if (fname == null) {
            infile = IOUtils.getStdinReader();
        } else {
            try {
                infile = IOUtils.openInFileReader(fname);
            } catch (FileNotFoundException e) {
                infile = null;
            }
        }
        return (infile != null);
    }

    public boolean openInMemory(String inputBuffer) throws IOException {
        close();
        if (inputBuffer == null) return false;
        // Reemplaza a fmemopen convirtiendo el String en un flujo de bytes UTF-8
        byte[] bytes = inputBuffer.getBytes(StandardCharsets.UTF_8);
        this.inputSource = new ByteArrayInputStream(bytes);
        return true;
    }

    public void openOrExit(String fname) throws IOException {
        if (!open(fname)) {
            System.err.println("Error: Unable to open '" + fname + "' for reading.");
            System.exit(1); // EXIT_FAILURE
        }
    }

    public void wrap(InputStream infile) throws IOException {
        close();
        this.inputSource = infile;
    }

    public void rewind() {
        if (inputSource != null) {
            try {
                inputSource.reset();
            } catch (IOException e) {
                System.err.println("Error: Unable to rewind file");
                System.exit(1);
            }
        }
    }
}
