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

import com.google.common.collect.ImmutableList;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.orient.entity.DeconflictStepSupport.pickLatest;

/**
 * Tests for {@link DeconflictStep}.
 */
public class DeconflictStepTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private final TestEntityAdapter entityAdapter = new TestEntityAdapter();

  private final DateTime TEST_TIME = DateTime.now();

  private TestEntity entity;

  private ODocument initialRecord;

  private static class TestEntity
      extends AbstractEntity
  {
    String text;

    DateTime time;
  }

  private static class TestEntityAdapter
      extends EntityAdapter<TestEntity>
  {
    static final String DB_CLASS = new OClassNameBuilder().type("test").build();

    TestEntityAdapter() {
      super(DB_CLASS);
    }

    @Override
    protected TestEntity newEntity() {
      return new TestEntity();
    }

    @Override
    protected void defineType(OClass type) {
      type.createProperty("text", OType.STRING);
      type.createProperty("time", OType.DATETIME);
    }

    @Override
    protected void writeFields(ODocument document, TestEntity entity) {
      document.field("text", entity.text);
      document.field("time", entity.time.toDate());
    }

    @Override
    protected void readFields(ODocument document, TestEntity entity) {
      entity.text = document.field("text", String.class);
      entity.time = new DateTime((long) document.field("time", Long.class));
    }

    @Override
    public boolean resolveConflicts() {
      return true;
    }
  }

  public void setup(final boolean withConflictHook) {

    if (withConflictHook) {
      entityAdapter.enableConflictHook(new ConflictHook(true));
      entityAdapter.setDeconflictSteps(ImmutableList.of(
          // attempts to deconflict the 'time' field by always picking the latest time
          (storedRecord, changeRecord) -> pickLatest(storedRecord, changeRecord, "time")));
    }

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);
    }

    entity = entityAdapter.newEntity();

    // first create our test entity with some default values
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      entity.text = "initial";
      entity.time = new DateTime(0);
      // keep initial record so we can use it to trigger conflicts
      initialRecord = entityAdapter.addEntity(db, entity);
      db.commit();
    }

    // update the entity, so it moves on from the initial record
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      entity.text = "updated";
      entity.time = TEST_TIME;
      entityAdapter.editEntity(db, entity);
      db.commit();
    }
  }

  @Test
  public void testConflictsRemainUnresolvedWithoutConflictHook() {
    setup(false);

    // all attempts to change initial record should fail due to MVCC

    DateTime oneHourBefore = TEST_TIME.minusHours(1);
    DateTime oneHourLater = TEST_TIME.plusHours(1);

    entity.text = "test!";
    entity.time = oneHourBefore;
    assertFalse(tryConflictingUpdate(entity));
    assertThat(entity.text, is("updated"));
    assertThat(entity.time, is(TEST_TIME));

    entity.text = "test!";
    entity.time = TEST_TIME;
    assertFalse(tryConflictingUpdate(entity));
    assertThat(entity.text, is("updated"));
    assertThat(entity.time, is(TEST_TIME));

    entity.text = "test!";
    entity.time = oneHourLater;
    assertFalse(tryConflictingUpdate(entity));
    assertThat(entity.text, is("updated"));
    assertThat(entity.time, is(TEST_TIME));

    entity.text = "updated";
    entity.time = oneHourBefore;
    assertFalse(tryConflictingUpdate(entity));
    assertThat(entity.text, is("updated"));
    assertThat(entity.time, is(TEST_TIME));

    entity.text = "updated";
    entity.time = TEST_TIME;
    assertFalse(tryConflictingUpdate(entity));
    assertThat(entity.text, is("updated"));
    assertThat(entity.time, is(TEST_TIME));

    entity.text = "updated";
    entity.time = oneHourLater;
    assertFalse(tryConflictingUpdate(entity));
    assertThat(entity.text, is("updated"));
    assertThat(entity.time, is(TEST_TIME));
  }

  @Test
  public void testConflictsAreResolvedWithConflictHook() {
    setup(true);

    DateTime oneHourBefore = TEST_TIME.minusHours(1);
    DateTime oneHourLater = TEST_TIME.plusHours(1);

    // denied - due to conflicting text update
    entity.text = "test!";
    entity.time = oneHourBefore;
    assertFalse(tryConflictingUpdate(entity));
    assertThat(entity.text, is("updated"));
    assertThat(entity.time, is(TEST_TIME));

    // denied - due to conflicting text update
    entity.text = "test!";
    entity.time = TEST_TIME;
    assertFalse(tryConflictingUpdate(entity));
    assertThat(entity.text, is("updated"));
    assertThat(entity.time, is(TEST_TIME));

    // denied - due to conflicting text update
    entity.text = "test!";
    entity.time = oneHourLater;
    assertFalse(tryConflictingUpdate(entity));
    assertThat(entity.text, is("updated"));
    assertThat(entity.time, is(TEST_TIME));

    // merged - text matches, resolves time conflict by picking latest (value in DB already)
    entity.text = "updated";
    entity.time = oneHourBefore;
    assertTrue(tryConflictingUpdate(entity));
    assertThat(entity.text, is("updated"));
    assertThat(entity.time, is(TEST_TIME));

    // allowed - text matches, time matches
    entity.text = "updated";
    entity.time = TEST_TIME;
    assertTrue(tryConflictingUpdate(entity));
    assertThat(entity.text, is("updated"));
    assertThat(entity.time, is(TEST_TIME));

    // allowed - text matches, resolves time conflict by picking latest (value in update)
    entity.text = "updated";
    entity.time = oneHourLater;
    assertTrue(tryConflictingUpdate(entity));
    assertThat(entity.text, is("updated"));
    assertThat(entity.time, is(oneHourLater));
  }

  private boolean tryConflictingUpdate(final TestEntity entity) {
    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();

      ODocument copy = initialRecord.copy();
      entityAdapter.writeFields(copy, entity);
      copy.save();

      try {
        db.commit();
        return true;
      }
      catch (OConcurrentModificationException e) {
        logger.debug("Update denied due to conflict", e);
        return false;
      }
    }
    finally {
      try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
        entityAdapter.readFields(db.load(entityAdapter.recordIdentity(entity)), entity);
      }
    }
  }
}
