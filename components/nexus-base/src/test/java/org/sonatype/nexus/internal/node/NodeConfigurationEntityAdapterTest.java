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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.node.NodeConfiguration;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link NodeConfigurationEntityAdapter}
 */
public class NodeConfigurationEntityAdapterTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private NodeConfigurationEntityAdapter underTest;

  @Before
  public void setUp() {
    underTest = new NodeConfigurationEntityAdapter();
  }

  @Test
  public void testBasicCrud() throws Exception {
    NodeConfiguration nodeConfig;
    ODocument nodeConfigDoc;

    // create it
    try (ODatabaseDocumentTx db = database.getInstance().connect()){
      underTest.register(db);
      nodeConfig = underTest.newEntity();
      nodeConfig.setId("id");
      nodeConfig.setFriendlyNodeName("test node");
      underTest.addEntity(db, nodeConfig);
    }

    // read it
    try (ODatabaseDocumentTx db = database.getInstance().connect()){
      underTest.register(db);
      nodeConfigDoc = underTest.selectById(db, "id");
      nodeConfig = underTest.readEntity(nodeConfigDoc);
    }
    assertThat(nodeConfig, notNullValue());
    assertThat(nodeConfig.getId(), equalTo("id"));
    assertThat(nodeConfig.getFriendlyNodeName(), equalTo("test node"));

    // update it
    try (ODatabaseDocumentTx db = database.getInstance().connect()){
      underTest.register(db);
      nodeConfig = new NodeConfiguration("id", "updated test node");
      nodeConfigDoc = underTest.selectById(db, "id");
      underTest.writeEntity(nodeConfigDoc, nodeConfig);
      nodeConfigDoc = underTest.selectById(db, "id");
      nodeConfig = underTest.readEntity(nodeConfigDoc);
    }
    assertThat(nodeConfig, notNullValue());
    assertThat(nodeConfig.getId(), equalTo("id"));
    assertThat(nodeConfig.getFriendlyNodeName(), equalTo("updated test node"));

    // delete it
    try (ODatabaseDocumentTx db = database.getInstance().connect()){
      underTest.register(db);
      nodeConfigDoc = underTest.selectById(db, "id");
      db.delete(nodeConfigDoc);
      nodeConfigDoc = underTest.selectById(db, "id");
    }
    assertThat(nodeConfigDoc, nullValue());
  }
}
