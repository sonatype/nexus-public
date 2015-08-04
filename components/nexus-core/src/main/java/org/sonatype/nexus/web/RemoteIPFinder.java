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
package org.sonatype.nexus.web;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

/**
 * Helper class to detect "real" IP address of remote client.
 * 
 * @since 2.7.0
 */
public class RemoteIPFinder
{
  public static final String FORWARD_HEADER = "X-Forwarded-For";

  /**
   * Returns the "real" IP address (as string) of the passed in {@link HttpServletRequest}.
   */
  public static String findIP(final HttpServletRequest request) {
    String forwardedIP = getFirstForwardedIp(request.getHeader(FORWARD_HEADER));

    if (forwardedIP != null) {
      return forwardedIP;
    }

    return request.getRemoteAddr();
  }

  /**
   * Returns the *left-most* resolvable IP from the given XFF string; otherwise null.
   */
  public static String getFirstForwardedIp(final String forwardedFor) {
    if (!StringUtils.isEmpty(forwardedFor)) {
      return resolveIp(forwardedFor.split("\\s*,\\s*"));
    }
    return null;
  }

  /**
   * Returns the *left-most* resolvable IP from the given sequence.
   */
  public static String resolveIp(final String[] ipAddresses) {
    for (String ip : ipAddresses) {
      InetAddress ipAdd;
      try {
        ipAdd = InetAddress.getByName(ip);
      }
      catch (UnknownHostException e) {
        continue;
      }
      if (ipAdd instanceof Inet4Address || ipAdd instanceof Inet6Address) {
        return ip;
      }
    }
    return null;
  }
}
