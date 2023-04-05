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
package org.sonatype.nexus.internal.node.datastore;

import java.util.Collections;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.node.datastore.NodeIdStore;
import org.sonatype.nexus.testdb.DataSessionRule;
import org.sonatype.nexus.transaction.TransactionModule;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.inject.Guice;
import com.google.inject.Provides;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Tests for local {@link NodeAccess}.
 */
@SuppressWarnings("HardCodedStringLiteral")
@Category(SQLTestGroup.class)
public class LocalNodeAccessTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(NodeIdDAO.class);

  @Mock
  private EventManager eventManager;

  private NodeAccess nodeAccess;

  private NodeIdStore store;

  @Before
  public void setUp() throws Exception {
    store = Guice.createInjector(new TransactionModule()
    {
      @Provides
      DataSessionSupplier getDataSessionSupplier() {
        return sessionRule;
      }

      @Provides
      EventManager getEventManager() {
        return eventManager;
      }
    }).getInstance(NodeIdStoreImpl.class);

    nodeAccess = new LocalNodeAccess(store);

    UnitOfWork.beginBatch(() -> sessionRule.openSession(DataStoreManager.DEFAULT_DATASTORE_NAME));
  }

  @After
  public void tearDown() throws Exception {
    if (nodeAccess != null) {
      nodeAccess.stop();
    }
    UnitOfWork.end();
  }

  @Test
  public void testGeneratesId() throws Exception {
    nodeAccess.start();

    Optional<String> nodeId = store.get();
    assertThat(nodeId.isPresent(), is(true));
  }

  @Test
  public void testUsesDatabaseId() throws Exception {
    store.set("foo");
    nodeAccess.start();

    assertThat(nodeAccess.getId(), is("foo"));
  }

  @Test
  public void localIsOldestNode() throws Exception {
    nodeAccess.start();
    assertThat(nodeAccess.isOldestNode(), is(true));
  }

  @Test
  public void getMemberAliasesKeyValueEqualToIdentity() throws Exception {
    nodeAccess.start();
    assertThat(nodeAccess.getMemberAliases(),
        equalTo(Collections.singletonMap(nodeAccess.getId(), nodeAccess.getId())));
  }
}
