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
package org.sonatype.nexus.internal.web;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.eclipse.sisu.Hidden;

/**
 * Filter for sanitizing http header values
 *
 * @since 3.2
 */
@Named
@Hidden // hide from DynamicFilterChainManager because we statically install it in WebModule
@Singleton
public class HeaderPatternFilter
    extends ComponentSupport
    implements Filter
{

  private static final String PATTERNS_PROPERTIES_FILE = "http-headers-patterns.properties";

  private ImmutableMap<String, Pattern> validHeaderPatterns;

  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {
    ImmutableMap.Builder<String, Pattern> builder = new ImmutableMap.Builder<>();
    Properties properties = new Properties();
    try (InputStream stream = getClass().getResourceAsStream(PATTERNS_PROPERTIES_FILE)) {
      properties.load(stream);
    }
    catch (IOException ioe) {
      log.error("IOException loading {} as a resource stream", PATTERNS_PROPERTIES_FILE, ioe);
    }
    for (String key : properties.stringPropertyNames()) {
      String val = properties.getProperty(key);
      try {
        builder.put(key, Pattern.compile(val));
      }
      catch (PatternSyntaxException pse) {
        log.error("unable to compile the pattern for the header '{}', failed pattern is '{}', skipping", key, val, pse);
      }
    }

    validHeaderPatterns = builder.build();
  }

  @Override
  public void doFilter(
      final ServletRequest request,
      final ServletResponse response,
      final FilterChain chain) throws IOException, ServletException
  {
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;
      for (Entry<String, Pattern> entry : validHeaderPatterns.entrySet()) {
        if (checkForBadHeader(httpRequest.getHeaders(entry.getKey()), entry.getValue())) {
          log.warn("rejecting request from {} due to invalid header '{}: {}'", request.getRemoteHost(), entry.getKey(),
              Joiner.on(",").join(Collections.list(httpRequest.getHeaders(entry.getKey()))));
          httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          return;
        }
      }
    }
    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    // ignore
  }

  private static boolean checkForBadHeader(final Enumeration<String> headers, final Pattern expression) {
    while (headers != null && headers.hasMoreElements()) {
      String header = headers.nextElement();
      if (!Strings.isNullOrEmpty(header) && !expression.matcher(header).matches()) {
        return true;
      }
    }
    return false;
  }

}
