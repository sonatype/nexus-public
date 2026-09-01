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
package org.sonatype.nexus.internal.node.upgrade;

import org.sonatype.nexus.internal.node.DeploymentIdDAO;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-database tests for {@link UpgradeDeploymentIdStore} against the {@code deployment_id} table.
 */
class UpgradeDeploymentIdStoreTest
{
  @DataSessionConfiguration(daos = {DeploymentIdDAO.class})
  TestDataSessionSupplier dataSessionSupplier;

  private UpgradeDeploymentIdStore store() {
    return new UpgradeDeploymentIdStore(dataSessionSupplier);
  }

  @DatabaseTest
  void get_whenEmptyTable_returnsEmpty() {
    assertThat(store().get()).isEmpty();
  }

  @DatabaseTest
  void set_whenEmpty_persistsValue() {
    UpgradeDeploymentIdStore store = store();
    store.set("deployment-1");
    assertThat(store.get()).contains("deployment-1");
  }

  @DatabaseTest
  void set_whenValueExists_overwrites() {
    UpgradeDeploymentIdStore store = store();
    store.set("first");
    store.set("second");
    assertThat(store.get()).contains("second");
  }
}
