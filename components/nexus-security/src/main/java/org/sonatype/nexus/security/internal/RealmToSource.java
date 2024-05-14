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
package org.sonatype.nexus.security.internal;

import java.util.Arrays;

/**
 * This is meant to be a utility enum that manages the mapping of realms against user
 * sources (An authenticating realm has a name that matches a user source).
 * @author Alejandro Tovar
 */
public enum RealmToSource {
  SAML_USER_SOURCE("SamlRealm", "SAML"),
  CROWD_USER_SOURCE("Crowd", "Crowd"),
  LDAP_USER_SOURCE("LdapRealm", "LDAP"),
  NEXUS_USER_SOURCE("NexusAuthenticatingRealm", "default");

  private final String realmName;
  private final String sourceName;

  RealmToSource(final String realmName, final String sourceName) {
    this.realmName = realmName;
    this.sourceName = sourceName;
  }

  /**
   * Method that searches for a user source based on a realm name
   * @param realm to be queried for a valid source for a user
   * @return String value that represent the user source for a realm
   */
  public static String getSource(String realm) {
    return Arrays.stream(values())
        .filter(keyRealm -> keyRealm.realmName.equals(realm))
        .map(keyRealm -> keyRealm.sourceName)
        .findFirst().orElse("default");
  }
}
