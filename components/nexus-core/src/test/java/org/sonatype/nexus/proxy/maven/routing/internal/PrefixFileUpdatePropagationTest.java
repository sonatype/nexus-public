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

import java.io.ByteArrayInputStream;
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
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

public class PrefixFileUpdatePropagationTest
    extends AbstractRoutingProxyTest
{
  private static final String HOSTED_REPO_ID = "hosted";

  private static final String PROXY_REPO_ID = "proxy";

  private static final String GROUP_REPO_ID = "group";

  private final int remoteServerPort;

  private Server server;

  public PrefixFileUpdatePropagationTest()
      throws Exception
  {
    // fluke server to not have proxy autoblock, as remote connection refused IS a valid reason to auto block
    this.server =
        Server.withPort(0).serve("/").withBehaviours(Behaviours.error(404, "don't bother yourself"));
    server.start();
    this.remoteServerPort = server.getPort();
  }

  @Override
  protected EnvironmentBuilder createEnvironmentBuilder()
      throws Exception
  {
    // we need one hosted repo only, so build it
    return new EnvironmentBuilder()
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
          repoConf.getRemoteStorage().setUrl("http://localhost:" + remoteServerPort + "/");
          repo.configure(repoConf);
          // repo.setCacheManager( env.getCacheManager() );
          reposes.add(repo.getId());
          env.getApplicationConfiguration().getConfigurationModel().addRepository(repoConf);
          env.getRepositoryRegistry().addRepository(repo);
        }
        {
          // adding one hosted
          final M2Repository repo = (M2Repository) container.lookup(Repository.class, "maven2");
          CRepository repoConf = new DefaultCRepository();
          repoConf.setProviderRole(Repository.class.getName());
          repoConf.setProviderHint("maven2");
          repoConf.setId(HOSTED_REPO_ID);
          repoConf.setName(HOSTED_REPO_ID);
          repoConf.setLocalStorage(new CLocalStorage());
          repoConf.getLocalStorage().setProvider("file");
          repoConf.getLocalStorage().setUrl(
              env.getApplicationConfiguration().getWorkingDirectory("proxy/store/" + HOSTED_REPO_ID).toURI().toURL()
                  .toString());
          Xpp3Dom exRepo = new Xpp3Dom("externalConfiguration");
          repoConf.setExternalConfiguration(exRepo);
          M2RepositoryConfiguration exRepoConf = new M2RepositoryConfiguration(exRepo);
          exRepoConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);
          exRepoConf.setChecksumPolicy(ChecksumPolicy.STRICT_IF_EXISTS);
          repo.configure(repoConf);
          reposes.add(repo.getId());
          env.getApplicationConfiguration().getConfigurationModel().addRepository(repoConf);
          env.getRepositoryRegistry().addRepository(repo);
        }
        {
          // add a group
          final M2GroupRepository group =
              (M2GroupRepository) container.lookup(GroupRepository.class, "maven2");
          CRepository repoGroupConf = new DefaultCRepository();
          repoGroupConf.setProviderRole(GroupRepository.class.getName());
          repoGroupConf.setProviderHint("maven2");
          repoGroupConf.setId(GROUP_REPO_ID);
          repoGroupConf.setName(GROUP_REPO_ID);
          repoGroupConf.setLocalStorage(new CLocalStorage());
          repoGroupConf.getLocalStorage().setProvider("file");
          repoGroupConf.getLocalStorage().setUrl(
              env.getApplicationConfiguration().getWorkingDirectory("proxy/store/test").toURI().toURL().toString());
          Xpp3Dom exGroupRepo = new Xpp3Dom("externalConfiguration");
          repoGroupConf.setExternalConfiguration(exGroupRepo);
          M2GroupRepositoryConfiguration exGroupRepoConf = new M2GroupRepositoryConfiguration(exGroupRepo);
          exGroupRepoConf.setMemberRepositoryIds(reposes);
          exGroupRepoConf.setMergeMetadata(true);
          group.configure(repoGroupConf);
          env.getApplicationConfiguration().getConfigurationModel().addRepository(repoGroupConf);
          env.getRepositoryRegistry().addRepository(group);
        }
      }
    };
  }

  @Override
  protected boolean enableAutomaticRoutingFeature() {
    return true;
  }

  protected String prefixFile1(boolean withComments) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    pw.println(TextFilePrefixSourceMarshaller.MAGIC);
    if (withComments) {
      pw.println("# This is mighty prefix file!");
    }
    pw.println("/org/apache/maven");
    pw.println("/org/sonatype");
    if (withComments) {
      pw.println(" # Added later");
    }
    pw.println("/eu/flatwhite");
    return sw.toString();
  }

  @Test
  public void smoke()
      throws Exception
  {
    final Manager wm = lookup(Manager.class);

    // deploy to hosted something
    {
      final MavenRepository mavenRepository =
          getRepositoryRegistry().getRepositoryWithFacet(HOSTED_REPO_ID, MavenRepository.class);
      mavenRepository.storeItemWithChecksums(new ResourceStoreRequest("/com/sonatype/test/1.0/test-1.0.txt"),
          new ByteArrayInputStream("Some fluke content".getBytes()), null);
    }

    {
      try {
        final PrefixSource hostedEntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(HOSTED_REPO_ID,
                MavenRepository.class));
        final PrefixSource proxyEntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY_REPO_ID,
                MavenRepository.class));
        final PrefixSource groupEntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP_REPO_ID,
                MavenRepository.class));

        assertThat("Hosted should have ES", hostedEntrySource.supported());
        assertThat("Proxy should not have ES", !proxyEntrySource.supported()); // as we serve noscrape prefix
        // file
        assertThat("Group cannot have ES", !groupEntrySource.supported()); // as proxy member does not have WL
      }
      finally {
        server.stop();
      }
    }

    {
      server =
          Server.withPort(remoteServerPort).serve("/.meta/prefixes.txt").withBehaviours(
              Behaviours.content(prefixFile1(true)));
      try {
        server.start();
        final MavenProxyRepository mavenProxyRepository =
            getRepositoryRegistry().getRepositoryWithFacet(PROXY_REPO_ID, MavenProxyRepository.class);
        wm.updatePrefixFile(mavenProxyRepository);
        waitForRoutingBackgroundUpdates();

        final PrefixSource hostedEntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(HOSTED_REPO_ID,
                MavenRepository.class));
        final PrefixSource proxyEntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY_REPO_ID,
                MavenRepository.class));
        final PrefixSource groupEntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP_REPO_ID,
                MavenRepository.class));

        assertThat("Hosted should have ES", hostedEntrySource.supported());
        assertThat("Proxy should have ES", proxyEntrySource.supported()); // as we did serve file ok
        assertThat("Group should have ES", groupEntrySource.supported()); // as all member should have it

        // GROUP wl must have 4 entries: 1 from hosted (/com/sonatype) + 3 from proxied prefix file
        final List<String> groupEntries = groupEntrySource.readEntries();
        assertThat(groupEntries.size(), equalTo(4));
        assertThat(groupEntries, hasItem("/com/sonatype"));
        assertThat(groupEntries, hasItem("/org/sonatype"));
        assertThat(groupEntries, hasItem("/org/apache/maven"));
        assertThat(groupEntries, hasItem("/eu/flatwhite"));
      }
      finally {
        server.stop();
      }
    }
  }
}
