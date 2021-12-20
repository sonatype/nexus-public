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
package org.sonatype.nexus.testsuite.npm.rebuild;

import java.io.File;

import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.testsuite.client.Scheduler;
import org.sonatype.nexus.testsuite.npm.NpmITSupport;
import org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers;

import com.bolyuba.nexus.plugin.npm.client.NpmHostedRepository;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.sonatype.sisu.goodies.testsupport.hamcrest.FileMatchers.exists;

/**
 * Rebuild NPM metadata.
 */
public class RebuildNpmMetadataIT
    extends NpmITSupport
{
  public RebuildNpmMetadataIT(String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void rebuildNpmMetadata() throws Exception {
    final NpmHostedRepository hostedRepository = createNpmHostedRepository(testMethodName());
    final File hostRepositoryStorage = new File(nexus().getWorkDirectory(), "storage/" + hostedRepository.id());
    FileUtils.copyFile(testData().resolveFile("commonjs-0.0.1.tgz"),
        new File(hostRepositoryStorage, "commonjs/-/commonjs-0.0.1.tgz"), false);
    FileUtils.copyFile(testData().resolveFile("uppercase-0.1.1.tgz"),
        new File(hostRepositoryStorage, "uppercase/-/uppercase-0.1.1.tgz"), false);
    FileUtils.copyFile(testData().resolveFile("boxeen-0.1.0.tgz"),
        new File(hostRepositoryStorage, "boxeen/-/boxeen-0.1.0.tgz"), false);

    final Scheduler scheduler = client().getSubsystem(Scheduler.class);
    scheduler.run("NpmHostedMetadataRebuildTask", null);
    scheduler.waitForAllTasksToStop();

    final File localDirectory = util.createTempDir();
    // download package root of commonjs
    {
      final File packageRootFile = new File(localDirectory, "commonjs.json");
      content().download(Location.repositoryLocation(testMethodName(), "commonjs"), packageRootFile);
      assertThat(packageRootFile, exists());
      final String packageRootString = Files.toString(packageRootFile, Charsets.UTF_8);

      assertThat(packageRootString, containsString("commonjs"));
      assertThat(packageRootString, containsString("0.0.1"));
      assertThat(packageRootString, containsString("x-nx-rebuilt"));
    }

    // download package root of uppercase
    {
      final File packageRootFile = new File(localDirectory, "uppercase.json");
      content().download(Location.repositoryLocation(testMethodName(), "uppercase"), packageRootFile);
      assertThat(packageRootFile, exists());
      final String packageRootString = Files.toString(packageRootFile, Charsets.UTF_8);

      assertThat(packageRootString, containsString("uppercase"));
      assertThat(packageRootString, containsString("0.1.1"));
      assertThat(packageRootString, containsString("x-nx-rebuilt"));
    }

    // download package root of boxeen (that contains invalid UTF character)
    {
      final File packageRootFile = new File(localDirectory, "boxeen.json");
      content().download(Location.repositoryLocation(testMethodName(), "boxeen"), packageRootFile);
      assertThat(packageRootFile, exists());
      final String packageRootString = Files.toString(packageRootFile, Charsets.UTF_8);

      assertThat(packageRootString, containsString("boxeen"));
      assertThat(packageRootString, containsString("0.1.0"));
      assertThat(packageRootString, containsString("Sérgio Ramos")); // invalid é
      assertThat(packageRootString, containsString("x-nx-rebuilt"));
      assertThat(nexus().getNexusLog(), FileMatchers.contains("boxeen-0.1.0.tgz contains non-UTF package.json, parsing as ISO-8859-1:"));
    }
  }
}