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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityUUID;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.testdb.DataSessionRule;

import com.google.common.collect.ImmutableMap;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

/**
 * Test the {@link DataSessionRule}.
 */
public class DataSessionRuleTest
    extends TestSupport
{
  /**
   * Intercepts MyBatis commits so we can arbitrarily fail them.
   */
  @Intercepts({@Signature(type = Executor.class, method = "commit", args = {boolean.class})})
  static class CommitInterceptor
      implements Interceptor
  {
    boolean failNextCommit; // one-shot flag to fail the next commit

    @Override
    public Object intercept(final Invocation invocation) throws Throwable {
      if (failNextCommit) {
        // close connection without telling MyBatis - proceeding commit will then fail
        ((Executor) invocation.getTarget()).getTransaction().getConnection().close();
        failNextCommit = false;
      }
      return invocation.proceed();
    }
  }

  private CommitInterceptor commitInterceptor = new CommitInterceptor();

  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(TestItemDAO.class).intercept(commitInterceptor);

  @Test
  public void testCrudOperations() {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      TestItemDAO dao = session.access(TestItemDAO.class);

      assertThat(dao.browse(), emptyIterable());

      TestItem itemA = new TestItem();
      itemA.setVersion(1);
      itemA.setEnabled(true);
      itemA.setNotes("test-entity");
      itemA.setProperties(ImmutableMap.of("sample", "data"));

      dao.create(itemA);

      assertThat(dao.read(itemA.getId()).get(), is(itemA));

      assertThat(dao.browse(), contains(itemA));

      TestItem itemB = new TestItem();
      itemB.setVersion(2);
      itemB.setEnabled(false);
      itemB.setNotes("test-entity");
      itemB.setProperties(ImmutableMap.of());

      dao.create(itemB);

      assertThat(dao.read(itemB.getId()).get(), is(itemB));

      assertThat(dao.browse(), contains(itemA, itemB));

      assertFalse(dao.read(new EntityUUID(new UUID(4, 2))).isPresent());

      dao.delete(itemA.getId());

      assertThat(dao.browse(), contains(itemB));

      dao.delete(itemB.getId());

      assertThat(dao.browse(), emptyIterable());
    }
  }

  @Test
  public void testEntityIdManagement() {
    TestItem itemA = new TestItem();
    TestItem itemB = new TestItem();

    itemA.setNotes("test-entity-1");
    itemA.setProperties(ImmutableMap.of());

    itemB.setNotes("test-entity-2");
    itemB.setProperties(ImmutableMap.of());

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      TestItemDAO dao = session.access(TestItemDAO.class);

      dao.create(itemA);
      dao.create(itemB);

      // implicit rollback
    }

    assertThat(itemA.getId(), nullValue()); // cleared on implicit rollback
    assertThat(itemB.getId(), nullValue());

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      TestItemDAO dao = session.access(TestItemDAO.class);

      dao.create(itemA);
      dao.create(itemB);

      commitInterceptor.failNextCommit = true;

      try {
        session.getTransaction().commit();
        fail("Expected this commit to fail");
      }
      catch (RuntimeException ignore) {
        // expected
      }
    }

    assertThat(itemA.getId(), nullValue());
    assertThat(itemB.getId(), nullValue());

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      TestItemDAO dao = session.access(TestItemDAO.class);

      dao.create(itemA);

      session.getTransaction().rollback();

      dao.create(itemB);

      session.getTransaction().commit();
    }

    assertThat(itemA.getId(), nullValue()); // cleared on explicit rollback
    assertThat(itemB.getId(), notNullValue());

    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      TestItemDAO dao = session.access(TestItemDAO.class);

      dao.delete(itemB.getId());

      session.getTransaction().commit();
    }

    assertThat(itemA.getId(), nullValue());
    assertThat(itemB.getId(), notNullValue()); // kept after delete
  }

  @Test
  public void testSensitiveAttributesEncryptedAtRest() throws SQLException {
    try (DataSession<?> session = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      TestItemDAO dao = session.access(TestItemDAO.class);

      assertThat(dao.browse(), emptyIterable());

      TestItem itemA = new TestItem();
      itemA.setVersion(1);
      itemA.setEnabled(true);
      itemA.setNotes("test-entity");
      itemA.setProperties(ImmutableMap.of("sample", "data"));

      dao.create(itemA);

      session.getTransaction().commit();

      assertThat(dao.read(itemA.getId()).get(), is(itemA));

      try (Connection connection = sessionRule.openConnection(DEFAULT_DATASTORE_NAME);
          PreparedStatement statement = connection.prepareStatement("SELECT * FROM test_item;");
          ResultSet resultSet = statement.executeQuery()) {
        assertTrue("Expected at least one test_item", resultSet.next());
        assertThat(resultSet.getString("properties").replaceAll(" *: *", ":"),
            allOf(containsString("\"sample\":\"data\"")));
      }

      assertThat(dao.browse(), contains(itemA));

      dao.delete(itemA.getId());

      session.getTransaction().commit();

      assertThat(dao.browse(), emptyIterable());
    }
  }
}
