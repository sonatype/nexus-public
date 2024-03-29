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

import java.util.List;
import java.util.Map;

/**
 * Factory for creation of Docker required objects
 */
public class DockerClientITConfigFactory
{
  private static final String IMAGE_NGINX = "docker-all.repo.sonatype.com/nginx";

  private DockerClientITConfigFactory() {
  }

  public static DockerContainerConfig createNginxConfig(
      final Map<String, String> pathBinds,
      final List<String> portMappingPorts)
  {
    return DockerContainerConfig.builder(IMAGE_NGINX)
        .withPathBinds(pathBinds)
        .withExposedPorts(portMappingPorts)
        .build();
  }
}
