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
package org.sonatype.nexus.cleanup.storage;

import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.goodies.lifecycle.Lifecycle;

/**
 * @since 3.14
 */
public interface CleanupPolicyStorage
    extends Lifecycle
{
  /**
   * Adds a cleanup policy
   *
   * @param item to be added
   * @return the added {@link CleanupPolicy}
   */
  CleanupPolicy add(CleanupPolicy item);

  /**
   * Updates stored cleanup policy if it exists.
   *
   * @param cleanupPolicy to be updated
   * @return the updated {@link CleanupPolicy}
   */
  CleanupPolicy update(CleanupPolicy cleanupPolicy);

  /**
   * Deletes stored cleanup policy if it exists.
   *
   * @param cleanupPolicy cleanup to be deleted
   */
  void remove(final CleanupPolicy cleanupPolicy);

  /**
   * Retrieves a single stored policy
   * @param cleanupPolicyName name of cleanup policy to retrieve
   * @return {@link CleanupPolicy}
   */
  @Nullable
  CleanupPolicy get(final String cleanupPolicyName);

  /**
   * Retrieves stored cleanup policies
   *
   * @return list of cleanup policies (never null)
   */
  List<CleanupPolicy> getAll();

  /**
   * Retrieves stored clean policies related to a {@link org.sonatype.nexus.repository.Format} type
   * @return list of cleanup policies
   */
  List<CleanupPolicy> getAllByFormat(final String format);

  /**
   * Checks whether a cleanup policy with a given name exists.
   *
   * @param cleanupPolicyName name of cleanup policy to check for existence.
   * @return true if it existed by the given name ignoring case, false otherwise.
   */
  boolean exists(final String cleanupPolicyName);
}
