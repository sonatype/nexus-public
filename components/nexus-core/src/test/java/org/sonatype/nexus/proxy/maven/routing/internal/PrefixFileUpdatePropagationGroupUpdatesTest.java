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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.MavenGroupRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.routing.events.PrefixFilePublishedRepositoryEvent;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.Subscribe;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;

/**
 * Testing issues NEXUS-5602 and NEXUS-5608
 *
 * @author cstamas
 */
public class PrefixFileUpdatePropagationGroupUpdatesTest
    extends AbstractRoutingProxyTest
{
  private static final String HOSTED1_REPO_ID = "hosted1";

  private static final String HOSTED2_REPO_ID = "hosted2";

  private static final String GROUP1_REPO_ID = "group1";

  private static final String GROUP2_REPO_ID = "group2";

  private final PrefixFileUpdateListener prefixFileUpdateListener;

  public PrefixFileUpdatePropagationGroupUpdatesTest()
      throws Exception
  {
    this.prefixFileUpdateListener = new PrefixFileUpdateListener();
  }

  protected static class PrefixFileUpdateListener
  {
    private final List<String> publishedIds;

    public PrefixFileUpdateListener() {
      this.publishedIds = new ArrayList<String>();
      reset();
    }

    @Subscribe
    public void on(final PrefixFilePublishedRepositoryEvent evt) {
      publishedIds.add(evt.getRepository().getId());
    }

    public List<String> getPublished() {
      return publishedIds;
    }

    public void reset() {
      publishedIds.clear();
    }
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
          // adding one hosted
          final M2Repository repo = (M2Repository) container.lookup(Repository.class, "maven2");
          CRepository repoConf = new DefaultCRepository();
          repoConf.setProviderRole(Repository.class.getName());
          repoConf.setProviderHint("maven2");
          repoConf.setId(HOSTED1_REPO_ID);
          repoConf.setName(HOSTED1_REPO_ID);
          repoConf.setLocalStorage(new CLocalStorage());
          repoConf.getLocalStorage().setProvider("file");
          repoConf.getLocalStorage().setUrl(
              env.getApplicationConfiguration().getWorkingDirectory("proxy/store/" + HOSTED1_REPO_ID).toURI().toURL()
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
          // adding one hosted
          final M2Repository repo = (M2Repository) container.lookup(Repository.class, "maven2");
          CRepository repoConf = new DefaultCRepository();
          repoConf.setProviderRole(Repository.class.getName());
          repoConf.setProviderHint("maven2");
          repoConf.setId(HOSTED2_REPO_ID);
          repoConf.setName(HOSTED2_REPO_ID);
          repoConf.setLocalStorage(new CLocalStorage());
          repoConf.getLocalStorage().setProvider("file");
          repoConf.getLocalStorage().setUrl(
              env.getApplicationConfiguration().getWorkingDirectory("proxy/store/" + HOSTED2_REPO_ID).toURI().toURL()
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

        // register it here BEFORE boot process starts but plx is already created
        container.lookup(EventBus.class).register(prefixFileUpdateListener);
      }
    };
  }

  @Override
  protected boolean enableAutomaticRoutingFeature() {
    return true;
  }

  @Test
  public void testUpdateCountOnBootWithoutWL() {
    // boot already happened
    // we have 2 hosted and both are members of the group
    // as we have no WL (clean boot/kinda upgrade), all of them was 1st marked for noscrape,
    // and then H1 and H2 got WL updated concurrently in bg job, and as side effect group WL got updated too
    // This means, that group might be updated once or twice, depending on concurrency.
    // So the list might contain (in any order):
    // HOSTED1, HOSTED2, GROUP1, GROUP2
    // or
    // HOSTED1, HOSTED2, GROUP1, GROUP1, GROUP2, GROUP...
    // (group1 one or two times updated).
    assertThat(prefixFileUpdateListener.getPublished(), hasItem(HOSTED1_REPO_ID));
    assertThat(prefixFileUpdateListener.getPublished(), hasItem(HOSTED2_REPO_ID));
    assertThat(prefixFileUpdateListener.getPublished(), hasItem(GROUP1_REPO_ID));
    assertThat(prefixFileUpdateListener.getPublished(), hasItem(GROUP2_REPO_ID));
  }

  @Test
  public void testUpdateCountOnGroupMemberChange()
      throws Exception
  {
    // in case of group member changes, the "cascade" is sync, hence
    // we have no ordering problem as we have with async updates of proxy/hosted
    // reposes on boot
    prefixFileUpdateListener.reset();

    final MavenGroupRepository mgr =
        getRepositoryRegistry().getRepositoryWithFacet(GROUP1_REPO_ID, MavenGroupRepository.class);

    mgr.removeMemberRepositoryId(HOSTED1_REPO_ID);
    getApplicationConfiguration().saveConfiguration();
    waitForRoutingBackgroundUpdates();

    assertThat(prefixFileUpdateListener.getPublished(), contains(GROUP1_REPO_ID, GROUP2_REPO_ID));

    mgr.addMemberRepositoryId(HOSTED1_REPO_ID);
    getApplicationConfiguration().saveConfiguration();
    waitForRoutingBackgroundUpdates();

    assertThat(prefixFileUpdateListener.getPublished(),
        contains(GROUP1_REPO_ID, GROUP2_REPO_ID, GROUP1_REPO_ID, GROUP2_REPO_ID));
  }

  @Test
  public void testUpdateCountOnGroupOfGroupMemberChange()
      throws Exception
  {
    // in case of group member changes, the "cascade" is sync, hence
    // we have no ordering problem as we have with async updates of proxy/hosted
    // reposes on boot
    prefixFileUpdateListener.reset();

    final MavenGroupRepository mgr =
        getRepositoryRegistry().getRepositoryWithFacet(GROUP2_REPO_ID, MavenGroupRepository.class);

    mgr.removeMemberRepositoryId(GROUP1_REPO_ID);
    mgr.addMemberRepositoryId(HOSTED1_REPO_ID);
    getApplicationConfiguration().saveConfiguration();
    waitForRoutingBackgroundUpdates();

    assertThat(prefixFileUpdateListener.getPublished(), contains(GROUP2_REPO_ID));

    mgr.addMemberRepositoryId(HOSTED2_REPO_ID);
    getApplicationConfiguration().saveConfiguration();
    waitForRoutingBackgroundUpdates();

    assertThat(prefixFileUpdateListener.getPublished(),
        contains(GROUP2_REPO_ID, GROUP2_REPO_ID));
  }
}
