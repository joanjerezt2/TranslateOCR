package org.apertium.separable;

import org.apertium.utils.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class File extends Reader{

    public int[] ubuffer = new int[16];
    public int bufferSize = 0;
    public InputStream infile;

    public int get() {
        if (bufferSize == 0) {
            internalRead();
        }
        return ubuffer[--bufferSize];
    }

    private void internalRead() {
        if (bufferSize > 0) {
            return;
        }

        try {
            int first = this.read();
            if (first == -1) { // EOF
                ubuffer[bufferSize++] = -1;
                return;
            } else if (first == '\0') {
                ubuffer[bufferSize++] = '\0';
                return;
            }

            int i;
            byte[] cbuffer = new byte[16];
            char[] cbuf = new char[16];
            for(i=0; i < cbuffer.length; i++){
                cbuf[i] = (char) cbuffer[i];
            }
            cbuffer[0] = (byte) first;

            // Análisis de prefijos UTF-8 (Exactamente igual al C++ original)
            if ((first & 0xF0) == 0xF0) {
                i += 3;
                if (this.read(cbuf, 1, 3) != 3) {
                    throw new RuntimeException("Could not read 3 expected bytes from stream");
                }
            } else if ((first & 0xE0) == 0xE0) {
                i += 2;
                if (this.read(cbuf, 1, 2) != 2) {
                    throw new RuntimeException("Could not read 2 expected bytes from stream");
                }
            } else if ((first & 0xC0) == 0xC0) {
                i += 1;
                if (this.read(cbuf, 1, 1) != 1) {
                    throw new RuntimeException("Could not read 1 expected byte from stream");
                }
            }
            // Decodificación UTF-8 a UTF-32 (Code Point) en Java
            String tempStr = new String(cbuffer, 0, i, StandardCharsets.UTF_8);
            ubuffer[0] = tempStr.codePointAt(0);
            bufferSize = 1;

        } catch (IOException e) {
            ubuffer[bufferSize++] = -1;
        }
    }

    public boolean eof() {
        if (bufferSize > 0) return false;

        try {
            // En Java no hay feof nativo, simulamos mirando si hay bytes disponibles
            // o intentando leer de forma no destructiva si el stream lo soporta.
            this.mark(1);
            int next = this.read();
            this.reset();
            return next == -1;
        } catch (IOException e) {
            return true;
        }
    }

    public int peek() {
        if (bufferSize == 0) {
            internalRead();
        }
        return ubuffer[bufferSize - 1];
    }

    public String finishWBlank() {
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

    public String readBlock(char start, char end) {
        StringBuilder ret = new StringBuilder();
        ret.appendCodePoint(start);
        int c = 0;
        while (c != end && !eof()) {
            c = get();
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

    @Override
    public int read(char[] chars, int i, int i1) throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {

    }

    public String readBlank(){
        return readBlank(false);
    }

    public String readBlank(boolean readwblank) {
        StringBuilder ret = new StringBuilder();
        while (!eof()) {
            int c = get();
            if (c == '^' || c == '\0' || c == -1) {
                unget(c);
                break;
            } else if (c == '[') {
                int c2 = get();
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
            } else {
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

    public void unget(int c) {
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
        this.infile = new ByteArrayInputStream(bytes);
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
        this.infile = infile;
    }

    public void rewind() {
        if (infile != null) {
            try {
                infile.reset();
            } catch (IOException e) {
                System.err.println("Error: Unable to rewind file");
                System.exit(1);
            }
        }
    }

}
