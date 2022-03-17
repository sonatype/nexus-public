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
package org.sonatype.nexus.testsuite.task.nexus4580;

import java.io.File;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.RequestFacade;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.tasks.descriptors.EmptyTrashTaskDescriptor;
import org.sonatype.nexus.test.utils.NexusRequestMatchers;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers;

import org.junit.Test;
import org.restlet.resource.StringRepresentation;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * IT for NEXUS-4580 Empty trash task should allow specifying repositories
 * 
 * @since 2.7.0
 * @see <a href="https://issues.sonatype.org/browse/NEXUS-4580">NEXUS-4580</a>
 */
public class Nexus4580EmptyTrashTaskRepositoryParameterIT
    extends AbstractNexusIntegrationTest
{

  @Test
  public void emptyTrashTaskOnOneRepo() throws Exception {
    // deploy
    RequestFacade.doPutForText(REPOSITORY_RELATIVE_URL + REPO_TEST_HARNESS_RELEASE_REPO + "/foo.txt",
        new StringRepresentation("dummy content"), NexusRequestMatchers.respondsWithStatusCode(201));
    RequestFacade.doPutForText(REPOSITORY_RELATIVE_URL + REPO_TEST_HARNESS_SNAPSHOT_REPO + "/foo.txt",
        new StringRepresentation("dummy content"), NexusRequestMatchers.respondsWithStatusCode(201));

    // delete both
    RequestFacade.doDelete(REPOSITORY_RELATIVE_URL + REPO_TEST_HARNESS_RELEASE_REPO + "/foo.txt",
        NexusRequestMatchers.respondsWithStatusCode(204));
    RequestFacade.doDelete(REPOSITORY_RELATIVE_URL + REPO_TEST_HARNESS_SNAPSHOT_REPO + "/foo.txt",
        NexusRequestMatchers.respondsWithStatusCode(204));

    final File releaseTrashFile = new File(nexusWorkDir, "storage/" + REPO_TEST_HARNESS_RELEASE_REPO
        + "/.nexus/trash/foo.txt");
    final File snapshotTrashFile = new File(nexusWorkDir, "storage/" + REPO_TEST_HARNESS_SNAPSHOT_REPO
        + "/.nexus/trash/foo.txt");

    // verify both repo trashes have the files
    assertThat(releaseTrashFile, FileMatchers.isFile());
    assertThat(snapshotTrashFile, FileMatchers.isFile());

    // empty trash but only on snapshot repo
    ScheduledServicePropertyResource age = new ScheduledServicePropertyResource();
    age.setKey(EmptyTrashTaskDescriptor.OLDER_THAN_FIELD_ID);
    age.setValue("0");
    ScheduledServicePropertyResource repository = new ScheduledServicePropertyResource();
    age.setKey(EmptyTrashTaskDescriptor.REPO_OR_GROUP_FIELD_ID);
    age.setValue(REPO_TEST_HARNESS_SNAPSHOT_REPO);
    TaskScheduleUtil.runTask("Empty Trash Older Than", EmptyTrashTaskDescriptor.ID, age, repository);

    // verify that only releases trash has the file (other was nuked)
    assertThat(releaseTrashFile, FileMatchers.isFile());
    assertThat("snapshot file should be deleted", !snapshotTrashFile.exists());
  }
}
