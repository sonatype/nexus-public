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
package org.sonatype.nexus.orient.quorum;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

/**
 * Status for Orient quorum.
 *
 * @since 3.4
 */
public class  DatabaseQuorumStatus
{
  private static final DatabaseQuorumStatus SINGLE =
      new DatabaseQuorumStatus(ImmutableSet.of("local"), 1, null);

  private final Set<String> members;

  private final int minimumForQuorum;

  private final String databaseName;

  /**
   * @param members          the ids of the orient cluster members
   * @param minimumForQuorum the number of members required to achieve quorum
   * @param databaseName     the name of the database that produced the observation (can be null)
   */
  public DatabaseQuorumStatus(final Collection<String> members, final int minimumForQuorum, final String databaseName) {
    this.members = Collections.unmodifiableSet(new HashSet<>(members));
    this.minimumForQuorum = minimumForQuorum;
    this.databaseName = databaseName;
  }

  /**
   * Single node orients always have quorum (e.g. {@link #isQuorumPresent()} always returns true).
   *
   * @return a {@link DatabaseQuorumStatus} reflective of a single node cluster.
   */
  public static DatabaseQuorumStatus singleNode() {
    return SINGLE;
  }

  public Set<String> getMembers() {
    return members;
  }

  public int getMinimumForQuorum() {
    return minimumForQuorum;
  }

  @Nullable
  public String getDatabaseName() {
    return databaseName;
  }

  /**
   * Computed field.
   *
   * @return true if we have enough active cluster members to achieve quorum
   */
  public boolean isQuorumPresent() {
    return getMembers().size() >= getMinimumForQuorum();
  }
}
