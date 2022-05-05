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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationDirectories;
import org.sonatype.nexus.orient.freeze.FreezeRequest;
import org.sonatype.nexus.orient.freeze.FreezeRequest.InitiatorType;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LocalDatabaseFrozenStateManager}.
 */
public class LocalDatabaseFrozenStateManagerTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock
  ApplicationDirectories applicationDirectories;

  LocalDatabaseFrozenStateManager manager;

  @Before
  public void setup() throws IOException {
    File workdir = temporaryFolder.newFolder();
    when(applicationDirectories.getWorkDirectory("db")).thenReturn(workdir);

    manager = new LocalDatabaseFrozenStateManager(applicationDirectories);
  }

  @Test
  public void getState_empty() {
    assertThat(manager.getState().isEmpty(), is(true));
  }

  @Test
  public void add_successful() {
    FreezeRequest request = new FreezeRequest(InitiatorType.USER_INITIATED, "admin");
    assertThat(manager.add(request), equalTo(request));
    assertThat(manager.getState().isEmpty(), is(false));
    assertThat(manager.getState().get(0), equalTo(request));
  }
  @Test
  public void add_successful_with_nodeId() {
    FreezeRequest request = new FreezeRequest(InitiatorType.USER_INITIATED, "admin")
        .setNodeId("ABCDE-12345-ABCDE");
    assertThat(manager.add(request), equalTo(request));
    assertThat(manager.getState().isEmpty(), is(false));
    assertThat(manager.getState().get(0), equalTo(request));
  }

  @Test
  public void add_multiple_successful() {
    for (int i = 0; i < 10; i++) {
      FreezeRequest request = new FreezeRequest(InitiatorType.SYSTEM, "task-" + i);
      assertThat(manager.add(request), equalTo(request));
    }

    assertThat(manager.getState().isEmpty(), is(false));
    assertThat(manager.getState().size(), equalTo(10));
  }

  @Test
  public void remove_successful() {
    FreezeRequest request = new FreezeRequest(InitiatorType.USER_INITIATED, "admin");
    assertThat(manager.add(request), equalTo(request));
    assertThat(manager.getState().isEmpty(), is(false));
    assertThat(manager.getState().get(0), equalTo(request));

    assertThat(manager.remove(request), is(true));
    assertThat(manager.getState().isEmpty(), is(true));
  }

  @Test
  public void remove_on_empty_has_no_effect() {
    assertThat(manager.getState().isEmpty(), is(true));
    assertThat(manager.remove(new FreezeRequest(InitiatorType.USER_INITIATED, "admin")), is(false));
  }

  /**
   * Issue a number of concurrent add/remove calls, expecting to leave behind only one request in state.
   */
  @Test
  public void concurrency_test() {
    List<FreezeRequest> requests = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      requests.add(new FreezeRequest(InitiatorType.SYSTEM, "task-" + i));
    }

    boolean completed = false;
    ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newWorkStealingPool());
    try {
      List<ListenableFuture<?>> futures = new ArrayList<>();
      requests.stream().forEach(request -> {
        futures.add(service.submit(() -> {
          // always add
          manager.add(request);
          // skip removing one of the requests
          if (!request.getInitiatorId().contains("51")) {
            manager.remove(request);
          }
        }));
      });

      Futures.allAsList(futures).get();
      completed = true;
    }
    catch (ExecutionException | InterruptedException e) {
      fail("caught exception " + e);
    }
    finally {
      assertThat(service.shutdownNow().isEmpty(), is(true));
    }

    assertThat(completed, is(true));
    // after all that noise, adding and deleting, we should have only one request present
    assertThat(manager.getState().isEmpty(), is(false));
    assertThat(manager.getState().get(0), equalTo(requests.get(51)));
  }
}
