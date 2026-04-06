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
package org.sonatype.nexus.testdb.example;

import java.sql.SQLException;

import org.sonatype.goodies.testsupport.Test5Support;
import org.sonatype.nexus.testdb.DataSessionConfiguration;
import org.sonatype.nexus.testdb.DatabaseTest;
import org.sonatype.nexus.testdb.TestDataSessionSupplier;

import com.google.common.collect.ImmutableMap;

import static org.assertj.db.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

class DatabaseExtensionTest
    extends Test5Support
{
  @DataSessionConfiguration(daos = TestItemDAO.class)
  TestDataSessionSupplier supplier;

  @DatabaseTest
  void testGetDataSource() {
    assertThat(supplier.getDataSource().orElse(null), notNullValue());
    assertThat(supplier.getDataSource(DEFAULT_DATASTORE_NAME).orElse(null), notNullValue());
  }

  @DatabaseTest
  void testOpenConnection() throws SQLException {
    assertThat(supplier.openConnection(), notNullValue());
    assertThat(supplier.openConnection(DEFAULT_DATASTORE_NAME), notNullValue());
  }

  @DatabaseTest
  void testOpenSession() {
    assertThat(supplier.openSession(), notNullValue());
    assertThat(supplier.openSession(DEFAULT_DATASTORE_NAME), notNullValue());
  }

  @DatabaseTest
  void testOpenSerializableTransactionSession() {
    assertThat(supplier.openSerializableTransactionSession(), notNullValue());
    assertThat(supplier.openSerializableTransactionSession(DEFAULT_DATASTORE_NAME), notNullValue());
  }

  @DatabaseTest
  void testStoresCreated() {
    assertThat(supplier.table("test_item")).exists();
  }

  @DatabaseTest
  void testCallDAO() {
    TestItem item = new TestItem();
    item.setVersion(1);
    item.setEnabled(true);
    item.setNotes("test-entity");
    item.setProperties(ImmutableMap.of("sample", "data"));

    supplier.callDAO(TestItemDAO.class, dao -> dao.create(item));
    assertThat(supplier.table("test_item")).hasNumberOfRows(1);
  }

  @DatabaseTest
  void testWithDAO() {
    Iterable<TestItem> iter = supplier.withDAO(TestItemDAO.class, dao -> dao.browse());
    assertThat(iter, notNullValue());
  }
}
