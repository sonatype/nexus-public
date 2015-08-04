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
package org.sonatype.nexus.plugins.rrb;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.resource.Resource;

public class ValidHTMLJettyDefaultServlet
    extends DefaultServlet
{


  /**
   * The default jetty implementation doesn't produce valid HTML, it misses the closing &lt;/A&gt; tag
   */
  @Override
  protected void sendDirectory(HttpServletRequest request,
                               HttpServletResponse response,
                               Resource resource,
                               String pathInContext)
      throws IOException
  {

    byte[] data = null;
    String base = URIUtil.addPaths(request.getRequestURI(), URIUtil.SLASH);
    String dir = resource.getListHTML(base, pathInContext.length()>1);

    if (dir == null) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
          "No directory");
      return;
    }

    // need to add in the missing </A>
    dir = dir.replaceAll("&nbsp;</TD><TD ALIGN=right>", "</A>&nbsp;</TD><TD ALIGN=right>");

    data = dir.getBytes("UTF-8");
    response.setContentType("text/html; charset=UTF-8");
    response.setContentLength(data.length);
    response.getOutputStream().write(data);
  }

}
