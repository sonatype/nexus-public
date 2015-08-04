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
package org.sonatype.nexus.testsuite.apachehttpclient;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.subsystem.ServerConfiguration;
import org.sonatype.nexus.client.core.subsystem.content.Content;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.sonatype.nexus.client.core.subsystem.content.Location.repositoryLocation;

/**
 * ITs related accessing a remote server that requires mutual trust client trusts server / server trusts client).
 *
 * @since 2.5.1
 */
@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public class SSLMutualTrustIT
    extends NexusRunningParametrizedITSupport
{

  private Server httpsRemoteServer;

  public SSLMutualTrustIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return super.configureNexus(configuration)
        .setSystemProperty("javax.net.ssl.keyStore", testData().resolveFile("keystore-client").getAbsolutePath())
        .setSystemProperty("javax.net.ssl.keyStorePassword", "changeit")
        .setSystemProperty("javax.net.ssl.trustStore", testData().resolveFile("truststore-client").getAbsolutePath())
        .setSystemProperty("javax.net.ssl.trustStorePassword", "changeit");
  }

  @Before
  public void initRemoteServer()
      throws Exception
  {
    httpsRemoteServer = Server
        .withPort(0)
        .withKeystore(testData().resolveFile("keystore-jetty").getAbsolutePath(), "changeit")
        .withTruststore(testData().resolveFile("truststore-jetty").getAbsolutePath(), "changeit")
        .requireClientAuth()
        .serve("/*").withBehaviours(Behaviours.get(testData().resolveFile("remote-repo")))
        .start();
  }

  @After
  public void stopRemoteServer()
      throws Exception
  {
    if (httpsRemoteServer != null) {
      httpsRemoteServer.stop();
    }
  }

  /**
   *
   */
  @Test
  public void downloadFromRemoteRequiringMutualTrust()
      throws Exception
  {
    final MavenProxyRepository repository = createMavenProxyRepository(httpsRemoteServer);
    downloadArtifact(repository.id());
  }

  private void downloadArtifact(final String repositoryId)
      throws IOException
  {
    content().download(
        repositoryLocation(repositoryId, "com/someorg/artifact/1.0/artifact-1.0.pom"),
        new File(testIndex().getDirectory("downloads"), "artifact-1.0.pom")
    );
  }

  private MavenProxyRepository createMavenProxyRepository(final Server remoteServer)
      throws MalformedURLException
  {
    return repositories()
        .create(MavenProxyRepository.class, repositoryIdForTest())
        .asProxyOf(remoteServer.getUrl().toExternalForm())
        .save();
  }

  public ServerConfiguration config() {
    return client().getSubsystem(ServerConfiguration.class);
  }

  public Repositories repositories() {
    return client().getSubsystem(Repositories.class);
  }

  public Content content() {
    return client().getSubsystem(Content.class);
  }

}
