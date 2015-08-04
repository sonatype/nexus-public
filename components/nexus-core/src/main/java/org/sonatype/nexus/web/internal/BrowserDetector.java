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
package org.sonatype.nexus.web.internal;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import eu.bitwalker.useragentutils.UserAgent;
import org.apache.shiro.web.util.WebUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper to detect if a servlet-request is initiated from a web-browser.
 *
 * @since 2.8
 */
@Named
@Singleton
public class BrowserDetector
  extends ComponentSupport
{
  @VisibleForTesting
  static final String USER_AGENT = "User-Agent";

  private final boolean disable;

  private final Set<String> excludedUserAgents = Sets.newHashSet();

  private final Cache<String, UserAgent> cache = CacheBuilder.newBuilder()
      .maximumSize(100)
      .expireAfterWrite(2, TimeUnit.HOURS)
      .build();

  @Inject
  public BrowserDetector(final @Named("${nexus.browserdetector.disable:-false}") boolean disable,
                         final @Named("${nexus.browserdetector.excludedUserAgents}") @Nullable String excludedUserAgents) {
    this.disable = disable;
    if (disable) {
      log.info("Browser detector disabled");
    }

    if (excludedUserAgents != null) {
      for (String userAgent : Splitter.on(Pattern.compile("\r?\n")).trimResults().split(excludedUserAgents)) {
        if (userAgent.length() > 0) {
          log.info("Browser detector excluding User-Agent: {}", userAgent);
          this.excludedUserAgents.add(userAgent);
        }
      }
    }
  }

  /**
   * Determine if the given request appears to be initiated from a web-browser.
   */
  public boolean isBrowserInitiated(final ServletRequest request) {
    checkNotNull(request);

    // skip if disabled
    if (disable) {
      return false;
    }

    String userAgentString = WebUtils.toHttp(request).getHeader(USER_AGENT);

    // skip if excluded
    if (excludedUserAgents.contains(userAgentString)) {
      return false;
    }

    UserAgent userAgent = parseUserAgent(userAgentString);
    if (userAgent != null) {
      switch (userAgent.getBrowser().getBrowserType()) {
        case WEB_BROWSER:
        case MOBILE_BROWSER:
        case TEXT_BROWSER:
          return true;
      }
    }
    return false;
  }

  @Nullable
  private UserAgent parseUserAgent(final String headerValue) {
    if (headerValue == null) {
      return null;
    }

    UserAgent userAgent = cache.getIfPresent(headerValue);
    if (userAgent == null) {
      userAgent = UserAgent.parseUserAgentString(headerValue);
      cache.put(headerValue, userAgent);
    }
    return userAgent;
  }
}
