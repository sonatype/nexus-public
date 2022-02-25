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

import static org.sonatype.nexus.repository.httpbridge.internal.HttpBridgeModule.MOUNT_POINT;

/**
 * Servlet module for Repository HTTP bridge.
 *
 * @since 3.38
 */
public abstract class HttpBridgeServletModule
    extends ServletModule
{
  @Override
  protected void configureServlets() {
    bind(ViewServlet.class);
    serve(MOUNT_POINT + "/*").with(ViewServlet.class);
    bindViewFiltersFor(MOUNT_POINT + "/*");
  }

  /**
   * Helper to make sure view-related filters are bound in the correct order by servlet filter.
   */
  private void bindViewFiltersFor(final String urlPattern, final String... morePatterns) {
    bindViewFilters(filter(urlPattern, morePatterns));
  }

  private void bindViewFilters(FilterKeyBindingBuilder filter) {
    filter.through(ExhaustRequestFilter.class);
    bindSecurityFilter(filter);
  }

  protected abstract void bindSecurityFilter(final FilterKeyBindingBuilder filter);
}
