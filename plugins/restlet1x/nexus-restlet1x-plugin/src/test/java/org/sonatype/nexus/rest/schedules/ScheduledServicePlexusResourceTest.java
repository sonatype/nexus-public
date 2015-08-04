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
package org.sonatype.nexus.rest.schedules;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.scheduling.DefaultScheduledTask;
import org.sonatype.scheduling.TaskState;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ScheduledServicePlexusResourceTest
{

  @Test
  public void testGetReadableState() {
    AbstractScheduledServicePlexusResource service = new AbstractScheduledServicePlexusResource()
    {

      @Override
      public String getResourceUri() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public PathProtectionDescriptor getResourceProtection() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Object getPayloadInstance() {
        // TODO Auto-generated method stub
        return null;
      }
    };

    TaskState[] states = TaskState.values();
    for (TaskState state : states) {
      service.getReadableState(state);
    }
  }

  @SuppressWarnings("rawtypes")
  private static class MockDefaultScheduledTask
      extends DefaultScheduledTask
  {

    public MockDefaultScheduledTask() {
      super("id", "name", "type", null, null, null);
    }

    @Override
    public void setLastStatus(TaskState lastStatus) {
      super.setLastStatus(lastStatus);
    }

    @Override
    public void setDuration(long duration) {
      super.setDuration(duration);
    }
  }

  @Test
  public void testGetLastRunResult() {
    AbstractScheduledServicePlexusResource service = new AbstractScheduledServicePlexusResource()
    {

      @Override
      public String getResourceUri() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public PathProtectionDescriptor getResourceProtection() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Object getPayloadInstance() {
        // TODO Auto-generated method stub
        return null;
      }
    };

    MockDefaultScheduledTask task = new MockDefaultScheduledTask();
    task.setLastStatus(TaskState.FINISHED);

    task.setDuration(58 * 1000);
    assertThat(service.getLastRunResult(task), equalTo("Ok [58s]"));

    task.setDuration(7 * 60 * 1000);
    assertThat(service.getLastRunResult(task), equalTo("Ok [7m0s]"));

    task.setDuration(3 * 60 * 60 * 1000);
    assertThat(service.getLastRunResult(task), equalTo("Ok [3h0m0s]"));

    task.setDuration(2 * 24 * 60 * 60 * 1000 + 5 * 60 * 60 * 1000 + 13 * 60 * 1000 + 22 * 1000);
    assertThat(service.getLastRunResult(task), equalTo("Ok [53h13m22s]"));
  }

}
