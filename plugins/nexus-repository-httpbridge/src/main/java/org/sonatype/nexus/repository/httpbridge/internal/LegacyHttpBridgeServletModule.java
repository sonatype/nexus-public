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

import com.google.inject.servlet.ServletModule;

/**
 * Servlet module for legacy HTTP bridge module.
 *
 * @since 3.38
 */
public abstract class LegacyHttpBridgeServletModule
    extends ServletModule
{
  @Override
  protected void configureServlets() {
    // this technically makes non-group repositories visible under /content/groups,
    // but this is acceptable since their IDs are unique and it keeps things simple
    serve("/content/groups/*", "/content/repositories/*", "/content/sites/*").with(LegacyViewServlet.class);
    bindViewFiltersFor("/content/groups/*", "/content/repositories/*", "/content/sites/*");

    // this makes /service/local/x/x available, as a view servlet. Note that we have to strip the last forward
    // slash so that our first group would be "/service/local/x" without the forward slash. This is needed so that
    // the following matcher group starts with the forward slash, which is needed for the NXRM to detect the endpoint.
    serveRegex("/service/local/.*?(/.*)").with(LegacyViewServlet.class);
    bindViewFiltersRegexFor("/service/local/.*?(/.*)");
  }

  /**
   * Helper to make sure view-related filters are bound in the correct order by servlet filter.
   */
  private void bindViewFiltersFor(final String urlPattern, final String... morePatterns) {
    bindViewFilters(filter(urlPattern, morePatterns));
  }

  /**
   * Helper to make sure view-related filters are bound in the correct order by regex filter.
   */
  private void bindViewFiltersRegexFor(final String urlPattern, final String... morePatterns) {
    bindViewFilters(filterRegex(urlPattern, morePatterns));
  }

  private void bindViewFilters(FilterKeyBindingBuilder filter) {
    filter.through(ExhaustRequestFilter.class);
    bindSecurityFilter(filter);
  }

  protected abstract void bindSecurityFilter(final FilterKeyBindingBuilder filter);
}
