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
package com.sonatype.nexus.docker.testsupport.saml.idp;

import java.util.List;

import com.sonatype.nexus.docker.testsupport.framework.DockerCommandLineConfig;
import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig;
import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig.Builder;

import com.spotify.docker.client.messages.HostConfig;

import static com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig.Builder.defaultDockerClientBuilder;
import static com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig.PORT_MAPPING_IP;
import static com.spotify.docker.client.messages.PortBinding.randomPort;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toMap;

public class SamlIdpITConfigFactory
{
  private SamlIdpITConfigFactory() {}

  public static DockerContainerConfig createKeycloakConfig(final String image,
                                                           final String tag,
                                                           final String userName,
                                                           final String password,
                                                           final List<String> portMappingPorts,
                                                           final DockerCommandLineConfig config)
  {
    return keycloakConfigBuilder(image + ":" + tag, userName, password, portMappingPorts, config).build();
  }

  private static Builder keycloakConfigBuilder(final String image,
                                               final String userName,
                                               final String password,
                                               final List<String> portMappingPorts,
                                               final DockerCommandLineConfig config)
  {
    HostConfig.Builder hostConfig = HostConfig.builder()
        .portBindings(portMappingPorts.stream()
            .collect(toMap(o -> o, b -> singletonList(randomPort(PORT_MAPPING_IP)))));

    return DockerContainerConfig.builder()
        .image(image)
        .env("KEYCLOAK_USER=" + userName, "KEYCLOAK_PASSWORD=" + password)
        .withHostConfigBuilder(hostConfig.appendBinds(config.getPathBinds()))
        .withDockerClientBuilder(defaultDockerClientBuilder().readTimeoutMillis(SECONDS.toMillis(5000)));
  }
}
