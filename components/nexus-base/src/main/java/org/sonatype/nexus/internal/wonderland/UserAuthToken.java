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
package org.sonatype.nexus.internal.wonderland;

import java.util.Objects;

/**
 * @since 3.15
 */
public class UserAuthToken
{
  private final String user;

  private final String token;

  private final String realmName;

  public UserAuthToken(final String user, final String token, final String realmName) {
    this.user = user;
    this.token = token;
    this.realmName = realmName;
  }

  public String getUser() {
    return user;
  }

  public String getToken() {
    return token;
  }

  public String getRealmName() {
    return realmName;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserAuthToken that = (UserAuthToken) o;
    return Objects.equals(user, that.user)
        && Objects.equals(token, that.token)
        && Objects.equals(realmName, that.realmName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, token, realmName);
  }

  @Override
  public String toString() {
    return getRealmName() + ":" + getUser() + ":" + getToken();
  }
}
