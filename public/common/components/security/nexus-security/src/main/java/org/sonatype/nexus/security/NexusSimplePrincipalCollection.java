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

import org.apache.shiro.subject.SimplePrincipalCollection;

import static java.util.Objects.requireNonNull;

/**
 * A {@link SimplePrincipalCollection} that also records whether its realm treats user IDs case-sensitively.
 *
 * <p>
 * This class deliberately does not override {@code equals} or {@code hashCode}. Shiro compares
 * {@code SimplePrincipalCollection} instances with {@code instanceof} over the realm-to-principals map, and an override
 * that narrowed the test to {@code getClass()} made equality asymmetric: a stored instance never matched an equivalent
 * {@code SimplePrincipalCollection}, which silently orphaned api key rows written before the OrientDB migration
 * (NEXUS-54691). The case mapping describes realm behaviour, not identity, and is read through
 * {@link #isRealmCaseSensitive()} instead.
 */
public class NexusSimplePrincipalCollection
    extends SimplePrincipalCollection
{
  /**
   * Pinned to the value Java derived for this class before {@code equals} and {@code hashCode} were removed. The
   * serialized state is unchanged - {@code realmCaseMapping} is still the only field - but the default UID is computed
   * from method signatures, so dropping those overrides would otherwise make every {@code principals} BLOB already in
   * the database unreadable with {@code InvalidClassException} (NEXUS-54691).
   */
  private static final long serialVersionUID = 6270148609559168170L;

  private final RealmCaseMapping realmCaseMapping;

  public NexusSimplePrincipalCollection(final String principal, final RealmCaseMapping realmCaseMapping) {
    super(principal, realmCaseMapping.getRealmName());
    this.realmCaseMapping = requireNonNull(realmCaseMapping);
  }

  public String getRealmName() {
    return realmCaseMapping.getRealmName();
  }

  public boolean isRealmCaseSensitive() {
    return realmCaseMapping.isCaseSensitive();
  }
}
