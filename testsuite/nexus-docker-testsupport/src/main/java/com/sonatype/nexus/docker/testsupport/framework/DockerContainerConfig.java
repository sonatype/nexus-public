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

import java.util.ArrayList;
import java.util.List;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.messages.HostConfig;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.spotify.docker.client.messages.PortBinding.randomPort;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toMap;

/**
 * Configuration object for Docker Container. Creation is available through it's {@link #builder()}.
 *
 * The main objective of this configuration object is to provide a high amount of default behavior so that
 * implementers of Docker Clients can focus on the Clients rather than docker configuration.
 *
 * @since 3.6.1
 */
public class DockerContainerConfig
{
  public static final String[] PORT_MAPPING_PORTS = new String[]{"22", "80", "443"}; // NOSONAR

  public static final String PORT_MAPPING_IP = "0.0.0.0"; // NOSONAR

  private static final String LATEST_TAG = "latest";

  private String image;

  private List<String> env;

  private HostConfig.Builder hostConfigBuilder;

  private DefaultDockerClient.Builder dockerClientBuilder;

  private DockerContainerConfig(Builder builder) {
    this.image = imageTag(builder.image);
    this.env = builder.env;
    this.hostConfigBuilder = builder.hostConfigBuilder;
    this.dockerClientBuilder = builder.dockerClientBuilder;
  }

  private String imageTag(final String image) {
    String imageTag = image;

    // By default we will use tag latest see https://github.com/spotify/docker-client/issues/857 for
    // more details on why this is needed to at lease always pull the latest
    if (imageTag.lastIndexOf(':') == -1) {
      imageTag = imageTag + ":" + LATEST_TAG;
    }

    return imageTag;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder
  {
    private String image;

    private List<String> env = new ArrayList<>();

    private HostConfig.Builder hostConfigBuilder;

    private DefaultDockerClient.Builder dockerClientBuilder;

    private Builder() {
    }

    public DockerContainerConfig build() {
      checkNotNull(image);

      if (isNull(this.hostConfigBuilder)) {
        this.hostConfigBuilder = defaultHostConfigBuilder();
      }

      if (isNull(this.dockerClientBuilder)) {
        this.dockerClientBuilder = defaultDockerClientBuilder();
      }

      return new DockerContainerConfig(this);
    }

    public Builder image(String image) {
      this.image = image;
      return this;
    }

    public Builder env(String... env) {
      this.env.addAll(asList(env));
      return this;
    }

    public Builder withHostConfigBuilder(HostConfig.Builder hostConfigBuilder) {
      this.hostConfigBuilder = hostConfigBuilder;
      return this;
    }

    public Builder withDockerClientBuilder(final DefaultDockerClient.Builder dockerClientBuilder) {
      this.dockerClientBuilder = dockerClientBuilder;
      return this;
    }

    public static HostConfig.Builder defaultHostConfigBuilder() {
      return HostConfig.builder()
          .portBindings(stream(PORT_MAPPING_PORTS)
              .collect(toMap(o -> o, b -> singletonList(randomPort(PORT_MAPPING_IP)))));
    }

    public static DefaultDockerClient.Builder defaultDockerClientBuilder() {
      DefaultDockerClient.Builder defaultClient = null;

      try {
        defaultClient = DefaultDockerClient.fromEnv();
      }
      catch (DockerCertificateException e) {
        throw new RuntimeException(e);
      }

      return defaultClient;
    }
  }

  public String getImage() {
    return image;
  }

  public List<String> getEnv() {
    return env;
  }

  public HostConfig.Builder getHostConfigBuilder() {
    return hostConfigBuilder;
  }

  public DefaultDockerClient.Builder getDockerClientBuilder() {
    return dockerClientBuilder;
  }
}
