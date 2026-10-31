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
package org.sonatype.nexus.internal.node.datastore.upgrade;

import org.sonatype.nexus.internal.node.datastore.NodeIdDAO;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-database tests for {@link UpgradeNodeIdStore} against the {@code node_id} table.
 */
class UpgradeNodeIdStoreTest
{
  @DataSessionConfiguration(daos = {NodeIdDAO.class})
  TestDataSessionSupplier dataSessionSupplier;

  private UpgradeNodeIdStore store() {
    return new UpgradeNodeIdStore(dataSessionSupplier);
  }

  @DatabaseTest
  void get_whenEmptyTable_returnsEmpty() {
    assertThat(store().get()).isEmpty();
  }

  @DatabaseTest
  void set_whenEmpty_persistsValue() {
    UpgradeNodeIdStore store = store();
    store.set("foo");
    assertThat(store.get()).contains("foo");
  }

  @DatabaseTest
  void set_whenValueExists_overwrites() {
    UpgradeNodeIdStore store = store();
    store.set("a");
    store.set("b");
    assertThat(store.get()).contains("b");
  }

  @DatabaseTest
  void getOrCreate_whenEmpty_generatesAndPersists() {
    UpgradeNodeIdStore store = store();
    String created = store.getOrCreate();
    assertThat(created).isNotBlank();
    assertThat(store.get()).contains(created);
  }

  @DatabaseTest
  void getOrCreate_whenPresent_returnsExisting() {
    UpgradeNodeIdStore store = store();
    store.set("existing-node");
    assertThat(store.getOrCreate()).isEqualTo("existing-node");
  }
}
