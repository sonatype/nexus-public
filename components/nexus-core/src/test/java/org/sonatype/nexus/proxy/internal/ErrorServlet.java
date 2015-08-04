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
package org.sonatype.nexus.proxy.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

public class ErrorServlet
    extends HttpServlet
{

  public static String CONTENT = "<html>some content</html>";

  private static Map<String, String> RESPONSE_HEADERS = new HashMap<String, String>();

  public static void clearHeaders() {
    RESPONSE_HEADERS.clear();
  }

  public static void addHeader(String key, String value) {
    RESPONSE_HEADERS.put(key, value);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    for (Entry<String, String> headerEntry : RESPONSE_HEADERS.entrySet()) {
      resp.addHeader(headerEntry.getKey(), headerEntry.getValue());
    }

    IOUtils.write(CONTENT, resp.getOutputStream());

  }

}
