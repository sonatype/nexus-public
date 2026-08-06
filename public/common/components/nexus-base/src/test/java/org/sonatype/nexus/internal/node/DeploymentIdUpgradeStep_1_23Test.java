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

import org.sonatype.nexus.internal.node.datastore.NodeIdDAO;
import org.sonatype.nexus.internal.node.datastore.upgrade.UpgradeNodeIdStore;
import org.sonatype.nexus.internal.node.upgrade.UpgradeDeploymentIdStore;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-database tests for {@link DeploymentIdUpgradeStep_1_23} using the direct-SQL Upgrade stores.
 */
class DeploymentIdUpgradeStep_1_23Test
{
  @DataSessionConfiguration(daos = {NodeIdDAO.class, DeploymentIdDAO.class})
  TestDataSessionSupplier dataSessionSupplier;

  private UpgradeNodeIdStore nodeIdStore() {
    return new UpgradeNodeIdStore(dataSessionSupplier);
  }

  private UpgradeDeploymentIdStore deploymentIdStore() {
    return new UpgradeDeploymentIdStore(dataSessionSupplier);
  }

  private DeploymentIdUpgradeStep_1_23 underTest() {
    return new DeploymentIdUpgradeStep_1_23(deploymentIdStore(), nodeIdStore());
  }

  @DatabaseTest
  void migrate_whenNoDeploymentId_setsItFromNewNodeId() throws Exception {
    try (Connection conn = dataSessionSupplier.openConnection()) {
      underTest().migrate(conn);
    }

    String nodeId = nodeIdStore().get().orElseThrow();
    assertThat(deploymentIdStore().get()).contains(nodeId);
  }

  @DatabaseTest
  void migrate_whenDeploymentIdExists_isNoOp() throws Exception {
    deploymentIdStore().set("existing-deployment");

    try (Connection conn = dataSessionSupplier.openConnection()) {
      underTest().migrate(conn);
    }

    assertThat(deploymentIdStore().get()).contains("existing-deployment");
    // node id was never created because the migration short-circuited
    assertThat(nodeIdStore().get()).isEmpty();
  }
}
