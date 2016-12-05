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
import org.sonatype.nexus.common.entity.Entity;
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
import com.orientechnologies.orient.core.record.impl.ODocument;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
      extends Entity
  {
    // nothing to add
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
      // nothing to add
    }

    @Override
    protected void writeFields(ODocument document, TestEntity entity) throws Exception {
      document.setDirty(); // nothing to add, but need to mark it as dirty to force update
    }

    @Override
    protected void readFields(ODocument document, TestEntity entity) throws Exception {
      // nothing to add
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
      ODocument firstEntity = entityAdapter.addEntity(db, new TestEntity());
      ODocument secondEntity = entityAdapter.addEntity(db, new TestEntity());
      db.commit();

      assertThat(subscriber.events, hasSize(2));

      event = subscriber.events.get(0);
      assertThat(event.getClass().getSimpleName(), is("EntityCreatedEvent"));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(firstEntity));
      assertTrue(event.isLocal());
      assertThat(event.getRemoteNodeId(), is(nullValue()));

      event = subscriber.events.get(1);
      assertThat(event.getClass().getSimpleName(), is("EntityCreatedEvent"));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(secondEntity));
      assertTrue(event.isLocal());
      assertThat(event.getRemoteNodeId(), is(nullValue()));

      // UPDATE
      EntityHook.asRemote("REMOTE-NODE", () -> {
        db.begin();
        entityAdapter.writeEntity(firstEntity, new TestEntity());
        entityAdapter.writeEntity(secondEntity, new TestEntity());
        db.commit();
      });

      assertThat(subscriber.events, hasSize(4));

      event = subscriber.events.get(2);
      assertThat(event.getClass().getSimpleName(), is("EntityUpdatedEvent"));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(firstEntity));
      assertFalse(event.isLocal());
      assertThat(event.getRemoteNodeId(), is("REMOTE-NODE"));

      event = subscriber.events.get(3);
      assertThat(event.getClass().getSimpleName(), is("EntityUpdatedEvent"));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(secondEntity));
      assertFalse(event.isLocal());
      assertThat(event.getRemoteNodeId(), is("REMOTE-NODE"));

      // DELETE
      db.begin();
      entityAdapter.deleteEntity(db, entityAdapter.readEntity(firstEntity));
      entityAdapter.deleteEntity(db, entityAdapter.readEntity(secondEntity));
      db.commit();

      assertThat(subscriber.events, hasSize(6));

      event = subscriber.events.get(4);
      assertThat(event.getClass().getSimpleName(), is("EntityDeletedEvent"));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(firstEntity));
      assertTrue(event.isLocal());
      assertThat(event.getRemoteNodeId(), is(nullValue()));

      event = subscriber.events.get(5);
      assertThat(event.getClass().getSimpleName(), is("EntityDeletedEvent"));
      assertThat(entityAdapter.recordIdentity(event.getId()), is(secondEntity));
      assertTrue(event.isLocal());
      assertThat(event.getRemoteNodeId(), is(nullValue()));

      entityHook.onClose(db);

      // make sure we're back to just the default listeners/hooks
      assertThat(ImmutableList.copyOf(db.getListeners()), is(listeners));
      assertThat(ImmutableList.copyOf(db.getHooks().keySet()), is(hooks));
    }
  }
}
