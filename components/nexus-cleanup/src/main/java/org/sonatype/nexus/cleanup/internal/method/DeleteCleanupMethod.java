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

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl.DeletionProgress;

/**
 * Provides a delete mechanism for cleanup
 * 
 * @since 3.14
 */
@Named
public class DeleteCleanupMethod
    extends ComponentSupport
    implements CleanupMethod
{
  private final int batchSize;

  @Inject
  public DeleteCleanupMethod(@Named("${nexus.cleanup.batchSize:-100}") final int batchSize) {
    this.batchSize = batchSize;
  }
  
  @Override
  public DeletionProgress run(final Repository repository,
                              final Iterable<EntityId> components,
                              final BooleanSupplier cancelledCheck)
  {
    return repository.facet(ComponentMaintenance.class).deleteComponents(components, cancelledCheck, batchSize);
  }
}
