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
package org.sonatype.nexus.security.ldap.realms.test.api.dto;

import org.sonatype.nexus.security.ldap.realms.api.dto.LdapUserAndGroupConfigurationDTO;

public class LdapUserAndGroupConfigTestRequestDTO
    extends LdapUserAndGroupConfigurationDTO
{
  // TODO: this should contain both an LdapUserAndGroupConfigurationDTO and a LdapConnectionInfoDTO
  // There are some issues with the UI doing this.

  /**
   * Search Base. Base DN for the connection.
   */
  private String searchBase;

  /**
   * System User. The username of user with access to the LDAP server.
   */
  private String systemUsername;

  /**
   * System Password. The password for the System User.
   */
  private String systemPassword;

  /**
   * Authentication Scheme. Method used for authentication: none, simple, etc.
   */
  private String authScheme;

  /**
   * Protocol. The protocol used in the ldap URL: ldap, ldaps.
   */
  private String protocol;

  /**
   * Host. The host name of the LDAP server.
   */
  private String host;

  /**
   * Port. The port of the LDAP Server.
   */
  private int port = 0;

  private int userLimitCount = 0;

  /**
   * SASL Realm. The authentication realm.
   */
  private String realm;

  /**
   * @return the searchBase
   */
  public String getSearchBase() {
    return searchBase;
  }

  /**
   * @param searchBase the searchBase to set
   */
  public void setSearchBase(String searchBase) {
    this.searchBase = searchBase;
  }

  /**
   * @return the systemUsername
   */
  public String getSystemUsername() {
    return systemUsername;
  }

  /**
   * @param systemUsername the systemUsername to set
   */
  public void setSystemUsername(String systemUsername) {
    this.systemUsername = systemUsername;
  }

  /**
   * @return the systemPassword
   */
  public String getSystemPassword() {
    return systemPassword;
  }

  /**
   * @param systemPassword the systemPassword to set
   */
  public void setSystemPassword(String systemPassword) {
    this.systemPassword = systemPassword;
  }

  /**
   * @return the authScheme
   */
  public String getAuthScheme() {
    return authScheme;
  }

  /**
   * @param authScheme the authScheme to set
   */
  public void setAuthScheme(String authScheme) {
    this.authScheme = authScheme;
  }

  /**
   * @return the protocol
   */
  public String getProtocol() {
    return protocol;
  }

  /**
   * @param protocol the protocol to set
   */
  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  /**
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * @param host the host to set
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * @param port the port to set
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * @return the realm
   */
  public String getRealm() {
    return realm;
  }

  /**
   * @param realm the realm to set
   */
  public void setRealm(String realm) {
    this.realm = realm;
  }

  public int getUserLimitCount() {
    return userLimitCount;
  }

  public void setUserLimitCount(int userLimitCount) {
    this.userLimitCount = userLimitCount;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((authScheme == null) ? 0 : authScheme.hashCode());
    result = prime * result + ((host == null) ? 0 : host.hashCode());
    result = prime * result + port;
    result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
    result = prime * result + ((realm == null) ? 0 : realm.hashCode());
    result = prime * result + ((searchBase == null) ? 0 : searchBase.hashCode());
    result = prime * result + ((systemPassword == null) ? 0 : systemPassword.hashCode());
    result = prime * result + ((systemUsername == null) ? 0 : systemUsername.hashCode());
    result = prime * result + userLimitCount;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final LdapUserAndGroupConfigTestRequestDTO other = (LdapUserAndGroupConfigTestRequestDTO) obj;
    if (authScheme == null) {
      if (other.authScheme != null) {
        return false;
      }
    }
    else if (!authScheme.equals(other.authScheme)) {
      return false;
    }
    if (host == null) {
      if (other.host != null) {
        return false;
      }
    }
    else if (!host.equals(other.host)) {
      return false;
    }
    if (port != other.port) {
      return false;
    }
    if (protocol == null) {
      if (other.protocol != null) {
        return false;
      }
    }
    else if (!protocol.equals(other.protocol)) {
      return false;
    }
    if (realm == null) {
      if (other.realm != null) {
        return false;
      }
    }
    else if (!realm.equals(other.realm)) {
      return false;
    }
    if (searchBase == null) {
      if (other.searchBase != null) {
        return false;
      }
    }
    else if (!searchBase.equals(other.searchBase)) {
      return false;
    }
    if (systemPassword == null) {
      if (other.systemPassword != null) {
        return false;
      }
    }
    else if (!systemPassword.equals(other.systemPassword)) {
      return false;
    }
    if (systemUsername == null) {
      if (other.systemUsername != null) {
        return false;
      }
    }
    else if (!systemUsername.equals(other.systemUsername)) {
      return false;
    }
    if (userLimitCount != other.userLimitCount) {
      return false;
    }
    return true;
  }

}
