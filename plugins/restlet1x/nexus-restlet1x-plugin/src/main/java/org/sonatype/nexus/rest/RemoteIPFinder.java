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
package org.sonatype.nexus.rest;

import java.util.List;

import org.restlet.data.Form;
import org.restlet.data.Request;

import static org.sonatype.nexus.web.RemoteIPFinder.FORWARD_HEADER;

/**
 * Restlet specific additions to {@link org.sonatype.nexus.web.RemoteIPFinder} helper class.
 */
public class RemoteIPFinder
{
  public static String findIP(Request request) {
    Form form = (Form) request.getAttributes().get("org.restlet.http.headers");

    String forwardedIP = org.sonatype.nexus.web.RemoteIPFinder.getFirstForwardedIp(
        form.getFirstValue(FORWARD_HEADER));

    if (forwardedIP != null) {
      return forwardedIP;
    }

    List<String> clientAddresses = request.getClientInfo().getAddresses();

    if (clientAddresses.size() > 1) {
      // restlet1x ClientInfo.getAddresses has *reverse* order to XFF
      // (this has been fixed in restlet2x, along with a clearer API)

      String[] ipAddresses = new String[clientAddresses.size()];
      for (int i = 0, j = ipAddresses.length - 1; j >= 0; i++, j--) {
        ipAddresses[i] = clientAddresses.get(j);
      }

      forwardedIP = org.sonatype.nexus.web.RemoteIPFinder.resolveIp(ipAddresses);

      if (forwardedIP != null) {
        return forwardedIP;
      }
    }

    return request.getClientInfo().getAddress();
  }
}
