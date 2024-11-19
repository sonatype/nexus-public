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

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import com.sonatype.nexus.docker.testsupport.ContainerCommandLineITSupport;
import com.sonatype.nexus.docker.testsupport.framework.DockerContainerConfig;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.common.io.NetworkHelper.findLocalHostAddress;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

public class IdpCommandLineITSupport
    extends ContainerCommandLineITSupport
{
  public static final String IDP_CONTAINER_PORT = "8080";

  private Integer idpHostPort;

  private String idpHost;

  public IdpCommandLineITSupport(final DockerContainerConfig dockerContainerConfig) {
    super(dockerContainerConfig, null);
  }

  public void awaitIdpServer() throws Exception {
    idpHostPort = getHostTcpPort(IDP_CONTAINER_PORT);
    idpHost = findLocalHostAddress();

    log.info("Awaiting idp server {}:{}", idpHost, idpHostPort);

    await().atMost(2, TimeUnit.MINUTES).until(this::isIdpServerAvailable); // waiting 120 seconds (like IQ)

    log.info("Finished waiting for idp server {}:{}", idpHost, idpHostPort);
  }

  private boolean isIdpServerAvailable() {
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) getIdpUrl().openConnection();
      connection.setConnectTimeout(1000);
      connection.setReadTimeout(1000);
      connection.getInputStream();
      return true;
    }
    catch (Exception ignore) { // NOSONAR
      return false;
    }
    finally {
      if (nonNull(connection)) {
        connection.disconnect();
      }
    }
  }

  public URL getIdpUrl() throws MalformedURLException {
    return getIdpUri().toURL();
  }

  public URI getIdpUri() {
    return URI.create(format("http://%s:%s", idpHost, idpHostPort)).normalize();
  }
}
