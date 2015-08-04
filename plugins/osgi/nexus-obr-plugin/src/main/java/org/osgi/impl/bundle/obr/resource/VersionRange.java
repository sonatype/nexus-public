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
package org.osgi.impl.bundle.obr.resource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Version;

public class VersionRange
    implements Comparable
{
  Version high;

  Version low;

  char start = '[';

  char end = ']';

  static String V = "[0-9]+(\\.[0-9]+(\\.[0-9]+(\\.[a-zA-Z0-9_-]+)?)?)?";

  static Pattern RANGE = Pattern.compile("(\\(|\\[)\\s*(" + V + ")\\s*,\\s*(" + V
      + ")\\s*(\\)|\\])");

  public VersionRange(String string) {
    string = string.trim();
    Matcher m = RANGE.matcher(string);
    if (m.matches()) {
      start = m.group(1).charAt(0);
      low = new Version(m.group(2));
      high = new Version(m.group(6));
      end = m.group(10).charAt(0);
      if (low.compareTo(high) > 0) {
        throw new IllegalArgumentException(
            "Low Range is higher than High Range: " + low + "-"
                + high);
      }

    }
    else {
      high = low = new Version(string);
    }
  }

  public boolean isRange() {
    return high != low;
  }

  public boolean includeLow() {
    return start == '[';
  }

  public boolean includeHigh() {
    return end == ']';
  }

  public String toString() {
    if (high == low) {
      return high.toString();
    }

    StringBuffer sb = new StringBuffer();
    sb.append(start);
    sb.append(low);
    sb.append(',');
    sb.append(high);
    sb.append(end);
    return sb.toString();
  }

  public boolean equals(Object other) {
    if (other instanceof VersionRange) {
      return compareTo(other) == 0;
    }
    return false;
  }

  public int hashCode() {
    return low.hashCode() * high.hashCode();
  }

  public int compareTo(Object other) {
    VersionRange range = (VersionRange) other;
    VersionRange a = this, b = range;
    if (range.isRange()) {
      a = range;
      b = this;
    }
    else {
      if (!isRange()) {
        return low.compareTo(range.high);
      }
    }
    int l = a.low.compareTo(b.low);
    boolean ll = false;
    if (a.includeLow()) {
      ll = l <= 0;
    }
    else {
      ll = l < 0;
    }

    if (!ll) {
      return -1;
    }

    int h = a.high.compareTo(b.high);
    if (a.includeHigh()) {
      ll = h >= 0;
    }
    else {
      ll = h > 0;
    }

    if (ll) {
      return 0;
    }
    else {
      return 1;
    }
  }
}