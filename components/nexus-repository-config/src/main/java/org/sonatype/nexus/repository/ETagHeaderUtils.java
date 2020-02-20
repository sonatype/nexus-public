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
package org.sonatype.nexus.repository;

import static org.apache.commons.lang.StringUtils.isEmpty;

public class ETagHeaderUtils
{
  public static final String WEAK_DESIGNATOR = "W/";

  private ETagHeaderUtils() {
  }

  /**
   * Adds quotes to etag header per spec.
   * https://tools.ietf.org/html/rfc7232#section-2.3
   */
  public static String quote(final String etag) {
    if (etag.startsWith(WEAK_DESIGNATOR)) {
      return etag;
    } else {
      return "\"" + etag + "\"";
    }
  }

  /**
   * Removes quotes from etag header.
   */
  public static String extract(final String etag) {
    if (!isEmpty(etag) && etag.startsWith("\"") && etag.endsWith("\"")) {
      return etag.substring(1, etag.length() - 1);
    } else {
      return etag;
    }
  }
}
