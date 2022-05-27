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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.content.testsuite.groups.SQLTestGroup;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.testdb.DataSessionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Category(SQLTestGroup.class)
public class NodeIdDAOTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule().access(NodeIdDAO.class);

  private DataSession session;

  private NodeIdDAO dao;

  @Before
  public void setup() {
    session = sessionRule.openSession(DEFAULT_DATASTORE_NAME);
    dao = (NodeIdDAO) session.access(NodeIdDAO.class);
  }

  @After
  public void cleanup() {
    session.close();
  }

  @Test
  public void testSetAndGet() {
    String nodeId = "";
    dao.set(nodeId);
    String readData = dao.get().orElse(null);

    assertThat(readData, not(nullValue()));
    assertThat(readData, is(nodeId));

    nodeId = "";
    readData = dao.get().orElse(null);

    assertThat(readData, not(nullValue()));
    assertThat(readData, is(nodeId));

    dao.clear();
    readData = dao.get().orElse(null);

    assertThat(readData, nullValue());
  }

  @Test
  public void testCreate() {
    String nodeId = "";
    dao.create(nodeId);

    String readData = dao.get().orElse(null);
    assertThat(readData, is(nodeId));
  }

  @Test(expected = DuplicateKeyException.class)
  public void testCreate_fail() {
    String nodeId = "";
    // setup
    dao.create(nodeId);

    // sanity check
    assertThat(dao.get().isPresent(), is(true));

    // call create when it already exists
    dao.create("b");
  }
}
