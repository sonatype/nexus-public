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
package org.sonatype.nexus.testsuite.group.nexus977metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.sonatype.nexus.integrationtests.AbstractNexusProxyIntegrationTest;
import org.sonatype.nexus.maven.tasks.descriptors.RebuildMavenMetadataTaskDescriptor;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;
import org.sonatype.nexus.rest.model.ScheduledServicePropertyResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;
import org.sonatype.nexus.util.DigesterUtils;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

public class Nexus977MavenMetadataGroupOfGroupsIT
    extends AbstractNexusProxyIntegrationTest
{

  @Override
  protected void runOnce()
      throws Exception
  {
    super.runOnce();

    ScheduledServicePropertyResource repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue("release");
    TaskScheduleUtil.runTask("RebuildMavenMetadata-release", RebuildMavenMetadataTaskDescriptor.ID, repo);

    repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue("release2");
    TaskScheduleUtil.runTask("RebuildMavenMetadata-release2", RebuildMavenMetadataTaskDescriptor.ID, repo);

    repo = new ScheduledServicePropertyResource();
    repo.setKey("repositoryId");
    repo.setValue("snapshot");
    TaskScheduleUtil.runTask("RebuildMavenMetadata-snapshot", RebuildMavenMetadataTaskDescriptor.ID, repo);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void checkMetadata()
      throws Exception
  {
    File metadataFile =
        downloadFile(new URL(nexusBaseUrl + "content/repositories/g4/"
            + "nexus977metadata/project/maven-metadata.xml"), "target/downloads/nexus977");

    try (FileInputStream in = new FileInputStream(metadataFile)) {
      Metadata metadata = MetadataBuilder.read(in);
      List<String> versions = metadata.getVersioning().getVersions();
      MatcherAssert.assertThat(versions, hasItems("1.5", "1.0.1", "1.0-SNAPSHOT", "0.8", "2.1"));
    }
  }

  @Test
  public void checkProperMetadataHashesCreated() throws Exception {
    //note that 'g4' is a group repository
    String basePath = nexusBaseUrl + "content/repositories/g4/nexus977metadata/project/maven-metadata.xml";

    File testFile = downloadFile(new URL(basePath), "target/downloads/nexus977.xml");
    MatcherAssert.assertThat("Metadata file not found!", testFile.exists());

    downloadAndAssertHash(basePath + ".sha1", DigesterUtils.getSha1Digest(testFile));
    downloadAndAssertHash(basePath + ".sha256", DigesterUtils.getSha256Digest(new FileInputStream(testFile)));
    downloadAndAssertHash(basePath + ".sha512", DigesterUtils.getSha512Digest(new FileInputStream(testFile)));
    downloadAndAssertHash(basePath + ".md5", DigesterUtils.getMd5Digest(new FileInputStream(testFile)));
    assertMissing(basePath + ".sha-256");
    assertMissing(basePath + ".sha-512");
  }

  private void downloadAndAssertHash(final String filePath, final String expectedHash) throws IOException {
    File file = downloadFile(new URL(filePath), "target/downloads/nexus977.hash");
    MatcherAssert.assertThat("Metadata hash file not found", file.exists());
    MatcherAssert.assertThat("Metadata hash not valid", expectedHash,
        equalTo(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).trim()));
  }

  private void assertMissing(final String filePath) throws IOException {
    try {
      downloadFile(new URL(filePath), "target/downloads/nexus977.hash");
      Assert.fail("Hashfile should not have been created: " + filePath);
    }
    catch (FileNotFoundException e) {
      //good
    }
  }
}
