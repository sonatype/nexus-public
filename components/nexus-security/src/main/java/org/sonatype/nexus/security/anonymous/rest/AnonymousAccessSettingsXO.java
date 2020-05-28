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
package org.sonatype.nexus.security.anonymous.rest;

import java.util.Objects;

import javax.validation.constraints.NotBlank;

import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;

/**
 * @since 3.24
 */
@JsonInclude
public class AnonymousAccessSettingsXO
{
  @ApiModelProperty("Whether or not Anonymous Access is enabled")
  private boolean enabled = false;

  @NotBlank
  @ApiModelProperty("The username of the anonymous account")
  private String userId = AnonymousConfiguration.DEFAULT_USER_ID;

  @NotBlank
  @ApiModelProperty("The name of the authentication realm for the anonymous account")
  private String realmName = AnonymousConfiguration.DEFAULT_REALM_NAME;

  public AnonymousAccessSettingsXO() {
  }

  public AnonymousAccessSettingsXO(AnonymousConfiguration anonymousConfiguration) {
    this.enabled = anonymousConfiguration.isEnabled();
    this.userId = anonymousConfiguration.getUserId();
    this.realmName = anonymousConfiguration.getRealmName();
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
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
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnonymousAccessSettingsXO that = (AnonymousAccessSettingsXO) o;
    return enabled == that.enabled &&
        Objects.equals(userId, that.userId) &&
        Objects.equals(realmName, that.realmName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, userId, realmName);
  }
}
