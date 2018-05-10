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

import java.util.ArrayList;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.testcommon.event.SimpleEventManager;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.orientechnologies.orient.core.db.ODatabaseListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.query.live.OLiveQueryHook;
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link EntityHook}.
 */
public class EntityHookTest
    extends TestSupport
{
  @Rule
  public DatabaseInstanceRule sendingDatabase = DatabaseInstanceRule.inMemory("sender");

  @Rule
  public DatabaseInstanceRule receivingDatabase = DatabaseInstanceRule.inMemory("receiver");

  TestEntityAdapter entityAdapter = new TestEntityAdapter();

  TestSubscriber subscriber = new TestSubscriber();

  EntityHook entityHook;

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

  class TestSubscriber
  {
    List<EntityEvent> events = new ArrayList<>();

    @Subscribe
    @AllowConcurrentEvents
    public void on(EntityEvent event) {
      // store events while connected to a different db than the sender
      try (ODatabaseDocumentTx db = receivingDatabase.getInstance().acquire()) {
        db.begin();
        events.add(event);
        db.commit();
      }
    }
  }

  @Before
  public void setup() throws Exception {
    EventManager eventManager = new SimpleEventManager();
    entityHook = new EntityHook(eventManager);
    entityAdapter.enableEntityHook(entityHook);
    eventManager.register(subscriber);
  }

  @Test
  public void testEntityEventsAreSent() {
    TestEntity entityA = new TestEntity();
    TestEntity entityB = new TestEntity();

    EntityEvent event;

    try (ODatabaseDocumentTx db = sendingDatabase.getInstance().acquire()) {

      // take a copy of the default listeners/hooks before adding our own
      List<ODatabaseListener> listeners = ImmutableList.copyOf(db.getListeners());
      List<ORecordHook> hooks = ImmutableList.copyOf(db.getHooks().keySet());

      // connect up the entity hook (this is normally done in the server, but we're using a test instance here)
      entityHook.onOpen(db);

      entityAdapter.register(db);

      // CREATE
      db.begin();
      entityA.text = "A is new";
      ODocument recordA = entityAdapter.addEntity(db, entityA);
      entityB.text = "B is new";
      ODocument recordB = entityAdapter.addEntity(db, entityB);
      db.commit();

      assertThat(subscriber.events, hasSize(2));

      event = subscriber.events.get(0);
      assertThat(event.getClass().getSimpleName(), is("EntityCreatedEvent"));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(recordA));
      assertThat(event.<TestEntity> getEntity().text, is("A is new"));
      assertTrue(event.isLocal());
      assertThat(event.getRemoteNodeId(), is(nullValue()));

      event = subscriber.events.get(1);
      assertThat(event.getClass().getSimpleName(), is("EntityCreatedEvent"));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(recordB));
      assertThat(event.<TestEntity> getEntity().text, is("B is new"));
      assertTrue(event.isLocal());
      assertThat(event.getRemoteNodeId(), is(nullValue()));

      subscriber.events.clear();

      // UPDATE
      EntityHook.asRemote("REMOTE-NODE", () -> {
        db.begin();
        entityA.text = "A was updated";
        entityAdapter.editEntity(db, entityA);
        entityB.text = "B was updated";
        entityAdapter.editEntity(db, entityB);
        db.commit();
      });

      assertThat(subscriber.events, hasSize(2));

      event = subscriber.events.get(0);
      assertThat(event.getClass().getSimpleName(), is("EntityUpdatedEvent"));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(recordA));
      assertThat(event.<TestEntity> getEntity().text, is("A was updated"));
      assertFalse(event.isLocal());
      assertThat(event.getRemoteNodeId(), is("REMOTE-NODE"));

      event = subscriber.events.get(1);
      assertThat(event.getClass().getSimpleName(), is("EntityUpdatedEvent"));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(recordB));
      assertThat(event.<TestEntity> getEntity().text, is("B was updated"));
      assertFalse(event.isLocal());
      assertThat(event.getRemoteNodeId(), is("REMOTE-NODE"));

      subscriber.events.clear();

      // DELETE
      db.begin();
      entityA.text = "A was deleted"; // not-persisted
      entityAdapter.deleteEntity(db, entityA);
      entityB.text = "B was deleted"; // not-persisted
      entityAdapter.deleteEntity(db, entityB);
      db.commit();

      assertThat(subscriber.events, hasSize(2));

      event = subscriber.events.get(0);
      assertThat(event.getClass().getSimpleName(), is("EntityDeletedEvent"));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(recordA));
      // text field should reflect last *persisted* value before the delete
      assertThat(event.<TestEntity> getEntity().text, is("A was updated"));
      assertTrue(event.isLocal());
      assertThat(event.getRemoteNodeId(), is(nullValue()));

      event = subscriber.events.get(1);
      assertThat(event.getClass().getSimpleName(), is("EntityDeletedEvent"));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(recordB));
      // text field should reflect last *persisted* value before the delete
      assertThat(event.<TestEntity> getEntity().text, is("B was updated"));
      assertTrue(event.isLocal());
      assertThat(event.getRemoteNodeId(), is(nullValue()));

      entityHook.onClose(db);

      // make sure we're back to just the default listeners/hooks (except OLiveQueryHook)
      assertThat(ImmutableList.copyOf(db.getListeners()), is(withoutOLiveQueryHook(listeners)));
      assertThat(ImmutableList.copyOf(db.getHooks().keySet()), is(withoutOLiveQueryHook(hooks)));
    }
  }

  private static <T> List<T> withoutOLiveQueryHook(final List<T> elements) {
    return elements.stream().filter(e -> !(e instanceof OLiveQueryHook)).collect(toList());
  }

  @Test
  public void eventsInSameTransactionAreMerged() {
    TestEntity entity = new TestEntity();
    EntityEvent event;

    try (ODatabaseDocumentTx db = sendingDatabase.getInstance().acquire()) {
      entityHook.onOpen(db);
      entityAdapter.register(db);

      // CREATE then DELETE phantom entity in single TX
      db.begin();
      ODocument phantomRecord = entityAdapter.addEntity(db, new TestEntity());
      entityAdapter.deleteEntity(db, entityAdapter.readEntity(phantomRecord));
      db.commit();

      assertThat(subscriber.events, hasSize(0));

      // CREATE then UPDATE entity in single TX
      db.begin();
      entity.text = "A";
      entityAdapter.addEntity(db, entity);
      entity.text = "B";
      entityAdapter.editEntity(db, entity);
      db.commit();

      assertThat(subscriber.events, hasSize(1));

      event = subscriber.events.get(0);
      assertThat(event.getClass().getSimpleName(), is("EntityCreatedEvent"));
      assertThat(event.<TestEntity> getEntity().text, is("B"));

      subscriber.events.clear();

      // UPDATE entity multiple times in single TX
      db.begin();
      entity.text = "C";
      entityAdapter.editEntity(db, entity);
      entity.text = "D";
      entityAdapter.editEntity(db, entity);
      db.commit();

      assertThat(subscriber.events, hasSize(1));

      event = subscriber.events.get(0);
      assertThat(event.getClass().getSimpleName(), is("EntityUpdatedEvent"));
      assertThat(event.<TestEntity> getEntity().text, is("D"));

      subscriber.events.clear();

      // UPDATE then DELETE entity in single TX
      db.begin();
      entity.text = "E";
      entityAdapter.editEntity(db, entity);
      entity.text = "F"; // not-persisted
      entityAdapter.deleteEntity(db, entity);
      db.commit();

      assertThat(subscriber.events, hasSize(1));

      event = subscriber.events.get(0);
      assertThat(event.getClass().getSimpleName(), is("EntityDeletedEvent"));
      // text field should reflect last *persisted* value before the delete
      assertThat(event.<TestEntity> getEntity().text, is("E"));

      entityHook.onClose(db);
    }
  }

  @Test
  public void mergedEventRefersToLatestDocument() {
    TestEntity entity = new TestEntity();
    EntityEvent event;

    try (ODatabaseDocumentTx db = sendingDatabase.getInstance().acquire()) {
      entityHook.onOpen(db);
      entityAdapter.register(db);

      db.begin();
      entity.text = "before";
      ODocument originalRecord = entityAdapter.addEntity(db, entity);
      entity.text = "after";
      // perform write using fresh copy of document; original will become disconnected
      ODocument updatedRecord = entityAdapter.writeEntity(originalRecord.copy(), entity);
      db.commit();

      assertTrue(originalRecord.getIdentity().isTemporary()); // disconnected
      assertTrue(updatedRecord.getIdentity().isPersistent());

      assertThat(subscriber.events, hasSize(1));

      event = subscriber.events.get(0);
      assertThat(event.getClass().getSimpleName(), is("EntityCreatedEvent"));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(not(originalRecord)));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(updatedRecord));
      assertThat(event.<TestEntity> getEntity().text, is("after"));

      entityHook.onClose(db);
    }
  }

  @Test
  public void eventsAreFlushedOutsideOfTransaction() {
    TestEntity entity = new TestEntity();
    EntityEvent event;

    try (ODatabaseDocumentTx db = sendingDatabase.getInstance().acquire()) {
      entityHook.onOpen(db);
      entityAdapter.register(db);

      entity.text = "A";
      entityAdapter.addEntity(db, entity);
      entity.text = "B";
      entityAdapter.editEntity(db, entity);
      entity.text = "C";
      entityAdapter.editEntity(db, entity);

      entityHook.onClose(db);

      assertThat(subscriber.events, hasSize(1));

      event = subscriber.events.get(0);
      assertThat(event.getClass().getSimpleName(), is("EntityCreatedEvent"));
      assertThat(event.<TestEntity> getEntity().text, is("C"));
    }
  }
}
