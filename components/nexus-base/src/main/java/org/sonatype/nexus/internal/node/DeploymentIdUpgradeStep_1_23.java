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
package org.sonatype.nexus.internal.node;

import java.sql.Connection;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.node.datastore.NodeIdStore;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
public class DeploymentIdUpgradeStep_1_23
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private final NodeIdStore nodeIdStore;

  private final DeploymentIdStore deploymentIdStore;

  @Inject
  public DeploymentIdUpgradeStep_1_23(final DeploymentIdStore deploymentIdStore, final NodeIdStore nodeIdStore) {
    this.nodeIdStore = checkNotNull(nodeIdStore);
    this.deploymentIdStore = checkNotNull(deploymentIdStore);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("1.23");
  }

  @Override
  public void migrate(final Connection connection) {
    Optional<String> deploymentId = deploymentIdStore.get();
    if (deploymentId.isPresent()) {
      return;
    }

    Optional<String> nodeId = nodeIdStore.get();
    deploymentIdStore.set(nodeId.orElseThrow(
        () -> new RuntimeException("Could not migrate deployment id. Reason: deploymentId and nodeId are absent.")));

    deploymentId = deploymentIdStore.get();
    if (deploymentId.isPresent()) {
      log.debug("Migration of deployment id was successful.");
    }
  }
}
