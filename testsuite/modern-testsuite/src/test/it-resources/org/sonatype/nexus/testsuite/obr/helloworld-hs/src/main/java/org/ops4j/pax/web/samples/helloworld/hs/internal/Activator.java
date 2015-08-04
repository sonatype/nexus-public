/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

package org.ops4j.pax.web.samples.helloworld.hs.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Hello World Activator.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 02, 2007
 */
public final class Activator
    implements BundleActivator
{

  /**
   * HttpService reference.
   */
  private ServiceReference m_httpServiceRef;

  /**
   * Called when the OSGi framework starts our bundle
   */
  @SuppressWarnings("unchecked")
  public void start(BundleContext bc)
      throws Exception
  {
    m_httpServiceRef = bc.getServiceReference(HttpService.class.getName());
    if (m_httpServiceRef != null) {
      final HttpService httpService = (HttpService) bc.getService(m_httpServiceRef);
      if (httpService != null) {
        // create a default context to share between registrations
        final HttpContext httpContext = httpService.createDefaultHttpContext();
        // register the hello world servlet
        final Dictionary initParams = new Hashtable();
        initParams.put("from", "HttpService");
        httpService.registerServlet(
            "/helloworld/hs",                           // alias
            new HelloWorldServlet("/helloworld/hs"),  // registered servlet
            initParams,                                 // init params
            httpContext                                 // http context
        );
        httpService.registerServlet(
            "/",                            // alias
            new HelloWorldServlet("/"),   // registered servlet
            initParams,                     // init params
            httpContext                     // http context
        );
        // register images as resources
        httpService.registerResources(
            "/images",
            "/images",
            httpContext
        );
      }
    }
  }

  /**
   * Called when the OSGi framework stops our bundle
   */
  public void stop(BundleContext bc)
      throws Exception
  {
    if (m_httpServiceRef != null) {
      bc.ungetService(m_httpServiceRef);
      m_httpServiceRef = null;
    }
  }
}

