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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.routing.Manager;
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.nexus.proxy.repository.Repository;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

public class PrefixFileMaintenanceTest
    extends AbstractRoutingProxyTest
{
  private static final String REPO_ID = "inhouse";

  private static final List<String> PATHS1 = Arrays.asList("/archetype-catalog.xml",
      "/org/sonatype/artifact/1.0/artifact-1.0.jar", "/org/apache/artifact/1.0/artifact-1.0.jar");

  private static final List<String> PATHS2 = Arrays.asList("/com/sonatype/artifact/1.0/artifact-1.0.jar",
      "/org/apache/artifact/2.0/artifact-2.0.jar");

  private static final List<String> PATHS3 = Arrays.asList("/org/sonatype/artifact/1.0/artifact-1.0.jar",
      "/org/apache/artifact/1.0/artifact-1.0.jar");

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
        // ading one hosted only
        final M2Repository repo = (M2Repository) container.lookup(Repository.class, "maven2");
        CRepository repoConf = new DefaultCRepository();
        repoConf.setProviderRole(Repository.class.getName());
        repoConf.setProviderHint("maven2");
        repoConf.setId(REPO_ID);
        repoConf.setLocalStorage(new CLocalStorage());
        repoConf.getLocalStorage().setProvider("file");
        repoConf.getLocalStorage().setUrl(
            env.getApplicationConfiguration().getWorkingDirectory("proxy/store/inhouse").toURI().toURL().toString());
        Xpp3Dom exRepo = new Xpp3Dom("externalConfiguration");
        repoConf.setExternalConfiguration(exRepo);
        M2RepositoryConfiguration exRepoConf = new M2RepositoryConfiguration(exRepo);
        exRepoConf.setRepositoryPolicy(RepositoryPolicy.RELEASE);
        repo.configure(repoConf);
        env.getApplicationConfiguration().getConfigurationModel().addRepository(repoConf);
        env.getRepositoryRegistry().addRepository(repo);
      }
    };
  }

  @Override
  protected boolean enableAutomaticRoutingFeature() {
    return true;
  }

  protected Manager manager;

  @Before
  public void prepare()
      throws Exception
  {
    manager = lookup(Manager.class);
  }

  protected List<String> getEntriesOf(final MavenRepository mavenRepository)
      throws IOException
  {
    final PrefixSource entrySource = manager.getPrefixSourceFor(mavenRepository);
    final ArrayList<String> result = new ArrayList<String>(entrySource.readEntries());
    Collections.sort(result);
    return result;
  }

  protected void addSomeContent(final MavenRepository mavenRepository, final List<String> paths)
      throws Exception
  {
    for (String path : paths) {
      final ResourceStoreRequest request = new ResourceStoreRequest(path);
      mavenRepository.storeItemWithChecksums(request,
          new ByteArrayInputStream("some fluke content".getBytes()), null);
    }
  }

  protected void removeSomeContent(final MavenRepository mavenRepository, final List<String> paths)
      throws Exception
  {
    for (String path : paths) {
      final ResourceStoreRequest request = new ResourceStoreRequest(path);
      mavenRepository.deleteItem(request);
    }
  }

  /**
   * Test is prefix list maintained properly during content changes in hosted repository (only type where we maintain
   * prefix list based on deploys or deletions). But, there is a trick: when you delete an artifact (by using
   * "direct"
   * full URL like the one you deployed it with), Nexus will leave parent directories empty. While when crawling this
   * is handled (we crawl all way down to content, but "cut the tree" at needed level), with events in case of DELETE
   * is not so simple. First, if you delete the given artifact only, Nexus will leave parent directories untouched
   * even if they are empty, no events will be emitted. So, entry deletion only works in cases when: a) the given
   * folder is deleted that is in the whitelist (see in test), OR, b) full update of given hosted repo is done.
   */
  @Test
  public void smoke()
      throws Exception
  {
    final MavenHostedRepository mavenRepository =
        getRepositoryRegistry().getRepositoryWithFacet(REPO_ID, MavenHostedRepository.class);

    // initially prefix list is empty
    {
      final List<String> entries = getEntriesOf(mavenRepository);
      assertThat(entries, hasSize(0));
    }

    addSomeContent(mavenRepository, PATHS1);

    {
      final List<String> entries = getEntriesOf(mavenRepository);
      assertThat(entries, hasSize(5));
      assertThat(
          entries,
          containsInAnyOrder("/archetype-catalog.xml", "/archetype-catalog.xml.sha1", "/archetype-catalog.xml.md5",
              "/org/apache", "/org/sonatype"));
    }

    addSomeContent(mavenRepository, PATHS2);

    {
      final List<String> entries = getEntriesOf(mavenRepository);
      assertThat(entries, hasSize(6));
      assertThat(
          entries,
          containsInAnyOrder("/archetype-catalog.xml", "/archetype-catalog.xml.sha1", "/archetype-catalog.xml.md5",
              "/com/sonatype", "/org/apache", "/org/sonatype"));
    }

    // PATHS3 contains full paths, but prefix list contains "/org/sonatype" and nothing will emit event carrying
    // that path!
    // so, the prefix list remains unchanged!
    removeSomeContent(mavenRepository, PATHS3);

    {
      final List<String> entries = getEntriesOf(mavenRepository);
      assertThat(entries, hasSize(6));
      assertThat(
          entries,
          containsInAnyOrder("/archetype-catalog.xml", "/archetype-catalog.xml.sha1", "/archetype-catalog.xml.md5",
              "/com/sonatype", "/org/apache", "/org/sonatype"));
    }

    // by deleting given folder, we will get the needed result.
    // prefix list entry "/org/sonatype" should be removed as we deleted the "/org/sonatype" folder
    removeSomeContent(mavenRepository, Arrays.asList("/org/sonatype"));

    {
      final List<String> entries = getEntriesOf(mavenRepository);
      assertThat(entries, hasSize(5));
      assertThat(
          entries,
          containsInAnyOrder("/archetype-catalog.xml", "/archetype-catalog.xml.sha1", "/archetype-catalog.xml.md5",
              "/com/sonatype", "/org/apache"));
    }

    // by deleting a parent of the given folder, we will also get the needed result.
    // prefix list entry "/com/sonatype" should be removed as we deleted the "/com" folder
    removeSomeContent(mavenRepository, Arrays.asList("/com"));

    {
      final List<String> entries = getEntriesOf(mavenRepository);
      assertThat(entries, hasSize(4));
      assertThat(
          entries,
          containsInAnyOrder("/archetype-catalog.xml", "/archetype-catalog.xml.sha1", "/archetype-catalog.xml.md5",
              "/org/apache"));
    }
  }
}
