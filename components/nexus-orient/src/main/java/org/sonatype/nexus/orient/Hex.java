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
package org.sonatype.nexus.orient;

import com.google.common.io.BaseEncoding;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * HEX encoding helpers.
 *
 * @since 3.0
 */
class Hex
{
  private static final BaseEncoding HEX = BaseEncoding.base16().lowerCase();

  /**
   * HEX encode bytes to string.
   */
  public static String encode(final byte[] bytes) {
    return HEX.encode(bytes);
  }

  /**
   * HEX decode string to bytes.
   */
  public static byte[] decode(final String encoded) {
    checkNotNull(encoded);
    return HEX.decode(encoded.toLowerCase());
  }
}
