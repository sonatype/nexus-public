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
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.routing.DiscoveryStatus.DStatus;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.nexus.proxy.maven.routing.PublishingStatus.PStatus;
import org.sonatype.nexus.proxy.maven.routing.RoutingStatus;
import org.sonatype.nexus.proxy.maven.routing.internal.scrape.DeliverBehaviour;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;

import com.google.common.base.Strings;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

public class RemoteContentDiscovererImplTest
    extends AbstractRoutingProxyTest
{
  private static final String PROXY1_REPO_ID = "proxy1";

  private static final String PROXY2_REPO_ID = "proxy2";

  private static final String GROUP_REPO_ID = "group";

  private Server server1;

  private Server server2;

  public RemoteContentDiscovererImplTest()
      throws Exception
  {
    // fluke server to not have proxy autoblock, as remote connection refused IS a valid reason to auto block
    this.server1 =
        Server.withPort(0).serve("/").withBehaviours(Behaviours.error(404, "don't bother yourself"));
    server1.start();
    this.server2 =
        Server.withPort(0).serve("/").withBehaviours(Behaviours.error(404, "don't bother yourself"));
    server2.start();
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
          repoConf.setId(PROXY1_REPO_ID);
          repoConf.setName(PROXY1_REPO_ID);
          repoConf.setNotFoundCacheActive(true);
          repoConf.setLocalStorage(new CLocalStorage());
          repoConf.getLocalStorage().setProvider("file");
          repoConf.getLocalStorage().setUrl(
              env.getApplicationConfiguration().getWorkingDirectory("proxy/store/" + PROXY1_REPO_ID).toURI().toURL()
                  .toString());
          Xpp3Dom ex = new Xpp3Dom("externalConfiguration");
          repoConf.setExternalConfiguration(ex);
          M2RepositoryConfiguration exConf = new M2RepositoryConfiguration(ex);
          exConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);
          exConf.setChecksumPolicy(ChecksumPolicy.STRICT_IF_EXISTS);
          repoConf.setRemoteStorage(new CRemoteStorage());
          repoConf.getRemoteStorage().setProvider(
              env.getRemoteProviderHintFactory().getDefaultHttpRoleHint());
          repoConf.getRemoteStorage().setUrl("http://localhost:" + server1.getPort() + "/");
          repo.configure(repoConf);
          // repo.setCacheManager( env.getCacheManager() );
          reposes.add(repo.getId());
          env.getApplicationConfiguration().getConfigurationModel().addRepository(repoConf);
          env.getRepositoryRegistry().addRepository(repo);
        }
        {
          // adding one proxy
          final M2Repository repo = (M2Repository) container.lookup(Repository.class, "maven2");
          CRepository repoConf = new DefaultCRepository();
          repoConf.setProviderRole(Repository.class.getName());
          repoConf.setProviderHint("maven2");
          repoConf.setId(PROXY2_REPO_ID);
          repoConf.setName(PROXY2_REPO_ID);
          repoConf.setNotFoundCacheActive(true);
          repoConf.setLocalStorage(new CLocalStorage());
          repoConf.getLocalStorage().setProvider("file");
          repoConf.getLocalStorage().setUrl(
              env.getApplicationConfiguration().getWorkingDirectory("proxy/store/" + PROXY2_REPO_ID).toURI().toURL()
                  .toString());
          Xpp3Dom ex = new Xpp3Dom("externalConfiguration");
          repoConf.setExternalConfiguration(ex);
          M2RepositoryConfiguration exConf = new M2RepositoryConfiguration(ex);
          exConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);
          exConf.setChecksumPolicy(ChecksumPolicy.STRICT_IF_EXISTS);
          repoConf.setRemoteStorage(new CRemoteStorage());
          repoConf.getRemoteStorage().setProvider(
              env.getRemoteProviderHintFactory().getDefaultHttpRoleHint());
          repoConf.getRemoteStorage().setUrl("http://localhost:" + server2.getPort() + "/");
          repo.configure(repoConf);
          // repo.setCacheManager( env.getCacheManager() );
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
      pw.println("# This is mighty prefix file1!");
    }
    pw.println("/org/apache/maven");
    pw.println("/org/sonatype");
    if (withComments) {
      pw.println(" # Added later");
    }
    pw.println("/eu/flatwhite");
    return sw.toString();
  }

  protected String prefixFile2(boolean withComments) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    pw.println(TextFilePrefixSourceMarshaller.MAGIC);
    if (withComments) {
      pw.println("# This is mighty prefix file2!");
    }
    pw.println("/com/sonatype");
    return sw.toString();
  }

  @Test
  public void noPrefixFilesServed()
      throws Exception
  {
    try {
      final Manager wm = lookup(Manager.class);
      {
        final PrefixSource proxy1EntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID,
                MavenRepository.class));
        final PrefixSource proxy2EntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY2_REPO_ID,
                MavenRepository.class));
        final PrefixSource groupEntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP_REPO_ID,
                MavenRepository.class));

        assertThat("Proxy1 should not have ES", !proxy1EntrySource.supported()); // we served noscrape file
        assertThat("Proxy2 should not have ES", !proxy2EntrySource.supported()); // we served noscrape file
        assertThat("Group cannot have ES", !groupEntrySource.supported()); // as proxy member are disabled
      }
    }
    finally {
      server1.stop();
      server2.stop();
    }
  }

  @Test
  public void oneOutOfTwoPrefixFilesServed()
      throws Exception
  {
    try {
      final int server1port = server1.getPort();
      server1.stop();
      server1 =
          Server.withPort(server1port).serve("/.meta/prefixes.txt").withBehaviours(
              Behaviours.content(prefixFile1(true)));
      server1.start();

      final Manager wm = lookup(Manager.class);
      wm.updatePrefixFile(getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID, MavenRepository.class));
      waitForRoutingBackgroundUpdates();
      {
        final PrefixSource proxy1EntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID,
                MavenRepository.class));
        final PrefixSource proxy2EntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY2_REPO_ID,
                MavenRepository.class));
        final PrefixSource groupEntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP_REPO_ID,
                MavenRepository.class));

        assertThat("Proxy1 should have ES", proxy1EntrySource.supported()); // we serve prefix file
        assertThat("Proxy2 should not have ES", !proxy2EntrySource.supported()); // we serve noscrape file
        assertThat("Group cannot have ES", !groupEntrySource.supported()); // we serve noscrape file
      }
    }
    finally {
      server1.stop();
      server2.stop();
    }
  }

  @Test
  public void twoOutOfTwoPrefixFilesServed()
      throws Exception
  {
    try {
      final int server1port = server1.getPort();
      server1.stop();
      server1 =
          Server.withPort(server1port).serve("/.meta/prefixes.txt").withBehaviours(
              Behaviours.content(prefixFile1(true)));
      server1.start();

      final int server2port = server2.getPort();
      server2.stop();
      server2 =
          Server.withPort(server2port).serve("/.meta/prefixes.txt").withBehaviours(
              Behaviours.content(prefixFile2(true)));
      server2.start();

      final Manager wm = lookup(Manager.class);
      wm.updatePrefixFile(getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID, MavenRepository.class));
      wm.updatePrefixFile(getRepositoryRegistry().getRepositoryWithFacet(PROXY2_REPO_ID, MavenRepository.class));
      waitForRoutingBackgroundUpdates();
      {
        final PrefixSource proxy1EntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID,
                MavenRepository.class));
        final PrefixSource proxy2EntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY2_REPO_ID,
                MavenRepository.class));
        final PrefixSource groupEntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP_REPO_ID,
                MavenRepository.class));

        assertThat("Proxy1 should have ES", proxy1EntrySource.supported()); // we served prefix file
        assertThat("Proxy2 should have ES", proxy2EntrySource.supported()); // we served prefix file
        assertThat("Group should have ES", groupEntrySource.supported()); // both proxies have it

        // GROUP wl must have 4 entries: 1 from hosted (/com/sonatype) + 3 from proxied prefix file
        final List<String> groupEntries = groupEntrySource.readEntries();
        assertThat(groupEntries.size(), equalTo(4));
        assertThat(groupEntries, hasItem("/com/sonatype"));
        assertThat(groupEntries, hasItem("/org/sonatype"));
        assertThat(groupEntries, hasItem("/org/apache/maven"));
        assertThat(groupEntries, hasItem("/eu/flatwhite"));
      }
    }
    finally {
      server1.stop();
      server2.stop();
    }
  }

  @Test
  public void oneInvalidAndOneValidOutOfTwoPrefixFilesServed()
      throws Exception
  {
    try {
      final int server1port = server1.getPort();
      server1.stop();
      server1 =
          Server.withPort(server1port).serve("/.meta/prefixes.txt").withBehaviours(
              Behaviours.content(prefixFile1(true)));
      server1.start();

      final int server2port = server2.getPort();
      server2.stop();
      server2 =
          Server.withPort(server2port).serve("/.meta/prefixes.txt").withBehaviours(
              Behaviours.content(TextFilePrefixSourceMarshaller.MAGIC + "\n" + Strings.repeat("/12345677890", 25)));
      server2.start();

      final Manager wm = lookup(Manager.class);
      wm.updatePrefixFile(getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID, MavenRepository.class));
      wm.updatePrefixFile(getRepositoryRegistry().getRepositoryWithFacet(PROXY2_REPO_ID, MavenRepository.class));
      waitForRoutingBackgroundUpdates();
      {
        final PrefixSource proxy1EntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID,
                MavenRepository.class));
        final PrefixSource proxy2EntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY2_REPO_ID,
                MavenRepository.class));
        final PrefixSource groupEntrySource =
            wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP_REPO_ID,
                MavenRepository.class));

        assertThat("Proxy1 should have ES", proxy1EntrySource.supported()); // we served prefix file
        assertThat("Proxy2 should not have ES", !proxy2EntrySource.supported()); // we served invalid prefix file
        assertThat("Group should not have ES", !groupEntrySource.supported()); // both proxies have it

        final RoutingStatus status =
            wm.getStatusFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY2_REPO_ID,
                MavenRepository.class));
        final String message = status.getDiscoveryStatus().getLastDiscoveryMessage();
        // Text should be:
        // "Remote strategy prefix-file detected invalid input,"
        // "results discarded: Prefix file contains line longer"
        // "than allowed (250 characters), refusing to load the file."
        assertThat(message, containsString("strategy prefix-file"));
        assertThat(message, containsString("longer than allowed"));
        assertThat(message, containsString("refusing"));
      }
    }
    finally {
      server1.stop();
      server2.stop();
    }
  }

  @Test
  public void prefixStrategyErrorResponseResultsInDiscoveryHardFailure()
      throws Exception
  {
    try {
      // remote peer will serve HTTP 500 when asked for prefix file
      final int server1port = server1.getPort();
      server1.stop();
      server1 =
          Server.withPort(server1port).serve("/.meta/prefixes.txt").withBehaviours(
              new DeliverBehaviour(500, "text/plain", "Ugly server error"));
      server1.start();

      final Manager wm = lookup(Manager.class);
      wm.updatePrefixFile(getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID, MavenRepository.class));
      waitForRoutingBackgroundUpdates();
      {
        final RoutingStatus status =
            wm.getStatusFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID,
                MavenRepository.class));

        // WL not published
        assertThat(status.getPublishingStatus().getStatus(), equalTo(PStatus.NOT_PUBLISHED));
        // status should be the new ERROR state
        assertThat(status.getDiscoveryStatus().getStatus(), equalTo(DStatus.ERROR));
        // it is "prefix" strategy that got into error
        assertThat(status.getDiscoveryStatus().getLastDiscoveryStrategy(),
            equalTo(RemotePrefixFileStrategy.ID));
        // message is "Unexpected response code..."
        assertThat(status.getDiscoveryStatus().getLastDiscoveryMessage(),
            containsString("Unexpected response code"));
      }
    }
    finally {
      server1.stop();
      server2.stop();
    }
  }

  @Test
  public void prefixStrategyErrorTransportResultsInDiscoveryHardFailure()
      throws Exception
  {
    try {
      // remote peer will be down
      server1.stop();

      final Manager wm = lookup(Manager.class);
      wm.updatePrefixFile(getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID, MavenRepository.class));
      waitForRoutingBackgroundUpdates();
      {
        final RoutingStatus status =
            wm.getStatusFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID,
                MavenRepository.class));

        // WL not published
        assertThat(status.getPublishingStatus().getStatus(), equalTo(PStatus.NOT_PUBLISHED));
        // status should be the new ERROR state
        assertThat(status.getDiscoveryStatus().getStatus(), equalTo(DStatus.ERROR));
        // it is "prefix" strategy that got into error
        assertThat(status.getDiscoveryStatus().getLastDiscoveryStrategy(),
            equalTo(RemotePrefixFileStrategy.ID));
        // message is "Connection to XXX refused"
        assertThat(status.getDiscoveryStatus().getLastDiscoveryMessage(), containsString("Connection"));
        assertThat(status.getDiscoveryStatus().getLastDiscoveryMessage(), containsString("refused"));
      }
    }
    finally {
      server1.stop();
      server2.stop();
    }
  }
}
