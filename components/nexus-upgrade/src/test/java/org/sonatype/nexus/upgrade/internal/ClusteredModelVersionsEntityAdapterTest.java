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
package org.sonatype.nexus.upgrade.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ClusteredModelVersionsEntityAdapterTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private ClusteredModelVersionsEntityAdapter entityAdapter;

  @Before
  public void setUp() {
    entityAdapter = new ClusteredModelVersionsEntityAdapter();
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
  public void testSaveAndLoad() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);

      assertThat(entityAdapter.singleton.get(db), is(nullValue()));

      ClusteredModelVersions entity = new ClusteredModelVersions();
      entity.put("model-a", "1.2");
      entity.put("model-b", "2.1");
      entityAdapter.singleton.set(db, entity);

      entity = entityAdapter.singleton.get(db);
      assertThat(entity, is(notNullValue()));
      assertThat(entity.getModelVersions(), hasEntry("model-a", "1.2"));
      assertThat(entity.getModelVersions(), hasEntry("model-b", "2.1"));
      assertThat(entity.getModelVersions().entrySet(), hasSize(2));

      entity.put("model-a", "1.3");
      entityAdapter.singleton.set(db, entity);

      entity = entityAdapter.singleton.get(db);
      assertThat(entity, is(notNullValue()));
      assertThat(entity.getModelVersions(), hasEntry("model-a", "1.3"));
      assertThat(entity.getModelVersions(), hasEntry("model-b", "2.1"));
      assertThat(entity.getModelVersions().entrySet(), hasSize(2));
    }
  }
}
