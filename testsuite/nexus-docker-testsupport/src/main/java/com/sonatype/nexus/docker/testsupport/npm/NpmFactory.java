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
package com.sonatype.nexus.docker.testsupport.npm;

import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig;
import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig.Builder;

/**
 * Factory for creation of NPM required objects
 *
 * @since 3.6.1
 */
public class NpmFactory
{
  private static final String IMAGE_NODE = "node";

  private NpmFactory() {
  }

  public static DockerContainerConfig createNodeConfig(NpmCommandLineConfig config) {
    return npmConfigBuilder(IMAGE_NODE, config).build();
  }

  private static DockerContainerConfig.Builder npmConfigBuilder(String image, NpmCommandLineConfig config) {
    return DockerContainerConfig.builder()
        .image(image)
        .withHostConfigBuilder(Builder.defaultHostConfigBuilder()
            .appendBinds(config.getPathBinds()));
  }
}
