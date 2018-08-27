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
package org.sonatype.nexus.orient.entity;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.orient.entity.ConflictState.ALLOW;

/**
 * Tests for {@link ConflictHook}.
 */
public class ConflictHookTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private static class TestEntity
      extends AbstractEntity
  {
    String text;
  }

  private static class TestEntityAdapter
      extends EntityAdapter<TestEntity>
  {
    static final String DB_CLASS = new OClassNameBuilder().type("test").build();

    final boolean resolveConflicts;

    int mvccCount;

    TestEntityAdapter(final boolean resolveConflicts) {
      super(DB_CLASS);

      this.resolveConflicts = resolveConflicts;
    }

    @Override
    protected TestEntity newEntity() {
      return new TestEntity();
    }

    @Override
    protected void defineType(OClass type) {
      type.createProperty("text", OType.STRING);
    }

    @Override
    protected void writeFields(ODocument document, TestEntity entity) throws Exception {
      document.field("text", entity.text);
    }

    @Override
    protected void readFields(ODocument document, TestEntity entity) throws Exception {
      entity.text = document.field("text");
    }

    @Override
    public boolean resolveConflicts() {
      return resolveConflicts;
    }

    @Override
    public ConflictState resolve(ODocument storedRecord, ODocument changeRecord) {
      mvccCount++;
      return ALLOW;
    }
  }

  @Test
  public void mvccResolvedWhenConflictHookEnabled() {
    TestEntityAdapter entityAdapter = new TestEntityAdapter(true);
    entityAdapter.enableConflictHook(new ConflictHook(true));

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);
    }

    TestEntity entity = entityAdapter.newEntity();

    ODocument initialRecord;

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      entity.text = "initial";
      initialRecord = entityAdapter.addEntity(db, entity);
      db.commit();
    }

    assertThat(entityAdapter.mvccCount, is(0));

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      entity.text = "updated";
      entityAdapter.editEntity(db, entity);
      db.commit();
    }

    assertThat(entityAdapter.mvccCount, is(0));

    // should record MVCC but still allow change to be committed
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      initialRecord.copy().field("text", "test!").save();
      db.commit();
    }

    assertThat(entityAdapter.mvccCount, is(1));

    // should record MVCC but still allow change to be committed
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      initialRecord.copy().field("text", "test!").save();
      db.commit();
    }

    assertThat(entityAdapter.mvccCount, is(2));
  }

  @Test
  public void mvccNotResolvedWhenConflictHookDisabled() {
    TestEntityAdapter entityAdapter = new TestEntityAdapter(true);
    entityAdapter.enableConflictHook(new ConflictHook(false));

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);
    }

    TestEntity entity = entityAdapter.newEntity();

    ODocument initialRecord;

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      entity.text = "initial";
      initialRecord = entityAdapter.addEntity(db, entity);
      db.commit();
    }

    assertThat(entityAdapter.mvccCount, is(0));

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      entity.text = "updated";
      entityAdapter.editEntity(db, entity);
      db.commit();
    }

    assertThat(entityAdapter.mvccCount, is(0));

    // MVCC won't trigger ConflictHook
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      initialRecord.copy().field("text", "test!").save();
      db.commit();
      fail("Expected OConcurrentModificationException");
    }
    catch (OConcurrentModificationException e) {
      // expected
    }

    assertThat(entityAdapter.mvccCount, is(0));

    // MVCC won't trigger ConflictHook
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      initialRecord.copy().field("text", "test!").save();
      db.commit();
      fail("Expected OConcurrentModificationException");
    }
    catch (OConcurrentModificationException e) {
      // expected
    }

    assertThat(entityAdapter.mvccCount, is(0));
  }

  @Test
  public void mvccNotResolvedWhenConflictHookEnabledButAdapterResolveConflictsDisabled() {
    TestEntityAdapter entityAdapter = new TestEntityAdapter(false);
    entityAdapter.enableConflictHook(new ConflictHook(true));

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);
    }

    TestEntity entity = entityAdapter.newEntity();

    ODocument initialRecord;

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      entity.text = "initial";
      initialRecord = entityAdapter.addEntity(db, entity);
      db.commit();
    }

    assertThat(entityAdapter.mvccCount, is(0));

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      entity.text = "updated";
      entityAdapter.editEntity(db, entity);
      db.commit();
    }

    assertThat(entityAdapter.mvccCount, is(0));

    // MVCC will trigger ConflictHook but adapter won't resolve it
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      initialRecord.copy().field("text", "test!").save();
      db.commit();
      fail("Expected OConcurrentModificationException");
    }
    catch (OConcurrentModificationException e) {
      // expected
    }

    assertThat(entityAdapter.mvccCount, is(0));

    // MVCC will trigger ConflictHook but adapter won't resolve it
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      initialRecord.copy().field("text", "test!").save();
      db.commit();
      fail("Expected OConcurrentModificationException");
    }
    catch (OConcurrentModificationException e) {
      // expected
    }

    assertThat(entityAdapter.mvccCount, is(0));
  }
}
