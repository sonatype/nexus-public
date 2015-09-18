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
package org.sonatype.nexus.security.anonymous;

import org.sonatype.nexus.common.entity.Entity;

import com.google.common.base.Throwables;

/**
 * Anonymous configuration.
 *
 * @since 3.0
 */
public class AnonymousConfiguration
  extends Entity
  implements Cloneable
{
  private boolean enabled;

  private String userId;

  private String realmName;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  // TODO: Sort out nullability of user-id and realm-name

  public String getUserId() {
    return userId;
  }

  public void setUserId(final String userId) {
    this.userId = userId;
  }

  public String getRealmName() {
    return realmName;
  }

  public void setRealmName(final String realmName) {
    this.realmName = realmName;
  }

  public AnonymousConfiguration copy() {
    try {
      return (AnonymousConfiguration) clone();
    }
    catch (CloneNotSupportedException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "enabled=" + enabled +
        ", userId='" + userId + '\'' +
        ", realmName='" + realmName + '\'' +
        '}';
  }
}
