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
package org.sonatype.nexus.security.config;

import java.io.Serializable;
import java.util.Set;

import org.sonatype.nexus.common.entity.Entity;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

/**
 * Persistent user-role mapping.
 */
public class CUserRoleMapping
    extends Entity
    implements Serializable, Cloneable
{
  private String userId;

  private String source;

  private Set<String> roles;

  private String version;

  public void addRole(String string) {
    getRoles().add(string);
  }

  public Set<String> getRoles() {
    if (this.roles == null) {
      this.roles = Sets.newHashSet();
    }
    return this.roles;
  }

  public String getSource() {
    return this.source;
  }

  public String getUserId() {
    return this.userId;
  }

  public void removeRole(String string) {
    getRoles().remove(string);
  }

  public void setRoles(Set<String> roles) {
    this.roles = roles;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  @Override
  public CUserRoleMapping clone() {
    try {
      CUserRoleMapping copy = (CUserRoleMapping) super.clone();

      if (this.roles != null) {
        copy.roles = Sets.newHashSet(this.roles);
      }

      return copy;
    }
    catch (CloneNotSupportedException e) {
      throw Throwables.propagate(e);
    }
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
