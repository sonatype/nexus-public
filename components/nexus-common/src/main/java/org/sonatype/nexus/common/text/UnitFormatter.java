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
package org.sonatype.nexus.common.text;

import java.util.Locale;

import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static java.lang.Math.log10;
import static java.lang.Math.pow;
import static java.lang.String.format;

/**
 * @since 3.24
 */
public final class UnitFormatter {

  //Must be in ascending order of exponent
  private enum StoragePrefix
  {
    BYTE("B", pow(10, 0)),
    KILO("KB", pow(10, 3)),
    MEGA("MB", pow(10, 6)),
    GIGA("GB", pow(10, 9)),
    TERA("TB", pow(10, 12)),
    PETA("PB", pow(10, 15)),
    EXA("EB", pow(10, 18));

    final String name;

    final double value;

    StoragePrefix(final String name, final double value) {
      this.name = name;
      this.value = value;
    }
  }

  public static String formatStorage(final long bytes) {
    StoragePrefix prefix;

    if (bytes == 0) {
      prefix = StoragePrefix.BYTE;
    }
    else {
      double exponent = floor(log10(abs(bytes)));
      prefix = StoragePrefix.values()[(int) exponent / 3];
    }

    return format(Locale.ENGLISH, "%.2f %s", bytes / prefix.value, prefix.name);
  }
}
