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
package com.sonatype.nexus.docker.testsupport.framework;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.Mutex;
import org.sonatype.nexus.common.net.PortAllocator;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang.StringUtils.left;
import static org.sonatype.nexus.common.text.Strings2.notBlank;
import static org.testcontainers.containers.BindMode.READ_WRITE;

/**
 * Support class for helping to managing Docker Containers.
 */
public class DockerContainerClient
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private static final String KEEP_ALIVE = "while true; do sleep 1; done";

  private static final int SHORT_ID_LENGTH = 12;

  private final Mutex lock = new Mutex();

  private final DockerContainerConfig config;

  private GenericContainer<?> dockerClient;

  private InspectContainerResponse startedContainer;

  public DockerContainerClient(final String image) {
    this(DockerContainerConfig.builder(image).build());
  }

  public DockerContainerClient(final DockerContainerConfig config) {
    this.config = checkNotNull(config);
  }

  /**
   * Runs a docker container for a given image. This method will pull the image that this container is supposed to run
   * for, if it's not already existing.
   */
  public void run() {
    runAndPullIfNotExist(null);
  }

  /**
   * Runs a docker container for a given image. This method will pull the image that this container is supposed to run
   * for, if it's not already existing. Additionally, the method will assure that the containers process one will be
   * running until stopped or killed.
   */
  public void runAndKeepAlive() {
    runAndPullIfNotExist(KEEP_ALIVE);
  }

  /**
   * Runs a docker container for given image. This method will pull the image that this container is supposed to run
   * for, if it's not already existing. Additionally, the method allows the caller to pass commands to the docker run
   * command. Unless provided in the commands to run the container will stop immediately after it as run, just as normal
   * docker behavior.
   *
   * @param commands to be run for docker container, can be {@code null}.
   */
  public void run(@Nullable final String commands) {
    runAndPullIfNotExist(commands);
  }

  /**
   * Execute commands on a docker container for a given image.
   *
   * @param commands to be executed within docker container.
   * @return results from a "docker exec" command.
   */
  public Optional<ExecResult> exec(final String commands) {
    run(KEEP_ALIVE);
    return execInDocker(commands);
  }

  /**
   * Close all resources for the underlying {@link GenericContainer} and kills and removes any containers that were
   * started, run and executed upon by this instance.
   */
  public void close() {
    if (dockerClient != null && dockerClient.isRunning()) {
      dockerClient.stop();
    }
  }

  /**
   * Download a container path to a local {@link File} location. This method will use the last running container if
   * possible.
   *
   * @param fromContainerPath the path in the container to download
   * @param toLocal           the path of the local file system to download to
   */
  public void download(final String fromContainerPath, final File toLocal) {
    run(KEEP_ALIVE);
    dockerClient.copyFileFromContainer(fromContainerPath, toLocal.getAbsolutePath());
  }

  private Optional<ExecResult> execInDocker(final String commands)
  {
    String image = config.getImage();
    if (startedContainer == null) {
      log.warn("Attempting to exec commands '{}' for image '{}' which is not started", commands, image);
      return Optional.empty();
    }

    String containerId = startedContainer.getId();
    String shortId = left(containerId, SHORT_ID_LENGTH);

    log.info("Attempting to exec commands '{}' in container '{}' for image '{}'", commands, shortId, image);

    try {
      ExecResult execResult = dockerClient.execInContainer(cmd(commands));
      log.debug("$ {}", commands);
      String stderr = execResult.getStderr();

      log.debug("Output of command '{}' in container '{}' for image '{}' was:\n{}",
          commands, shortId, image, execResult);
      if (!stderr.isEmpty() && execResult.getExitCode() != 0) {
        log.error("Failed exec commands '{}' in container '{}' for image '{}'. Error message: {}",
            commands, shortId, image, stderr);
      }
      else {
        log.info("Successfully exec commands '{}' in container '{}' for image '{}'", commands, shortId, image);
      }

      return Optional.of(execResult);
    }
    catch (IOException | InterruptedException e) { // NOSONAR
      log.error("Failed to exec commands '{}' in container '{}' for image '{}'", commands, shortId, image, e);
    }

    return Optional.empty();
  }

  private void runAndPullIfNotExist(@Nullable final String commands) {
    String image = config.getImage();
    Path dockerfile = config.getDockerfile();
    // assure that we don't have multiple threads set the started container
    synchronized (lock) {
      // reuse existing containers if they are running
      if (nonNull(startedContainer) && dockerClient.isRunning()) {
        String shortDockerId = left(startedContainer.getId(), SHORT_ID_LENGTH);
        String msg = image != null ?
            String.format("image '%s'", image) : String.format("Dockerfile '%s'", dockerfile);
        log.info("Using existing container '{}' for {}", shortDockerId, msg);
        return;
      }
      if (log.isInfoEnabled()) {
        log.info(buildLogMessage("Attempting to run container", image, dockerfile, commands));
      }

      // Build the docker image based on the name or the Dockerfile
      dockerClient = image != null ? new GenericContainer<>(image) :
          new GenericContainer<>(new ImageFromDockerfile().withDockerfile(dockerfile));

      dockerClient.setCommand(cmd(commands));
      config.getEnv().forEach((key, value) -> dockerClient.addEnv(key, value));
      config.getPathBinds().forEach((key, value) -> dockerClient.addFileSystemBind(key, value, READ_WRITE));
      if (!config.getExposedPorts().isEmpty()) {
        List<String> portBindings = config.getExposedPorts().stream()
            // hostPort:containerPort
            .map(port -> PortAllocator.nextFreePort() + ":" + port)
            .collect(Collectors.toList());
        dockerClient.setPortBindings(portBindings);
        dockerClient.setWaitStrategy(Wait.forListeningPort());
      }
      if (notBlank(config.getWorkingDir())) {
        dockerClient.setWorkingDirectory(config.getWorkingDir());
      }

      // Work around for an issue in ITs which results in Testcontainers using the wrong class loader
      ClassLoader threadLoader = Thread.currentThread().getContextClassLoader();
      try {
        Thread.currentThread().setContextClassLoader(GenericContainer.class.getClassLoader());
        dockerClient.start();
      }
      finally {
        if (threadLoader != null) {
          Thread.currentThread().setContextClassLoader(threadLoader);
        }
      }
      startedContainer = dockerClient.getContainerInfo();

      String containerId = startedContainer.getId();
      String shortId = left(containerId, SHORT_ID_LENGTH);

      if (log.isInfoEnabled()) {
        log.info(buildLogMessage("Successfully run container '" + shortId + "'", image, dockerfile, commands));
      }
    }
  }

  private static String buildLogMessage(
      final String message,
      final @Nullable String image,
      final @Nullable Path dockerfile,
      final @Nullable String commands)
  {
    StringBuilder msg = new StringBuilder(message);
    if (commands != null) {
      msg.append(" with commands '").append(commands).append("'");
    }
    if (image != null) {
      msg.append(" for image '").append(image).append("'");
    }
    if (dockerfile != null) {
      msg.append(" for Dockerfile '").append(dockerfile).append("'");
    }

    return msg.toString();
  }

  private String[] cmd(String commands) {
    return nonNull(commands) ? new String[] {"/bin/sh", "-c", commands} : new String[] {};
  }

  public Integer getMappedPort(final String containerPort) {
    return dockerClient.getMappedPort(Integer.parseInt(containerPort));
  }
}
