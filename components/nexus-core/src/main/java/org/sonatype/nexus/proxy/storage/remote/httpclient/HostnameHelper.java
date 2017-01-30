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
package org.sonatype.nexus.proxy.storage.remote.httpclient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.util.SystemPropertiesHelper;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * Common utils for normalizing host names
 *
 * @since 2.14.3
 */
public class HostnameHelper
{
  private HostnameHelper() {
    //hide the constructor
  }

  /**
   * Normalizes proxy repository's hostname extracted from it's {@link ProxyRepository#getRemoteUrl()} method. Never
   * returns {@code null}.
   *
   * @see #normalizeHostname(String)
   */
  public static String normalizeHostname(final ProxyRepository proxyRepository) {
    try {
      final URI uri = new URI(proxyRepository.getRemoteUrl());
      return normalizeHostname(uri.getHost());
    }
    catch (URISyntaxException e) { // NOSONAR
      // ignore
    }
    return "";
  }

  /**
   * Normalizes passed in host name string by lower casing it. Never returns {@code null} even if input was {@code
   * null}.
   */
  public static String normalizeHostname(@Nullable final String hostName) {
    if (hostName == null) {
      return "";
    }
    else {
      return hostName.toLowerCase(Locale.US).trim();
    }
  }

  /**
   * Parses and normalizes (by lower-casing) CSV of host names under given property key. Never returns {@code null}.
   * Never returns a set that contains {@code null} or empty strings.
   */
  public static Set<String> parseAndNormalizeCsvProperty(final String systemPropertyKey) {
    return Sets.newHashSet(
        Iterables.transform(
            Splitter.on(",")
                .trimResults()
                .omitEmptyStrings()
                .split(SystemPropertiesHelper.getString(systemPropertyKey, "")),
            new Function<String, String>()
            {
              @Override
              public String apply(final String input) {
                return normalizeHostname(input);
              }
            }
        )
    );
  }
}
