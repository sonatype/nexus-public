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
package org.sonatype.nexus.testsuite.maven.nexus634;

import java.io.File;
import java.util.Collection;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.maven.tasks.descriptors.SnapshotRemovalTaskDescriptor;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.RepositoryMessageUtil;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.io.FileUtils;
import org.junit.Before;

public abstract class AbstractSnapshotRemoverIT
    extends AbstractNexusIntegrationTest
{

  @SuppressWarnings("unchecked")
  public static Collection<File> listFiles(File directory, String[] extensions, boolean recursive) {
    return FileUtils.listFiles(directory, extensions, recursive);
  }

  protected File artifactFolder;

  protected File repositoryPath;

  public AbstractSnapshotRemoverIT() {
    super("nexus-test-harness-snapshot-repo");
  }

  @Before
  public void deploySnapshotArtifacts()
      throws Exception
  {
    initFolders();

    File oldSnapshot = getTestFile("repo");

    // Copying to keep an old timestamp
    FileUtils.copyDirectory(oldSnapshot, repositoryPath);

    RepositoryMessageUtil.updateIndexes("nexus-test-harness-snapshot-repo");

    // Gav gav =
    // new Gav( "nexus634", "artifact", "1.0-SNAPSHOT", null, "jar", 0, 0L, null, true, false, null, false, null );
    // File fileToDeploy = getTestFile( "artifact-1.jar" );
    //
    // // Deploying a fresh timestamp artifact
    // MavenDeployer.deployAndGetVerifier( gav, getNexusTestRepoUrl(), fileToDeploy, null );
    //
    // // Artifacts should be deployed here
    // Assert.assertTrue( "nexus643:artifact:1.0-SNAPSHOT folder doesn't exists!", artifactFolder.isDirectory() );
  }

  public void initFolders()
      throws Exception
  {
    repositoryPath = new File(nexusWorkDir, "storage/nexus-test-harness-snapshot-repo");
    artifactFolder = new File(repositoryPath, "nexus634/artifact/1.0-SNAPSHOT");
  }

  protected void runSnapshotRemover(String repositoryId, int minSnapshotsToKeep, int removeOlderThanDays,
                                    boolean removeIfReleaseExists)
      throws Exception
  {
    ScheduledServicePropertyResource repositoryProp = new ScheduledServicePropertyResource();
    repositoryProp.setKey("repositoryId");
    repositoryProp.setValue(repositoryId);

    ScheduledServicePropertyResource keepSnapshotsProp = new ScheduledServicePropertyResource();
    keepSnapshotsProp.setKey("minSnapshotsToKeep");
    keepSnapshotsProp.setValue(String.valueOf(minSnapshotsToKeep));

    ScheduledServicePropertyResource ageProp = new ScheduledServicePropertyResource();
    ageProp.setKey("removeOlderThanDays");
    ageProp.setValue(String.valueOf(removeOlderThanDays));

    ScheduledServicePropertyResource removeReleasedProp = new ScheduledServicePropertyResource();
    removeReleasedProp.setKey("removeIfReleaseExists");
    removeReleasedProp.setValue(String.valueOf(removeIfReleaseExists));

    TaskScheduleUtil.runTask(SnapshotRemovalTaskDescriptor.ID, repositoryProp, keepSnapshotsProp, ageProp,
        removeReleasedProp);
  }

}