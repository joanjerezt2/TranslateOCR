/*
 * Copyright (C) 2010 Stephen Tigner
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
package org.apertium.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stephen Tigner
 *
 */
public class MiscUtils {
  public static String getLineSeparator() {
    return System.lineSeparator();
  }

  /* Joan Jerez, ToBeIT */

  public static int findAndReplace(StringBuilder str, String from, String to) {
    if (from == null || from.isEmpty() || str == null) return 0;

    // 1. Contar cuántas veces aparece "from" antes de reemplazar
    int ocurrencias = 0;
    int index = 0;
    while ((index = str.indexOf(from, index)) != -1) {
      ocurrencias++;
      index += from.length();
    }

    // 2. Hacer el reemplazo global de un solo golpe
    if (ocurrencias > 0) {
      String reemplazado = str.toString().replace(from, to);
      str.setLength(0);
      str.append(reemplazado);
    }

    return ocurrencias;
  }

}
