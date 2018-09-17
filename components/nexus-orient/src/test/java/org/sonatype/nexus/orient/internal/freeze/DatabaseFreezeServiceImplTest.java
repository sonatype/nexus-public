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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.node.NodeMergedEvent;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.freeze.DatabaseFreezeChangeEvent;
import org.sonatype.nexus.orient.freeze.DatabaseFrozenStateManager;
import org.sonatype.nexus.orient.freeze.FreezeRequest;
import org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType;
import org.sonatype.nexus.orient.freeze.ReadOnlyState;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;
import org.sonatype.nexus.security.SecurityHelper;

import com.google.common.collect.Sets;
import com.orientechnologies.common.concur.lock.OModificationOperationProhibitedException;
import com.orientechnologies.common.util.OCallable;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
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
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DatabaseFreezeServiceImplTest
    extends TestSupport
{

  static final String DB_CLASS = "freeze-test";

  private static final String P_NAME = "name";

  private static final String INITIATOR_ID = "DatabaseFreezeServiceImplTest";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  EventManager eventManager;

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

  @Mock
  ApplicationDirectories applicationDirectories;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  SecurityHelper securityHelper;

  DatabaseFrozenStateManager databaseFrozenStateManager;

  DatabaseFreezeServiceImpl underTest;

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inFilesystem("test");

  @Rule
  public DatabaseInstanceRule database2 = DatabaseInstanceRule.inFilesystem("test-2");

  private Set<Provider<DatabaseInstance>> providerSet;

  @Before
  public void setup() throws Exception {
    File workDir = temporaryFolder.newFolder();
    when(applicationDirectories.getWorkDirectory("db")).thenReturn(workDir);

    databaseFrozenStateManager = new LocalDatabaseFrozenStateManager(applicationDirectories);

    when(distributedServerManager.getMessageService()).thenReturn(distributedMessageService);
    when(distributedMessageService.getDatabases()).thenReturn(Collections.singleton("test"));
    when(distributedServerManager.executeInDistributedDatabaseLock(eq("test"), anyLong(),
        any(OModifiableDistributedConfiguration.class),
        (OCallable<Object, OModifiableDistributedConfiguration>) notNull()))
        .then(invoc -> ((OCallable) invoc.getArguments()[3]).call(distributedConfiguration));
    when(distributedServerManager.getDatabaseConfiguration("test"))
        .thenReturn(distributedConfiguration);

    when(nodeAccess.isClustered()).thenReturn(false);

    providerSet = of(database.getInstanceProvider(), database2.getInstanceProvider()).collect(toSet());

    underTest = new DatabaseFreezeServiceImpl(
        providerSet,
        eventManager,
        databaseFrozenStateManager,
        () -> server,
        nodeAccess,
        securityHelper);

    when(server.getDistributedManager()).thenReturn(distributedServerManager);

    for (Provider<DatabaseInstance> provider : providerSet) {
      try (ODatabaseDocumentTx db = provider.get().connect()) {
        OSchema schema = db.getMetadata().getSchema();
        schema.createClass(DB_CLASS);
      }
    }

    underTest.doStart();
  }

  @Test
  public void testFreeze() {
    ArgumentCaptor<DatabaseFreezeChangeEvent> freezeChangeEventArgumentCaptor = forClass(
        DatabaseFreezeChangeEvent.class);

    FreezeRequest request = underTest.requestFreeze(InitiatorType.SYSTEM, INITIATOR_ID);
    verifyWriteFails(true);

    underTest.releaseRequest(request);
    verifyWriteFails(false);

    verify(eventManager, times(2)).post(freezeChangeEventArgumentCaptor.capture());

    List<DatabaseFreezeChangeEvent> databaseFreezeChangeEvents = freezeChangeEventArgumentCaptor.getAllValues();
    assertThat(databaseFreezeChangeEvents.get(0).isFrozen(), is(true));
    assertThat(databaseFreezeChangeEvents.get(1).isFrozen(), is(false));
  }

  /**
   * {@link DatabaseFreezeServiceImpl#requestFreeze(InitiatorType, String)} per contract must reject any additional
   * user initiated requests if one is open.
   */
  @Test
  public void testMultipleUserInitiatedFreezes() {
    FreezeRequest request = underTest.requestFreeze(InitiatorType.USER_INITIATED, "DatabaseFreezeServiceImplTest");
    verifyWriteFails(true);
    // attempt to stack a second user initiated will fail
    assertThat(underTest.requestFreeze(InitiatorType.USER_INITIATED, "DatabaseFreezeServiceImplTest"), equalTo(null));

    // still frozen
    verifyWriteFails(true);

    assertThat(underTest.releaseRequest(request), is(true));
    verifyWriteFails(false);

    // one event has frozen=true, one has frozen=false
    verify(eventManager, times(2)).post(Mockito.isA(DatabaseFreezeChangeEvent.class));
  }

  /**
   * {@link DatabaseFreezeServiceImpl#requestFreeze(InitiatorType, String)} per contract allows multiple system
   * initiated freeze requests to stack.
   * Freeze is not released until the last is released.
   */
  @Test
  public void testMultipleSystemInitiatedFreezes() {
    // system requests stack
    FreezeRequest request1 = underTest.requestFreeze(InitiatorType.SYSTEM,"DatabaseFreezeServiceImplTest");
    verifyWriteFails(true);

    FreezeRequest request2 =underTest.requestFreeze(InitiatorType.SYSTEM,"DatabaseFreezeServiceImplTest");
    verifyWriteFails(true);

    FreezeRequest request3 =underTest.requestFreeze(InitiatorType.SYSTEM,"DatabaseFreezeServiceImplTest");
    verifyWriteFails(true);

    assertThat(underTest.releaseRequest(request1), is(true));
    verifyWriteFails(true);

    assertThat(underTest.releaseRequest(request2), is(true));
    verifyWriteFails(true);

    // writes won't succeed until the state is empty
    assertThat(underTest.releaseRequest(request3), is(true));
    verifyWriteFails(false);

    // one event on initial freeze, one event on final release
    verify(eventManager, times(2)).post(Mockito.isA(DatabaseFreezeChangeEvent.class));
  }

  @Test
  public void testVerifyUnfrozen() {
    underTest.freezeLocalDatabases();
    try {
      underTest.checkUnfrozen("test");
      fail("Should have thrown OModificationProhibtedException");
    }
    catch (OModificationOperationProhibitedException e) {
      assertThat(e.getMessage(), is("test"));
    }
    underTest.releaseLocalDatabases();
    underTest.checkUnfrozen(); //should be no errors
  }

  /**
   * Replace the {@link DatabaseFrozenStateManager} with a mock to simulate a local node that may be out of sync
   * with the cluster.
   *
   * Control experiment: expect no changes when local state matches {@link DatabaseFrozenStateManager#getState()}.
   */
  @Test
  public void onNodeMergedNoChange() {
    DatabaseFrozenStateManager mockManager = mock(DatabaseFrozenStateManager.class);
    underTest = new DatabaseFreezeServiceImpl(Collections.singleton(() -> databaseInstance), eventManager,
        mockManager, () -> server, nodeAccess, securityHelper);

    // verify we are in writable state
    verifyWriteFails(false);

    // given: frozenStateManager reports empty state
    when(mockManager.getState()).thenReturn(Collections.emptyList());

    // when: a new node is added
    underTest.onNodeMerged(new NodeMergedEvent());

    // then: expect we can still write and we don't try to change server role or setFrozen
    verifyWriteFails(false);
    verifyNoMoreInteractions(distributedServerManager);
  }

  /**
   * Replace the {@link DatabaseFrozenStateManager} with a mock to simulate a local node that may be out of sync
   * with the cluster.
   *
   * Expect {@link DatabaseFreezeServiceImpl#freezeLocalDatabases()} when {@link DatabaseFrozenStateManager#getState()}
   * reports an open request and we aren't locally frozen.
   */
  @Test
  public void onNodeMergedStateRequiresFreeze() {
    DatabaseFrozenStateManager mockManager = mock(DatabaseFrozenStateManager.class);
    underTest = new DatabaseFreezeServiceImpl(providerSet, eventManager,
        mockManager, () -> server, nodeAccess, securityHelper);

    // verify we start in writable state
    verifyWriteFails(false);

    // given: frozenStateManager reports an active FreezeRequest
    when(mockManager.getState()).thenReturn(Arrays.asList(new FreezeRequest(InitiatorType.SYSTEM, INITIATOR_ID)));

    // when: a new node is added
    underTest.onNodeMerged(new NodeMergedEvent());

    // then: expect side effects of freezeLocalDatabases - can't write
    verifyWriteFails(true);
  }

  /**
   * Replace the {@link DatabaseFrozenStateManager} with a mock to simulate a local node that may be out of sync
   * with the cluster.
   *
   * Expect {@link DatabaseFreezeServiceImpl#releaseLocalDatabases()} when {@link DatabaseFrozenStateManager#getState()}
   * reports empty state and we are locally frozen.
   */
  @Test
  public void onNodeMergedStateRequiresRelease() {
    DatabaseFrozenStateManager mockManager = mock(DatabaseFrozenStateManager.class);
    underTest = new DatabaseFreezeServiceImpl(providerSet, eventManager,
        mockManager, () -> server, nodeAccess, securityHelper);

    underTest.freezeLocalDatabases();
    // verify we start in read-only state
    verifyWriteFails(true);

    // given: frozenStateManager reports empty state
    when(mockManager.getState()).thenReturn(Collections.emptyList());

    // when: a new node is added
    underTest.onNodeMerged(new NodeMergedEvent());

    // then: expect side effects of releaseLocalDatabases - we can write again
    verifyWriteFails(false);
  }

  @Test
  public void isFrozenChecksAllDatabases() {
    DatabaseInstance instance = mock(DatabaseInstance.class);
    underTest = new DatabaseFreezeServiceImpl(Sets.newHashSet(
        () -> instance,
        () -> instance,
        () -> instance,
        () -> instance
    ), eventManager, databaseFrozenStateManager, () -> server, nodeAccess, securityHelper);

    when(instance.isFrozen())
        .thenReturn(true, true, true, false);
    assertThat(underTest.isFrozen(), is(false));

    verify(instance, times(4)).isFrozen();
  }

  @Test
  public void isFrozenShortcircuit() {
    DatabaseInstance instance = mock(DatabaseInstance.class);
    underTest = new DatabaseFreezeServiceImpl(Sets.newHashSet(
        () -> instance,
        () -> instance,
        () -> instance,
        () -> instance
    ), eventManager, databaseFrozenStateManager, () -> server, nodeAccess, securityHelper);

    when(instance.isFrozen())
        .thenReturn(false, true, true, true);
    assertThat(underTest.isFrozen(), is(false));

    verify(instance, times(1)).isFrozen();
  }

  @Test
  public void getReadOnlyState() {
    // start in empty state
    verifyState(underTest.getReadOnlyState(), false, "", false);

    // user initiated freeze
    FreezeRequest request = underTest.requestFreeze(InitiatorType.USER_INITIATED, INITIATOR_ID);
    verifyState(underTest.getReadOnlyState(), true, "", false);
    // as authenticated person
    when(securityHelper.allPermitted(any())).thenReturn(true);
    verifyState(underTest.getReadOnlyState(), true,
        s -> s.contains("activated by an administrator"), false);
    when(securityHelper.allPermitted(any())).thenReturn(false);

    // back to empty
    underTest.releaseRequest(request);
    verifyState(underTest.getReadOnlyState(), false, "", false);

    // system initiated freeze
    underTest.requestFreeze(InitiatorType.SYSTEM, INITIATOR_ID);
    verifyState(underTest.getReadOnlyState(), true, "", true);
    when(securityHelper.allPermitted(any())).thenReturn(true);
    verifyState(underTest.getReadOnlyState(), true,
        s -> s.contains("activated by 1 running system"), true);
    when(securityHelper.allPermitted(any())).thenReturn(false);

    // stack a second system freeze
    underTest.requestFreeze(InitiatorType.SYSTEM, INITIATOR_ID);
    verifyState(underTest.getReadOnlyState(), true, "", true);
    when(securityHelper.allPermitted(any())).thenReturn(true);
    verifyState(underTest.getReadOnlyState(), true,
        s -> s.contains("activated by 2 running system"), true);
    when(securityHelper.allPermitted(any())).thenReturn(false);

    // cleanup
    underTest.releaseAllRequests();
    verifyState(underTest.getReadOnlyState(), false, "", false);

  }

  /**
   * @param state {@link ReadOnlyState} to check
   * @param frozen expected value for {@link ReadOnlyState#isFrozen()}
   * @param message expected exact value for {@link ReadOnlyState#getSummaryReason()}
   * @param system expected value for {@link ReadOnlyState#isSystemInitiated()}
   */
  void verifyState(ReadOnlyState state, boolean frozen, String message, boolean system) {
    verifyState(state, frozen, s -> s.equals(message), system);
  }

  /**
   * @param state {@link ReadOnlyState} to check
   * @param frozen expected value for {@link ReadOnlyState#isFrozen()}
   * @param stringPredicate test for {@link ReadOnlyState#getSummaryReason()}
   * @param system expected value for {@link ReadOnlyState#isSystemInitiated()}
   */
  void verifyState(ReadOnlyState state, boolean frozen, Predicate<String> stringPredicate, boolean system) {
    assertThat(state.isFrozen(), is(frozen));
    assertTrue(state.getSummaryReason() + " doesn't match expectation", stringPredicate.test(state.getSummaryReason()));
    assertThat(state.isSystemInitiated(), is(system));
  }

  /**
   * Utility method to check the status of the databases by attempting writes.
   * Note: this method only works in non-HA simulations in this test class.
   *
   * @param errorExpected if true, this method will still pass the test when a write fails, and fail if no error is
   *                      encountered. If false, it will fail a test if the write fails.
   */
  void verifyWriteFails(boolean errorExpected) {
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

  /**
   * When HA enabled, don't use {@link DatabaseInstance#setFrozen(boolean)}, use
   * {@link OModifiableDistributedConfiguration#setServerRole(String, ROLES)} instead.
   */
  @Test
  public void freezeAndReleaseHAUsesServerRole() {
    when(nodeAccess.isClustered()).thenReturn(true);

    FreezeRequest request = underTest.requestFreeze(InitiatorType.SYSTEM, INITIATOR_ID);
    verify(distributedConfiguration).setServerRole(DatabaseFreezeServiceImpl.SERVER_NAME, ROLES.REPLICA);
    verify(databaseInstance, never()).setFrozen(true);

    assertThat(underTest.releaseRequest(request), is(true));
    verify(distributedConfiguration).setServerRole(DatabaseFreezeServiceImpl.SERVER_NAME, ROLES.MASTER);
    verify(databaseInstance, never()).setFrozen(true);
  }

  @Test
  public void isFrozenHAUsesServerRole() {
    when(nodeAccess.isClustered()).thenReturn(true);
    when(distributedConfiguration.getServerRole(any())).thenReturn(ROLES.MASTER);

    assertThat(underTest.isFrozen(), is(false));

    when(distributedConfiguration.getServerRole(any())).thenReturn(ROLES.REPLICA);

    assertThat(underTest.isFrozen(), is(true));
  }

  @Test
  public void systemFreezeRequestDiscardedOnStartupWhenLocal() {
    underTest = new DatabaseFreezeServiceImpl(Collections.singleton(() -> databaseInstance), eventManager,
        databaseFrozenStateManager, () -> server, nodeAccess, securityHelper);

    when(nodeAccess.isClustered()).thenReturn(false);

    underTest.refreezeOnStartup(Arrays.asList(new FreezeRequest(InitiatorType.SYSTEM, INITIATOR_ID)));

    verify(databaseInstance, never()).setFrozen(true);
  }

  @Test
  public void userFreezeRequestAppliedOnStartupWhenLocal() {
    underTest = new DatabaseFreezeServiceImpl(Collections.singleton(() -> databaseInstance), eventManager,
        databaseFrozenStateManager, () -> server, nodeAccess, securityHelper);

    when(nodeAccess.isClustered()).thenReturn(false);

    underTest.refreezeOnStartup(Arrays.asList(new FreezeRequest(InitiatorType.USER_INITIATED, INITIATOR_ID)));

    verify(databaseInstance).setFrozen(true);
  }

  @Test
  public void systemFreezeRequestsAppliedWhenHA() {
    when(nodeAccess.isClustered()).thenReturn(true);

    underTest.refreezeOnStartup(Arrays.asList(new FreezeRequest(InitiatorType.SYSTEM, INITIATOR_ID)));

    verify(distributedConfiguration).setServerRole(DatabaseFreezeServiceImpl.SERVER_NAME, ROLES.REPLICA);
  }

  @Test
  public void userFreezeRequestsAppliedWhenHA() {
    when(nodeAccess.isClustered()).thenReturn(true);

    underTest.refreezeOnStartup(Arrays.asList(new FreezeRequest(InitiatorType.USER_INITIATED, INITIATOR_ID)));

    verify(distributedConfiguration).setServerRole(DatabaseFreezeServiceImpl.SERVER_NAME, ROLES.REPLICA);
  }
}
