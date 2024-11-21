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
package com.sonatype.nexus.docker.testsupport.maven;

import java.util.List;
import java.util.Optional;

import com.sonatype.nexus.docker.testsupport.ContainerCommandLineITSupport;
import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;

/**
 * @since 3.16
 */
public class MavenCommandLineITSupport
    extends ContainerCommandLineITSupport
{
  public static final String SETTINGS_XML_PATH = "/root/.m2/settings.xml";

  private static final String MVN = "mvn ";

  private static final String CD = "cd ";

  public MavenCommandLineITSupport(final DockerContainerConfig dockerContainerConfig) {
    super(dockerContainerConfig);
  }

  public Optional<List<String>> cleanInstall(
      final String directory,
      final String url)
  {
    return cleanInstall(directory, url, randomUUID().toString(), randomUUID().toString(), "1.0.0");
  }

  public Optional<List<String>> cleanInstall(
      final String directory,
      final String repositoryUrl,
      final String groupId,
      final String artifactId,
      final String version)
  {
    updatePom(directory, repositoryUrl, groupId, artifactId, version);

    return exec(CD + directory + " && " + MVN + "clean install");
  }

  public Optional<List<String>> deploy(
      final String directory,
      final String repositoryUrl,
      final String groupId,
      final String artifactId,
      final String version)
  {
    updatePom(directory, repositoryUrl, groupId, artifactId, version);

    return exec(CD + directory + " && " + MVN + "clean deploy -Dmaven.test.skip=true");
  }

  private void updatePom(
      final String directory,
      final String repositoryUrl,
      final String groupId,
      final String artifactId,
      final String version)
  {
    String pomTemplate = directory + "/pom-template.xml";
    String pom = directory + "/pom.xml";

    String sedCommand = format("sed 's/${project.artifactId}/%s/g' %s" +
        "| sed 's/${project.groupId}/%s/g'" +
        "| sed 's/${project.version}/%s/g'" +
        "| sed 's,${deploy.url},%s,g'" +
        "| sed 's,${site.url},%s,g'" +
        " > %s", artifactId, pomTemplate, groupId, version, repositoryUrl, repositoryUrl, pom);

    exec(sedCommand);
  }
}
