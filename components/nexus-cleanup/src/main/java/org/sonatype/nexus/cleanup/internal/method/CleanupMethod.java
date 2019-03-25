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
package org.sonatype.nexus.cleanup.internal.method;

import java.util.function.BooleanSupplier;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl.DeletionProgress;

/**
 * Cleans up components.
 * 
 * @since 3.14
 */
public interface CleanupMethod
{
  /**
   * Cleans up the given list of components.
   * 
   * @param repository - the repository containing the components to cleanup
   * @param components - what to cleanup (delete / move)
   * @param cancelledCheck - allows the cleanup to be stopped by the caller before finishing iterating the components
   * @return the number of components cleaned up
   */
  DeletionProgress run(Repository repository,
                       Iterable<EntityId> components,
                       BooleanSupplier cancelledCheck);
}
