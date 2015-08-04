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
import java.util.HashSet;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.ApplicationStatusSource;
import org.sonatype.nexus.SystemState;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.ChecksumContentValidator;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.util.DigesterUtils;
import org.sonatype.tests.http.server.api.Behaviour;
import org.sonatype.tests.http.server.fluent.Server;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * UT for NEXUS-6249: The remote hashes should get updated when remote content is re-cached.
 */
public class NEXUS6249M2ProxyRepositoryHashUpdateTest
    extends AbstractProxyTestEnvironment
{
  private static final String PROXY_REPO_ID = "proxy-repo";

  private EnvironmentBuilder environmentBuilder;

  private MavenContent mavenContent;

  private Server server;

  /**
   * Special behaviour that serves some String, but recognizes Maven request for SHA1 and server the current content's
   * SHA1 in that case.
   */
  public class MavenContent
      implements Behaviour
  {
    private String content;

    public boolean execute(HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx)
        throws Exception
    {
      System.out.println(request.getMethod() + " " + request.getRequestURI());
      // see if client wants content or .sha1?
      String content = request.getPathInfo().endsWith(".sha1") ? DigesterUtils
          .getSha1Digest(this.content) : this.content;
      response.addDateHeader("Last-Modified", System.currentTimeMillis()); // just say now always to make Nx pull it
      response.setContentType("text/plain");
      final byte[] payload = content.getBytes(Charsets.UTF_8);
      response.setContentLength(payload.length);
      response.getOutputStream().write(payload);
      return false;
    }

    public String getContent() {
      return content;
    }

    public void setContent(final String content) {
      this.content = content;
    }
  }

  public NEXUS6249M2ProxyRepositoryHashUpdateTest() throws Exception {
    mavenContent = new MavenContent();
    mavenContent.setContent("something"); // to prevent auto block
    server = Server.withPort(0).serve("/*").withBehaviours(mavenContent);
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

  public StorageItem retrieveItem()
      throws Exception
  {
    final ResourceStoreRequest request = new ResourceStoreRequest("/something.txt",
        false, false);
    request.setRequestRemoteOnly(true);
    return getRepository().retrieveItem(request);
  }

  @Test
  public void testItemUpdateUpdatesRemoteHash()
      throws Exception
  {
    StorageItem item;
    final HashSet<String> hashes = Sets.newHashSet();

    mavenContent.setContent("First version of content");

    item = retrieveItem();
    hashes.add(item.getRepositoryItemAttributes().get(ChecksumContentValidator.ATTR_REMOTE_SHA1));

    mavenContent.setContent("Second version of content");

    item = retrieveItem();
    hashes.add(item.getRepositoryItemAttributes().get(ChecksumContentValidator.ATTR_REMOTE_SHA1));

    mavenContent.setContent("Third version of content");

    item = retrieveItem();
    hashes.add(item.getRepositoryItemAttributes().get(ChecksumContentValidator.ATTR_REMOTE_SHA1));

    // we expect three different hashes, NEXUS-6249 caused one same hash to be served always,
    // but as this UT sets up a proxy repo with ChecksumPolicy.STRICT_IF_EXISTS due to same bug
    // 2nd retrieval attempt would end up in validation failure, as Nx Core would use stale hashes
    // to validate
    assertThat(hashes, hasSize(3));
  }
}
