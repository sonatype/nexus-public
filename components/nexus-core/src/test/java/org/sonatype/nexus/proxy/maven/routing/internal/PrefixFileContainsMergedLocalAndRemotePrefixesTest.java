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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRemoteStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;

import com.google.common.io.Files;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Test for NEXUS-5661 Proxy repo prefix file does not include paths only available from local storage.
 *
 * @author cstamas
 */
public class PrefixFileContainsMergedLocalAndRemotePrefixesTest
    extends AbstractRoutingProxyTest
{
  private static final String PROXY_REPO_ID = "proxy";

  private Server server;

  private EnvironmentBuilder environmentBuilder;

  @After
  public void stopServers()
      throws Exception
  {
    if (server != null) {
      server.stop();
    }
  }

  @Override
  protected EnvironmentBuilder createEnvironmentBuilder()
      throws Exception
  {
    if (environmentBuilder == null) {
      server =
          Server.withPort(0).serve("/.meta/prefixes.txt").withBehaviours(
              Behaviours.content(remotePrefixFile()));
      server.start();

      // we need one hosted repo only, so build it
      environmentBuilder = new EnvironmentBuilder()
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
          // deploy a file into cache of proxy repo, that is NOT on remote
          final File repoRoot =
              env.getApplicationConfiguration().getWorkingDirectory("proxy/store/" + PROXY_REPO_ID);
          final File fakeArtifact = new File(repoRoot, "com/sonatype/foo/1.0/foo-1.0.pom");
          Files.createParentDirs(fakeArtifact);
          Files.write("dummy content".getBytes(), fakeArtifact);

          final PlexusContainer container = env.getPlexusContainer();
          final List<String> reposes = new ArrayList<String>();
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
            // repo.setCacheManager( env.getCacheManager() );
            reposes.add(repo.getId());
            env.getApplicationConfiguration().getConfigurationModel().addRepository(repoConf);
            env.getRepositoryRegistry().addRepository(repo);
          }
        }
      };
    }
    return environmentBuilder;
  }

  @Override
  protected boolean enableAutomaticRoutingFeature() {
    return true;
  }

  protected String remotePrefixFile() {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    pw.println(TextFilePrefixSourceMarshaller.MAGIC);
    pw.println("# This is mighty prefix file!");
    pw.println("/org/apache/maven");
    pw.println("/org/sonatype");
    pw.println(" # Added later");
    pw.println("/eu/flatwhite");
    return sw.toString();
  }

  @Test
  public void proxyPrefixFileIsUnchanged()
      throws Exception
  {
    // all is settled now, proxy should have prefix file pulled from remote AND merged with cache content
    final MavenProxyRepository proxyRepository =
        getRepositoryRegistry().getRepositoryWithFacet(PROXY_REPO_ID, MavenProxyRepository.class);

    final Manager routingManager = lookup(Manager.class);
    final PrefixSource proxyPrefixSource = routingManager.getPrefixSourceFor(proxyRepository);

    assertThat("Prefix file for proxy repository should exists", proxyPrefixSource.exists());
    assertThat("Prefix file for proxy repository should be discovered", proxyPrefixSource.supported());
    assertThat("Prefix file should be instanceof FilePrefixSource", proxyPrefixSource instanceof FilePrefixSource);

    final List<String> entries = proxyPrefixSource.readEntries();
    // first 3 entries are from remote prefix file, see remotePrefixFile() method
    // last 4th entry was "sneaked" in to storage (simulating locally but not remotely available file)
    assertThat(entries,
        containsInAnyOrder("/org/apache/maven", "/org/sonatype", "/eu/flatwhite", "/com/sonatype"));
  }
}
