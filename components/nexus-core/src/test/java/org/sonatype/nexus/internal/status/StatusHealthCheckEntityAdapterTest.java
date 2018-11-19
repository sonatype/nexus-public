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
package org.sonatype.nexus.internal.status;

import java.util.Date;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.internal.status.StatusHealthCheckEntityAdapter.NodeHealthCheck;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class StatusHealthCheckEntityAdapterTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private StatusHealthCheckEntityAdapter entityAdapter;

  @Before
  public void before() {
    entityAdapter = new StatusHealthCheckEntityAdapter();
  }

  @Test
  public void testRegister() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);
      OSchema schema = db.getMetadata().getSchema();
      assertThat(schema.getClass(entityAdapter.getTypeName()), is(notNullValue()));
    }
  }

  @Test
  public void testReadWrite() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);

      NodeHealthCheck entity = entityAdapter.read(db, "node1");
      assertThat(entity, is(nullValue()));

      Date expectedTime = new Date();
      entity = entityAdapter.newEntity();
      entity.nodeId = "node1";
      entity.lastHealthCheck = expectedTime;
      entityAdapter.addEntity(db, entity);

      entity = entityAdapter.read(db, "node1");
      assertThat(entity, is(notNullValue()));
      assertThat(entity.lastHealthCheck, is(expectedTime));
    }
  }
}
