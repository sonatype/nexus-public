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
package org.sonatype.nexus.repository.httpbridge.internal;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.sonatype.nexus.capability.CapabilityEvent;
import org.sonatype.nexus.common.event.EventAware;
import org.sonatype.nexus.repository.httpbridge.legacy.LegacyUrlCapabilityDescriptor;
import org.sonatype.nexus.repository.httpbridge.legacy.LegacyUrlEnabledHelper;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.springframework.stereotype.Component;

import static jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND;

@WebFilter(urlPatterns = {"/content/groups/*", "/content/repositories/*", "/content/sites/*", "/service/local/*"})
@Component
public class LegacyViewFilter
    implements EventAware, Filter
{
  private final LegacyUrlEnabledHelper legacyUrlEnabledHelper;

  private volatile boolean isEnabled;

  public LegacyViewFilter(final LegacyUrlEnabledHelper legacyUrlEnabledHelper) {
    this.legacyUrlEnabledHelper = legacyUrlEnabledHelper;
    this.isEnabled = legacyUrlEnabledHelper.isEnabled();
  }

  @Override
  public void doFilter(
      final ServletRequest req,
      final ServletResponse resp,
      final FilterChain chain) throws IOException, ServletException
  {
    final HttpServletRequest request = (HttpServletRequest) req;
    final HttpServletResponse response = (HttpServletResponse) resp;

    if (!isEnabled) {
      response.sendError(SC_NOT_FOUND);
      return;
    }

    chain.doFilter(request, response);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void handle(final CapabilityEvent event) {
    if (event.getReference().context().descriptor().type().equals(LegacyUrlCapabilityDescriptor.TYPE)) {
      toggleLegacyHttpBridgeModule();
    }
  }

  private synchronized void toggleLegacyHttpBridgeModule() {
    isEnabled = legacyUrlEnabledHelper.isEnabled();
  }
}
