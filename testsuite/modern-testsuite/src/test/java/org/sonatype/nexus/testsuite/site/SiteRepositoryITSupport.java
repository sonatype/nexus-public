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
package org.sonatype.nexus.testsuite.site;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.subsystem.content.Content;
import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;

import static org.sonatype.sisu.filetasks.builder.FileRef.file;

public abstract class SiteRepositoryITSupport
    extends NexusRunningParametrizedITSupport
{

  public SiteRepositoryITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return configuration.addPlugins(
        artifactResolver().resolvePluginFromDependencyManagement(
            "org.sonatype.nexus.plugins", "nexus-site-repository-plugin"
        )
    );
  }

  protected Repositories repositories() {
    return client().getSubsystem(Repositories.class);
  }

  protected void copySiteContentToRepository(final String sitePath, final String repositoryId) {
    tasks().copy().directory(file(testData().resolveFile(sitePath)))
        .to().directory(file(new File(nexus().getWorkDirectory(), "storage/" + repositoryId)))
        .run();
  }

  protected ClientResponse getStatusOf(final String uri) {
    return ((JerseyNexusClient) client()).uri(uri).get(ClientResponse.class);
  }

  protected String repositoryIdForTest() {
    String methodName = testName.getMethodName();
    if (methodName.contains("[")) {
      return methodName.substring(0, methodName.indexOf("["));
    }
    return methodName;
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

  protected File downloadFromSite(final String repositoryId, final String path)
      throws IOException
  {
    final File downloaded = new File(testIndex().getDirectory("downloads"), path);
    client().getSubsystem(Content.class).download(new Location(repositoryId, path), downloaded);
    return downloaded;
  }

}
