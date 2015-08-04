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
package org.sonatype.nexus.testsuite.unpack;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.security.User;
import org.sonatype.nexus.client.core.subsystem.security.Users;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;
import org.sonatype.sisu.litmus.testsupport.hamcrest.FileMatchers;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.sonatype.sisu.filetasks.builder.FileRef.file;

/**
 * @since 2.5.1
 */
@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public abstract class UnpackITSupport
    extends NexusRunningParametrizedITSupport
{

  protected static final String PASSWORD = "secret";

  public UnpackITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return configuration.addPlugins(
        artifactResolver().resolvePluginFromDependencyManagement(
            "org.sonatype.nexus.plugins", "nexus-unpack-plugin"
        )
    );
  }

  protected Repositories repositories() {
    return client().getSubsystem(Repositories.class);
  }

  protected Users users() {
    return client().getSubsystem(Users.class);
  }

  protected File executeMaven(final String projectName, final String repositoryId, final String... goals)
      throws VerificationException
  {
    final File projectToBuildSource = testData().resolveFile(projectName);
    final File mavenSettingsSource = testData().resolveFile("settings.xml");

    final File projectToBuildTarget = testIndex().getDirectory("maven/" + projectName);
    final File mavenSettingsTarget = new File(testIndex().getDirectory("maven"), "settings.xml");

    final Properties properties = new Properties();
    properties.setProperty("nexus-base-url", nexus().getUrl().toExternalForm());
    properties.setProperty("nexus-repository-id", repositoryId);

    tasks().copy().directory(file(projectToBuildSource))
        .filterUsing(properties)
        .to().directory(file(projectToBuildTarget)).run();
    tasks().copy().file(file(mavenSettingsSource))
        .filterUsing(properties)
        .to().file(file(mavenSettingsTarget)).run();

    final File mavenHome = util.resolveFile("target/apache-maven-3.0.4");
    final File localRepo = util.resolveFile("target/apache-maven-local-repository");

    tasks().chmod(file(new File(mavenHome, "bin"))).include("mvn").permissions("755").run();

    System.setProperty("maven.home", mavenHome.getAbsolutePath());
    final Verifier verifier = new Verifier(projectToBuildTarget.getAbsolutePath(), false);
    verifier.setAutoclean(true);

    verifier.setLocalRepo(localRepo.getAbsolutePath());
    verifier.setMavenDebug(true);
    verifier.setCliOptions(Arrays.asList("-s " + mavenSettingsTarget.getAbsolutePath()));

    verifier.resetStreams();

    verifier.setLogFileName("maven.log");
    verifier.executeGoals(Arrays.asList(goals));
    verifier.verifyErrorFreeLog();
    testIndex().recordLink(
        verifier.getLogFileName(), new File(projectToBuildTarget, verifier.getLogFileName())
    );

    return projectToBuildTarget;
  }

  protected static String uniqueName(final String prefix) {
    return prefix + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
  }

  protected void upload(final NexusClient client, final String repositoryId, final File target, final String path,
                        final boolean useDeleteFlag)
      throws IOException
  {
    final JerseyNexusClient jerseyNexusClient = (JerseyNexusClient) client;
    try {
      jerseyNexusClient
          .uri(
              "service/local/repositories/" + repositoryId + "/content-compressed"
                  + (path != null ? "/" + path : "")
                  + (useDeleteFlag ? "?delete" : "")
          )
          .put(target);
    }
    catch (UniformInterfaceException e) {
      throw jerseyNexusClient.convert(e);
    }
    catch (ClientHandlerException e) {
      throw jerseyNexusClient.convert(e);
    }
  }

  protected void assertFilesPresentOnStorage(final String repositoryId,
                                             final boolean shouldExist,
                                             final String... fileNames)
      throws Exception
  {
    File repositoryRootDirectory = new File(nexus().getWorkDirectory(), "storage/" + repositoryId);

    for (String fileName : fileNames) {
      if (shouldExist) {
        assertThat(new File(repositoryRootDirectory, fileName), FileMatchers.exists());
      }
      else {
        assertThat(new File(repositoryRootDirectory, fileName), not(FileMatchers.exists()));
      }
    }
  }

  protected User createUser() {
    return users().create(uniqueName("unpack"))
        .withFirstName(testMethodName())
        .withLastName("Bithub")
        .withEmail(testMethodName() + "@sonatype.com")
        .withPassword(PASSWORD)
        .withRole("nx-deployment")
        .withRole("repository-any-full")
        .save();
  }

}
