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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.sonatype.nexus.docker.testsupport.framework.DockerContainerClient;
import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig;

import com.spotify.docker.client.messages.PortBinding;

import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Abstract implementation of {@link CommandLine} to allow the sharing of commonalities
 * between Docker Container Command lines.
 *
 * @since 3.6.1
 */
public abstract class ContainerCommandLineITSupport
    implements CommandLine
{
  private static final String CMD_LS = "ls ";

  protected DockerContainerClient dockerContainerClient;

  /**
   * Constructor. Uses default {@link DockerContainerConfig}
   *
   * @param image name of image to use, can include tag. For example, centos:7
   * @see ContainerCommandLineITSupport#ContainerCommandLineITSupport(DockerContainerConfig)
   */
  protected ContainerCommandLineITSupport(final String image) {
    this(DockerContainerConfig.builder().image(image).build());
  }

  /**
   * @param dockerContainerConfig {@link DockerContainerConfig}
   * @param commands              to be run for docker container
   * @see ContainerCommandLineITSupport#ContainerCommandLineITSupport(DockerContainerConfig)
   */
  protected ContainerCommandLineITSupport(DockerContainerConfig dockerContainerConfig, final String commands) {
    dockerContainerClient = new DockerContainerClient(dockerContainerConfig);
    dockerContainerClient.run(commands);
  }

  /**
   * Constructor that creates the {@link DockerContainerClient} to be used as the
   * underlying client to run commands on. It additionally runs the {@link #init()} method
   * to allow implementers to assure that certain setup would have been done before allowing
   * of actual commands to be executed.
   *
   * @param dockerContainerConfig {@link DockerContainerConfig}
   */
  protected ContainerCommandLineITSupport(DockerContainerConfig dockerContainerConfig) {
    dockerContainerClient = new DockerContainerClient(dockerContainerConfig);
    dockerContainerClient.run();
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
    return dockerContainerClient.exec(s);
  }

  @Override
  public Optional<Set<File>> download(final String fromContainerPath, final File toLocal) {
    return dockerContainerClient.download(fromContainerPath, toLocal);
  }

  @Override
  public Optional<Map<String, List<PortBinding>>> hostPortBindings() {
    return dockerContainerClient.hostPortBindings();
  }

  @Override
  @Nullable
  public String getHostTcpPort(final String containerPort) {
    Map<String, List<PortBinding>> hostPortBindings = hostPortBindings().orElse(emptyMap());
    List<PortBinding> portBindings = hostPortBindings.get(containerPort + "/tcp");

    if (isNull(portBindings) || portBindings.isEmpty()) {
      // be kind and attempt to find without appendix of tcp
      portBindings = hostPortBindings.get(containerPort);
    }

    if (nonNull(portBindings) && !portBindings.isEmpty()) {
      PortBinding portBinding = portBindings.get(0);

      if(nonNull(portBinding)) {
        return portBinding.hostPort();
      }
    }

    return null;
  }

  /**
   * Run ls command.
   *
   * @param arguments that are allowed for the ls command
   * @see #exec(String)
   */
  public Optional<List<String>> ls(String arguments) {
    return exec(CMD_LS + arguments);
  }
}
