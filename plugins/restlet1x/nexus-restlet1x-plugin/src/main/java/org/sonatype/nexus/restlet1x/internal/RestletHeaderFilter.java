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

import java.util.Collection;
import java.util.Iterator;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.google.common.net.HttpHeaders;
import com.noelios.restlet.Engine;
import org.apache.shiro.web.servlet.AdviceFilter;

/**
 * HTTP Header handling for Restlet.
 *
 * The main purpose is to sanitize the Server header in the response to include any existing Server header
 * value and finally the Restlet version header value at the end.
 *
 * @see NexusHttpServerConverter
 * @since 2.11.2
 */
@Named
@Singleton
public class RestletHeaderFilter
    extends AdviceFilter
{
  private static final String SP = " ";

  /**
   * Join any already set Server headers and append the Restlet Server header value to a single Server header.
   */
  @Override
  protected boolean preHandle(final ServletRequest request, final ServletResponse response) throws Exception {
    if (response instanceof HttpServletResponse) {
     HttpServletResponse r = (HttpServletResponse) response;
      final Collection<String> serverHeaders = r.getHeaders(HttpHeaders.SERVER);
      int numHeaders = serverHeaders.size();
      if (numHeaders == 1) {
        r.setHeader(HttpHeaders.SERVER, serverHeaders.iterator().next() + SP + Engine.VERSION_HEADER);
      }
      else if (numHeaders == 0) {
        r.setHeader(HttpHeaders.SERVER, Engine.VERSION_HEADER);
      }
      else {
        Iterator<String> it = serverHeaders.iterator();
        StringBuilder sb = new StringBuilder(it.next());
        while (it.hasNext()) {
          sb.append(SP).append(it.next());
        }
        sb.append(SP).append(Engine.VERSION_HEADER);
        r.setHeader(HttpHeaders.SERVER, sb.toString());
      }
    }
    return true;
  }
}
