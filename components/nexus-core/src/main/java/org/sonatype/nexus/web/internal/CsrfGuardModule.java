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

import org.sonatype.nexus.guice.FilterChainModule;

import com.google.inject.AbstractModule;
import com.google.inject.servlet.ServletModule;

// HACK: Disable CSRFGuard support for now, its too problematic
//import org.sonatype.nexus.csrfguard.CsrfGuardServlet;

/**
 * CSRF Guard Guice module.
 *
 * @since 2.9
 */
public class CsrfGuardModule
    extends AbstractModule
{
  public static final String MOUNT_POINT = "/csrfguard.js";

  @Override
  protected void configure() {
    install(new ServletModule()
    {
      @Override
      protected void configureServlets() {
        // HACK: Disable CSRFGuard support for now, its too problematic
        //serve(MOUNT_POINT).with(CsrfGuardServlet.class);
        filter(MOUNT_POINT).through(SecurityFilter.class);
      }
    });
    install(new FilterChainModule()
    {
      @Override
      protected void configure() {
        addFilterChain(MOUNT_POINT, "noSessionCreation,authcBasic");
      }
    });
  }
}
