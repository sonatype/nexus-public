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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;

/**
 * Configuration object for the Docker Container.
 */
public class DockerContainerConfig
{
  private final String image;

  private final Path dockerfile;

  private List<Integer> exposedPorts;

  /**
   * environment variable key value to environment variable value
   */
  private Map<String, String> env;

  /**
   * host path to container path
   */
  private Map<String, String> pathBinds;

  private String workingDir;

  private DockerContainerConfig(@Nullable final String image, @Nullable final Path dockerfile) {
    checkArgument(!(image == null && dockerfile == null), "Image name or Dockerfile should be presented");
    checkArgument(!(image != null && dockerfile != null), "Image name and Dockerfile should not be presented both");
    this.image = image;
    this.dockerfile = dockerfile;
  }

  public String getImage() {
    return image;
  }

  public Path getDockerfile() {
    return dockerfile;
  }

  public List<Integer> getExposedPorts() {
    return exposedPorts;
  }

  public Map<String, String> getEnv() {
    return env;
  }

  public Map<String, String> getPathBinds() {
    return pathBinds;
  }

  public String getWorkingDir() {
    return workingDir;
  }

  public static Builder builder(final String image) {
    return new Builder(image);
  }

  public static Builder builder(final Path dockerfile) {
    return new Builder(dockerfile);
  }

  public static final class Builder
  {
    private String image;

    private Path dockerfile;

    private List<Integer> exposedPorts = new ArrayList<>();

    private Map<String, String> env = new HashMap<>();

    private Map<String, String> pathBinds = new HashMap<>();

    private String workingDir;

    private Builder(final String image) {
      this.image = checkNotNull(image);
    }

    private Builder(final Path dockerfile) {
      this.dockerfile = checkNotNull(dockerfile);
    }

    public Builder withExposedPort(final String exposedPort) {
      this.exposedPorts = singletonList(Integer.parseInt(exposedPort));
      return this;
    }

    public Builder withExposedPorts(final List<String> exposedPorts) {
      this.exposedPorts = exposedPorts.stream().map(Integer::parseInt).collect(Collectors.toList());
      return this;
    }

    public Builder withEnv(final Map<String, String> env) {
      this.env = env;
      return this;
    }

    public Builder withPathBinds(final Map<String, String> pathBinds) {
      this.pathBinds = pathBinds;
      return this;
    }

    public Builder withWorkingDir(final String workingDir) {
      this.workingDir = workingDir;
      return this;
    }

    public DockerContainerConfig build() {
      DockerContainerConfig dockerContainerConfig = new DockerContainerConfig(image, dockerfile);
      dockerContainerConfig.pathBinds = this.pathBinds;
      dockerContainerConfig.exposedPorts = this.exposedPorts;
      dockerContainerConfig.workingDir = this.workingDir;
      dockerContainerConfig.env = this.env;
      return dockerContainerConfig;
    }
  }
}
