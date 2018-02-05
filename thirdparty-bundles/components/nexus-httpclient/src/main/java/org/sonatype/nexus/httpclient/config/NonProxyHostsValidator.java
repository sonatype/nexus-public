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
package org.sonatype.nexus.httpclient.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

/**
 * {@link NonProxyHosts} validator.
 *
 * @since 3.0
 */
public class NonProxyHostsValidator
    extends ConstraintValidatorSupport<NonProxyHosts, Collection<String>>
{
  /**
   * Adapter for validating array fields.
   */
  public static class ForArray
      extends ConstraintValidatorSupport<NonProxyHosts, String[]>
  {
    @Override
    public boolean isValid(final String[] values, final ConstraintValidatorContext context) {
      return values != null ? NonProxyHostsValidator.isValid(Arrays.asList(values)) : true;
    }
  }

  /**
   * Pattern checking for allowed characters, best we can do, as input may be:
   * <ul>
   * <li>hostname w/o or w/ wildcard (incomplete)</li>
   * <li>IPv4 address w/o or w/ wildcard (incomplete)</li>
   * <li>IPv6 non-compressed address w/o or w/ wildcard (incomplete)</li>
   * <li>IPv6 compressed address w/o or w/ wildcard (incomplete)</li>
   * </ul>
   * Due to the "incomplete" case (hostname may be missing domain, IPv4 might have one, two, three or four segments,
   * IPv6 might have three or more segments) the validation we perform here is basically just enforcing allowed
   * characters, and simply not treating these as hostname or IP address, but merely as an opaque pattern.
   * If wildcard present, wildcard in a nonProxyHost element may be only on it's beginning or end, nowhere else.
   */
  private static final Pattern CONTENT_PATTERN = Pattern
      .compile("\\*?[\\p{IsAlphabetic}|\\d|\\-|\\_|\\.|\\:|\\[|\\]]+\\*?");

  @Override
  public boolean isValid(final Collection<String> values, final ConstraintValidatorContext context) {
    return values != null ? isValid(values) : true;
  }

  static boolean isValid(Collection<String> values) {
    for (String value : values) {
      if (!isValid(value)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if value is considered as valid nonProxyHosts expression. This is NOT validating the
   * single-string used to set system property (where expressions are delimited with "|")!
   * 
   * @since 3.1
   */
  public static boolean isValid(String value) {
    // A value should be a non-empty string optionally prefixed or suffixed with an asterisk
    // must be non-empty, non-blank
    if (Strings2.isBlank(value)) {
      return false;
    }
    // must not contain | separator (used to separate multiple values in system properties)
    if (value.indexOf('|') > -1) {
      return false;
    }
    // asterisk '*' can be only on beginning or end
    return CONTENT_PATTERN.matcher(value).matches();
  }
}
