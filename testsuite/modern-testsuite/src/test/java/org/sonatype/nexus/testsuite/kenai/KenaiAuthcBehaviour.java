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
package org.sonatype.nexus.testsuite.kenai;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.tests.http.server.api.Behaviour;

/**
 * {@link Behaviour} implementation that mocks Kenai AuthAPI.
 *
 * @author cstamas
 * @see <a href="https://kenai.com/projects/kenaiapis/pages/AuthAPI">Kenai AuthAPI</a>
 */
public class KenaiAuthcBehaviour
    implements Behaviour
{
  @Override
  public boolean execute(final HttpServletRequest request, final HttpServletResponse response,
                         final Map<Object, Object> context)
      throws Exception
  {
    final String username = request.getParameter("username");
    final String password = request.getParameter("password");

    final PrintWriter pw = new PrintWriter(response.getWriter());
    try {

      if ("POST".equals(request.getMethod()) && username != null && password != null
          && (username + "123").equals(password)) {
        // we are fine
        response.setStatus(200);
        pw.println("{ \"status\":\"200 OK\",\"message\":\"Authenticate successful\",\"username\":\""
            + username + "\"}");
      }
      else {
        // we are not fine
        response.setStatus(401);
        pw.println("{ \"status\":\"401 Forbidden\",\"message\":\"Authenticate unsuccessful\",\"username\":\""
            + username + "\"}");
      }
    }
    finally {
      pw.flush();
      pw.close();
    }
    return false;
  }
}
