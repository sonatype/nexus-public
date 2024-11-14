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
package org.sonatype.nexus.rapture.internal.security;

import java.util.HashSet;
import java.util.Set;

/**
 * User exchange object.
 *
 * @since 3.0
 */
public class UserXO
{
  private String id;

  private boolean authenticated;

  private boolean administrator;

  private Set<String> authenticatedRealms = new HashSet<>();

  public UserXO() {
  }

  public UserXO(String id, boolean authenticated, boolean administrator, Set<String> authenticatedRealms) {
    this.id = id;
    this.authenticated = authenticated;
    this.administrator = administrator;
    this.authenticatedRealms = authenticatedRealms;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean isAuthenticated() {
    return authenticated;
  }

  public void setAuthenticated(boolean authenticated) {
    this.authenticated = authenticated;
  }

  public boolean isAdministrator() {
    return administrator;
  }

  public void setAdministrator(boolean administrator) {
    this.administrator = administrator;
  }

  public Set<String> getAuthenticatedRealms() {
    return authenticatedRealms;
  }

  public void setAuthenticatedRealms(Set<String> authenticatedRealms) {
    this.authenticatedRealms = authenticatedRealms;
  }

  @Override
  public String toString() {
    return "UserXO{" +
        "id='" + id + '\'' +
        ", authenticated=" + authenticated +
        ", administrator=" + administrator +
        ", authenticatedRealms=" + authenticatedRealms +
        '}';
  }
}
