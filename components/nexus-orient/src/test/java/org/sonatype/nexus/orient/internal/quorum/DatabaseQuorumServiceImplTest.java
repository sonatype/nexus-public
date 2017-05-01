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
package org.sonatype.nexus.orient.internal.quorum;

import java.util.Iterator;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.DatabaseClusterManager;
import org.sonatype.nexus.orient.DatabaseServer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import com.google.inject.util.Providers;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.distributed.ODistributedConfiguration;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DatabaseQuorumServiceImpl}.
 */
public class DatabaseQuorumServiceImplTest
    extends TestSupport
{
  private static final List<String> NAMES = ImmutableList.of("component", "security", "analytics",
      "audit", "config", "accesslog");

  @Mock
  OServer oServer;

  @Mock
  DatabaseServer databaseServer;

  @Mock
  ODistributedServerManager serverManager;

  @Mock
  ODistributedConfiguration configuration;

  @Mock
  NodeAccess nodeAccess;

  @Mock
  DatabaseClusterManager databaseClusterManager;

  DatabaseQuorumServiceImpl underTest;

  @Before
  public void setup() {
    when(oServer.getDistributedManager()).thenReturn(serverManager);
    when(serverManager.getDatabaseConfiguration(any(String.class))).thenReturn(configuration);
    when(databaseServer.databases()).thenReturn(NAMES);
    underTest = new DatabaseQuorumServiceImpl(Providers.of(oServer), databaseServer, nodeAccess,
        databaseClusterManager);

    when(configuration.getAllConfiguredServers()).thenReturn(ImmutableSet.of("one", "two", "three"));
    when(configuration.getWriteQuorum(any(String.class), eq(3), any(String.class))).thenReturn(2);
  }

  @Test
  public void getQuorumStatusNotClustered() {
    when(nodeAccess.isClustered()).thenReturn(false);
    assertTrue(underTest.getQuorumStatus().isQuorumPresent());
  }

  @Test
  public void getQuorumStatusPositive() {
    List<String> nodes = ImmutableList.of("one", "two", "three");
    when(nodeAccess.isClustered()).thenReturn(true);

    when(serverManager.getOnlineNodes(any(String.class))).thenReturn(nodes);

    assertTrue(underTest.getQuorumStatus().isQuorumPresent());
  }

  @Test
  public void getQuorumStatusNegativeAllDatabasesOffline() {
    List<String> nodes = ImmutableList.of("one");
    when(nodeAccess.isClustered()).thenReturn(true);

    when(serverManager.getOnlineNodes(any(String.class))).thenReturn(nodes);

    assertFalse(underTest.getQuorumStatus().isQuorumPresent());
  }

  @Test
  public void getQuorumStatusNegativeOnlyOneDatabaseOffline() {
    List<String> nodes = ImmutableList.of("one", "two", "three");
    when(nodeAccess.isClustered()).thenReturn(true);

    for (Iterator<String> iterator = NAMES.iterator(); iterator.hasNext(); ) {
      String name = iterator.next();
      if (iterator.hasNext()) {
        when(serverManager.getOnlineNodes(name)).thenReturn(nodes);
      }
      else {
        when(serverManager.getOnlineNodes(name)).thenReturn(ImmutableList.of("one"));
      }
    }

    assertFalse(underTest.getQuorumStatus().isQuorumPresent());
  }

  @Test
  public void getSingleDatabaseQuorumStatus() {
    when(nodeAccess.isClustered()).thenReturn(true);
    when(serverManager.getOnlineNodes(any(String.class))).thenReturn(ImmutableList.of("one", "two", "three"));

    assertThat(underTest.getQuorumStatus(NAMES.get(0)).isQuorumPresent(), is(Boolean.TRUE));
    assertThat(underTest.getQuorumStatus(NAMES.get(0)).getDatabaseName(), is(NAMES.get(0)));
  }

  @Test
  public void resetQuorum() {
    when(nodeAccess.isClustered()).thenReturn(true);
    when(nodeAccess.getId()).thenReturn("one");

    underTest.resetWriteQuorum();
    verify(databaseClusterManager).removeServer("two");
    verify(databaseClusterManager).removeNodeFromConfiguration("two");
    verify(databaseClusterManager).removeServer("three");
    verify(databaseClusterManager).removeNodeFromConfiguration("three");
  }
}
