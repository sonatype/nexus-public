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
package org.sonatype.nexus.coreui;

/**
 * Anonymous Security Settings exchange object.
 */
public class AnonymousSettingsXO
{
  private Boolean enabled;

  private String userId;

  private String realmName;

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(final Boolean enabled) {
    this.enabled = enabled;
  }

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

  @Override
  public String toString() {
    return "AnonymousSettingsXO(" +
        "enabled:" + enabled +
        ", userId:" + userId +
        ", realmName:" + realmName +
        ")";
  }
}
