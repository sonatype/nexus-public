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
package org.sonatype.nexus.internal.security.anonymous;

import java.util.Objects;

import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;

/**
 * {@link AnonymousConfiguration} data.
 *
 * @since 3.21
 */
public class AnonymousConfigurationData
    implements AnonymousConfiguration, Cloneable
{
  private boolean enabled;

  private String userId;

  private String realmName;

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public String getUserId() {
    return userId;
  }

  @Override
  public void setUserId(final String userId) {
    this.userId = userId;
  }

  @Override
  public String getRealmName() {
    return realmName;
  }

  @Override
  public void setRealmName(final String realmName) {
    this.realmName = realmName;
  }

  @Override
  public AnonymousConfiguration copy() {
    try {
      return (AnonymousConfiguration) clone();
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnonymousConfigurationData that = (AnonymousConfigurationData) o;
    return enabled == that.enabled &&
        Objects.equals(userId, that.userId) &&
        Objects.equals(realmName, that.realmName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, userId, realmName);
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
