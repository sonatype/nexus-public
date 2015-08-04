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
package org.sonatype.nexus.maven.tasks;

import javax.inject.Singleton;

import org.sonatype.nexus.AbstractMavenRepoContentTests;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.scheduling.CancellableProgressListenerWrapper;
import org.sonatype.scheduling.TaskInterruptedException;
import org.sonatype.scheduling.TaskUtil;

import com.google.common.collect.ObjectArrays;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Names;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * See Nexus-4588, testing SnapshotRemover cancellation. Note: Snapshot remover task is just a "thin wrapper" doing
 * not much around the SnapshotRemover that is a singleto component in system. Hence, to make this test more
 * simpler, we test the SnapshotRemover component, without the "tasks" fuss.
 *
 * @author: cstamas
 */
public class Nexus4588CancellationTest
    extends AbstractMavenRepoContentTests
{

  @Override
  protected Module[] getTestCustomModules() {
    Module[] modules = super.getTestCustomModules();
    if (modules == null) {
      modules = new Module[0];
    }
    modules = ObjectArrays.concat(modules, new Module()
    {
      @Override
      public void configure(final Binder binder) {
        binder.bind(Nexus4588CancellationEventInspector.class);
      }
    });
    return modules;
  }

  @Before
  public void setUpProgressListener()
      throws Exception
  {
    new TaskUtil()
    {
      {
        setCurrent(new CancellableProgressListenerWrapper(null));
      }
    };
  }

  @After
  public void removeProgressListener()
      throws Exception
  {
    new TaskUtil()
    {
      {
        setCurrent(null);
      }
    };
  }

  @Test(expected = TaskInterruptedException.class)
  public void testNexus4588()
      throws Exception
  {
    fillInRepo();

    SnapshotRemovalRequest snapshotRemovalRequest =
        new SnapshotRemovalRequest(snapshots.getId(), 1, 10, true);

    TaskUtil.getCurrentProgressListener().cancel();

    SnapshotRemovalResult result = lookup(SnapshotRemover.class).removeSnapshots(snapshotRemovalRequest);
  }

  @Test(expected = TaskInterruptedException.class)
  public void testNexus4588After1stWalk()
      throws Exception
  {
    fillInRepo();

    SnapshotRemovalRequest snapshotRemovalRequest =
        new SnapshotRemovalRequest(snapshots.getId(), 1, 10, true);

    // activate the molester
    // the molester will cancel the task once it receives cache expired event, which is sent
    // after 1st pass. This is an implementation details, so this test is actually fragile
    // against SnapshotRemover component implementation changes
    ((Nexus4588CancellationEventInspector) lookup(EventSubscriber.class, "nexus4588")).setActive(true);

    SnapshotRemovalResult result = lookup(SnapshotRemover.class).removeSnapshots(snapshotRemovalRequest);
  }

}
