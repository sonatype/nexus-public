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

import java.util.stream.IntStream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.entity.EntityLog.UnknownDeltaException;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.impl.local.paginated.OLocalPaginatedStorage;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.OLogSequenceNumber;
import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.ONonTxOperationPerformedWALRecord;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static com.orientechnologies.orient.core.config.OGlobalConfiguration.STORAGE_TRACK_CHANGED_RECORDS_IN_WAL;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.orient.entity.EntityLog.ENTITY_LOG_LIMIT_KEY;

/**
 * Tests for {@link EntityLog}.
 */
public class EntityLogTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inFilesystem("test");

  TestEntityAdapter entityAdapter = new TestEntityAdapter();

  static class TestEntity
      extends AbstractEntity
  {
    String text;
  }

  static class TestEntityAdapter
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
    public boolean sendEvents() {
      return true;
    }
  }

  @After
  public void reset() {
    STORAGE_TRACK_CHANGED_RECORDS_IN_WAL.setValue(STORAGE_TRACK_CHANGED_RECORDS_IN_WAL.getDefValue());
  }

  @Test
  public void testWithRecordTrackingEnabled() throws Exception {

    // this is true when in distributed mode; we set it here for testing purposes
    STORAGE_TRACK_CHANGED_RECORDS_IN_WAL.setValue(true);

    EntityLog log = new EntityLog(database.getInstanceProvider(), entityAdapter);

    TestEntity entityA = new TestEntity();
    TestEntity entityB = new TestEntity();
    TestEntity entityC = new TestEntity();

    ODocument recordA;
    ODocument recordB;
    ODocument recordC;

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      entityAdapter.register(db);

      OLogSequenceNumber mark1 = log.mark();

      // verify no changes since the first mark
      assertThat(log.since(mark1), is(ImmutableMap.of()));

      db.begin();
      entityA.text = "A is new";
      recordA = entityAdapter.addEntity(db, entityA);
      db.commit();

      // log should now show entity A changed since first mark
      assertThat(log.since(mark1), is(ImmutableMap.of(
          recordA.getIdentity(), entityAdapter)));

      OLogSequenceNumber mark2 = log.mark();

      // verify no changes since the second mark
      assertThat(log.since(mark2), is(ImmutableMap.of()));

      db.begin();
      entityB.text = "B is new";
      recordB = entityAdapter.addEntity(db, entityB);
      db.commit();

      db.begin();
      entityB.text = "B is updated";
      recordB = entityAdapter.editEntity(db, entityB);
      db.commit();

      // log should now show entity A and B changed since first mark
      assertThat(log.since(mark1), is(ImmutableMap.of(
          recordA.getIdentity(), entityAdapter,
          recordB.getIdentity(), entityAdapter)));

      // log should now show entity B changed since second mark
      assertThat(log.since(mark2), is(ImmutableMap.of(
          recordB.getIdentity(), entityAdapter)));

      OLogSequenceNumber mark3 = log.mark();

      // verify no changes since the third mark
      assertThat(log.since(mark3), is(ImmutableMap.of()));

      db.begin();
      entityC.text = "C is new";
      recordC = entityAdapter.addEntity(db, entityC);
      db.commit();

      db.begin();
      entityC.text = "C is updated";
      recordC = entityAdapter.editEntity(db, entityC);
      db.commit();

      db.begin();
      entityAdapter.deleteEntity(db, entityC);
      db.commit();

      // log should now show entity A, B, and C changed since first mark
      assertThat(log.since(mark1), is(ImmutableMap.of(
          recordA.getIdentity(), entityAdapter,
          recordB.getIdentity(), entityAdapter,
          recordC.getIdentity(), entityAdapter)));

      // log should now show entity B and C changed since second mark
      assertThat(log.since(mark2), is(ImmutableMap.of(
          recordB.getIdentity(), entityAdapter,
          recordC.getIdentity(), entityAdapter)));

      // log should now show entity C changed since third mark
      assertThat(log.since(mark3), is(ImmutableMap.of(
          recordC.getIdentity(), entityAdapter)));

      OLogSequenceNumber mark4 = log.mark();

      // verify no changes since the fourth mark
      assertThat(log.since(mark4), is(ImmutableMap.of()));

      // insert an entry into the log that isn't associated with an entity change
      ((OLocalPaginatedStorage) db.getStorage().getUnderlying())
          .getWALInstance().log(new ONonTxOperationPerformedWALRecord());

      // verify still no changes since the fourth mark
      assertThat(log.since(mark4), is(ImmutableMap.of()));
    }
  }

  @Test
  public void testWithRecordTrackingDisabled() throws Exception {

    EntityLog log = new EntityLog(database.getInstanceProvider(), entityAdapter);

    TestEntity entityA = new TestEntity();
    TestEntity entityB = new TestEntity();
    TestEntity entityC = new TestEntity();

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      entityAdapter.register(db);

      OLogSequenceNumber mark1 = log.mark();

      // verify no changes since the first mark
      assertThat(log.since(mark1), is(ImmutableMap.of()));

      db.begin();
      entityA.text = "A is new";
      entityAdapter.addEntity(db, entityA);
      db.commit();

      try {
        log.since(mark1); // log should report there is a change, but delta is unknown
        fail("Expected UnknownDeltaException");
      }
      catch (UnknownDeltaException e) {
        assertThat(e.getMessage(), containsString(mark1.toString()));
      }

      OLogSequenceNumber mark2 = log.mark();

      // verify no changes since the second mark
      assertThat(log.since(mark2), is(ImmutableMap.of()));

      db.begin();
      entityB.text = "B is new";
      entityAdapter.addEntity(db, entityB);
      db.commit();

      db.begin();
      entityB.text = "B is updated";
      entityAdapter.editEntity(db, entityB);
      db.commit();

      try {
        log.since(mark2); // log should report there is a change, but delta is unknown
        fail("Expected UnknownDeltaException");
      }
      catch (UnknownDeltaException e) {
        assertThat(e.getMessage(), containsString(mark2.toString()));
      }

      OLogSequenceNumber mark3 = log.mark();

      // verify no changes since the third mark
      assertThat(log.since(mark3), is(ImmutableMap.of()));

      db.begin();
      entityC.text = "C is new";
      entityAdapter.addEntity(db, entityC);
      db.commit();

      db.begin();
      entityC.text = "C is updated";
      entityAdapter.editEntity(db, entityC);
      db.commit();

      db.begin();
      entityAdapter.deleteEntity(db, entityC);
      db.commit();

      try {
        log.since(mark3); // log should report there is a change, but delta is unknown
        fail("Expected UnknownDeltaException");
      }
      catch (UnknownDeltaException e) {
        assertThat(e.getMessage(), containsString(mark3.toString()));
      }

      OLogSequenceNumber mark4 = log.mark();

      // verify no changes since the fourth mark
      assertThat(log.since(mark4), is(ImmutableMap.of()));

      // insert an entry into the log that isn't associated with an entity change
      ((OLocalPaginatedStorage) db.getStorage().getUnderlying())
          .getWALInstance().log(new ONonTxOperationPerformedWALRecord());

      // verify still no changes since the fourth mark
      assertThat(log.since(mark4), is(ImmutableMap.of()));
    }
  }

  @Test
  public void testLogMarkerBoundaries() {

    EntityLog log = new EntityLog(database.getInstanceProvider(), entityAdapter);

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      entityAdapter.register(db);

      try {
        log.since(null);
        fail("Expected NullPointerException");
      }
      catch (NullPointerException e) {
        // expected
      }

      OLogSequenceNumber mark = log.mark();

      // move marker into far future, to represent a dangling marker after a restore
      mark = new OLogSequenceNumber(mark.getSegment() + 8, mark.getPosition() + 1024);

      try {
        log.since(mark); // log should report there is a change, but delta is unknown
        fail("Expected UnknownDeltaException");
      }
      catch (UnknownDeltaException e) {
        assertThat(e.getMessage(), containsString(mark.toString()));
      }
    }
  }

  @Test
  public void entityLogHasDefaultLimit() throws Exception {

    // this is true when in distributed mode; we set it here for testing purposes
    STORAGE_TRACK_CHANGED_RECORDS_IN_WAL.setValue(true);

    EntityLog log = new EntityLog(database.getInstanceProvider(), entityAdapter);

    OLogSequenceNumber mark = log.mark();

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      entityAdapter.register(db);
      db.begin();
      IntStream.range(0, 1000).forEach(i -> entityAdapter.addEntity(db, new TestEntity()));
      db.commit();
    }

    assertThat(log.since(mark).size(), is(1000));

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      entityAdapter.addEntity(db, new TestEntity());
      db.commit();
    }

    try {
      log.since(mark);
      fail("Expected UnknownDeltaException");
    }
    catch (UnknownDeltaException e) {
      assertThat(e.getCause().getMessage(), containsString("Too many changes to return"));
    }
  }

  @Test
  public void entityLogRespectsCustomLimit() throws Exception {

    // this is true when in distributed mode; we set it here for testing purposes
    STORAGE_TRACK_CHANGED_RECORDS_IN_WAL.setValue(true);

    System.setProperty(ENTITY_LOG_LIMIT_KEY, "200");

    EntityLog log = new EntityLog(database.getInstanceProvider(), entityAdapter);

    OLogSequenceNumber mark = log.mark();

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      entityAdapter.register(db);
      db.begin();
      IntStream.range(0, 200).forEach(i -> entityAdapter.addEntity(db, new TestEntity()));
      db.commit();
    }

    assertThat(log.since(mark).size(), is(200));

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      db.begin();
      entityAdapter.addEntity(db, new TestEntity());
      db.commit();
    }

    try {
      log.since(mark);
      fail("Expected UnknownDeltaException");
    }
    catch (UnknownDeltaException e) {
      assertThat(e.getCause().getMessage(), containsString("Too many changes to return"));
    }
  }

  @Test
  public void negativeLimitImpliesNoLimit() throws Exception {

    // this is true when in distributed mode; we set it here for testing purposes
    STORAGE_TRACK_CHANGED_RECORDS_IN_WAL.setValue(true);

    System.setProperty(ENTITY_LOG_LIMIT_KEY, "-1");

    EntityLog log = new EntityLog(database.getInstanceProvider(), entityAdapter);

    OLogSequenceNumber mark = log.mark();

    try (ODatabaseDocumentTx db = database.getInstance().acquire()) {
      entityAdapter.register(db);
      db.begin();
      IntStream.range(0, 1024).forEach(i -> entityAdapter.addEntity(db, new TestEntity()));
      db.commit();
    }

    assertThat(log.since(mark).size(), is(1024));
  }
}
