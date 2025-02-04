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
package org.sonatype.nexus.security.config.memory;

import java.util.HashSet;
import java.util.Set;

import org.sonatype.nexus.security.config.CUserRoleMapping;

import static com.google.common.collect.Sets.newHashSet;

/**
 * An implementation of {@link CUserRoleMapping} suitable for an in-memory backing store.
 *
 * @since 3.0
 */
public class MemoryCUserRoleMapping
    implements CUserRoleMapping
{
  private Set<String> roles;

  private String source;

  private String userId;

  private String version;

  @Override
  public void addRole(final String string) {
    getRoles().add(string);
  }

  @Override
  public MemoryCUserRoleMapping clone() {
    try {
      MemoryCUserRoleMapping copy = (MemoryCUserRoleMapping) super.clone();

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
    return version;
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
    this.version = version;
  }

  public MemoryCUserRoleMapping withRoles(final String... roles) {
    setRoles(new HashSet<>(Set.of(roles)));
    return this;
  }

  public MemoryCUserRoleMapping withSource(final String source) {
    this.source = source;
    return this;
  }

  public MemoryCUserRoleMapping withUserId(final String userId) {
    this.userId = userId;
    return this;
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
