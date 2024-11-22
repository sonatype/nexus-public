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

import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig;

import com.google.common.collect.ImmutableMap;

public class SamlIdpITConfigFactory
{
  private SamlIdpITConfigFactory() {
  }

  public static DockerContainerConfig createKeycloakConfig(
      final String image,
      final String tag,
      final String userName,
      final String password,
      final String portMappingPort)
  {
    return DockerContainerConfig.builder(image + ":" + tag)
        .withEnv(ImmutableMap.of("KEYCLOAK_USER", userName, "KEYCLOAK_PASSWORD", password))
        .withExposedPort(portMappingPort)
        .build();
  }
}
