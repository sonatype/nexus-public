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
package org.sonatype.nexus.proxy;

import java.io.IOException;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemState;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;
import org.sonatype.tests.http.server.jetty.behaviour.Record;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * UT for NEXUS-6177: The "?asExpired" flag does not invalidate NFC
 */
public class RepositoryRequestAsExpiredTest
    extends AbstractProxyTestEnvironment
{
  private static final String PROXY_REPO_ID = "proxy-repo";

  private EnvironmentBuilder environmentBuilder;

  private final Record record;

  private Server server;

  public RepositoryRequestAsExpiredTest() throws Exception {
    // just a dummy server doing 404 all the time
    // it is the request we are interested in
    this.record = new Record();
    this.server =
        Server.withPort(0).serve("/").withBehaviours(record, Behaviours.error(404, "don't bother yourself"));
    server.start();
  }

  @After
  public void stopServer()
      throws Exception
  {
    server.stop();
  }

  @Override
  protected final EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    lookup(ApplicationStatusSource.class).setState(SystemState.STARTED);
    if (environmentBuilder == null) {
      this.environmentBuilder = new EnvironmentBuilder()
      {
        @Override
        public void startService() {
        }

        @Override
        public void stopService() {
        }

        @Override
        public void buildEnvironment(AbstractProxyTestEnvironment env)
            throws ConfigurationException, IOException, ComponentLookupException
        {
          final PlexusContainer container = env.getPlexusContainer();
          {
            // adding one proxy
            final M2Repository repo = (M2Repository) container.lookup(Repository.class, "maven2");
            CRepository repoConf = new DefaultCRepository();
            repoConf.setProviderRole(Repository.class.getName());
            repoConf.setProviderHint("maven2");
            repoConf.setId(PROXY_REPO_ID);
            repoConf.setName(PROXY_REPO_ID);
            repoConf.setNotFoundCacheActive(true);
            repoConf.setLocalStorage(new CLocalStorage());
            repoConf.getLocalStorage().setProvider("file");
            repoConf.getLocalStorage().setUrl(
                env.getApplicationConfiguration().getWorkingDirectory("proxy/store/" + PROXY_REPO_ID).toURI().toURL()
                    .toString());
            Xpp3Dom ex = new Xpp3Dom("externalConfiguration");
            repoConf.setExternalConfiguration(ex);
            M2RepositoryConfiguration exConf = new M2RepositoryConfiguration(ex);
            exConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);
            exConf.setChecksumPolicy(ChecksumPolicy.STRICT_IF_EXISTS);
            repoConf.setRemoteStorage(new CRemoteStorage());
            repoConf.getRemoteStorage().setProvider(
                env.getRemoteProviderHintFactory().getDefaultHttpRoleHint());
            repoConf.getRemoteStorage().setUrl("http://localhost:" + server.getPort() + "/");
            repo.configure(repoConf);
            env.getApplicationConfiguration().getConfigurationModel().addRepository(repoConf);
            env.getRepositoryRegistry().addRepository(repo);
          }
        }
      };
    }
    return environmentBuilder;
  }

  protected Repository getRepository()
      throws NoSuchResourceStoreException
  {
    return getRepositoryRegistry().getRepository(PROXY_REPO_ID);
  }

  public void retrieveItem(boolean asExpired)
      throws Exception
  {
    try {
      final ResourceStoreRequest request = new ResourceStoreRequest("/activemq/activemq-core/1.2/activemq-core-1.2.jar",
          false, false);
      request.setRequestAsExpired(asExpired);
      getRepository().retrieveItem(request);
    }
    catch (ItemNotFoundException e) {
      // suppress it
    }
  }

  @Test
  public void testRequestAsExpiredCircumventsNfc()
      throws Exception
  {
    // do a request, as server does only 404s it should get into NFC
    retrieveItem(false);
    assertThat(record.getRequests(), hasSize(1));
    assertThat(getRepository().getNotFoundCache().getStatistics().getSize(), equalTo(1L));
    record.clear();

    // do 2nd request, path is already in NFC, no remote call should happen
    retrieveItem(false);
    assertThat(record.getRequests(), hasSize(0));
    assertThat(getRepository().getNotFoundCache().getStatistics().getSize(), equalTo(1L));
    record.clear();

    // do 3rd request "as expired", path should remain in NFC, but remote request should happen
    retrieveItem(true);
    assertThat(record.getRequests(), hasSize(1));
    assertThat(getRepository().getNotFoundCache().getStatistics().getSize(), equalTo(1L));
    record.clear();

    // do 4th request, path is already in NFC, no remote call should happen
    retrieveItem(false);
    assertThat(record.getRequests(), hasSize(0));
    assertThat(getRepository().getNotFoundCache().getStatistics().getSize(), equalTo(1L));
    record.clear();
  }
}
