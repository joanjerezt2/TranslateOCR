package org.apertium.lttoolbox;

/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
import org.apertium.CommandLineInterface;
import org.xml.sax.SAXException;
import java.io.*;
import java.nio.file.Files;

/**
 * main class for dictionary expansion
 *
 * @author Raah
 */
public class LTExpand {
  private static void showHelp() {
      System.out.println(" v" + CommandLineInterface.PACKAGE_VERSION + ": expand the contents of a dictionary file"
              + "\nUSAGE: " + "lt-expand-j" + " dictionary_file [output_file]"
              + "\nUSAGE: " + "lt-expand-j" + " - [output_file] reads dix file from stdin and expands it"
      );
  }

  /**
   * Main method
   *
   * @param argv the command line arguments
   */
  public static void main(String[] argv) throws IOException, SAXException {

    int argc = argv.length;
    Writer output;

    switch (argc) {
      case 1:
        output = new OutputStreamWriter(System.out);
        break;

      case 2:
        output = fwrite(argv[1]);
        break;
      default:
        showHelp();
        return;
    }

    Expander e = new Expander();
    e.expand(argv[0], output);
    output.close();

  }

  private static Writer fwrite(String s) throws IOException {
    final File f = new File(s);
    return new OutputStreamWriter(Files.newOutputStream(f.toPath()));
  }
}
