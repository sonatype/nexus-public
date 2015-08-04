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
import org.sonatype.nexus.proxy.maven.MavenGroupRepository;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.routing.DiscoveryStatus.DStatus;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.PublishingStatus.PStatus;
import org.sonatype.nexus.proxy.maven.routing.RoutingStatus;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.ProxyMode;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.tests.http.server.fluent.Behaviours;
import org.sonatype.tests.http.server.fluent.Server;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Testing interaction of {@link Repository#getLocalStatus()} and WL.
 *
 * @author cstamas
 */
public class RoutingAndProxyModeTest
    extends AbstractRoutingProxyTest
{
  private static final String HOSTED_REPO_ID = "hosted";

  private static final String PROXY1_REPO_ID = "proxy1";

  private static final String PROXY2_REPO_ID = "proxy2";

  private static final String GROUP_REPO_ID = "group";

  private Server server;

  public RoutingAndProxyModeTest()
      throws Exception
  {
    // one server serving up stuff for both proxies, it does have prefix file published but nothing else
    // this only serves the purpose to not have repo autoblock
    this.server =
        Server.withPort(0).serve("/").withBehaviours(Behaviours.error(404, "don't bother yourself")).serve(
            "/.meta/prefixes.txt").withBehaviours(Behaviours.content(prefixFile()));
    server.start();
  }

  @After
  public void stopServer()
      throws Exception
  {
    server.stop();
  }

  protected String prefixFile() {
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
          // adding 1st proxy, that is blocked
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
          exConf.setProxyMode(ProxyMode.BLOCKED_MANUAL);
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
        {
          // adding 2nd proxy
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
          repoConf.getRemoteStorage().setUrl("http://localhost:" + server.getPort() + "/");
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

  @Test
  public void manuallyBlockedRepositoryDoesNotAffectWLInitialization()
      throws Exception
  {
    // at this point, NexusStartedEvent was fired, and hence, WL's should be inited
    final Manager wm = lookup(Manager.class);
    final MavenProxyRepository proxy1 =
        getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID, MavenProxyRepository.class);

    assertThat(proxy1.getProxyMode(), equalTo(ProxyMode.BLOCKED_MANUAL));
    waitForRoutingBackgroundUpdates();

    // let's check states

    {
      // proxy1
      final RoutingStatus proxy1status = wm.getStatusFor(proxy1);
      // this repo is Out of Service
      assertThat(proxy1status.getPublishingStatus().getStatus(), equalTo(PStatus.NOT_PUBLISHED));
      assertThat(proxy1status.getDiscoveryStatus().getStatus(), equalTo(DStatus.ENABLED_NOT_POSSIBLE));
      assertThat(proxy1status.getDiscoveryStatus().getLastDiscoveryStrategy(), is("none"));
      // Remark: the combination of those three above simply means "discovery never tried against it"
      // yet.
    }

    {
      // proxy2
      final RoutingStatus proxy2status =
          wm.getStatusFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY2_REPO_ID,
              MavenProxyRepository.class));
      // this repo should be good
      assertThat(proxy2status.getPublishingStatus().getStatus(), equalTo(PStatus.PUBLISHED));
      assertThat(proxy2status.getDiscoveryStatus().getStatus(), equalTo(DStatus.SUCCESSFUL));
    }

    {
      // hosted
      final RoutingStatus hostedStatus =
          wm.getStatusFor(getRepositoryRegistry().getRepositoryWithFacet(HOSTED_REPO_ID,
              MavenHostedRepository.class));
      // this repo should be good
      assertThat(hostedStatus.getPublishingStatus().getStatus(), equalTo(PStatus.PUBLISHED));
      assertThat(hostedStatus.getDiscoveryStatus().getStatus(), equalTo(DStatus.NOT_A_PROXY));
    }

    {
      // group
      final RoutingStatus groupStatus =
          wm.getStatusFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP_REPO_ID,
              MavenGroupRepository.class));
      // not all members have WL, unpublished
      assertThat(groupStatus.getPublishingStatus().getStatus(), equalTo(PStatus.NOT_PUBLISHED));
      // message should refer to proxy1 as reason of not publishing group WL
      assertThat(groupStatus.getPublishingStatus().getLastPublishedMessage(), containsString(proxy1.getName()));
      assertThat(groupStatus.getDiscoveryStatus().getStatus(), equalTo(DStatus.NOT_A_PROXY));
    }
  }

  @Test
  public void flippingProxyModeUpdatesWL()
      throws Exception
  {
    // at this point, NexusStartedEvent was fired, and hence, WL's should be inited
    final Manager wm = lookup(Manager.class);
    final MavenProxyRepository proxy1 =
        getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID, MavenProxyRepository.class);

    assertThat(proxy1.getProxyMode(), equalTo(ProxyMode.BLOCKED_MANUAL));
    waitForRoutingBackgroundUpdates();

    // let's check states
    {
      // proxy1
      final RoutingStatus proxy1status = wm.getStatusFor(proxy1);
      // this repo is Blocked
      assertThat(proxy1status.getPublishingStatus().getStatus(), equalTo(PStatus.NOT_PUBLISHED));
      assertThat(proxy1status.getDiscoveryStatus().getStatus(), equalTo(DStatus.ENABLED_NOT_POSSIBLE));
      assertThat(proxy1status.getDiscoveryStatus().getLastDiscoveryStrategy(), is("none"));
      // Remark: the combination of those three above simply means "discovery never tried against it"
      // yet.
    }
    {
      // group
      final RoutingStatus groupStatus =
          wm.getStatusFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP_REPO_ID,
              MavenGroupRepository.class));
      // not all members have WL, unpublished
      assertThat(groupStatus.getPublishingStatus().getStatus(), equalTo(PStatus.NOT_PUBLISHED));
      // message should refer to proxy1 as reason of not publishing group WL
      assertThat(groupStatus.getPublishingStatus().getLastPublishedMessage(), containsString(proxy1.getName()));
      assertThat(groupStatus.getDiscoveryStatus().getStatus(), equalTo(DStatus.NOT_A_PROXY));
    }

    {
      // let's flip proxy1 now
      proxy1.setProxyMode(ProxyMode.ALLOW);
      getApplicationConfiguration().saveConfiguration();
      Thread.yield();
      wairForAsyncEventsToCalmDown();
      waitForRoutingBackgroundUpdates();
    }

    // let's check states again, now with enabled proxy1

    {
      // proxy1
      final RoutingStatus proxy1status =
          wm.getStatusFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID,
              MavenProxyRepository.class));
      // this repo is Out of Service
      assertThat(proxy1status.getPublishingStatus().getStatus(), equalTo(PStatus.PUBLISHED));
      assertThat(proxy1status.getDiscoveryStatus().getStatus(), equalTo(DStatus.SUCCESSFUL));
      assertThat(proxy1status.getDiscoveryStatus().getLastDiscoveryStrategy(), is(RemotePrefixFileStrategy.ID));
    }
    {
      // group
      final RoutingStatus groupStatus =
          wm.getStatusFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP_REPO_ID,
              MavenGroupRepository.class));
      assertThat(groupStatus.getPublishingStatus().getStatus(), equalTo(PStatus.PUBLISHED));
      assertThat(groupStatus.getDiscoveryStatus().getStatus(), equalTo(DStatus.NOT_A_PROXY));
    }

    {
      // let's flip proxy1 now back
      proxy1.setProxyMode(ProxyMode.BLOCKED_MANUAL);
      getApplicationConfiguration().saveConfiguration();
      Thread.yield();
      wairForAsyncEventsToCalmDown();
      waitForRoutingBackgroundUpdates();
    }

    // let's check states again, now with enabled proxy1

    {
      // proxy1
      final RoutingStatus proxy1status =
          wm.getStatusFor(getRepositoryRegistry().getRepositoryWithFacet(PROXY1_REPO_ID,
              MavenProxyRepository.class));
      // this repo is blocked
      assertThat(proxy1status.getPublishingStatus().getStatus(), equalTo(PStatus.PUBLISHED));
      assertThat(proxy1status.getDiscoveryStatus().getStatus(), equalTo(DStatus.ENABLED_NOT_POSSIBLE));
      assertThat(proxy1status.getDiscoveryStatus().getLastDiscoveryStrategy(), is("none"));
    }
    {
      // group
      final RoutingStatus groupStatus =
          wm.getStatusFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP_REPO_ID,
              MavenGroupRepository.class));
      assertThat(groupStatus.getPublishingStatus().getStatus(), equalTo(PStatus.PUBLISHED));
      assertThat(groupStatus.getDiscoveryStatus().getStatus(), equalTo(DStatus.NOT_A_PROXY));
    }
  }
}
