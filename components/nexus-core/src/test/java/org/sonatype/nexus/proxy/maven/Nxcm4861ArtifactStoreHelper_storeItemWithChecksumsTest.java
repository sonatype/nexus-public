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
package org.sonatype.nexus.proxy.maven;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.model.CLocalStorage;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.DefaultCRepository;
import org.sonatype.nexus.proxy.AbstractProxyTestEnvironment;
import org.sonatype.nexus.proxy.EnvironmentBuilder;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;
import org.sonatype.nexus.proxy.repository.Repository;

import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.verify;

/**
 * UT for NXCM-4861:
 * {@link ArtifactStoreHelper#storeItemWithChecksums(boolean, org.sonatype.nexus.proxy.item.AbstractStorageItem)} was
 * in
 * some cases causing proxying to kick in (after store item was retrieved from remote -- only in case of procurement
 * reposes but still wrong). Despite extending {@link AbstractProxyTestEnvironment} it actually uses a hosted repo
 * only,
 * and using spy determines actually happened invocations.
 */
public class Nxcm4861ArtifactStoreHelper_storeItemWithChecksumsTest
    extends AbstractProxyTestEnvironment
{

  private static final String REPO_ID = "inhouse";

  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
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

  @Test
  public void retrieveAfterStoreShouldBeLocalOnly()
      throws Exception
  {
    final MavenRepository realMavenRepository =
        getRepositoryRegistry().getRepositoryWithFacet(REPO_ID, MavenRepository.class);
    final MavenRepository mavenRepository = Mockito.spy(realMavenRepository);
    // why? There is no related change, but it works on master?
    Mockito.when(mavenRepository.getArtifactStoreHelper()).thenReturn(new ArtifactStoreHelper(mavenRepository));

    // invoke storeWithChecksums
    final String PATH = "/group/artifact/1.0/artifact-1.0.jar";
    final ResourceStoreRequest request = new ResourceStoreRequest(PATH);
    mavenRepository.storeItemWithChecksums(request, new ByteArrayInputStream("some fluke content".getBytes()),
        null);

    // verify side effects (storage changes)
    assertThat("Artifact should be stored",
        mavenRepository.getLocalStorage().containsItem(mavenRepository, new ResourceStoreRequest(PATH)),
        is(true));
    assertThat(
        "Artifact checksum should be stored",
        mavenRepository.getLocalStorage().containsItem(mavenRepository, new ResourceStoreRequest(PATH + ".sha1")),
        is(true));

    // verify params
    final ArgumentCaptor<ResourceStoreRequest> ac = ArgumentCaptor.forClass(ResourceStoreRequest.class);
    verify(mavenRepository).retrieveItem(anyBoolean(), ac.capture());

    // we need to have exactly one retrieve invocation
    assertThat("We must have one retrieve happened!", ac.getAllValues().size(), equalTo(1));
    // and that one must be localOnly
    assertThat("Request " + ac.getValue() + " should be localOnly!", ac.getValue().isRequestLocalOnly(), is(true));
  }
}
