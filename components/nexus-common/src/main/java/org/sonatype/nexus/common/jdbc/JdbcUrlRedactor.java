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
package org.sonatype.nexus.common.jdbc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

/**
 * Offers static helper methods for common operations to hide sensitive data in the jdbcUrl.
 *
 * @since 3.36
 */
public final class JdbcUrlRedactor
{
  private static final Pattern passwordPattern = compile("password=(.*?)(?=&\\w*=)|password=(.*)");

  public static final String REDACTED = "**REDACTED**";

  private JdbcUrlRedactor() {
    // empty
  }

  /**
   * Hide sensitive data in the jdbcUrl
   *
   * @param jdbcUrl original url
   * @return redacted url
   */
  public static String redactPassword(final String jdbcUrl) {
    String result = jdbcUrl;
    Matcher matcher = passwordPattern.matcher(jdbcUrl);
    while (matcher.find()) {
      String password = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
      result = result.replaceFirst("password=" + password, "password=" + REDACTED);
    }
    return result;
  }
}
