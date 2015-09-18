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
package org.sonatype.nexus.rapture.internal;

import javax.inject.Named;

import org.sonatype.nexus.rapture.internal.security.SessionAuthenticationFilter;
import org.sonatype.nexus.rapture.internal.security.SessionServlet;
import org.sonatype.nexus.security.CookieFilter;
import org.sonatype.nexus.security.FilterChainModule;
import org.sonatype.nexus.security.SecurityFilter;

import com.google.inject.AbstractModule;
import com.google.inject.servlet.ServletModule;

import static org.sonatype.nexus.security.FilterProviderSupport.filterKey;

/**
 * Rapture Guice module.
 *
 * @since 3.0
 */
@Named
public class RaptureModule
    extends AbstractModule
{
  private static final String MOUNT_POINT = "/service/rapture";

  private static final String SESSION_MP = MOUNT_POINT + "/session";

  @Override
  protected void configure() {
    bind(filterKey(SessionAuthenticationFilter.NAME)).to(SessionAuthenticationFilter.class);

    install(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        serve(SESSION_MP).with(SessionServlet.class);
        filter(SESSION_MP).through(SecurityFilter.class);
        filter(SESSION_MP).through(CookieFilter.class);
      }
    });

    install(new FilterChainModule()
    {
      @Override
      protected void configure() {
        addFilterChain(SESSION_MP, SessionAuthenticationFilter.NAME);
      }
    });
  }
}
