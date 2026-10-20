/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apertium.transfer;

import org.apertium.lttoolbox.Compression;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author Jacob Nordfalk
 */
public class ApertiumRE {
  Pattern re;
  private static final int EXIT_FAILURE = 1;

  // Consider using http://jakarta.apache.org/regexp/apidocs/org/apache/regexp/RE.html
  public ApertiumRE(String regex) {
    re = Pattern.compile(regex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE); // |Pattern.UNICODE_CASE
    // PCRE_DOTALL|PCRE_CASELESS|PCRE_EXTENDED|PCRE_UTF8,
  }

  public String toString() {
    return re.toString();
  }
  public static final String EMPTY_STRING = "";

  public String match(String source) {
    Matcher m = re.matcher(source);
    if (!m.find())
      return "";
    return m.group();
  }

  public String replace(String source, String value) {
    Matcher m = re.matcher(source);
    if (!m.find())
      return source;
    return source.substring(0, m.start()) + value + source.substring(m.end());
  }

  /**
   * @author Joan Jerez, ToBeIT
   */

  public void read(InputStream input) throws IOException {
    int size = Compression.multibyte_read(input);

    try {
      long bytesSkipped = input.skip(size);
      if (bytesSkipped != size) {
        // En Java, skip puede saltar menos bytes si llega al final del archivo (EOF)
        throw new IOException("No se pudo saltar la cantidad de bytes requerida");
      }
    } catch (IOException e) {
      System.err.println("Error reading regexp");
      System.exit(1); // Equivalente a EXIT_FAILURE
    }
  }

  public void compile(String str) {
    // En Java no hace falta hacer "delete re", el Garbage Collector se encarga automáticamente.
    try {
      // UREGEX_DOTALL -> Pattern.DOTALL
      // UREGEX_CASE_INSENSITIVE -> Pattern.CASE_INSENSITIVE
      // El flag Pattern.UNICODE_CASE se añade para asegurar el comportamiento Unicode idéntico a ICU
      int flags = Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
      re = Pattern.compile(str, flags);

    } catch (PatternSyntaxException err) {
      // El bloque catch captura los errores equivalentes a U_FAILURE(err)
      System.err.println("Error: unable to compile regular expression '" + str + "'.");
      System.err.println("error code: " + err.getMessage());
      System.exit(EXIT_FAILURE);
    }
  }
}
