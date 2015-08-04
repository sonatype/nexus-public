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
package org.sonatype.nexus.plugins.tasks.api;

import javax.inject.Named;

import org.sonatype.nexus.scheduling.AbstractNexusRepositoriesTask;

import org.codehaus.plexus.util.StringUtils;

@Named("SleepRepositoryTask")
public class SleepRepositoryTask
    extends AbstractNexusRepositoriesTask<Object>
{

  private boolean cancellable;

  @Override
  protected Object doRun()
      throws Exception
  {
    cancellable = Boolean.parseBoolean(getParameter("cancellable"));

    getLogger().debug(getMessage());

    final int time = getTime();
    sleep(time);
    getRepositoryRegistry().getRepository(getRepositoryId());
    sleep(time);
    return null;
  }

  protected void sleep(final int time)
      throws InterruptedException
  {
    for (int i = 0; i < time; i++) {
      Thread.sleep(1000 / 2);
      if (cancellable) {
        checkInterruption();
      }
    }
  }

  private int getTime() {
    String t = getParameter("time");

    if (StringUtils.isEmpty(t)) {
      return 5;
    }
    else {
      return new Integer(t);
    }
  }

  @Override
  protected String getAction() {
    return "Sleeping";
  }

  @Override
  protected String getMessage() {
    return "Sleeping for " + getTime() + " seconds (cancellable: " + cancellable + ")!";
  }

}
