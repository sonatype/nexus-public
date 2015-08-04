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
package org.sonatype.nexus.proxy;

import org.sonatype.jettytestsuite.ServletServer;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.maven.MavenHostedRepository;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.proxy.storage.remote.RemoteRepositoryStorage;

import org.junit.Test;

public class RepoConversionTest
    extends AbstractProxyTestEnvironment
{
  private M2TestsuiteEnvironmentBuilder jettyTestsuiteEnvironmentBuilder;

  private RemoteRepositoryStorage remoteRepositoryStorage;

  public void setUp()
      throws Exception
  {
    super.setUp();

    remoteRepositoryStorage = lookup(RemoteRepositoryStorage.class,
        getRemoteProviderHintFactory().getDefaultHttpRoleHint());
  }

  @Override
  protected EnvironmentBuilder getEnvironmentBuilder()
      throws Exception
  {
    ServletServer ss = (ServletServer) lookup(ServletServer.ROLE);
    this.jettyTestsuiteEnvironmentBuilder = new M2TestsuiteEnvironmentBuilder(ss);
    return jettyTestsuiteEnvironmentBuilder;
  }

  // WARNING!
  // ////////////////////////////
  // The casts you see in this code should be considered ILLEGAL!
  // This code simply tests some "spicy" nature, but Nexus plugins and other API consumers should
  // NEVER use casts like these below!

  protected void convertHosted2Proxy(MavenHostedRepository patient)
      throws Exception
  {
    // check
    assertTrue("repo is hosted", patient.getRepositoryKind().isFacetAvailable(HostedRepository.class));
    assertTrue("repo is hosted", patient.getRepositoryKind().isFacetAvailable(MavenHostedRepository.class));
    assertFalse("repo is not proxied", patient.getRepositoryKind().isFacetAvailable(ProxyRepository.class));
    assertFalse("repo is not proxied", patient.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class));

    // do the conversion
    // forcing cast
    M2Repository repoToBeTreated = (M2Repository) patient;

    repoToBeTreated.setRemoteStorage(remoteRepositoryStorage);

    repoToBeTreated.setRemoteUrl("http://repo1.maven.org/maven2/");

    getApplicationConfiguration().saveConfiguration();

    // check
    assertFalse("repo is not hosted", patient.getRepositoryKind().isFacetAvailable(HostedRepository.class));
    assertFalse("repo is not hosted", patient.getRepositoryKind().isFacetAvailable(MavenHostedRepository.class));
    assertTrue("repo is proxied", patient.getRepositoryKind().isFacetAvailable(ProxyRepository.class));
    assertTrue("repo is proxied", patient.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class));

    // now we just walk in, like nothing of above happened :)
    M2Repository afterTreatment =
        (M2Repository) getRepositoryRegistry().getRepositoryWithFacet(patient.getId(), MavenProxyRepository.class);

    assertNotNull("It should exists (heh, but NoSuchRepo exception should be thrown anyway)", afterTreatment);

    assertEquals("This should match, since they should be the same!", remoteRepositoryStorage.getProviderId(),
        afterTreatment.getRemoteStorage().getProviderId());

    // before NEXUS-5258, this test used to check that the provider was set. Nexus does not set the provider anymore
    // if it is the default provider, to let repos pick up the system default.

    assertEquals("Config should state the same as object is", afterTreatment.getRemoteUrl(),
        (((CRepositoryCoreConfiguration) afterTreatment.getCurrentCoreConfiguration())
            .getConfiguration(false)).getRemoteStorage().getUrl());
  }

  protected void convertProxy2Hosted(MavenProxyRepository patient)
      throws Exception
  {
    // check
    assertFalse("repo is not hosted", patient.getRepositoryKind().isFacetAvailable(HostedRepository.class));
    assertFalse("repo is not hosted", patient.getRepositoryKind().isFacetAvailable(MavenHostedRepository.class));
    assertTrue("repo is proxied", patient.getRepositoryKind().isFacetAvailable(ProxyRepository.class));
    assertTrue("repo is proxied", patient.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class));

    // do the conversion
    patient.setRemoteStorage(null);

    getApplicationConfiguration().saveConfiguration();

    // check
    assertTrue("repo is hosted", patient.getRepositoryKind().isFacetAvailable(HostedRepository.class));
    assertTrue("repo is hosted", patient.getRepositoryKind().isFacetAvailable(MavenHostedRepository.class));
    assertFalse("repo is not proxied", patient.getRepositoryKind().isFacetAvailable(ProxyRepository.class));
    assertFalse("repo is not proxied", patient.getRepositoryKind().isFacetAvailable(MavenProxyRepository.class));

    // now we just walk in, like nothing of above happened :)
    MavenHostedRepository afterTreatment =
        getRepositoryRegistry().getRepositoryWithFacet(patient.getId(), MavenHostedRepository.class);

    assertNotNull("It should exists (heh, but NoSuchRepo exception should be thrown anyway)", afterTreatment);
  }

  @Test
  public void testHosted2Proxy()
      throws Exception
  {
    Repository patient = getRepositoryRegistry().getRepositoryWithFacet("inhouse", MavenHostedRepository.class);

    assertTrue("This repo should not be READ only!", RepositoryWritePolicy.READ_ONLY != patient.getWritePolicy());

    convertHosted2Proxy((MavenHostedRepository) patient);

    assertTrue("Partient should be proxy", patient.getRepositoryKind()
        .isFacetAvailable(MavenProxyRepository.class));

    assertTrue("This repo should be READ only!", RepositoryWritePolicy.READ_ONLY == patient.getWritePolicy());
  }

  @Test
  public void testProxy2Hosted()
      throws Exception
  {
    Repository patient = getRepositoryRegistry().getRepositoryWithFacet("repo1", MavenProxyRepository.class);

    assertTrue("This repo should be READ only!", RepositoryWritePolicy.READ_ONLY == patient.getWritePolicy());

    convertProxy2Hosted((MavenProxyRepository) patient);

    assertTrue("Partient should be hosted", patient.getRepositoryKind()
        .isFacetAvailable(MavenHostedRepository.class));
  }

  @Test
  public void testHosted2Proxy2Hosted()
      throws Exception
  {
    Repository patient = getRepositoryRegistry().getRepositoryWithFacet("inhouse", MavenHostedRepository.class);

    convertHosted2Proxy((MavenHostedRepository) patient);

    convertProxy2Hosted((MavenProxyRepository) patient);

    assertTrue("Partient should be hosted", patient.getRepositoryKind()
        .isFacetAvailable(MavenHostedRepository.class));
  }

  @Test
  public void testProxy2Hosted2Proxy()
      throws Exception
  {
    Repository patient = getRepositoryRegistry().getRepositoryWithFacet("repo1", MavenProxyRepository.class);

    convertProxy2Hosted((MavenProxyRepository) patient);

    convertHosted2Proxy((MavenHostedRepository) patient);

    assertTrue("Partient should be proxy", patient.getRepositoryKind()
        .isFacetAvailable(MavenProxyRepository.class));
  }

}
