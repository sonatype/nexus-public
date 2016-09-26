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
package org.sonatype.nexus.repository.maven.internal.hosted;

import javax.inject.Named;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.maven.MavenHostedFacet;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.repository.storage.DefaultComponentMaintenanceImpl;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.orientechnologies.common.concur.ONeedRetryException;

import static org.sonatype.nexus.repository.maven.internal.Attributes.P_ARTIFACT_ID;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_BASE_VERSION;
import static org.sonatype.nexus.repository.maven.internal.Attributes.P_GROUP_ID;
import static org.sonatype.nexus.transaction.Operations.transactional;

/**
 * maven format specific hosted {@link ComponentMaintenance}.
 *
 * @since 3.0
 */
@Named
public class MavenHostedComponentMaintenanceFacet
    extends DefaultComponentMaintenanceImpl
{
  @Override
  public void deleteComponent(final EntityId componentId) {
    String[] coordinates = transactional(getRepository().facet(StorageFacet.class).txSupplier())
        .retryOn(IllegalStateException.class).swallow(ONeedRetryException.class)
        .call(() -> {
          final StorageTx tx = UnitOfWork.currentTx();
          Component component = tx.findComponentInBucket(componentId, tx.findBucket(getRepository()));
          if (component != null) {
            return new String[]{
                component.formatAttributes().get(P_GROUP_ID, String.class),
                component.formatAttributes().get(P_ARTIFACT_ID, String.class),
                component.formatAttributes().get(P_BASE_VERSION, String.class)
            };
          }
          return null;
        });
    super.deleteComponent(componentId);
    if (coordinates != null) {
      getRepository().facet(MavenHostedFacet.class).deleteMetadata(coordinates[0], coordinates[1], coordinates[2]);
    }
  }
}
