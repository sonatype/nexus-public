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
package com.sonatype.nexus.ssl.plugin.internal.keystore;

import java.nio.charset.StandardCharsets;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.orient.entity.EntityAdapter.EventKind;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class KeyStoreDataEntityAdapterTest
    extends TestSupport
{
  private static final String KEY_STORE_NAME = "ssl/test.ks";

  private static final byte[] KEY_STORE_DATA = "test-key-store-data".getBytes(StandardCharsets.UTF_8);

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private KeyStoreDataEntityAdapter entityAdapter;

  @Before
  public void setUp() {
    entityAdapter = new KeyStoreDataEntityAdapter();
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

      assertThat(entityAdapter.load(db, KEY_STORE_NAME), is(nullValue()));

      KeyStoreData entity = new KeyStoreData();
      entity.setName(KEY_STORE_NAME);
      entity.setBytes(KEY_STORE_DATA);
      entityAdapter.save(db, entity);

      entity = entityAdapter.load(db, KEY_STORE_NAME);
      assertThat(entity, is(notNullValue()));
      assertThat(entity.getName(), is(KEY_STORE_NAME));
      assertThat(entity.getBytes(), is(KEY_STORE_DATA));

      entity = new KeyStoreData();
      entity.setName(KEY_STORE_NAME);
      entity.setBytes(new byte[0]);
      entityAdapter.save(db, entity);

      entity = entityAdapter.load(db, KEY_STORE_NAME);
      assertThat(entity, is(notNullValue()));
      assertThat(entity.getName(), is(KEY_STORE_NAME));
      assertThat(entity.getBytes(), is(new byte[0]));
    }
  }

  @Test(expected = ORecordDuplicatedException.class)
  public void testIndex_EnforceUniqueName() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);

      KeyStoreData entity = new KeyStoreData();
      entity.setName(KEY_STORE_NAME);
      entity.setBytes(KEY_STORE_DATA);
      entityAdapter.save(db, entity);

      entity = new KeyStoreData();
      entity.setName(KEY_STORE_NAME);
      entity.setBytes(KEY_STORE_DATA);
      entityAdapter.addEntity(db, entity);
    }
  }

  @Test
  public void testNewEvent_Create() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);

      KeyStoreData entity = new KeyStoreData();
      entity.setName(KEY_STORE_NAME);
      entity.setBytes(KEY_STORE_DATA);
      ODocument document = entityAdapter.addEntity(db, entity);

      EntityEvent event = entityAdapter.newEvent(document, EventKind.CREATE);
      assertThat(event, is(instanceOf(KeyStoreDataCreatedEvent.class)));
      assertThat(((KeyStoreDataEvent) event).getKeyStoreName(), is(KEY_STORE_NAME));
    }
  }

  @Test
  public void testNewEvent_Update() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);

      KeyStoreData entity = new KeyStoreData();
      entity.setName(KEY_STORE_NAME);
      entity.setBytes(KEY_STORE_DATA);
      ODocument document = entityAdapter.addEntity(db, entity);

      EntityEvent event = entityAdapter.newEvent(document, EventKind.UPDATE);
      assertThat(event, is(instanceOf(KeyStoreDataUpdatedEvent.class)));
      assertThat(((KeyStoreDataEvent) event).getKeyStoreName(), is(KEY_STORE_NAME));
    }
  }

  @Test
  public void testNewEvent_Delete() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);

      KeyStoreData entity = new KeyStoreData();
      entity.setName(KEY_STORE_NAME);
      entity.setBytes(KEY_STORE_DATA);
      ODocument document = entityAdapter.addEntity(db, entity);

      EntityEvent event = entityAdapter.newEvent(document, EventKind.DELETE);
      assertThat(event, is(nullValue()));
    }
  }
}
