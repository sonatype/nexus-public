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
package org.sonatype.nexus.testsuite.maven;

import java.io.File;
import java.util.Map;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.subsystem.content.Content;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenHostedRepository;
import org.sonatype.nexus.testsuite.client.Scheduler;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;
import org.sonatype.sisu.goodies.common.Time;

import com.google.common.collect.Maps;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.client.core.subsystem.content.Location.repositoryLocation;
import static org.sonatype.sisu.filetasks.builder.FileRef.file;

/**
 * ITs related to unused snapshots remover task.
 *
 * @since 2.7.0
 */
@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public class UnusedSnapshotRemoverTaskIT
    extends NexusRunningParametrizedITSupport
{

  public UnusedSnapshotRemoverTaskIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return configuration
        .setLogLevel("org.sonatype.nexus.maven.tasks", "TRACE");
  }

  @Test
  public void removeUnusedSnapshots()
      throws Exception
  {
    final MavenHostedRepository repository = repositories()
        .create(MavenHostedRepository.class, repositoryIdForTest())
        .withRepoPolicy("SNAPSHOT")
        .excludeFromSearchResults()
        .save();

    final long today = System.currentTimeMillis();
    final long fiveDaysAgo = today - Time.days(5).toMillis();

    tasks().copy().directory(file(testData().resolveFile("removeUnusedSnapshots-storage")))
        .to().directory(file(new File(nexus().getWorkDirectory(), "storage/" + repository.id())))
        .filterUsing("today", String.valueOf(today))
        .filterUsing("fiveDaysAgo", String.valueOf(fiveDaysAgo))
        .run();

    final Map<String, String> taskProperties = Maps.newHashMap();
    taskProperties.put("repositoryId", repository.id());
    taskProperties.put("daysSinceLastRequested", "4");

    scheduler().run("UnusedSnapshotRemoverTask", taskProperties);

    // the following have requested timestamp = today
    assertExists(repository.id(), "20130102.120000-2.pom");
    assertExists(repository.id(), "20130102.120000-2.jar");
    assertExists(repository.id(), "20130102.120000-2-copy.jar");

    // the following have requested timestamp = fiveDaysAgo
    assertDoesNotExists(repository.id(), "20130101.120000-1.pom");
    assertDoesNotExists(repository.id(), "20130101.120000-1.jar");
    assertDoesNotExists(repository.id(), "20130101.120000-1-copy.jar");

    // the following have requested timestamp = fiveDaysAgo
    assertDoesNotExists(repository.id(), "20130103.120000-3.pom");
  }

  private void assertExists(final String repositoryId, final String name)
      throws Exception
  {
    content().download(
        repositoryLocation(repositoryId, "aopalliance/aopalliance/1.0-SNAPSHOT/aopalliance-1.0-" + name),
        new File(testIndex().getDirectory("downloads"), "aopalliance-1.0-" + name)
    );
  }

  private void assertDoesNotExists(final String repositoryId, final String name)
      throws Exception
  {
    try {
      content().download(
          repositoryLocation(repositoryId, "aopalliance/aopalliance/1.0-SNAPSHOT/aopalliance-1.0-" + name),
          new File(testIndex().getDirectory("downloads"), "aopalliance-1.0-" + name)
      );
      assertThat(
          "Snapshot was not removed: aopalliance/aopalliance/1.0-SNAPSHOT/aopalliance-1.0-" + name, false
      );
    }
    catch (NexusClientNotFoundException e) {
      // expected
    }
  }

  private Repositories repositories() {
    return client().getSubsystem(Repositories.class);
  }

  private Scheduler scheduler() {
    return client().getSubsystem(Scheduler.class);
  }

  private Content content() {
    return client().getSubsystem(Content.class);
  }

}
