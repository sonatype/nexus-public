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
package org.sonatype.nexus.restlet1x.internal;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.web.internal.CookieFilter;
import org.sonatype.nexus.web.internal.NexusGuiceFilter;
import org.sonatype.nexus.web.internal.SecurityFilter;

import com.google.inject.servlet.ServletModule;

/**
 * Guice module for binding nexus servlets.
 *
 * @author adreghiciu
 */
class RestletServletModule
    extends ServletModule
{
  @Override
  protected void configureServlets() {
    requestStaticInjection(NexusGuiceFilter.class);

    serve("/service/local/*").with(RestletServlet.class, nexusRestletServletInitParams());
    filter("/service/local/*").through(SecurityFilter.class);
    filter("/service/local/*").through(RestletHeaderFilter.class);
    filter("/service/local/authentication/login").through(CookieFilter.class);
    filter("/service/local/authentication/logout").through(CookieFilter.class);
  }

  private Map<String, String> nexusRestletServletInitParams() {
    Map<String, String> params = new HashMap<String, String>();
    params.put("nexus.role", "org.restlet.Application");
    params.put("nexus.roleHint", "nexus");
    params.put("nexus.org.restlet.clients", "FILE CLAP");
    params.put("plexus.discoverResources", "true");
    return params;
  }
}
