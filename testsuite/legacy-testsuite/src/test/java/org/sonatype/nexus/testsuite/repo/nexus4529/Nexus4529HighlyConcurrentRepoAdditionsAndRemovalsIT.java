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
package org.sonatype.nexus.testsuite.repo.nexus4529;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;

import org.junit.Test;
import org.restlet.data.MediaType;
import org.restlet.data.Method;

/**
 * See NEXUS-4529: This IT bombards Nexus with rest requests to create and drop repositories.
 *
 * @author cstamas
 */
public class Nexus4529HighlyConcurrentRepoAdditionsAndRemovalsIT
    extends AbstractNexusIntegrationTest
{
  protected RepositoryMessageUtil repoUtil = new RepositoryMessageUtil(this, getJsonXStream(),
      MediaType.APPLICATION_JSON);

  @Test
  public void doTheTest()
      throws Exception
  {
    execute(100);
    verify();
  }

  public void execute(int threadCount)
      throws Exception
  {
    ArrayList<RepoAddRemove> runnables = new ArrayList<RepoAddRemove>(threadCount);
    ArrayList<Future<?>> futures = new ArrayList<Future<?>>(threadCount);

    for (int i = 0; i < threadCount; i++) {
      runnables.add(new RepoAddRemove(repoUtil, String.valueOf(i)));
    }

    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

    try {
      for (RepoAddRemove r : runnables) {
        futures.add(executor.submit(r));
      }

      // wait
      Thread.sleep(10000);

      for (RepoAddRemove r : runnables) {
        r.stop();
      }

      // wait
      Thread.sleep(1000);

      for (Future<?> f : futures) {
        // this will throw exception if thread failed with HTTP 500 for example
        f.get();
      }
    }
    finally {
      executor.shutdownNow();
    }
  }

  public void verify() {
    // well, nothing here, the original issue NEXUS-4529 mentions nexus replies with HTTP 500
    // and that will make previous execute() fail anyway. Still, maybe some log search for ERROR?

  }

  public static class RepoAddRemove
      implements Callable<Object>
  {
    private final RepositoryMessageUtil repoUtil;

    private final String prefix;

    private boolean running = true;

    public RepoAddRemove(final RepositoryMessageUtil repoUtil, final String prefix) {
      this.repoUtil = repoUtil;

      this.prefix = prefix;
    }

    public void stop() {
      this.running = false;
    }

    protected void createRepository(final String repoId)
        throws IOException
    {
      RepositoryResource repo = new RepositoryResource();
      repo.setId(repoId);
      repo.setRepoType("hosted");
      repo.setName(repoId);
      repo.setProvider("maven2");
      repo.setFormat("maven2");
      repo.setRepoPolicy(RepositoryPolicy.RELEASE.name());
      repoUtil.createRepository(repo, false);
    }

    protected void deleteRepository(final String repoId)
        throws IOException
    {
      repoUtil.sendMessage(Method.DELETE, null, repoId);
    }

    protected String getRepoId(final int counter) {
      return prefix + "-" + counter;
    }

    @Override
    public Object call()
        throws Exception
    {
      int counter = 1;

      while (running) {
        createRepository(getRepoId(counter));

        // wait
        Thread.sleep(500);

        deleteRepository(getRepoId(counter));

        counter++;
      }

      return null;
    }
  }
}
