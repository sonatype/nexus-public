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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogMessage;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.ExecCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.ImageInfo;
import com.spotify.docker.client.messages.PortBinding;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.spotify.docker.client.DockerClient.ExecCreateParam.attachStderr;
import static com.spotify.docker.client.DockerClient.ExecCreateParam.attachStdin;
import static com.spotify.docker.client.DockerClient.ExecCreateParam.attachStdout;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang.StringUtils.left;

/**
 * Support class for helping to managing Docker Containers.
 *
 * @since 3.6.1
 */
public class DockerContainerClient
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  private static final String P1_KEEP_ALIVE = "while :; do sleep 1; done";

  private static final int SHORT_ID_LENGTH = 12;

  private final Object reuseStartedContainerLock = new Object[0];

  private final Object clearStartedContainerLock = new Object[0];

  private final DockerClient dockerClient;

  private final String image;

  private final List<String> env;

  private final HostConfig hostConfig;

  private ContainerInfo startedContainer;

  /**
   * Constructor that will use a default configuration based on {@link DockerContainerConfig}.
   *
   * @param image name of image to use, can include tag. For example, centos:7
   */
  public DockerContainerClient(final String image) {
    this(DockerContainerConfig.builder().image(image).build());
  }

  /**
   * Constructor.
   *
   * @param dockerContainerConfig configuration on how to handle this {@link DockerContainerClient}
   */
  public DockerContainerClient(DockerContainerConfig dockerContainerConfig) {
    this.dockerClient = dockerContainerConfig.getDockerClientBuilder().build();
    this.image = dockerContainerConfig.getImage();
    this.env = dockerContainerConfig.getEnv();
    this.hostConfig = dockerContainerConfig.getHostConfigBuilder().build();

    logDockerInfo();
  }

  /**
   * Pull a docker container image.
   *
   * @return Optional of {@link ImageInfo}
   */
  public Optional<ImageInfo> pull() {
    return pullAndInspect();
  }

  /**
   * Runs a docker container for given image. This method will pull the image that this container
   * is supposed to run for, if it's not already existing. Additionally the method will
   * assure that the containers process one will be running until stopped or killed.
   *
   * @return Optional of {@link ContainerInfo}, present if container was run by docker.
   */
  public Optional<ContainerInfo> run() {
    return runAndPullIfNotExist(P1_KEEP_ALIVE);
  }

  /**
   * Runs a docker container for given image. This method will pull the image that this container
   * is supposed to run for, if it's not already existing. Additionally the method allows the caller
   * to pass commands to the docker run command. Unless provided in the commands to run
   * the container will stop immediately after it as run, just as normal docker behavior.
   *
   * @param commands to be run for docker container
   * @return Optional of {@link ContainerInfo}, present if container was run by docker.
   */
  public Optional<ContainerInfo> run(final String commands) {
    return runAndPullIfNotExist(commands);
  }

  /**
   * Execute commands on a docker container for given image. The main difference between this and
   * {@link #exec(String, OutputStream)} is that we provide our own {@link OutputStream}
   * and return a {@link List} of {@link String}s that represents the output of the execution
   * of the given commands.
   *
   * @param commands to be executed within docker container
   * @return Optional of a {@link List} of {@link String}s representing command line output,
   * present if execution of commands was successful
   * @see #exec(String, OutputStream)
   */
  public Optional<List<String>> exec(final String commands) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    if (exec(commands, outputStream)) {
      try {
        return of(asList(outputStream.toString(UTF_8.name()).split("\\r?\\n")));
      }
      catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }

    return empty();
  }

  /**
   * Execute commands on a docker container for given image. Also see {@link #run()} for further understanding on
   * how the underlying container was run. The main implication of {@link #run()} is that consecutive calls to
   * this will use the last run container. I.e. previous executed commands are "persisted" until container
   * is stopped or killed.
   *
   * @param commands     to be executed within docker container
   * @param outputStream to stream command line output to (stdout and stderr)
   * @return true if execution was successful, false otherwise.
   * @see #run()
   */
  public boolean exec(final String commands, final OutputStream outputStream) {
    return run(P1_KEEP_ALIVE).map(containerInfo -> exec(containerInfo, commands, outputStream)).orElse(false);
  }

  /**
   * Close all resources for the underlying {@link DockerClient} and kills and removes any containers
   * that were started, run and executed upon by this instance.
   */
  public void close() {
    killAndRemoveStartedContainer();
    dockerClient.close();
  }

  /**
   * Download a container path to a local {@link File} location. This method will use the last running
   * container if possible.
   *
   * @param fromContainerPath - the path in the container to download
   * @param toLocal           - the path of the local file system to download to
   * @return Optional {@link Set} of {@link File}s that were downloaded locally.
   */
  public Optional<Set<File>> download(final String fromContainerPath, final File toLocal) {
    return run(P1_KEEP_ALIVE).map(containerInfo -> download(containerInfo, fromContainerPath, toLocal));
  }

  /**
   * Retrieve the host ports that were opened at runtime for communicating with the docker container. This method
   * is only useful to call after the container is started as the host ports are not known before.
   *
   * @return Optional {@link Map} of container port keys with a {@link List} of {@link PortBinding}s
   */
  public Optional<Map<String, List<PortBinding>>> hostPortBindings() {
    // reuse existing containers if they are running
    if (nonNull(startedContainer)) {
      try {
        log.info("Inspecting container '{}' for host ports.", left(startedContainer.id(), SHORT_ID_LENGTH));

        return ofNullable(dockerClient.inspectContainer(startedContainer.id()).networkSettings().ports());
      }
      catch (DockerException | InterruptedException e) {
        log.error("Unable to inspect container for host ports '{}'", left(startedContainer.id(), SHORT_ID_LENGTH), e);
      }
    }

    return empty();
  }

  private void logDockerInfo() {
    try {
      log.info("Docker version {} on host {}", dockerClient.version(), dockerClient.getHost());
    }
    catch (DockerException | InterruptedException e) {
      log.error("Failed to log Docker information", e);
    }
  }

  private boolean exec(final ContainerInfo container,
                       final String commands,
                       final OutputStream outputStream)
  {
    String containerId = container.id();
    String shortId = left(containerId, SHORT_ID_LENGTH);

    log.info("Attempting to exec commands '{}' in container '{}' for image '{}'", commands, shortId, image);

    try {
      // attach stdin as well as stdout/stderr to workaround https://github.com/spotify/docker-client/issues/513
      final ExecCreation execCreation = dockerClient
          .execCreate(containerId, cmd(commands), attachStdin(), attachStdout(), attachStderr());

      try (final LogStream stream = dockerClient.execStart(execCreation.id())) {
        // pretend to be a command line, by printing command to run
        log.debug("$ " + commands);

        // Why read each, instead attaching to out and err stream? Mostly because
        // Logstream preserves order of written out and err if/when they get written.
        stream.forEachRemaining(logMessage -> write(outputStream, logMessage));
      }

      log.info("Successfully exec commands '{}' in container '{}' for image '{}'", commands, shortId, image);

      return true;
    }
    catch (DockerException | InterruptedException e) {
      log.error("Failed to exec commands '{}' in container '{}' for image '{}'", commands, shortId, image, e);
    }

    return false;
  }

  private void write(final OutputStream outputStream, final LogMessage logMessage) {
    try {
      String lines = UTF_8.decode(logMessage.content()).toString();
      outputStream.write(lines.getBytes(UTF_8.name()));

      // pretend to be a command line, by printing results
      for (String line : lines.split("\\r?\\n")) {
        if (line.length() > 0) {
          log.trace(line);
        }
      }
    }
    catch (IOException ignore) { // NOSONAR
      // we don't care, let loop continue
    }
  }

  private Optional<ContainerInfo> runAndPullIfNotExist(String commands) {
    // assure that we don't have multiple threads set the started container
    synchronized (reuseStartedContainerLock) {
      // reuse existing containers if they are running
      if (nonNull(startedContainer)) {
        log.info("Using existing container '{}' for image '{}'", left(startedContainer.id(), SHORT_ID_LENGTH), image);
        return of(startedContainer);
      }

      if (pullIfNotExist().isPresent()) {
        try {
          ContainerCreation container = dockerClient
              .createContainer(ContainerConfig
                  .builder()
                  .hostConfig(hostConfig)
                  .exposedPorts(hostConfig.portBindings().keySet())
                  .image(image)
                  .env(env)
                  .cmd(cmd(commands))
                  .build());

          String containerId = container.id();
          String shortId = left(containerId, SHORT_ID_LENGTH);

          startedContainer = dockerClient.inspectContainer(containerId);

          log.info("Attempting to run container '{}' with commands '{}' for image '{}'", shortId, commands, image);

          dockerClient.startContainer(containerId);

          log.info("Successfully run container '{}' with commands '{}' for image '{}'", shortId, commands, image);

          return of(startedContainer);
        }
        catch (Exception e) {
          log.error("Failed to run container for image '{}'", image, e);
        }
      }

      return empty();
    }
  }

  private Set<File> download(final ContainerInfo containerInfo, final String fromContainerPath, final File toLocal) {
    String containerId = containerInfo.id();
    String shortId = left(containerId, SHORT_ID_LENGTH);

    ImmutableSet.Builder<File> files = ImmutableSet.builder();

    log.info("Attempting to download from path '{}' in container '{}' for image '{}'",
        fromContainerPath, shortId, image);

    try (final TarArchiveInputStream tarStream = new TarArchiveInputStream(
        dockerClient.archiveContainer(containerId, fromContainerPath))) {

      TarArchiveEntry entry;
      while ((entry = tarStream.getNextTarEntry()) != null) {
        log.info("Downloading entry '{}' in container '{}' for image '{}'", entry.getName(), shortId, image);

        String entryName = entry.getName();
        entryName = entryName.substring(entryName.indexOf('/') + 1);
        if (entryName.isEmpty()) {
          continue;
        }

        File file = (toLocal.exists() && toLocal.isDirectory()) ? new File(toLocal, entryName) : toLocal;
        files.add(file);

        try (OutputStream outStream = new FileOutputStream(file)) {
          copy(tarStream, outStream);
        }
      }
    }
    catch (Exception e) {
      log.error("Failed to download from path '{}' in container '{}' for image '{}'",
          fromContainerPath, shortId, image, e);
    }

    return files.build();
  }

  private void killAndRemoveStartedContainer() {
    // assure that we do not have multiple threads clearing the started container at once.
    synchronized (clearStartedContainerLock) {
      if (nonNull(startedContainer)) {
        String containerId = startedContainer.id();
        String shortId = left(containerId, SHORT_ID_LENGTH);

        startedContainer = null;

        try {
          log.info("Attempting to kill and remove container '{}' for image '{}'", shortId, image);

          dockerClient.killContainer(containerId);
          dockerClient.removeContainer(containerId);

          log.info("Successfully killed and removed container '{}' for image '{}'", shortId, image);
        }
        catch (DockerException | InterruptedException e) { // NOSONAR
          log.error("Failed to kill and/or remove container '{}'", shortId, log.isDebugEnabled() ? e : null);
        }
      }
    }
  }

  private Optional<ImageInfo> pullIfNotExist() {
    return ofNullable(inspectImage().orElseGet(() -> {
      log.info("Image '{}' is not local, attempting to pull", image);
      return pull().orElse(null);
    }));
  }

  private Optional<ImageInfo> pullAndInspect() {
    try {
      dockerClient.pull(image);

      return inspectImage();
    }
    catch (DockerException | InterruptedException e) {
      log.error("Failed to pull docker image '{}'", image, e);
    }

    return empty();
  }

  private Optional<ImageInfo> inspectImage() {
    try {
      return ofNullable(dockerClient.inspectImage(image));
    }
    catch (DockerException | InterruptedException e) {
      log.warn("Unable to inspect image '{}'", image);
      log.debug("Exception is : ", e);
    }

    return empty();
  }

  private String[] cmd(String commands) {
    return nonNull(commands) ? new String[]{"sh", "-c", commands} : new String[]{};
  }
}
