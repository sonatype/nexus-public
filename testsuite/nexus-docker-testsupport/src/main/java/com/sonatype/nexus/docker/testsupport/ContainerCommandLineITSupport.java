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
package com.sonatype.nexus.docker.testsupport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sonatype.nexus.docker.testsupport.framework.DockerContainerClient;
import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig;

import org.sonatype.goodies.common.ComponentSupport;

import org.testcontainers.containers.Container.ExecResult;

import static java.util.Arrays.asList;
import static org.sonatype.nexus.common.text.Strings2.notBlank;

/**
 * Abstract implementation of {@link CommandLine} to allow the sharing of commonalities between
 * Docker Container Command lines.
 */
public abstract class ContainerCommandLineITSupport
    extends ComponentSupport
    implements CommandLine
{
  protected DockerContainerClient dockerContainerClient;

  /**
   * Constructor. Uses default {@link DockerContainerConfig}
   *
   * @param image name of image to use, can include tag. For example, centos:7
   * @see ContainerCommandLineITSupport#ContainerCommandLineITSupport(DockerContainerConfig)
   */
  protected ContainerCommandLineITSupport(final String image) {
    this(DockerContainerConfig.builder(image).build());
  }

  /**
   * Constructor that creates and run the container with the corresponding commands based on provided configuration.
   *
   * @param dockerContainerConfig parameters to run a container.
   * @param commands to be run for docker container.
   */
  protected ContainerCommandLineITSupport(final DockerContainerConfig dockerContainerConfig, final String commands) {
    dockerContainerClient = new DockerContainerClient(dockerContainerConfig);
    dockerContainerClient.run(commands);
  }

  /**
   * Constructor that creates and run the container based on provided configuration.
   *
   * @param dockerContainerConfig parameters to run a container.
   */
  protected ContainerCommandLineITSupport(final DockerContainerConfig dockerContainerConfig) {
    dockerContainerClient = new DockerContainerClient(dockerContainerConfig);
    dockerContainerClient.runAndKeepAlive();
  }

  @Override
  public void init() {
    // no-op
  }

  @Override
  public void exit() {
    dockerContainerClient.close();
  }

  @Override
  public Optional<List<String>> exec(final String s) {
    Optional<ExecResult> execResult = dockerContainerClient.exec(s);
    if (execResult.isPresent()) {
      List<String> output = new ArrayList<>();
      // we need all logs from the container
      ExecResult result = execResult.get();
      String stdout = result.getStdout();
      if (notBlank(stdout)) {
        output.addAll(asList(stdout.split("\\r?\\n")));
      }
      String stderr = result.getStderr();
      if (notBlank(stderr)) {
        output.addAll(asList(stderr.split("\\r?\\n")));
      }
      return Optional.of(output);
    }

    return Optional.empty();
  }

  @Override
  public void download(final String fromContainerPath, final File toLocal) {
    dockerContainerClient.download(fromContainerPath, toLocal);
  }

  @Override
  public Integer getHostTcpPort(final String containerPort) {
    return dockerContainerClient.getMappedPort(containerPort);
  }
}
