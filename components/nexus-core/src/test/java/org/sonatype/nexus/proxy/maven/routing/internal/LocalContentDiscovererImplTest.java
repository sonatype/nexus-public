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
import java.util.Arrays;
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
import org.sonatype.nexus.proxy.maven.routing.PrefixSource;
import org.sonatype.nexus.proxy.maven.routing.discovery.DiscoveryResult;
import org.sonatype.nexus.proxy.maven.routing.discovery.LocalContentDiscoverer;
import org.sonatype.nexus.proxy.repository.Repository;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class LocalContentDiscovererImplTest
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

  protected LocalContentDiscoverer localContentDiscoverer;

  @Before
  public void prepare()
      throws Exception
  {
    localContentDiscoverer = lookup(LocalContentDiscoverer.class);
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
      mavenRepository.deleteItemWithChecksums(request);
    }
  }

  @Test
  public void smoke()
      throws Exception
  {
    final MavenHostedRepository mavenRepository =
        getRepositoryRegistry().getRepositoryWithFacet(REPO_ID, MavenHostedRepository.class);

    addSomeContent(mavenRepository, PATHS1);

    {
      final DiscoveryResult<MavenRepository> result =
          localContentDiscoverer.discoverLocalContent(mavenRepository);
      assertThat(result, notNullValue());
      assertThat(result.isSuccessful(), is(true));
      assertThat(result.getLastResult().getStrategyId(), equalTo("local"));
      assertThat(result.getLastResult().getMessage(), notNullValue());
      final PrefixSource entrySource = result.getPrefixSource();
      assertThat(
          entrySource.readEntries(),
          hasItems("/archetype-catalog.xml", "/archetype-catalog.xml.sha1", "/archetype-catalog.xml.md5",
              "/org/sonatype", "/org/apache"));
      assertThat(entrySource.readEntries(), not(hasItems("/com/sonatype")));
      assertThat(entrySource.readEntries().size(), equalTo(5));
    }

    addSomeContent(mavenRepository, PATHS2);

    {
      final DiscoveryResult<MavenRepository> result =
          localContentDiscoverer.discoverLocalContent(mavenRepository);
      assertThat(result, notNullValue());
      assertThat(result.isSuccessful(), is(true));
      assertThat(result.getLastResult().getStrategyId(), equalTo("local"));
      assertThat(result.getLastResult().getMessage(), notNullValue());
      final PrefixSource entrySource = result.getPrefixSource();
      assertThat(
          entrySource.readEntries(),
          hasItems("/archetype-catalog.xml", "/archetype-catalog.xml.sha1", "/archetype-catalog.xml.md5",
              "/org/sonatype", "/com/sonatype", "/org/apache"));
      assertThat(entrySource.readEntries().size(), equalTo(6));
    }

    removeSomeContent(mavenRepository, PATHS3);

    {
      final DiscoveryResult<MavenRepository> result =
          localContentDiscoverer.discoverLocalContent(mavenRepository);
      assertThat(result, notNullValue());
      assertThat(result.isSuccessful(), is(true));
      assertThat(result.getLastResult().getStrategyId(), equalTo("local"));
      assertThat(result.getLastResult().getMessage(), notNullValue());
      final PrefixSource entrySource = result.getPrefixSource();
      assertThat(
          entrySource.readEntries(),
          hasItems("/archetype-catalog.xml", "/archetype-catalog.xml.sha1", "/archetype-catalog.xml.md5",
              "/com/sonatype", "/org/apache"));
      // NEXUS-6485: Not true anymore, we do include empty directories due to "depth" optimization
      // see LocalContentDiscovererImpl
      // assertThat(entrySource.readEntries(), not(hasItems("/org/sonatype")));
      assertThat(entrySource.readEntries().size(), equalTo(6)); // was 5
    }
  }
}
