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
package org.sonatype.nexus.repository.search.normalize;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

/**
 * Parses a version string and expands any numbers found to a fixed width using 0's.
 *
 * This ensures that the version string when sorted lexically will also be sorted numerically.
 * e.g.
 * <ul>
 * <li>000000001</li>
 * <li>000000002</li>
 * <li>000000010</li>
 * <li>000000113</li>
 * </ul>
 */
public class VersionNumberExpander
{
  private static final String NUMBER_FORMAT = "%09d";

  private static final String SEGMENT_PREFIX = "1";

  private static final String PREFIXED_NUMBER_FORMAT = SEGMENT_PREFIX + NUMBER_FORMAT;

  private static final Pattern DIGITS = Pattern.compile("\\d+");

  private VersionNumberExpander() {
  }

  public static final Logger log = LoggerFactory.getLogger(VersionNumberExpander.class);

  public static String expand(String version) {
    if (StringUtils.isBlank(version)) {
      return "";
    }

    Matcher matcher = DIGITS.matcher(version);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      try {
        matcher.appendReplacement(result, String.format(NUMBER_FORMAT, Long.parseLong(matcher.group())));
      }
      catch (NumberFormatException e) {
        log.debug("Unable to parse number as long '{}'", matcher.group());
        matcher.appendReplacement(result, matcher.group());
      }
    }

    matcher.appendTail(result);
    return result.toString();
  }

  /**
   * Same width expansion as {@link #expand(String)}, but every expanded number is additionally
   * prefixed with a {@code 1}.
   *
   * This lifts real version segments out of the {@code 0} leading-digit space so that a tier marker
   * prefixed with {@code 0} sorts below any real segment, at any segment depth. Formats whose keys
   * append a tier marker to a variable number of segments require this; without it the marker
   * outranks the digit a deeper version carries in the same position.
   */
  public static String expandPrefixed(String version) {
    if (StringUtils.isBlank(version)) {
      return "";
    }

    Matcher matcher = DIGITS.matcher(version);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      try {
        matcher.appendReplacement(result, String.format(PREFIXED_NUMBER_FORMAT, Long.parseLong(matcher.group())));
      }
      catch (NumberFormatException e) {
        log.debug("Unable to parse number as long '{}'", matcher.group());
        matcher.appendReplacement(result, SEGMENT_PREFIX + matcher.group());
      }
    }

    matcher.appendTail(result);
    return result.toString();
  }

  /**
   * Expands a single number to the same prefixed form used by {@link #expandPrefixed(String)}.
   */
  public static String padPrefixed(long number) {
    return String.format(PREFIXED_NUMBER_FORMAT, number);
  }
}
