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
package org.sonatype.nexus.security;

import java.util.Objects;

import org.apache.shiro.subject.SimplePrincipalCollection;

import static com.google.common.base.Preconditions.checkNotNull;

public class NexusSimplePrincipalCollection
    extends SimplePrincipalCollection
{
  private final RealmCaseMapping realmCaseMapping;

  public NexusSimplePrincipalCollection(final String principal, final RealmCaseMapping realmCaseMapping) {
    super(principal, realmCaseMapping.getRealmName());
    this.realmCaseMapping = checkNotNull(realmCaseMapping);
  }

  public String getRealmName() {
    return realmCaseMapping.getRealmName();
  }

  public boolean isRealmCaseSensitive() {
    return realmCaseMapping.isCaseSensitive();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    NexusSimplePrincipalCollection that = (NexusSimplePrincipalCollection) o;
    return Objects.equals(realmCaseMapping, that.realmCaseMapping);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), realmCaseMapping);
  }
}
