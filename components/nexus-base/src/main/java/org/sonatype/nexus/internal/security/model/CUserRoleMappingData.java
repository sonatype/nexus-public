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
package org.sonatype.nexus.internal.security.model;

import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.security.config.CUserRoleMapping;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static com.google.common.collect.Sets.newHashSet;
import static org.sonatype.nexus.common.text.Strings2.lower;
import static org.sonatype.nexus.security.config.SecuritySourceUtil.isCaseInsensitiveSource;

/**
 * {@link CUserRoleMapping} data.
 *
 * @since 3.21
 */
public class CUserRoleMappingData
    implements CUserRoleMapping
{
  private Set<String> roles;

  private String source;

  private String userId;

  private int version = 1;

  @Override
  public void addRole(final String string) {
    getRoles().add(string);
  }

  @Override
  public CUserRoleMappingData clone() {
    try {
      CUserRoleMappingData copy = (CUserRoleMappingData) super.clone();

      if (this.roles != null) {
        copy.roles = newHashSet(this.roles);
      }

      return copy;
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Set<String> getRoles() {
    if (this.roles == null) {
      this.roles = newHashSet();
    }
    return this.roles;
  }

  @Override
  public String getSource() {
    return this.source;
  }

  @Override
  public String getUserId() {
    return this.userId;
  }

  @Override
  public String getVersion() {
    return Integer.toString(version);
  }

  @Override
  public void removeRole(final String string) {
    getRoles().remove(string);
  }

  @Override
  public void setRoles(final Set<String> roles) {
    this.roles = roles;
  }

  @Override
  public void setSource(final String source) {
    this.source = source;
  }

  @Override
  public void setUserId(final String userId) {
    this.userId = userId;
  }

  @Override
  public void setVersion(final String version) {
    this.version = version != null ? Integer.parseInt(version) : 0;
  }

  /**
   * Returns lower-case userId if we need to use case-insensitive search.
   */
  @JsonIgnore
  @Nullable
  public String getUserLo() {
    return isCaseInsensitiveSource(source) ? lower(this.userId) : null;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "userId='" + userId + '\'' +
        ", source='" + source + '\'' +
        ", roles=" + roles +
        ", version='" + version + '\'' +
        '}';
  }
}
