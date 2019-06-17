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
package org.sonatype.nexus.repository.apt.internal.debian;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * More info about debian version could be found by ref:
 *
 * @see <a href="https://www.debian.org/doc/debian-policy/ch-controlfields.html#id6">https://www.debian.org/doc/</a>
 * @since 3.17
 */
public class DebianVersion
    implements Comparable<DebianVersion>
{
  private static final Pattern VERSION_PART = Pattern.compile("(\\D*)(\\d*)");

  private int epoch = 0;

  private String debianRevision = "";

  private String upstreamVersion;

  public DebianVersion(final String version) {
    checkNotNull(version);
    int colonIndex = version.indexOf(':');
    int hyphenIndex = version.lastIndexOf('-');

    this.epoch = parseEpoch(version, colonIndex);
    this.debianRevision = parseDebianRevision(version, hyphenIndex);
    this.upstreamVersion = parseUpstreamVersion(version, colonIndex, hyphenIndex);
  }

  public int getEpoch() {
    return epoch;
  }

  public String getDebianRevision() {
    return debianRevision;
  }

  public String getUpstreamVersion() {
    return upstreamVersion;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (epoch > 0) {
      sb.append(epoch);
      sb.append(":");
    }
    sb.append(upstreamVersion);
    if (debianRevision.length() > 0) {
      sb.append("-");
      sb.append(debianRevision);
    }
    return sb.toString();
  }

  @Override
  public int compareTo(final DebianVersion o) {
    if (this.epoch < o.epoch) {
      return -1;
    }
    else if (this.epoch > o.epoch) {
      return 1;
    }
    else {
      int uv = compareDebianVersion(this.upstreamVersion, o.upstreamVersion);
      if (uv != 0) {
        return uv;
      }

      return compareDebianVersion(this.debianRevision, o.debianRevision);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DebianVersion version = (DebianVersion) o;
    return epoch == version.epoch &&
        Objects.equals(debianRevision, version.debianRevision) &&
        Objects.equals(upstreamVersion, version.upstreamVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(epoch, debianRevision, upstreamVersion);
  }

  private int parseEpoch(final String version, final int colonIndex) {
    return colonIndex > 0 ? Integer.parseInt(version.substring(0, colonIndex)) : 0;
  }

  private String parseUpstreamVersion(final String version, final int colonIndex, final int hyphenIndex) {
    final int beginIndex = colonIndex > 0 ? colonIndex + 1 : 0;
    final int endIndex = hyphenIndex > 0 ? hyphenIndex : version.length();
    return version.substring(beginIndex, endIndex);
  }

  private String parseDebianRevision(final String version, final int hyphenIndex) {
    return hyphenIndex > 0 ? version.substring(hyphenIndex + 1) : "";
  }

  private static int compareDebianVersion(final String a, final String b) {
    Matcher ma = VERSION_PART.matcher(a);
    Matcher mb = VERSION_PART.matcher(b);

    String na = "";
    String nna = "";
    String nb = "";
    String nnb = "";
    do {
      if (ma.find()) {
        nna = ma.group(1);
        na = ma.group(2);
      }
      else {
        nna = "";
        na = "";
      }
      if (mb.find()) {
        nnb = mb.group(1);
        nb = mb.group(2);
      }
      else {
        nnb = "";
        nb = "";
      }

      int nn = compareNonNumeric(nna, nnb);
      if (nn != 0) {
        return nn;
      }

      int n = compareNumeric(na, nb);
      if (n != 0) {
        return n;
      }
    }
    while (na.length() > 0 || nna.length() > 0 || nb.length() > 0 || nnb.length() > 0);

    return 0;
  }

  private static int compareNonNumeric(final String a, final String b) {
    int len = Math.max(a.length(), b.length());
    for (int i = 0; i < len; i++) {
      int ac;
      int bc;
      if (i >= a.length()) {
        ac = -1;
      }
      else {
        ac = a.codePointAt(i);
      }
      if (i >= b.length()) {
        bc = -1;
      }
      else {
        bc = b.codePointAt(i);
      }

      if (priorityClass(ac) < priorityClass(bc)) {
        return -1;
      }
      else if (priorityClass(ac) > priorityClass(bc)) {
        return 1;
      }
      else if (ac < bc) {
        return -1;
      }
      else if (ac > bc) {
        return -1;
      }
    }

    return 0;
  }

  private static int compareNumeric(final String a, final String b) {
    if (a.isEmpty() && !b.isEmpty()) {
      return -1;
    }
    else if (b.isEmpty() && !a.isEmpty()) {
      return 1;
    }
    else if (a.isEmpty() && b.isEmpty()) {
      return 0;
    }

    return Long.compare(Long.parseLong(a), Long.parseLong(b));
  }

  private static int priorityClass(final int c) {
    return c == '~' ? -2 : c == -1 ? -1 : Character.isLetter(c) ? 0 : 1;
  }
}
