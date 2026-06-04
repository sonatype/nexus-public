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
package org.sonatype.nexus.common.io;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.io.ByteSize.ByteUnit.BYTES;
import static org.sonatype.nexus.common.io.ByteSize.ByteUnit.GIGABYTES;
import static org.sonatype.nexus.common.io.ByteSize.ByteUnit.KILOBYTES;
import static org.sonatype.nexus.common.io.ByteSize.ByteUnit.MEGABYTES;
import static org.sonatype.nexus.common.io.ByteSize.ByteUnit.TERABYTES;

/**
 * Representation of a byte size.
 *
 * Supports:
 *
 * <ul>
 * <li>BYTES</li>
 * <li>KILOBYTES</li>
 * <li>MEGABYTES</li>
 * <li>GIGABYTES</li>
 * <li>TERABYTES</li>
 * </ul>
 *
 */
public final class ByteSize
{
  public static enum ByteUnit
  {
    BYTES(1L),
    KILOBYTES(1024L),
    MEGABYTES(1024L * 1024),
    GIGABYTES(1024L * 1024 * 1024),
    TERABYTES(1024L * 1024 * 1024 * 1024);

    private final long byteFactor;

    ByteUnit(final long byteFactor) {
      this.byteFactor = byteFactor;
    }

    private long convert(final long value, final ByteUnit target) {
      return value * byteFactor / target.byteFactor;
    }

    public long asBytes(final long value) {
      return convert(value, BYTES);
    }

    public long asKiloBytes(final long value) {
      return convert(value, KILOBYTES);
    }

    public long asMegaBytes(final long value) {
      return convert(value, MEGABYTES);
    }

    public long asGigaBytes(final long value) {
      return convert(value, GIGABYTES);
    }

    public long asTeraBytes(final long value) {
      return convert(value, TERABYTES);
    }
  }

  private final long value;

  private final ByteUnit unit;

  public ByteSize(final long value, final ByteUnit unit) {
    this.value = value;
    this.unit = checkNotNull(unit);
  }

  public long value() {
    return value;
  }

  /**
   */
  public int valueI() {
    return (int) value();
  }

  public ByteUnit unit() {
    return unit;
  }

  public long toBytes() {
    return unit.asBytes(value);
  }

  /**
   */
  public int toBytesI() {
    return (int) toBytes();
  }

  public ByteSize asBytes() {
    return bytes(toBytes());
  }

  public long toKiloBytes() {
    return unit.asKiloBytes(value);
  }

  /**
   */
  public int toKiloBytesI() {
    return (int) toKiloBytes();
  }

  public ByteSize asKiloBytes() {
    return kiloBytes(toKiloBytes());
  }

  public long toMegaBytes() {
    return unit.asMegaBytes(value);
  }

  /**
   */
  public int toMegaBytesI() {
    return (int) toMegaBytes();
  }

  public ByteSize asMegaBytes() {
    return megaBytes(toMegaBytes());
  }

  public long toGigaBytes() {
    return unit.asGigaBytes(value);
  }

  /**
   */
  public int toGigaBytesI() {
    return (int) toGigaBytes();
  }

  public ByteSize asGigaBytes() {
    return gigaBytes(toGigaBytes());
  }

  public long toTeraBytes() {
    return unit.asTeraBytes(value);
  }

  /**
   */
  public int toTeraBytesI() {
    return (int) toTeraBytes();
  }

  public ByteSize asTeraBytes() {
    return teraBytes(toTeraBytes());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ByteSize that = (ByteSize) obj;
    return value == that.value && unit == that.unit;
  }

  @Override
  public int hashCode() {
    int result = (int) (value ^ (value >>> 32));
    result = 31 * result + unit.hashCode();
    return result;
  }

  private String unitName() {
    // TODO: i18n support?
    String name = unit.name().toLowerCase();
    if (value == 1) {
      name = name.substring(0, name.length() - 1);
    }
    return name;
  }

  @Override
  public String toString() {
    return String.format("%d %s", value, unitName());
  }

  public static ByteSize size(final long value, final ByteUnit unit) {
    return new ByteSize(value, unit);
  }

  public static ByteSize bytes(final long value) {
    return new ByteSize(value, BYTES);
  }

  public static ByteSize kiloBytes(final long value) {
    return new ByteSize(value, KILOBYTES);
  }

  public static ByteSize megaBytes(final long value) {
    return new ByteSize(value, MEGABYTES);
  }

  public static ByteSize gigaBytes(final long value) {
    return new ByteSize(value, GIGABYTES);
  }

  public static ByteSize teraBytes(final long value) {
    return new ByteSize(value, TERABYTES);
  }

  //
  // Parsing
  //

  public static ByteSize parse(final String value) {
    if (value != null) {
      return doParse(value.trim().toLowerCase());
    }
    return null;
  }

  private static class ParseConfig
  {
    final ByteUnit unit;

    final String[] suffixes;

    private ParseConfig(final ByteUnit unit, final String... suffixes) {
      this.unit = unit;
      this.suffixes = suffixes;
    }
  }

  private static final ParseConfig[] PARSE_CONFIGS = {
      new ParseConfig(BYTES, "bytes", "byte", "b"),
      new ParseConfig(KILOBYTES, "kilobytes", "kilobyte", "kib", "kb", "k"),
      new ParseConfig(MEGABYTES, "megabytes", "megabyte", "mib", "mb", "m"),
      new ParseConfig(GIGABYTES, "gigabytes", "gigabyte", "gib", "gb", "g"),
      new ParseConfig(TERABYTES, "terabytes", "terabyte", "tib", "tb", "t"),
  };

  private static ByteSize doParse(final String value) {
    for (ParseConfig config : PARSE_CONFIGS) {
      ByteSize t = extract(value, config.unit, config.suffixes);
      if (t != null) {
        return t;
      }
    }
    throw new RuntimeException("Unable to parse: " + value);
  }

  private static ByteSize extract(final String value, final ByteUnit unit, final String... suffixes) {
    String number = null, units = null;

    for (int p = 0; p < value.length(); p++) {
      // skip until we find a non-digit
      if (Character.isDigit(value.charAt(p))) {
        continue;
      }
      // split number and units suffix string
      number = value.substring(0, p);
      units = value.substring(p, value.length()).trim();
      break;
    }

    // if decoded units, check if its one of the supported suffixes
    if (units != null) {
      for (String suffix : suffixes) {
        if (suffix.equals(units)) {
          long n = Long.parseLong(number.trim());
          return new ByteSize(n, unit);
        }
      }
    }

    // else can not extract
    return null;
  }
}
