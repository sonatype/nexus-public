/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.util;

import java.util.Comparator;

/**
 * This is an updated version with enhancements made by Daniel Migowski, Andre Bogus, and David Koelle To convert to
 * use
 * Templates (Java 1.5+): - Change "implements Comparator" to "implements Comparator<String>" - Change "compare(Object
 * o1, Object o2)" to "compare(String s1, String s2)" - Remove the type checking and casting in compare(). To use this
 * class: Use the static "sort" method from the java.util.Collections class: Collections.sort(your list, new
 * AlphanumComparator());
 */
public class AlphanumComparator
    implements Comparator<String>
{
  private final boolean isDigit(char ch) {
    return ch >= 48 && ch <= 57;
  }

  /**
   * Length of string is passed in for improved efficiency (only need to calculate it once) *
   */
  private final String getChunk(String s, int slength, int marker) {
    StringBuilder chunk = new StringBuilder();
    char c = s.charAt(marker);
    chunk.append(c);
    marker++;
    if (isDigit(c)) {
      while (marker < slength) {
        c = s.charAt(marker);
        if (!isDigit(c)) {
          break;
        }
        chunk.append(c);
        marker++;
      }
    }
    else {
      while (marker < slength) {
        c = s.charAt(marker);
        if (isDigit(c)) {
          break;
        }
        chunk.append(c);
        marker++;
      }
    }
    return chunk.toString();
  }

  public int compare(String s1, String s2) {
    if (s1 == null && s2 == null) {
      return 0;
    }

    if (s1 == null && s2 != null) {
      return -1;
    }

    if (s1 != null && s2 == null) {
      return 1;
    }

    int thisMarker = 0;
    int thatMarker = 0;
    int s1Length = s1.length();
    int s2Length = s2.length();

    while (thisMarker < s1Length && thatMarker < s2Length) {
      String thisChunk = getChunk(s1, s1Length, thisMarker);
      thisMarker += thisChunk.length();

      String thatChunk = getChunk(s2, s2Length, thatMarker);
      thatMarker += thatChunk.length();

      // If both chunks contain numeric characters, sort them numerically
      int result = 0;
      if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0))) {
        // Simple chunk comparison by length.
        int thisChunkLength = thisChunk.length();
        result = thisChunkLength - thatChunk.length();
        // If equal, the first different number counts
        if (result == 0) {
          for (int i = 0; i < thisChunkLength; i++) {
            result = thisChunk.charAt(i) - thatChunk.charAt(i);
            if (result != 0) {
              return result;
            }
          }
        }
      }
      else {
        result = thisChunk.compareTo(thatChunk);
      }

      if (result != 0) {
        return result;
      }
    }

    return s1Length - s2Length;
  }
}
