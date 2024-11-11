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
package org.sonatype.nexus.validation.constraint;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidatorContext;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.validation.ConstraintValidatorSupport;

/**
 * Validates a uri.
 *
 * @since 3.3
 */
public class UrlValidator
    extends ConstraintValidatorSupport<Url, URI>
{
  private static final Pattern SCHEME_RE = Pattern.compile("^https?$", Pattern.CASE_INSENSITIVE);

  private static final Pattern USER_INFO_RE = Pattern.compile("^(?:\\S+(?::\\S*)?)?$", Pattern.CASE_INSENSITIVE);

  private static final Pattern RESOURCE_RE = Pattern.compile("^(?:[ \\S]*\\S)?$", Pattern.CASE_INSENSITIVE);

  private static final Pattern HOSTNAME_RE = Pattern.compile("^(?:[^\"<>^`{|}:/]+)$", Pattern.CASE_INSENSITIVE);

  private static final Pattern IPV6_RE = Pattern.compile("^\\[(?<ipv6>[0-9:a-f]{3,39})\\]$", Pattern.CASE_INSENSITIVE);

  @Override
  public boolean isValid(final URI uri, final ConstraintValidatorContext constraintValidatorContext) {
    if (uri == null) {
      return true;
    }

    try {
      @SuppressWarnings("unused")
      URL ignored = uri.toURL(); // NOSONAR

      return isValidScheme(uri.getScheme())
          && isValidUserInfo(uri.getUserInfo())
          && isValidHost(uri.getHost())
          && isValidPort(uri.getPort())
          && isValidPath(uri.getPath())
          && isValidFragment(uri.getFragment());
    }
    catch (MalformedURLException | IllegalArgumentException e) {// NOSONAR
      log.debug("Failed to parse URL from {} with message {}", uri, e.getMessage());
    }

    return false;
  }

  private boolean isMatch(final Pattern pattern, final String string, final boolean allowBlank) {
    return (allowBlank && Strings2.isBlank(string))
        || (string != null && pattern.matcher(string).find());
  }

  private boolean isMatch(final Pattern pattern, final String string) {
    return isMatch(pattern, string, true);
  }

  private boolean isValidScheme(final String scheme) {
    return isMatch(SCHEME_RE, scheme);
  }

  private boolean isValidUserInfo(final String userInfo) {
    return isMatch(USER_INFO_RE, userInfo);
  }

  private boolean isValidHost(final String host) {
    return isMatch(HOSTNAME_RE, host, false)
        || (isMatch(IPV6_RE, host, false));
  }

  private boolean isValidPort(final int port) {
    return port == -1 || (port > 0 && port <= 65535);
  }

  private boolean isValidPath(final String path) {
    return isMatch(RESOURCE_RE, path);
  }

  private boolean isValidFragment(final String fragment) {
    return isMatch(RESOURCE_RE, fragment);
  }
}
