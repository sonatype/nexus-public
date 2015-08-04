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
package org.sonatype.nexus.testsuite.repository;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.client.core.subsystem.repository.ProxyRepository;
import org.sonatype.nexus.client.core.subsystem.repository.Repositories;
import org.sonatype.nexus.client.core.subsystem.repository.maven.MavenProxyRepository;
import org.sonatype.nexus.client.internal.msg.ErrorMessage;
import org.sonatype.nexus.client.internal.msg.ErrorResponse;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.RepositoryResourceRemoteStorage;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Verify that default values are applied when a new repository is created with only the mandatory parameters.
 */
@NexusStartAndStopStrategy(NexusStartAndStopStrategy.Strategy.EACH_TEST)
public class RepositoryDefaultValuesOnCreationIT
    extends NexusRunningParametrizedITSupport
{

  public RepositoryDefaultValuesOnCreationIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    configuration.addPlugins(artifactResolver().resolvePluginFromDependencyManagement(
        "org.sonatype.nexus.plugins", "nexus-restlet1x-testsupport-plugin"));

    return configuration;
  }

  @Test
  public void createMaven2RepoWithDefaultValues() {
    final String id = uniqueName("nxcm5131-m2");

    doCreateMaven2Repo(id);

    MavenProxyRepository repository = repositories().get(id);

    assertThat(repository.id(), is(id));
    assertThat(repository.name(), is(id));

    assertThat(repository.itemMaxAge(), is(1440));
    assertThat(repository.artifactMaxAge(), is(0));
    assertThat(repository.metadataMaxAge(), is(0));

    assertThat(repository.isExposed(), is(false));
    assertThat(repository.isBrowsable(), is(false));
    assertThat(repository.isAutoBlocking(), is(true));
  }

  @Test
  public void createNonMaven2RepoWithDefaultValues() {
    final String id = uniqueName("nxcm5131");

    doCreateNxcm5131Repo(id);

    ProxyRepository repository = repositories().get(id);

    assertThat(repository.id(), is(id));
    assertThat(repository.name(), is(id));

    assertThat(repository.itemMaxAge(), is(1440));

    assertThat(repository.isExposed(), is(false));
    assertThat(repository.isAutoBlocking(), is(true));
  }

  private void doCreateNxcm5131Repo(final String id) {
    final RepositoryResourceResponse request = new RepositoryResourceResponse();
    final RepositoryProxyResource repo = new RepositoryProxyResource();
    repo.setId(id);
    repo.setFormat("nxcm5131");
    repo.setRepoType("proxy");
    repo.setRepoPolicy("MIXED");
    repo.setChecksumPolicy("IGNORE");
    repo.setProvider("nxcm5131");
    repo.setProviderRole("org.sonatype.nexus.proxy.repository.Repository");
    final RepositoryResourceRemoteStorage remoteStorage = new RepositoryResourceRemoteStorage();
    remoteStorage.setRemoteStorageUrl("http://obvious.fake/url");
    repo.setRemoteStorage(remoteStorage);
    request.setData(repo);

    doCreateRepo(request);
  }

  private void doCreateMaven2Repo(final String id) {
    final RepositoryResourceResponse request = new RepositoryResourceResponse();
    final RepositoryProxyResource repo = new RepositoryProxyResource();
    repo.setId(id);
    repo.setFormat("maven2");
    repo.setRepoType("proxy");
    repo.setRepoPolicy("RELEASE");
    repo.setChecksumPolicy("IGNORE");
    repo.setProvider("maven2");
    repo.setProviderRole("org.sonatype.nexus.proxy.repository.Repository");
    final RepositoryResourceRemoteStorage remoteStorage = new RepositoryResourceRemoteStorage();
    remoteStorage.setRemoteStorageUrl("http://obvious.fake/url");
    repo.setRemoteStorage(remoteStorage);
    request.setData(repo);

    doCreateRepo(request);
  }

  private void doCreateRepo(final RepositoryResourceResponse request) {
    try {
      final RepositoryBaseResource response = ((JerseyNexusClient) client())
          .serviceResource("repositories")
          .post(RepositoryResourceResponse.class, request)
          .getData();
    }
    catch (UniformInterfaceException e) {
      final ErrorResponse response = e.getResponse().getEntity(ErrorResponse.class);
      for (ErrorMessage message : response.getErrors()) {
        System.err.println(message.getId() + ": " + message.getMsg());
      }
      throw e;
    }
  }

  private static String uniqueName(final String prefix) {
    return prefix + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
  }

  private Repositories repositories() {
    return client().getSubsystem(Repositories.class);
  }

}
