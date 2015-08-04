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
import java.util.Collections;
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
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

/**
 * Testing group WL content on group member changes.
 *
 * @author cstamas
 */
public class PrefixFileUpdatePropagationContentTest
    extends AbstractRoutingProxyTest
{
  private static final String PROXY1_REPO_ID = "proxy1";

  private static final String PROXY2_REPO_ID = "proxy2";

  private static final String PROXY3_REPO_ID = "proxy3";

  private static final String GROUP1_REPO_ID = "group1";

  private static final String GROUP2_REPO_ID = "group2";

  private final Server server1;

  private final Server server2;

  private final Server server3;

  public PrefixFileUpdatePropagationContentTest()
      throws Exception
  {
    this.server1 =
        Server.withPort(0).serve("/.meta/prefixes.txt").withBehaviours(Behaviours.content(prefixFile1()));
    this.server2 =
        Server.withPort(0).serve("/.meta/prefixes.txt").withBehaviours(Behaviours.content(prefixFile2()));
    this.server3 =
        Server.withPort(0).serve("/.meta/prefixes.txt").withBehaviours(Behaviours.content(prefixFile3()));
    server1.start();
    server2.start();
    server3.start();
  }

  @After
  public void stopServers()
      throws Exception
  {
    server1.stop();
    server2.stop();
    server3.stop();
  }

  @Override
  protected boolean enableAutomaticRoutingFeature() {
    return true;
  }

  protected String prefixFile1() {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    pw.println(TextFilePrefixSourceMarshaller.MAGIC);
    pw.println("# This is first prefix file!");
    pw.println("/org/apache");
    return sw.toString();
  }

  protected String prefixFile2() {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    pw.println(TextFilePrefixSourceMarshaller.MAGIC);
    pw.println("# This is second prefix file!");
    pw.println("/org/sonatype");
    pw.println("/com/sonatype");
    return sw.toString();
  }

  protected String prefixFile3() {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    pw.println(TextFilePrefixSourceMarshaller.MAGIC);
    pw.println("# This is third prefix file!");
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
          // adding one proxy
          final M2Repository repo = (M2Repository) container.lookup(Repository.class, "maven2");
          CRepository repoConf = new DefaultCRepository();
          repoConf.setProviderRole(Repository.class.getName());
          repoConf.setProviderHint("maven2");
          repoConf.setId(PROXY3_REPO_ID);
          repoConf.setName(PROXY3_REPO_ID);
          repoConf.setNotFoundCacheActive(true);
          repoConf.setLocalStorage(new CLocalStorage());
          repoConf.getLocalStorage().setProvider("file");
          repoConf.getLocalStorage().setUrl(
              env.getApplicationConfiguration().getWorkingDirectory("proxy/store/" + PROXY3_REPO_ID).toURI().toURL()
                  .toString());
          Xpp3Dom ex = new Xpp3Dom("externalConfiguration");
          repoConf.setExternalConfiguration(ex);
          M2RepositoryConfiguration exConf = new M2RepositoryConfiguration(ex);
          exConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);
          exConf.setChecksumPolicy(ChecksumPolicy.STRICT_IF_EXISTS);
          repoConf.setRemoteStorage(new CRemoteStorage());
          repoConf.getRemoteStorage().setProvider(
              env.getRemoteProviderHintFactory().getDefaultHttpRoleHint());
          repoConf.getRemoteStorage().setUrl("http://localhost:" + server3.getPort() + "/");
          repo.configure(repoConf);
          // repo.setCacheManager( env.getCacheManager() );
          // reposes.add( repo.getId() );
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
          repoGroupConf.setId(GROUP1_REPO_ID);
          repoGroupConf.setName(GROUP1_REPO_ID);
          repoGroupConf.setLocalStorage(new CLocalStorage());
          repoGroupConf.getLocalStorage().setProvider("file");
          repoGroupConf.getLocalStorage().setUrl(
              env.getApplicationConfiguration().getWorkingDirectory("proxy/store/" + GROUP1_REPO_ID).toURI().toURL()
                  .toString());
          Xpp3Dom exGroupRepo = new Xpp3Dom("externalConfiguration");
          repoGroupConf.setExternalConfiguration(exGroupRepo);
          M2GroupRepositoryConfiguration exGroupRepoConf = new M2GroupRepositoryConfiguration(exGroupRepo);
          exGroupRepoConf.setMemberRepositoryIds(reposes);
          exGroupRepoConf.setMergeMetadata(true);
          group.configure(repoGroupConf);
          env.getApplicationConfiguration().getConfigurationModel().addRepository(repoGroupConf);
          env.getRepositoryRegistry().addRepository(group);
        }
        {
          // add 2nd group
          final M2GroupRepository group =
              (M2GroupRepository) container.lookup(GroupRepository.class, "maven2");
          CRepository repoGroupConf = new DefaultCRepository();
          repoGroupConf.setProviderRole(GroupRepository.class.getName());
          repoGroupConf.setProviderHint("maven2");
          repoGroupConf.setId(GROUP2_REPO_ID);
          repoGroupConf.setName(GROUP2_REPO_ID);
          repoGroupConf.setLocalStorage(new CLocalStorage());
          repoGroupConf.getLocalStorage().setProvider("file");
          repoGroupConf.getLocalStorage().setUrl(
              env.getApplicationConfiguration().getWorkingDirectory("proxy/store/" + GROUP2_REPO_ID).toURI().toURL()
                  .toString());
          Xpp3Dom exGroupRepo = new Xpp3Dom("externalConfiguration");
          repoGroupConf.setExternalConfiguration(exGroupRepo);
          M2GroupRepositoryConfiguration exGroupRepoConf = new M2GroupRepositoryConfiguration(exGroupRepo);
          exGroupRepoConf.setMemberRepositoryIds(Collections.singletonList(GROUP1_REPO_ID));
          exGroupRepoConf.setMergeMetadata(true);
          group.configure(repoGroupConf);
          env.getApplicationConfiguration().getConfigurationModel().addRepository(repoGroupConf);
          env.getRepositoryRegistry().addRepository(group);
        }
      }
    };
  }

  @Test
  public void contentUponBoot()
      throws Exception
  {
    final Manager wm = lookup(Manager.class);
    waitForRoutingBackgroundUpdates();

    // after boot, we should have group WL exist (as all 2 member WLs should exist)
    // group1 WL should be union of the 2 proxy member WLs
    {
      final PrefixSource groupPrefixSource =
          wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP1_REPO_ID,
              MavenRepository.class));

      assertThat("Group1 should have WL", groupPrefixSource.supported());

      final List<String> groupEntries = groupPrefixSource.readEntries();
      assertThat(groupEntries, hasSize(3));
      assertThat(groupEntries, hasItem("/com/sonatype"));
      assertThat(groupEntries, hasItem("/org/sonatype"));
      assertThat(groupEntries, hasItem("/org/apache"));
    }
    // group2 WL should be same as group1 (is only member)
    {
      final PrefixSource groupPrefixSource =
          wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP2_REPO_ID,
              MavenRepository.class));

      assertThat("Group2 should have WL", groupPrefixSource.supported());

      final List<String> groupEntries = groupPrefixSource.readEntries();
      assertThat(groupEntries, hasSize(3));
      assertThat(groupEntries, hasItem("/com/sonatype"));
      assertThat(groupEntries, hasItem("/org/sonatype"));
      assertThat(groupEntries, hasItem("/org/apache"));
    }
  }

  @Test
  public void contentOnMemberRemoval()
      throws Exception
  {
    // remove the proxy2 from group
    getRepositoryRegistry().getRepositoryWithFacet(GROUP1_REPO_ID, MavenGroupRepository.class).removeMemberRepositoryId(
        PROXY2_REPO_ID);
    getApplicationConfiguration().saveConfiguration();

    final Manager wm = lookup(Manager.class);
    waitForRoutingBackgroundUpdates();

    // group1 WL should have only proxy1 WL
    {
      final PrefixSource groupPrefixSource =
          wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP1_REPO_ID,
              MavenRepository.class));

      assertThat("Group1 should have WL", groupPrefixSource.supported());

      final List<String> groupEntries = groupPrefixSource.readEntries();
      assertThat(groupEntries, hasSize(1));
      assertThat(groupEntries, hasItem("/org/apache"));
    }
    // group2 WL should be same as group1 (is only member)
    {
      final PrefixSource groupPrefixSource =
          wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP2_REPO_ID,
              MavenRepository.class));

      assertThat("Group2 should have WL", groupPrefixSource.supported());

      final List<String> groupEntries = groupPrefixSource.readEntries();
      assertThat(groupEntries, hasSize(1));
      assertThat(groupEntries, hasItem("/org/apache"));
    }
  }

  @Test
  public void contentOnMemberAddition()
      throws Exception
  {
    // add the proxy3 to group
    getRepositoryRegistry().getRepositoryWithFacet(GROUP1_REPO_ID, MavenGroupRepository.class).addMemberRepositoryId(
        PROXY3_REPO_ID);
    getApplicationConfiguration().saveConfiguration();

    final Manager wm = lookup(Manager.class);
    waitForRoutingBackgroundUpdates();

    // after member addition, we should have all 3 WLs member in group
    // group1 WL should be union of the 3 proxy member WLs
    {
      final PrefixSource groupPrefixSource =
          wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP1_REPO_ID,
              MavenRepository.class));

      assertThat("Group1 should have WL", groupPrefixSource.supported());

      final List<String> groupEntries = groupPrefixSource.readEntries();
      assertThat(groupEntries, hasSize(4));
      assertThat(groupEntries, hasItem("/com/sonatype"));
      assertThat(groupEntries, hasItem("/org/sonatype"));
      assertThat(groupEntries, hasItem("/org/apache"));
      assertThat(groupEntries, hasItem("/eu/flatwhite"));
    }
    // group2 WL should be same as group1 (is only member)
    {
      final PrefixSource groupPrefixSource =
          wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP2_REPO_ID,
              MavenRepository.class));

      assertThat("Group2 should have WL", groupPrefixSource.supported());

      final List<String> groupEntries = groupPrefixSource.readEntries();
      assertThat(groupEntries, hasSize(4));
      assertThat(groupEntries, hasItem("/com/sonatype"));
      assertThat(groupEntries, hasItem("/org/sonatype"));
      assertThat(groupEntries, hasItem("/org/apache"));
      assertThat(groupEntries, hasItem("/eu/flatwhite"));
    }
  }

  @Test
  public void contentOnMemberAdditionAndRemoval()
      throws Exception
  {
    // remove the proxy2 from group
    // add the proxy3 to group
    getRepositoryRegistry().getRepositoryWithFacet(GROUP1_REPO_ID, MavenGroupRepository.class).removeMemberRepositoryId(
        PROXY2_REPO_ID);
    getRepositoryRegistry().getRepositoryWithFacet(GROUP1_REPO_ID, MavenGroupRepository.class).addMemberRepositoryId(
        PROXY3_REPO_ID);
    getApplicationConfiguration().saveConfiguration();

    final Manager wm = lookup(Manager.class);
    waitForRoutingBackgroundUpdates();

    // after member addition and removal, group1 should have proxy1 and proxt3 WL in group
    {
      final PrefixSource groupPrefixSource =
          wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP1_REPO_ID,
              MavenRepository.class));

      assertThat("Group1 should have WL", groupPrefixSource.supported());

      final List<String> groupEntries = groupPrefixSource.readEntries();
      assertThat(groupEntries, hasSize(2));
      assertThat(groupEntries, hasItem("/org/apache"));
      assertThat(groupEntries, hasItem("/eu/flatwhite"));
    }
    // group2 WL should be same as group1 (is only member)
    {
      final PrefixSource groupPrefixSource =
          wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP2_REPO_ID,
              MavenRepository.class));

      assertThat("Group2 should have WL", groupPrefixSource.supported());

      final List<String> groupEntries = groupPrefixSource.readEntries();
      assertThat(groupEntries, hasSize(2));
      assertThat(groupEntries, hasItem("/org/apache"));
      assertThat(groupEntries, hasItem("/eu/flatwhite"));
    }
  }

  @Test
  public void contentOnMemberAdditionAndRemovalInDifferentGroups()
      throws Exception
  {
    // remove the proxy2 from group1
    getRepositoryRegistry().getRepositoryWithFacet(GROUP1_REPO_ID, MavenGroupRepository.class).removeMemberRepositoryId(
        PROXY2_REPO_ID);
    getApplicationConfiguration().saveConfiguration();
    // add the proxy3 to group2
    getRepositoryRegistry().getRepositoryWithFacet(GROUP2_REPO_ID, MavenGroupRepository.class).addMemberRepositoryId(
        PROXY3_REPO_ID);
    getApplicationConfiguration().saveConfiguration();

    final Manager wm = lookup(Manager.class);
    waitForRoutingBackgroundUpdates();

    // after member removal, group1 should have proxy1 WL in group
    {
      final PrefixSource groupPrefixSource =
          wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP1_REPO_ID,
              MavenRepository.class));

      assertThat("Group1 should have WL", groupPrefixSource.supported());

      final List<String> groupEntries = groupPrefixSource.readEntries();
      assertThat(groupEntries, hasSize(1));
      assertThat(groupEntries, hasItem("/org/apache"));
    }
    // after member addition and removal (from member group1), group2 should have proxy1 and proxt3 WL in group
    {
      final PrefixSource groupPrefixSource =
          wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP2_REPO_ID,
              MavenRepository.class));

      assertThat("Group2 should have WL", groupPrefixSource.supported());

      final List<String> groupEntries = groupPrefixSource.readEntries();
      assertThat(groupEntries, hasSize(2));
      assertThat(groupEntries, hasItem("/org/apache"));
      assertThat(groupEntries, hasItem("/eu/flatwhite"));
    }
  }

  @Test
  public void contentOnMemberAdditionOfSameInDifferentGroups()
      throws Exception
  {
    // add the proxy3 to group1
    getRepositoryRegistry().getRepositoryWithFacet(GROUP1_REPO_ID, MavenGroupRepository.class).addMemberRepositoryId(
        PROXY3_REPO_ID);
    getApplicationConfiguration().saveConfiguration();
    // add the proxy3 to group2
    getRepositoryRegistry().getRepositoryWithFacet(GROUP2_REPO_ID, MavenGroupRepository.class).addMemberRepositoryId(
        PROXY3_REPO_ID);
    getApplicationConfiguration().saveConfiguration();

    final Manager wm = lookup(Manager.class);
    waitForRoutingBackgroundUpdates();

    // after member change, group1 should have proxy1, proxy2 and proxy3 WL in group
    {
      final PrefixSource groupPrefixSource =
          wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP1_REPO_ID,
              MavenRepository.class));

      assertThat("Group1 should have WL", groupPrefixSource.supported());

      final List<String> groupEntries = groupPrefixSource.readEntries();
      assertThat(groupEntries, hasSize(4));
      assertThat(groupEntries, hasItem("/org/apache"));
      assertThat(groupEntries, hasItem("/org/sonatype"));
      assertThat(groupEntries, hasItem("/com/sonatype"));
      assertThat(groupEntries, hasItem("/eu/flatwhite"));
    }
    // after member change, group2 should have g1+p3 but is same as g1 (since p3 is member of g1 too) -- (WL is
    // kept unique)
    {
      final PrefixSource groupPrefixSource =
          wm.getPrefixSourceFor(getRepositoryRegistry().getRepositoryWithFacet(GROUP2_REPO_ID,
              MavenRepository.class));

      assertThat("Group2 should have WL", groupPrefixSource.supported());

      final List<String> groupEntries = groupPrefixSource.readEntries();
      assertThat(groupEntries, hasSize(4));
      assertThat(groupEntries, hasItem("/org/apache"));
      assertThat(groupEntries, hasItem("/org/sonatype"));
      assertThat(groupEntries, hasItem("/com/sonatype"));
      assertThat(groupEntries, hasItem("/eu/flatwhite"));
    }
  }
}
