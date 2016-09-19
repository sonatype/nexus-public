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
import org.sonatype.nexus.upgrade.plan.DependencyResolver.CyclicDependencyException;
import org.sonatype.nexus.upgrade.plan.DependencyResolver.UnresolvedDependencyException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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

    UpgradeManager upgradeManager = new UpgradeManager(checkpoints, upgrades);

    Map<String, String> modelVersions = ImmutableMap.of();

    List<Upgrade> plan = upgradeManager.plan(modelVersions);

    assertThat(plan, contains(
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeFoo_1_1.class),
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeBar_1_1.class),
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2.class),
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0.class)
    ));

    List<Checkpoint> prepare = upgradeManager.prepare(plan);

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

    UpgradeManager upgradeManager = new UpgradeManager(checkpoints, upgrades);

    Map<String, String> modelVersions = ImmutableMap.of("foo", "1.1");

    List<Upgrade> plan = upgradeManager.plan(modelVersions);

    assertThat(plan, contains(
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeBar_1_1.class),
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2.class),
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0.class)
    ));

    List<Checkpoint> prepare = upgradeManager.prepare(plan);

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

    UpgradeManager upgradeManager = new UpgradeManager(checkpoints, upgrades);

    Map<String, String> modelVersions = ImmutableMap.of("foo", "1.1", "bar", "1.1");

    List<Upgrade> plan = upgradeManager.plan(modelVersions);

    assertThat(plan, containsInAnyOrder(
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeFoo_1_2.class),
        instanceOf(org.sonatype.nexus.upgrade.example.UpgradeWibble_2_0.class)
    ));

    List<Checkpoint> prepare = upgradeManager.prepare(plan);

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

    UpgradeManager upgradeManager = new UpgradeManager(checkpoints, upgrades);

    Map<String, String> modelVersions = ImmutableMap.of("foo", "1.3", "bar", "1.2", "wibble", "2.1", "qux", "1.7");

    List<Upgrade> plan = upgradeManager.plan(modelVersions);

    assertThat(plan, is(empty()));

    List<Checkpoint> prepare = upgradeManager.prepare(plan);

    assertThat(prepare, is(empty()));
  }

  @Test
  public void testBadVersionIsDetected() {

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.bad.UpgradeFoo_1_0()
    );

    UpgradeManager upgradeManager = new UpgradeManager(ImmutableList.of(), upgrades);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("upgrade version '1.0' is not after '1.0'");
    upgradeManager.plan(ImmutableMap.of());
  }

  @Test
  public void testUpgradeCycleIsDetected() {

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.cycle.UpgradeFoo_1_1(),
        new org.sonatype.nexus.upgrade.cycle.UpgradeBar_1_1(),
        new org.sonatype.nexus.upgrade.cycle.UpgradeWibble_1_1()
    );

    UpgradeManager upgradeManager = new UpgradeManager(ImmutableList.of(), upgrades);

    thrown.expect(CyclicDependencyException.class);
    upgradeManager.plan(ImmutableMap.of());
  }

  @Test
  public void testUpgradeGapIsDetected() {

    List<Upgrade> upgrades = ImmutableList.of(
        new org.sonatype.nexus.upgrade.gap.UpgradeFoo_1_1(),
        new org.sonatype.nexus.upgrade.gap.UpgradeFoo_1_3()
    );

    UpgradeManager upgradeManager = new UpgradeManager(ImmutableList.of(), upgrades);

    thrown.expect(UnresolvedDependencyException.class);
    upgradeManager.plan(ImmutableMap.of());
  }

  @Test
  public void testGetLocalModels() {
    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo(),
        new org.sonatype.nexus.upgrade.example.CheckpointBar(),
        new org.sonatype.nexus.upgrade.example.CheckpointWibble()
    );

    UpgradeManager upgradeManager = new UpgradeManager(checkpoints, ImmutableList.of());

    assertThat(upgradeManager.getLocalModels(), containsInAnyOrder("foo"));
  }

  @Test
  public void testGetClusteredModels() {
    List<Checkpoint> checkpoints = ImmutableList.of(
        new org.sonatype.nexus.upgrade.example.CheckpointFoo(),
        new org.sonatype.nexus.upgrade.example.CheckpointBar(),
        new org.sonatype.nexus.upgrade.example.CheckpointWibble()
    );

    UpgradeManager upgradeManager = new UpgradeManager(checkpoints, ImmutableList.of());

    assertThat(upgradeManager.getClusteredModels(), containsInAnyOrder("bar", "wibble"));
  }
}
