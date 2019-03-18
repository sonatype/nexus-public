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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.upgrade.Checkpoint;
import org.sonatype.nexus.common.upgrade.Upgrade;
import org.sonatype.nexus.upgrade.UpgradeService;
import org.sonatype.nexus.upgrade.example.CheckpointBar;
import org.sonatype.nexus.upgrade.example.CheckpointFoo;
import org.sonatype.nexus.upgrade.example.CheckpointMock;
import org.sonatype.nexus.upgrade.example.CheckpointWibble;
import org.sonatype.nexus.upgrade.example.UpgradeBar_1_1;
import org.sonatype.nexus.upgrade.example.UpgradeFoo_1_1;
import org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2;
import org.sonatype.nexus.upgrade.example.UpgradeMock;
import org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UpgradeServiceTest
    extends TestSupport
{

  @Mock
  private ModelVersionStore modelVersionStore;

  @Mock
  private NodeAccess nodeAccess;

  private Checkpoint checkpointFoo;

  private Checkpoint checkpointBar;

  private Checkpoint checkpointWibble;

  private Upgrade upgradeFoo_1_1;

  private Upgrade upgradeFoo_1_2;

  private Upgrade upgradeBar_1_1;

  private Upgrade upgradeWibble_2_0;

  private UpgradeService upgradeService;

  @Before
  public void setupService() throws Exception {

    CheckpointMock[] checkpoints = {
        new CheckpointFoo(),
        new CheckpointBar(),
        new CheckpointWibble()
    };

    checkpointFoo = checkpoints[0].mock;
    checkpointBar = checkpoints[1].mock;
    checkpointWibble = checkpoints[2].mock;

    UpgradeMock upgrades[] = {
        new UpgradeFoo_1_1(),
        new UpgradeFoo_1_2(),
        new UpgradeBar_1_1(),
        new UpgradeWibble_2_0()
    };

    upgradeFoo_1_1 = upgrades[0].mock;
    upgradeFoo_1_2 = upgrades[1].mock;
    upgradeBar_1_1 = upgrades[2].mock;
    upgradeWibble_2_0 = upgrades[3].mock;

    UpgradeManager upgradeManager = new UpgradeManager(asList(checkpoints), asList(upgrades), false);
    upgradeService = new UpgradeServiceImpl(upgradeManager, modelVersionStore, nodeAccess);

    // defaults to testing non-clustered existing instance
    when(modelVersionStore.isNewInstance()).thenReturn(false);
    when(nodeAccess.isClustered()).thenReturn(false);
    when(nodeAccess.isOldestNode()).thenReturn(true);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Map<String, String> verifyModelVersionsSaved() {
    ArgumentCaptor<Map> modelVersionsCaptor = ArgumentCaptor.forClass(Map.class);
    verify(modelVersionStore).save(modelVersionsCaptor.capture());
    return modelVersionsCaptor.getValue();
  }

  @Test
  public void testManagesLifecycleOfVersionStore() throws Exception {
    UpgradeManager upgradeManager = new UpgradeManager(asList(), asList(), false);
    upgradeService = new UpgradeServiceImpl(upgradeManager, modelVersionStore, nodeAccess);

    upgradeService.start();
    verify(modelVersionStore).start();

    upgradeService.stop();
    verify(modelVersionStore).stop();
  }

  @Test
  public void testNoUpgradesDoesNothing() throws Exception {
    UpgradeManager upgradeManager = new UpgradeManager(asList(), asList(), false);
    upgradeService = new UpgradeServiceImpl(upgradeManager, modelVersionStore, nodeAccess);

    upgradeService.start();

    verifyNoMoreInteractions(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);
  }

  @Test
  public void testInventoryTakenForFreshInstallation() throws Exception {
    when(modelVersionStore.isNewInstance()).thenReturn(true);

    upgradeService.start();

    assertThat(verifyModelVersionsSaved(),
        is(ImmutableMap.of("foo", "1.2", "bar", "1.1", "wibble", "2.0")));

    verifyNoMoreInteractions(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);
  }

  @Test
  public void testInventoryTakenForFreshCluster() throws Exception {
    when(modelVersionStore.isNewInstance()).thenReturn(true);
    when(nodeAccess.isClustered()).thenReturn(true);

    upgradeService.start();

    assertThat(verifyModelVersionsSaved(),
        is(ImmutableMap.of("foo", "1.2", "bar", "1.1", "wibble", "2.0")));

    verifyNoMoreInteractions(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);
  }

  @Test
  public void testLocalInventoryTakenForFreshNodeJoiningExistingCluster() throws Exception {
    when(modelVersionStore.isNewInstance()).thenReturn(true);
    when(nodeAccess.isClustered()).thenReturn(true);
    when(nodeAccess.isOldestNode()).thenReturn(false);

    upgradeService.start();

    assertThat(verifyModelVersionsSaved(),
        is(ImmutableMap.of("foo", "1.2"))); // only foo is a local model

    verifyNoMoreInteractions(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);
  }

  @Test
  public void testUpgradeExistingInstallation() throws Exception {

    when(modelVersionStore.load()).thenReturn(new HashMap<>(ImmutableMap.of("foo", "1.1", "bar", "1.1")));

    upgradeService.start();

    assertThat(verifyModelVersionsSaved(),
        is(ImmutableMap.of("foo", "1.2", "bar", "1.1", "wibble", "2.0")));

    InOrder order = inOrder(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);

    order.verify(checkpointFoo).begin("1.1");
    order.verify(checkpointWibble).begin("1.0");

    order.verify(upgradeFoo_1_2).apply();
    order.verify(upgradeWibble_2_0).apply();

    order.verify(checkpointFoo).commit();
    order.verify(checkpointWibble).commit();

    order.verify(checkpointFoo).end();
    order.verify(checkpointWibble).end();

    verifyNoMoreInteractions(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);
  }

  @Test
  public void testOnlyLocalUpgradesForNodeJoiningExistingCluster() throws Exception {
    when(nodeAccess.isClustered()).thenReturn(true);
    when(nodeAccess.isOldestNode()).thenReturn(false);

    when(modelVersionStore.load()).thenReturn(new HashMap<>(ImmutableMap.of("foo", "1.1")));

    upgradeService.start();

    assertThat(verifyModelVersionsSaved(), is(ImmutableMap.of("foo", "1.2")));

    InOrder order = inOrder(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);

    order.verify(checkpointFoo).begin("1.1");

    order.verify(upgradeFoo_1_2).apply();

    order.verify(checkpointFoo).commit();

    order.verify(checkpointFoo).end();

    verifyNoMoreInteractions(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);
  }

  @Test
  public void testUpgradeOnlyAppliedOnce() throws Exception {

    upgradeService.start();

    Map<String, String> upgradedVersions = ImmutableMap.of("foo", "1.2", "bar", "1.1", "wibble", "2.0");
    assertThat(verifyModelVersionsSaved(), is(upgradedVersions));

    upgradeService.stop();
    when(modelVersionStore.load()).thenReturn(new HashMap<>(upgradedVersions));
    upgradeService.start();

    upgradeService.stop();
    upgradeService.start();

    InOrder order = inOrder(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);

    order.verify(checkpointFoo).begin("1.0");
    order.verify(checkpointBar).begin("1.0");
    order.verify(checkpointWibble).begin("1.0");

    order.verify(upgradeFoo_1_1).apply();
    order.verify(upgradeBar_1_1).apply();
    order.verify(upgradeFoo_1_2).apply();
    order.verify(upgradeWibble_2_0).apply();

    order.verify(checkpointFoo).commit();
    order.verify(checkpointBar).commit();
    order.verify(checkpointWibble).commit();

    order.verify(checkpointFoo).end();
    order.verify(checkpointBar).end();
    order.verify(checkpointWibble).end();

    verifyNoMoreInteractions(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);
  }

  @Test
  public void testBadUpgradeRollsBack() throws Exception {

    doThrow(new IOException()).when(upgradeBar_1_1).apply();

    try {
      upgradeService.start();
      fail("Expected IOException");
    }
    catch (Exception e) {
      assertThat(e, instanceOf(IOException.class));
    }

    verify(modelVersionStore, never()).save(any());

    InOrder order = inOrder(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);

    order.verify(checkpointFoo).begin("1.0");
    order.verify(checkpointBar).begin("1.0");
    order.verify(checkpointWibble).begin("1.0");

    order.verify(upgradeFoo_1_1).apply();
    order.verify(upgradeBar_1_1).apply();

    order.verify(checkpointFoo).rollback();
    order.verify(checkpointBar).rollback();
    order.verify(checkpointWibble).rollback();

    verifyNoMoreInteractions(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);
  }

  @Test
  public void testBadCheckpointStopsUpgrade() throws Exception {

    doThrow(new IOException()).when(checkpointBar).begin(anyString());

    try {
      upgradeService.start();
      fail("Expected IOException");
    }
    catch (Exception e) {
      assertThat(e, instanceOf(IOException.class));
    }

    verify(modelVersionStore, never()).save(any());

    InOrder order = inOrder(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);

    order.verify(checkpointFoo).begin("1.0");
    order.verify(checkpointBar).begin("1.0");

    verifyNoMoreInteractions(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);
  }

  @Test
  public void testBadCommitRollsBack() throws Exception {

    doThrow(new IOException()).when(checkpointBar).commit();

    try {
      upgradeService.start();
      fail("Expected IOException");
    }
    catch (Exception e) {
      assertThat(e, instanceOf(IOException.class));
    }

    verify(modelVersionStore, never()).save(any());

    InOrder order = inOrder(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);

    order.verify(checkpointFoo).begin("1.0");
    order.verify(checkpointBar).begin("1.0");
    order.verify(checkpointWibble).begin("1.0");

    order.verify(upgradeFoo_1_1).apply();
    order.verify(upgradeBar_1_1).apply();
    order.verify(upgradeFoo_1_2).apply();
    order.verify(upgradeWibble_2_0).apply();

    order.verify(checkpointFoo).commit();
    order.verify(checkpointBar).commit();

    order.verify(checkpointFoo).rollback();
    order.verify(checkpointBar).rollback();
    order.verify(checkpointWibble).rollback();

    verifyNoMoreInteractions(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);
  }

  @Test
  public void testRollbackKeepsGoingOnError() throws Exception {

    doThrow(new IOException()).when(checkpointBar).commit();
    doThrow(new IOException()).when(checkpointBar).rollback();

    try {
      upgradeService.start();
      fail("Expected IOException");
    }
    catch (Exception e) {
      assertThat(e, instanceOf(IOException.class));
    }

    verify(modelVersionStore, never()).save(any());

    InOrder order = inOrder(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);

    order.verify(checkpointFoo).begin("1.0");
    order.verify(checkpointBar).begin("1.0");
    order.verify(checkpointWibble).begin("1.0");

    order.verify(upgradeFoo_1_1).apply();
    order.verify(upgradeBar_1_1).apply();
    order.verify(upgradeFoo_1_2).apply();
    order.verify(upgradeWibble_2_0).apply();

    order.verify(checkpointFoo).commit();
    order.verify(checkpointBar).commit();

    order.verify(checkpointFoo).rollback();
    order.verify(checkpointBar).rollback();
    order.verify(checkpointWibble).rollback();

    verifyNoMoreInteractions(
        checkpointFoo,
        checkpointBar,
        checkpointWibble,
        upgradeFoo_1_1,
        upgradeFoo_1_2,
        upgradeBar_1_1,
        upgradeWibble_2_0);
  }

}
