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
package org.sonatype.nexus.coreui;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.scheduling.ClusteredTaskState;
import org.sonatype.nexus.scheduling.TaskInfo.EndState;
import org.sonatype.nexus.scheduling.TaskInfo.RunState;
import org.sonatype.nexus.scheduling.TaskInfo.State;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests {@link TaskComponent}.
 */
public class TaskComponentTest
    extends TestSupport
{
  private TaskComponent component;

  @Before
  public void setUp() {
    component = new TaskComponent();
  }

  @Test
  public void testGetAggregateState_RunningVsWaiting() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.RUNNING, RunState.STARTING, null, null, null),
        new ClusteredTaskState("node-b", State.WAITING, null, null, null, null));
    assertThat(component.getAggregateState(states), is(State.RUNNING));
  }

  @Test
  public void testGetAggregateState_RunningVsDone() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.RUNNING, RunState.STARTING, null, null, null),
        new ClusteredTaskState("node-b", State.DONE, null, null, null, null));
    assertThat(component.getAggregateState(states), is(State.RUNNING));
  }

  @Test
  public void testGetAggregateState_WaitingVsDone() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.WAITING, null, null, null, null),
        new ClusteredTaskState("node-b", State.DONE, null, null, null, null));
    assertThat(component.getAggregateState(states), is(State.WAITING));
  }

  @Test
  public void testGetAggregateRunState_CanceledVsRunning() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.RUNNING, RunState.CANCELED, null, null, null),
        new ClusteredTaskState("node-b", State.RUNNING, RunState.RUNNING, null, null, null),
        new ClusteredTaskState("node-c", State.WAITING, null, null, null, null));
    assertThat(component.getAggregateRunState(states), is(RunState.CANCELED));
  }

  @Test
  public void testGetAggregateRunState_CanceledVsBlocked() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.RUNNING, RunState.CANCELED, null, null, null),
        new ClusteredTaskState("node-b", State.RUNNING, RunState.BLOCKED, null, null, null),
        new ClusteredTaskState("node-c", State.WAITING, null, null, null, null));
    assertThat(component.getAggregateRunState(states), is(RunState.CANCELED));
  }

  @Test
  public void testGetAggregateRunState_CanceledVsStarting() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.RUNNING, RunState.CANCELED, null, null, null),
        new ClusteredTaskState("node-b", State.RUNNING, RunState.STARTING, null, null, null),
        new ClusteredTaskState("node-c", State.WAITING, null, null, null, null));
    assertThat(component.getAggregateRunState(states), is(RunState.CANCELED));
  }

  @Test
  public void testGetAggregateRunState_RunningVsBlocked() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.RUNNING, RunState.RUNNING, null, null, null),
        new ClusteredTaskState("node-b", State.RUNNING, RunState.BLOCKED, null, null, null),
        new ClusteredTaskState("node-c", State.WAITING, null, null, null, null));
    assertThat(component.getAggregateRunState(states), is(RunState.RUNNING));
  }

  @Test
  public void testGetAggregateRunState_RunningVsStarting() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.RUNNING, RunState.RUNNING, null, null, null),
        new ClusteredTaskState("node-b", State.RUNNING, RunState.STARTING, null, null, null),
        new ClusteredTaskState("node-c", State.WAITING, null, null, null, null));
    assertThat(component.getAggregateRunState(states), is(RunState.RUNNING));
  }

  @Test
  public void testGetAggregateRunState_BlockedVsStarting() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.RUNNING, RunState.BLOCKED, null, null, null),
        new ClusteredTaskState("node-b", State.RUNNING, RunState.STARTING, null, null, null),
        new ClusteredTaskState("node-c", State.WAITING, null, null, null, null));
    assertThat(component.getAggregateRunState(states), is(RunState.BLOCKED));
  }

  @Test
  public void testGetAggregateEndState_FailedVsCanceled() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.WAITING, null, EndState.FAILED, null, null),
        new ClusteredTaskState("node-b", State.WAITING, null, EndState.CANCELED, null, null),
        new ClusteredTaskState("node-c", State.WAITING, null, null, null, null));
    assertThat(component.getAggregateEndState(states), is(EndState.FAILED));
  }

  @Test
  public void testGetAggregateEndState_FailedVsOk() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.WAITING, null, EndState.FAILED, null, null),
        new ClusteredTaskState("node-b", State.WAITING, null, EndState.OK, null, null),
        new ClusteredTaskState("node-c", State.WAITING, null, null, null, null));
    assertThat(component.getAggregateEndState(states), is(EndState.FAILED));
  }

  @Test
  public void testGetAggregateEndState_CanceledVsOk() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.WAITING, null, EndState.CANCELED, null, null),
        new ClusteredTaskState("node-b", State.WAITING, null, EndState.OK, null, null),
        new ClusteredTaskState("node-c", State.WAITING, null, null, null, null));
    assertThat(component.getAggregateEndState(states), is(EndState.CANCELED));
  }

  @Test
  public void testGetAggregateLastRun() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.WAITING, null, null, new Date(1234567890), null),
        new ClusteredTaskState("node-b", State.WAITING, null, null, new Date(987654321), null),
        new ClusteredTaskState("node-c", State.WAITING, null, null, null, null));
    assertThat(component.getAggregateLastRun(states), is(new Date(1234567890)));
  }

  @Test
  public void testGetAggregateRunDuration() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.WAITING, null, null, null, 123456L),
        new ClusteredTaskState("node-b", State.WAITING, null, null, null, 654321L),
        new ClusteredTaskState("node-c", State.WAITING, null, null, null, null));
    assertThat(component.getAggregateRunDuration(states), is(654321L));
  }

  @Test
  public void testAsTaskStates() {
    assertThat(component.asTaskStates(null), is(nullValue()));
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.RUNNING, RunState.STARTING, null, null, null),
        new ClusteredTaskState("node-b", State.RUNNING, RunState.BLOCKED, EndState.OK, null, 10000L));
    List<TaskStateXO> xos = component.asTaskStates(states);
    assertThat(xos, hasSize(2));
    assertThat(xos.get(0).getNodeId(), is("node-a"));
    assertThat(xos.get(0).getStatus(), is(State.RUNNING.name()));
    assertThat(xos.get(0).getStatusDescription(), is("Starting"));
    assertThat(xos.get(1).getNodeId(), is("node-b"));
    assertThat(xos.get(1).getStatus(), is(State.RUNNING.name()));
    assertThat(xos.get(1).getStatusDescription(), is("Blocked"));
    assertThat(xos.get(1).getLastRunResult(), is("Ok [10s]"));
  }

  @Test
  public void testAsTaskStates_SuppressedIfAllCompletedSuccessfully() {
    List<ClusteredTaskState> states = Arrays.asList(
        new ClusteredTaskState("node-a", State.WAITING, null, null, null, null),
        new ClusteredTaskState("node-b", State.DONE, null, null, null, null),
        new ClusteredTaskState("node-c", State.WAITING, null, EndState.OK, null, null));
    assertThat(component.asTaskStates(states), is(nullValue()));
  }
}
