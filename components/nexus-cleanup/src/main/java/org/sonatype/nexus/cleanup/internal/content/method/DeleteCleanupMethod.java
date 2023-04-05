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
package org.sonatype.nexus.cleanup.internal.content.method;

import java.util.function.BooleanSupplier;
import java.util.stream.Stream;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.cleanup.internal.method.CleanupMethod;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.task.DeletionProgress;

/**
 * Provides a delete mechanism for cleanup
 *
 * @since 3.29
 */
@Named
public class DeleteCleanupMethod
    extends ComponentSupport
    implements CleanupMethod
{
  @Override
  public DeletionProgress run(
      final Repository repository,
      final Stream<FluentComponent> components,
      final BooleanSupplier cancelledCheck)
  {
    ContentMaintenanceFacet maintenance = repository.facet(ContentMaintenanceFacet.class);
    DeletionProgress progress = new DeletionProgress();


    progress.addComponentCount(maintenance.deleteComponents(components));
    

    return progress;
  }
}
