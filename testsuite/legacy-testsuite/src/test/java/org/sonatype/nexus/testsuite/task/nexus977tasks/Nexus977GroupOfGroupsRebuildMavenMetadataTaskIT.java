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
package org.sonatype.nexus.testsuite.task.nexus977tasks;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.maven.tasks.descriptors.RebuildMavenMetadataTaskDescriptor;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItems;

public class Nexus977GroupOfGroupsRebuildMavenMetadataTaskIT
    extends AbstractNexusProxyIntegrationTest
{

  @Test
  public void checkMetadata()
      throws Exception
  {
    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue("g4");
    TaskScheduleUtil.runTask("RebuildMavenMetadata-snapshot", RebuildMavenMetadataTaskDescriptor.ID, repo);

    File metadataFile =
        downloadFile(new URL(nexusBaseUrl + "content/repositories/g4/"
            + "nexus977tasks/project/maven-metadata.xml"), "target/downloads/nexus977");

    try (FileInputStream in = new FileInputStream(metadataFile)) {
      Metadata metadata = MetadataBuilder.read(in);
      List<String> versions = metadata.getVersioning().getVersions();
      MatcherAssert.assertThat(versions, hasItems("1.5", "1.0.1", "1.0-SNAPSHOT", "0.8", "2.1"));
    }
  }
}
