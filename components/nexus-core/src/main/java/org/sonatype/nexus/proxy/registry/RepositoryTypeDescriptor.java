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
package org.sonatype.nexus.proxy.registry;

import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.nexus.plugins.RepositoryType;
import org.sonatype.nexus.proxy.repository.Repository;

import org.codehaus.plexus.util.StringUtils;

/**
 * A simple descriptor for all roles implementing a Nexus Repository.
 *
 * @author cstamas
 */
public class RepositoryTypeDescriptor
{
  private final Class<? extends Repository> role;

  private final String hint;

  private final String prefix;

  private final int repositoryMaxInstanceCount;

  private AtomicInteger instanceCount = new AtomicInteger(0);

  public RepositoryTypeDescriptor(Class<? extends Repository> role, String hint, String prefix) {
    this(role, hint, prefix, RepositoryType.UNLIMITED_INSTANCES);
  }

  public RepositoryTypeDescriptor(Class<? extends Repository> role, String hint, String prefix,
                                  int repositoryMaxInstanceCount)
  {
    this.role = role;

    this.hint = hint;

    this.prefix = prefix;

    this.repositoryMaxInstanceCount = repositoryMaxInstanceCount;
  }

  public Class<? extends Repository> getRole() {
    return role;
  }

  public String getHint() {
    return hint;
  }

  public String getPrefix() {
    return prefix;
  }

  public int getRepositoryMaxInstanceCount() {
    return repositoryMaxInstanceCount;
  }

  public int getInstanceCount() {
    return instanceCount.get();
  }

  public int instanceRegistered(RepositoryRegistry registry) {
    return instanceCount.incrementAndGet();
  }

  public int instanceUnregistered(RepositoryRegistry registry) {
    return instanceCount.decrementAndGet();
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || (o.getClass() != this.getClass())) {
      return false;
    }

    RepositoryTypeDescriptor other = (RepositoryTypeDescriptor) o;

    return getRole().equals(other.getRole()) && StringUtils.equals(getHint(), other.getHint())
        && StringUtils.equals(getPrefix(), other.getPrefix());
  }

  public int hashCode() {
    int result = 7;

    result = 31 * result + (role == null ? 0 : role.hashCode());

    result = 31 * result + (hint == null ? 0 : hint.hashCode());

    result = 31 * result + (prefix == null ? 0 : prefix.hashCode());

    return result;
  }

  public String toString() {
    return "RepositoryType=(" + getRole().getName() + ":" + getHint() + ")";
  }
}
