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
package org.sonatype.nexus.auth;

/**
 * Client info about WHO is doing something.
 *
 * @author cstamas
 */
public class ClientInfo
{
  private final String userid;

  private final String remoteIP;

  private final String userAgent;

  public ClientInfo(final String userid, final String remoteIP, final String userAgent) {
    this.userid = userid;
    this.remoteIP = remoteIP;
    this.userAgent = userAgent;
  }

  public String getRemoteIP() {
    return remoteIP;
  }

  public String getUserid() {
    return userid;
  }

  public String getUserAgent() {
    return userAgent;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((remoteIP == null) ? 0 : remoteIP.hashCode());
    result = prime * result + ((userAgent == null) ? 0 : userAgent.hashCode());
    result = prime * result + ((userid == null) ? 0 : userid.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ClientInfo other = (ClientInfo) obj;
    if (remoteIP == null) {
      if (other.remoteIP != null) {
        return false;
      }
    }
    else if (!remoteIP.equals(other.remoteIP)) {
      return false;
    }
    if (userAgent == null) {
      if (other.userAgent != null) {
        return false;
      }
    }
    else if (!userAgent.equals(other.userAgent)) {
      return false;
    }
    if (userid == null) {
      if (other.userid != null) {
        return false;
      }
    }
    else if (!userid.equals(other.userid)) {
      return false;
    }
    return true;
  }
}
