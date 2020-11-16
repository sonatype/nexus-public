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
package com.sonatype.nexus.docker.testsupport.conda;

import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig;
import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig.Builder;

import static com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig.Builder.defaultDockerClientBuilder;
import static com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig.Builder.defaultHostConfigBuilder;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Factory for creation of Conda required objects
 *
 * @since 3.19
 */
public class CondaClientITConfigFactory
{
  private static final String IMAGE_CONDA = "docker-all.repo.sonatype.com/continuumio/miniconda3";

  private CondaClientITConfigFactory() {
  }

  public static DockerContainerConfig createCondaConfig() {
    return condaConfigBuilder(IMAGE_CONDA).build();
  }

  private static Builder condaConfigBuilder(final String image) {
    return DockerContainerConfig.builder()
        .image(image)
        .withHostConfigBuilder(defaultHostConfigBuilder())
        .withDockerClientBuilder(defaultDockerClientBuilder().readTimeoutMillis(SECONDS.toMillis(5000)));
  }
}
