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

import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.upgrade.Checkpoint;
import org.sonatype.nexus.common.upgrade.Upgrade;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.upgrade.plan.DependencyResolver.CyclicDependencyException;
import org.sonatype.nexus.upgrade.plan.DependencyResolver.UnresolvedDependencyException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.util.Providers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.lang.System.lineSeparator;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class UpgradeManagerTest
    extends TestSupport
{
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testUpgradeOrdering() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo(),
        new org.sonatype.nexus.upgrade.example.CheckpointBar(),
        new org.sonatype.nexus.upgrade.example.CheckpointWibble()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.UpgradeBar_1_1(),
        new org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_1()
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    Map<String, String> modelVersions = ImmutableMap.of();

    List<Upgrade> plan = upgradeManager.selectUpgrades(modelVersions, false);

    assertThat(plan, contains(
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeFoo_1_1.class),
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeBar_1_1.class),
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2.class),
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0.class)
    ));

    List<Checkpoint> prepare = upgradeManager.selectCheckpoints(plan);

    assertThat(prepare, containsInAnyOrder(
        instanceOf(org.sonatype.nexus.upgrade.example.CheckpointFoo.class),
        instanceOf(org.sonatype.nexus.upgrade.example.CheckpointBar.class),
        instanceOf(org.sonatype.nexus.upgrade.example.CheckpointWibble.class)
    ));
  }

  @Test
  public void testUpgradeAfterSingleUpgrade() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo(),
        new org.sonatype.nexus.upgrade.example.CheckpointBar(),
        new org.sonatype.nexus.upgrade.example.CheckpointWibble()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.UpgradeBar_1_1(),
        new org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_1()
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    Map<String, String> modelVersions = ImmutableMap.of("foo", "1.1");

    List<Upgrade> plan = upgradeManager.selectUpgrades(modelVersions, false);

    assertThat(plan, contains(
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeBar_1_1.class),
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2.class),
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0.class)
    ));

    List<Checkpoint> prepare = upgradeManager.selectCheckpoints(plan);

    assertThat(prepare, containsInAnyOrder(
        instanceOf(org.sonatype.nexus.upgrade.example.CheckpointFoo.class),
        instanceOf(org.sonatype.nexus.upgrade.example.CheckpointBar.class),
        instanceOf(org.sonatype.nexus.upgrade.example.CheckpointWibble.class)
    ));
  }

  @Test
  public void testUpgradeAfterSeveralUpgrades() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo(),
        new org.sonatype.nexus.upgrade.example.CheckpointBar(),
        new org.sonatype.nexus.upgrade.example.CheckpointWibble()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.UpgradeBar_1_1(),
        new org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_1()
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    Map<String, String> modelVersions = ImmutableMap.of("foo", "1.1", "bar", "1.1");

    List<Upgrade> plan = upgradeManager.selectUpgrades(modelVersions, false);

    assertThat(plan, containsInAnyOrder(
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2.class),
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0.class)
    ));

    List<Checkpoint> prepare = upgradeManager.selectCheckpoints(plan);

    assertThat(prepare, containsInAnyOrder(
        instanceOf(org.sonatype.nexus.upgrade.example.CheckpointFoo.class),
        instanceOf(org.sonatype.nexus.upgrade.example.CheckpointWibble.class)
    ));
  }

  @Test
  public void testUpgradeAfterAllUpgrades() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo(),
        new org.sonatype.nexus.upgrade.example.CheckpointBar(),
        new org.sonatype.nexus.upgrade.example.CheckpointWibble()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.UpgradeBar_1_1(),
        new org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_1()
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    Map<String, String> modelVersions = ImmutableMap.of("foo", "1.3", "bar", "1.2", "wibble", "2.1", "qux", "1.7");

    List<Upgrade> plan = upgradeManager.selectUpgrades(modelVersions, false);

    assertThat(plan, is(empty()));

    List<Checkpoint> prepare = upgradeManager.selectCheckpoints(plan);

    assertThat(prepare, is(empty()));
  }

  @Test
  public void testBadVersionIsDetected() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.bad.UpgradeFoo_1_1(),
        new org.sonatype.nexus.upgrade.bad.UpgradeFoo_1_2()
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Found 2 problem(s) with upgrades:" + lineSeparator()
        + "Upgrade step org.sonatype.nexus.upgrade.bad.UpgradeFoo_1_1 "
        + "has invalid version: 1.1 is not after 1.1" + lineSeparator()
        + "Upgrade step org.sonatype.nexus.upgrade.bad.UpgradeFoo_1_2 "
        + "has invalid version: 1.1 is not after 1.2");
    upgradeManager.selectUpgrades(ImmutableMap.of(), false);
  }

  @Test
  public void testUpgradeCycleIsDetected() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo(),
        new org.sonatype.nexus.upgrade.example.CheckpointBar(),
        new org.sonatype.nexus.upgrade.example.CheckpointWibble()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.cycle.UpgradeFoo_1_1(),
        new org.sonatype.nexus.upgrade.cycle.UpgradeBar_1_1(),
        new org.sonatype.nexus.upgrade.cycle.UpgradeWibble_1_1()
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    thrown.expect(CyclicDependencyException.class);
    upgradeManager.selectUpgrades(ImmutableMap.of(), false);
  }

  @Test
  public void testUpgradeGapIsDetected() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.gap.UpgradeFoo_1_1(),
        new org.sonatype.nexus.upgrade.gap.UpgradeFoo_1_3()
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    thrown.expect(UnresolvedDependencyException.class);
    upgradeManager.selectUpgrades(ImmutableMap.of(), false);
  }

  @Test
  public void testUpgradeGapIsDetectedAndLogged() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.gap.UpgradeFoo_1_1(),
        new org.sonatype.nexus.upgrade.gap.UpgradeFoo_1_3()
    );

    UpgradeManager upgradeManager = createUpgradeManagerWithWarnings(checkpoints, upgrades);

    List<Upgrade> plan = upgradeManager.selectUpgrades(ImmutableMap.of(), false);

    assertThat(plan, contains(
        instanceOf(org.sonatype.nexus.upgrade.gap.UpgradeFoo_1_1.class),
        instanceOf(org.sonatype.nexus.upgrade.gap.UpgradeFoo_1_3.class)
    ));
  }

  @Test
  public void testGetLocalModels() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo(),
        new org.sonatype.nexus.upgrade.example.CheckpointBar(),
        new org.sonatype.nexus.upgrade.example.CheckpointWibble()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.UpgradeBar_1_1(),
        new org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_1()
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    assertThat(upgradeManager.getLocalModels(), containsInAnyOrder("foo"));
  }

  @Test
  public void testGetClusteredModels() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo(),
        new org.sonatype.nexus.upgrade.example.CheckpointBar(),
        new org.sonatype.nexus.upgrade.example.CheckpointWibble()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.UpgradeBar_1_1(),
        new org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_1()
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    assertThat(upgradeManager.getClusteredModels(), containsInAnyOrder("bar", "wibble"));
  }

  @Test
  public void testLatestKnownModelVersions() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo(),
        new org.sonatype.nexus.upgrade.example.CheckpointBar(),
        new org.sonatype.nexus.upgrade.example.CheckpointWibble()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.UpgradeBar_1_1(),
        new org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_1()
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    assertThat(upgradeManager.latestKnownModelVersions(),
        equalTo(ImmutableMap.of("bar", "1.1", "wibble", "2.0", "foo", "1.2")));
  }

  @Test
  public void testLatestKnownModelVersionsWithSingleInheritance() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo(),
        new org.sonatype.nexus.upgrade.example.CheckpointBar(),
        new org.sonatype.nexus.upgrade.example.CheckpointWibble()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.UpgradeBar_1_1(),
        new org.sonatype.nexus.upgrade.example.UpgradeExtendsWibble_2_0(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_1()
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    assertThat(upgradeManager.latestKnownModelVersions(),
        equalTo(ImmutableMap.of("bar", "1.1", "wibble", "2.0", "foo", "1.2")));
  }

  @Test
  public void testLatestKnownModelVersionsWithDoubleInheritance() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo(),
        new org.sonatype.nexus.upgrade.example.CheckpointBar(),
        new org.sonatype.nexus.upgrade.example.CheckpointWibble()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.UpgradeBar_1_1(),
        new org.sonatype.nexus.upgrade.example.UpgradeExtendsExtendsWibble_2_0(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2(),
        new org.sonatype.nexus.upgrade.example.UpgradeFoo_1_1()
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    assertThat(upgradeManager.latestKnownModelVersions(),
        equalTo(ImmutableMap.of("bar", "1.1", "wibble", "2.0", "foo", "1.2")));
  }

  @Test
  public void testDuplicateCheckpointsAreIllegal() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.duplicate.CheckpointFoo(),
        new org.sonatype.nexus.upgrade.duplicate.CheckpointFoo_Duplicate()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.duplicate.UpgradeFoo_1_1()
    );

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Found 1 problem(s) with upgrades:" + lineSeparator()
        + "Checkpoint of model: foo duplicated by classes: "
        + "org.sonatype.nexus.upgrade.duplicate.CheckpointFoo, "
        + "org.sonatype.nexus.upgrade.duplicate.CheckpointFoo_Duplicate");

    createUpgradeManager(checkpoints, upgrades);
  }

  @Test
  public void testDuplicateUpgradesAreIllegal() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.duplicate.UpgradeFoo_1_1(),
        new org.sonatype.nexus.upgrade.duplicate.UpgradeFoo_1_2(),
        new org.sonatype.nexus.upgrade.duplicate.UpgradeFoo_1_2_Duplicate(),
        new org.sonatype.nexus.upgrade.duplicate.UpgradeFoo_1_3()
    );

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Found 1 problem(s) with upgrades:" + lineSeparator()
        + "Upgrade of model: foo from: 1.1 to: 1.2 duplicated by classes: "
        + "org.sonatype.nexus.upgrade.duplicate.UpgradeFoo_1_2, "
        + "org.sonatype.nexus.upgrade.duplicate.UpgradeFoo_1_2_Duplicate");

    createUpgradeManager(checkpoints, upgrades);
  }

  @Test
  public void testPrivateUpgrades() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.UpgradePrivateModel_1_1(Providers.of(mock(DatabaseInstance.class)))
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    List<Upgrade> plan = upgradeManager.selectUpgrades(ImmutableMap.of(), false);

    assertThat(plan, contains(
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradePrivateModel_1_1.class)
    ));
  }

  @Test
  public void testPrivateUpgradesWithMissingCheckpointDependenciesAreIllegal() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.bad.UpgradePrivateModel_1_2(Providers.of(mock(DatabaseInstance.class)))
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Found 2 problem(s) with upgrades:" + lineSeparator()
        + "Upgrade step org.sonatype.nexus.upgrade.bad.UpgradePrivateModel_1_2 "
        + "has undeclared model dependencies: [foo]" + lineSeparator()
        + "Upgrade step org.sonatype.nexus.upgrade.bad.UpgradePrivateModel_1_2 "
        + "does not trigger a checkpoint");

    upgradeManager.selectUpgrades(ImmutableMap.of(), false);
  }

  @Test
  public void testPrivateUpgradesWithoutAnyCheckpointsAreIllegal() {

    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo()
    );

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.bad.UpgradePrivateModel_1_3(Providers.of(mock(DatabaseInstance.class)))
    );

    UpgradeManager upgradeManager = createUpgradeManager(checkpoints, upgrades);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Found 1 problem(s) with upgrades:" + lineSeparator()
        + "Upgrade step org.sonatype.nexus.upgrade.bad.UpgradePrivateModel_1_3 "
        + "does not trigger a checkpoint");

    upgradeManager.selectUpgrades(ImmutableMap.of(), false);
  }

  private static UpgradeManager createUpgradeManager(final List<Checkpoint> checkpoints,
                                                     final List<Upgrade> upgrades)
  {
    return new UpgradeManager(checkpoints, upgrades, false);
  }

  private static UpgradeManager createUpgradeManagerWithWarnings(final List<Checkpoint> checkpoints,
                                                                 final List<Upgrade> upgrades)
  {
    return new UpgradeManager(checkpoints, upgrades, true);
  }
}
