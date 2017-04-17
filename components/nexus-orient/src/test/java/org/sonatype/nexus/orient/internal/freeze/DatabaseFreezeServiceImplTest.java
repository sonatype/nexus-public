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
package org.sonatype.nexus.orient.internal.freeze;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeMergedEvent;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeChangeEvent;
import org.sonatype.nexus.orient.freeze.DatabaseFrozenStateManager;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.orientechnologies.common.concur.lock.OModificationOperationProhibitedException;
import com.orientechnologies.common.util.OCallable;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.distributed.ODistributedConfiguration.ROLES;
import com.orientechnologies.orient.server.distributed.ODistributedMessageService;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;
import com.orientechnologies.orient.server.distributed.OModifiableDistributedConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DatabaseFreezeServiceImplTest
    extends TestSupport
{
  static final String DB_CLASS = "freeze-test";

  private static final String P_NAME = "name";

  @Mock
  EventManager eventManager;

  @Mock
  DatabaseFrozenStateManager databaseFrozenStateManager;

  @Mock
  OServer server;

  @Mock
  ODistributedServerManager distributedServerManager;

  @Mock
  ODistributedMessageService distributedMessageService;

  @Mock
  OModifiableDistributedConfiguration distributedConfiguration;

  @Mock
  DatabaseInstance databaseInstance;

  DatabaseFreezeServiceImpl underTest;

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inFilesystem("test");

  @Rule
  public DatabaseInstanceRule database2 = DatabaseInstanceRule.inFilesystem("test-2");

  private Set<Provider<DatabaseInstance>> providerSet;

  @Before
  public void setup() throws Exception {
    when(distributedServerManager.getMessageService()).thenReturn(distributedMessageService);
    when(distributedMessageService.getDatabases()).thenReturn(Collections.singleton("test"));
    when(distributedServerManager.executeInDistributedDatabaseLock(eq("test"), anyLong(),
        any(OModifiableDistributedConfiguration.class),
        (OCallable<Object, OModifiableDistributedConfiguration>) notNull()))
            .then(invoc -> ((OCallable) invoc.getArguments()[3]).call(distributedConfiguration));
    providerSet = of(database.getInstanceProvider(), database2.getInstanceProvider()).collect(toSet());
    underTest = new DatabaseFreezeServiceImpl(providerSet, eventManager, databaseFrozenStateManager, () -> server);

    for (Provider<DatabaseInstance> provider : providerSet) {
      try (ODatabaseDocumentTx db = provider.get().connect()) {
        OSchema schema = db.getMetadata().getSchema();
        OClass type = schema.createClass(DB_CLASS);
      }
    }

    underTest.doStart();
  }

  @Test
  public void testFreeze() {

    ArgumentCaptor<DatabaseFreezeChangeEvent> freezeChangeEventArgumentCaptor = forClass(
        DatabaseFreezeChangeEvent.class);

    underTest.freezeAllDatabases();
    verifyWrite(true);
    verify(databaseFrozenStateManager).set(true);

    underTest.releaseAllDatabases();
    verifyWrite(false);
    verify(databaseFrozenStateManager).set(false);

    verify(eventManager, times(2)).post(freezeChangeEventArgumentCaptor.capture());

    List<DatabaseFreezeChangeEvent> databaseFreezeChangeEvents = freezeChangeEventArgumentCaptor.getAllValues();
    assertThat(databaseFreezeChangeEvents.get(0).isFrozen(), is(true));
    assertThat(databaseFreezeChangeEvents.get(1).isFrozen(), is(false));
  }

  @Test
  public void testMultipleFreezes() {
    //Multiple freezes should only require one release
    underTest.freezeAllDatabases();
    underTest.freezeAllDatabases();
    verifyWrite(true);

    underTest.releaseAllDatabases();
    verifyWrite(false);

    verify(eventManager, times(2)).post(Mockito.isA(DatabaseFreezeChangeEvent.class));
  }

  @Test
  public void testMultipleReleases() {
    underTest.freezeAllDatabases();
    verifyWrite(true);

    //multiple releases should only require one freeze to lock back up
    underTest.releaseAllDatabases();
    underTest.releaseAllDatabases();
    verifyWrite(false);

    underTest.freezeAllDatabases();
    verifyWrite(true);
    verify(eventManager, times(3)).post(Mockito.isA(DatabaseFreezeChangeEvent.class));
  }

  @Test
  public void testVerifyUnfrozen() {
    underTest.freezeAllDatabases();
    try {
      underTest.checkUnfrozen("test");
      fail("Should have thrown OModificationProhibtedException");
    }
    catch (OModificationOperationProhibitedException e) {
      assertThat(e.getMessage(), is("test"));
    }
    underTest.releaseAllDatabases();
    underTest.checkUnfrozen(); //should be no errors
  }

  @Test
  public void testDatabasesNotFrozenWhenFreezeEventsPosted() {
    doAnswer(invocation -> {
      verifyWrite(false);
      return null;
    }).when(eventManager).post(any(DatabaseFreezeChangeEvent.class));

    underTest.freezeAllDatabases();
    underTest.releaseAllDatabases();
  }


  @Test
  public void testOnNodeAddedShouldFreeze() {
    verifyWrite(false);
    when(databaseFrozenStateManager.get()).thenReturn(true);
    underTest.onNodeMerged(new NodeMergedEvent());
    verifyWrite(true);
    verify(eventManager).post(Mockito.isA(DatabaseFreezeChangeEvent.class));
  }

  @Test
  public void testOnNodeAddedShouldRelease() {
    underTest.freezeAllDatabases();
    verifyWrite(true);
    when(databaseFrozenStateManager.get()).thenReturn(false);
    underTest.onNodeMerged(new NodeMergedEvent());
    verifyWrite(false);
    verify(eventManager, times(2)).post(Mockito.isA(DatabaseFreezeChangeEvent.class));
  }

  @Test
  public void testOnNodeAddedNoChangeReleased() {
    verifyWrite(false);
    when(databaseFrozenStateManager.get()).thenReturn(false);
    underTest.onNodeMerged(new NodeMergedEvent());
    verifyWrite(false);
    verifyNoMoreInteractions(eventManager);
  }

  @Test
  public void testOnNodeAddedNoChangeFrozen() {
    underTest.freezeAllDatabases();
    verifyWrite(true);
    when(databaseFrozenStateManager.get()).thenReturn(true);
    underTest.onNodeMerged(new NodeMergedEvent());
    verifyWrite(true);
    verify(eventManager).post(Mockito.isA(DatabaseFreezeChangeEvent.class));
  }

  /**
   * Check the status of the databases by attempting writes.
   *
   * @param errorExpected if true, this method will still pass the test when a write fails, and fail if no error is
   *                      encountered. If false, it will fail a test if the write fails.
   */
  void verifyWrite(boolean errorExpected) {
    for (Provider<DatabaseInstance> provider : providerSet) {
      try (ODatabaseDocumentTx db = provider.get().connect()) {
        db.begin();

        ODocument document = db.newInstance(DB_CLASS);
        document.field(P_NAME, "test");
        document.save();

        try {
          db.commit();
          if (errorExpected) {
            fail("Expected OModificationOperationProhibitedException");
          }
        }
        catch (OModificationOperationProhibitedException e) {
          if (!errorExpected) {
            throw e;
          }
        }
      }
    }
  }

  @Test
  public void testSwitchToReplicaModeAfterFreeze() {
    underTest = new DatabaseFreezeServiceImpl(Collections.singleton(() -> databaseInstance), eventManager,
        databaseFrozenStateManager, () -> server);
    when(server.getDistributedManager()).thenReturn(distributedServerManager);
    underTest.freezeAllDatabases();
    InOrder order = inOrder(databaseInstance, distributedConfiguration);
    order.verify(databaseInstance).setFrozen(true);
    order.verify(distributedConfiguration).setServerRole("*", ROLES.REPLICA);
  }

  @Test
  public void testSwitchToMasterModeBeforeRelease() {
    underTest = new DatabaseFreezeServiceImpl(Collections.singleton(() -> databaseInstance), eventManager,
        databaseFrozenStateManager, () -> server);
    when(server.getDistributedManager()).thenReturn(distributedServerManager);
    underTest.freezeAllDatabases();
    underTest.releaseAllDatabases();
    InOrder order = inOrder(databaseInstance, distributedConfiguration);
    order.verify(distributedConfiguration).setServerRole("*", ROLES.MASTER);
    order.verify(databaseInstance).setFrozen(false);
  }
}
