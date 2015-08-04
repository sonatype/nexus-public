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

import java.io.File;

import org.sonatype.nexus.AbstractMavenRepoContentTests;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.scheduling.NexusScheduler;
import org.sonatype.scheduling.ScheduledTask;
import org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers;

import org.codehaus.plexus.util.DirectoryScanner;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class RebuildMavenMetadataTaskTest
    extends AbstractMavenRepoContentTests
{
  protected NexusScheduler nexusScheduler;

  protected void setUp()
      throws Exception
  {
    super.setUp();

    nexusScheduler = lookup(NexusScheduler.class);
  }

  protected void tearDown()
      throws Exception
  {
    super.tearDown();
  }

  protected int countFiles(final MavenRepository repository, final String[] includepattern,
                           final String[] excludepattern)
      throws Exception
  {
    // get the root
    final File repoStorageRoot = retrieveFile(repository, "");

    // use scanner
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(repoStorageRoot);
    scanner.setIncludes(includepattern);

    scanner.setExcludes(excludepattern);

    // count
    scanner.scan();

    return scanner.getIncludedFiles().length;
  }

  @Test
  public void testOneRun()
      throws Exception
  {
    fillInRepo();

    final int countTotalBefore =
        countFiles(snapshots, new String[]{"**/maven-metadata.xml"}, new String[]{".nexus/**"});

    RebuildMavenMetadataTask task = nexusScheduler.createTaskInstance( //
        RebuildMavenMetadataTask.class);

    task.setRepositoryId(snapshots.getId());

    ScheduledTask<Object> handle = nexusScheduler.submit("task", task);

    // block until it finishes
    handle.get();

    // count it again
    final int countTotalAfter =
        countFiles(snapshots, new String[]{"**/maven-metadata.xml"}, new String[]{".nexus/**"});

    // assert
    assertTrue("We should have more md's after rebuilding them, since we have some of them missing!",
        countTotalBefore < countTotalAfter);
  }

  @Test
  public void testOneRunWithSubpath()
      throws Exception
  {
    fillInRepo();

    // we will initiate a task with "subpath" of /org/sonatype, so we count the files of total and the processed and
    // non-processed set
    // to be able to perform checks at the end
    final int countTotalBefore =
        countFiles(snapshots, new String[]{"**/maven-metadata.xml"}, new String[]{".nexus/**"});
    final int countNonProcessedSubBefore =
        countFiles(snapshots, new String[]{"**/maven-metadata.xml"}, new String[]{
            ".nexus/**",
            "org/sonatype/**/maven-metadata.xml"
        });
    final int countProcessedSubBefore =
        countFiles(snapshots, new String[]{"org/sonatype/**/maven-metadata.xml"}, new String[]{".nexus/**"});

    RebuildMavenMetadataTask task = nexusScheduler.createTaskInstance( //
        RebuildMavenMetadataTask.class);

    task.setRepositoryId(snapshots.getId());
    task.setResourceStorePath("/org/sonatype");

    ScheduledTask<Object> handle = nexusScheduler.submit("task", task);

    // block until it finishes
    handle.get();

    // count it again
    final int countTotalAfter =
        countFiles(snapshots, new String[]{"**/maven-metadata.xml"}, new String[]{".nexus/**"});
    final int countNonProcessedSubAfter =
        countFiles(snapshots, new String[]{"**/maven-metadata.xml"}, new String[]{
            ".nexus/**",
            "org/sonatype/**/maven-metadata.xml"
        });
    final int countProcessedSubAfter =
        countFiles(snapshots, new String[]{"org/sonatype/**/maven-metadata.xml"}, new String[]{".nexus/**"});

    // assert
    assertTrue(String.format(
        "We should have more md's after rebuilding them, since we have some of them missing! (%s, %s)",
        new Object[]{countTotalBefore, countTotalAfter}), countTotalBefore < countTotalAfter);
    assertTrue(String.format(
        "We should have same count of md's after rebuilding them for non-processed ones! (%s, %s)", new Object[]{
        countNonProcessedSubBefore, countNonProcessedSubAfter
    }),
        countNonProcessedSubBefore == countNonProcessedSubAfter);
    assertTrue(
        String.format(
            "We should have more md's after rebuilding them for processed ones, since we have some of them missing! (%s, %s)",
            new Object[]{countProcessedSubBefore, countProcessedSubAfter}),
        countProcessedSubBefore < countProcessedSubAfter);

    // the total change has to equals to processed change
    assertTrue(
        "We should have same change on total level as we have on processed ones, since we have some of them missing!",
        (countTotalAfter - countTotalBefore) == (countProcessedSubAfter - countProcessedSubBefore));
  }

  @Test
  public void testClassifierWithDots()
      throws Exception
  {
    fillInRepo();

    final RebuildMavenMetadataTask task = nexusScheduler.createTaskInstance(RebuildMavenMetadataTask.class);
    task.setRepositoryId(snapshots.getId());
    nexusScheduler.submit("task", task).get();

    final File mdFile = retrieveFile(snapshots, "/org/sonatype/nexus/nexus/1.3.0-SNAPSHOT/maven-metadata.xml");
    assertThat(mdFile, FileMatchers.exists());
    assertThat(mdFile, FileMatchers.contains("<classifier>classifier.with.dots</classifier>"));
  }

}
