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
package org.sonatype.nexus.repository.apt.api;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.apt.internal.AptFormat;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class AptApiRepositoryAdapterTest
    extends TestSupport
{
  private AptApiRepositoryAdapter underTest;

  @Mock
  private RoutingRuleStore routingRuleStore;

  private Configuration configuration = new Configuration();

  @Before
  public void setup() {
    underTest = new AptApiRepositoryAdapter(routingRuleStore);
    BaseUrlHolder.set("http://nexus-url");

    configuration.setOnline(true);
  }

  @Test
  public void testAdapt_hostedRepository() throws Exception {
    Repository repository = createRepository(new HostedType(), "bionic", "asdf", null, null);

    AptHostedApiRepository hostedRepository = (AptHostedApiRepository) underTest.adapt(repository);
    assertRepository(hostedRepository, "hosted", true);
    assertThat(hostedRepository.getApt().getDistribution(), is("bionic"));
    assertThat(hostedRepository.getAptSigning(), nullValue());

    // only include public key if it is encrypted
    repository.getConfiguration().attributes("apt").set("passphrase", "mypass");
    hostedRepository = (AptHostedApiRepository) underTest.adapt(repository);
    assertThat(hostedRepository.getAptSigning().getKeypair(), is("asdf"));
    assertThat(hostedRepository.getAptSigning().getPassphrase(), nullValue());
  }

  @Test
  public void testAdapt_proxyRepository() throws Exception {
    Repository repository = createRepository(new ProxyType(), "bionic", null, null, true);

    AptProxyApiRepository proxyRepository = (AptProxyApiRepository) underTest.adapt(repository);
    assertRepository(proxyRepository, "proxy", true);
    assertThat(proxyRepository.getApt().getDistribution(), is("bionic"));
    assertThat(proxyRepository.getApt().getFlat(), is(true));
  }

  private static void assertRepository(
      final AbstractApiRepository repository,
      final String type,
      final Boolean online)
  {
    assertThat(repository.getFormat(), is("apt"));
    assertThat(repository.getName(), is("my-repo"));
    assertThat(repository.getOnline(), is(online));
    assertThat(repository.getType(), is(type));
    assertThat(repository.getUrl(), is(BaseUrlHolder.get() + "/repository/my-repo"));
  }

  private static Configuration config(final String repositoryName) {
    Configuration configuration = new Configuration();
    configuration.setOnline(true);
    configuration.setRepositoryName(repositoryName);
    return configuration;
  }

  private static Repository createRepository(
      final Type type,
      final String distribution,
      final String keypair,
      final String passphrase,
      final Boolean flat) throws Exception
  {
    Repository repository = new RepositoryImpl(Mockito.mock(EventManager.class), type, new AptFormat());

    Configuration configuration = config("my-repo");
    NestedAttributesMap maven = configuration.attributes("apt");
    maven.set("distribution", distribution);
    maven.set("keypair", keypair);
    maven.set("passphrase", passphrase);
    maven.set("flat", flat);
    repository.init(configuration);
    return repository;
  }
}
