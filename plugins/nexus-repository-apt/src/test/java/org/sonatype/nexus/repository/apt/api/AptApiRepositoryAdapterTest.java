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

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AptApiRepositoryAdapterTest
    extends TestSupport
{
  private AptApiRepositoryAdapter underTest;

  @Mock
  private RoutingRuleStore routingRuleStore;

  @Before
  public void setup() {
    underTest = new AptApiRepositoryAdapter(routingRuleStore);
    BaseUrlHolder.set("http://nexus-url", "");
  }

  @Test
  public void testAdapt_hostedRepository() throws Exception {
    Repository repository = createRepository(new HostedType(), "bionic", "asdf", null, null);

    AptHostedApiRepository hostedRepository = (AptHostedApiRepository) underTest.adapt(repository);
    assertRepository(hostedRepository, "hosted", true);
    assertThat(hostedRepository.getApt().getDistribution(), is("bionic"));
    assertThat(hostedRepository.getAptSigning(), nullValue());

    // only include public key if it is encrypted
    repository.getConfiguration().attributes("aptSigning").set("passphrase", "mypass");
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
    Configuration configuration = mock(Configuration.class);
    when(configuration.isOnline()).thenReturn(true);
    when(configuration.getRepositoryName()).thenReturn(repositoryName);
    when(configuration.attributes(not(startsWith("apt")))).thenReturn(new NestedAttributesMap("dummy", newHashMap()));
    return configuration;
  }

  private static Repository createRepository(
      final Type type,
      final String distribution,
      final String keypair,
      final String passphrase,
      final Boolean flat) throws Exception
  {
    Repository repository = new RepositoryImpl(mock(EventManager.class), type, new AptFormat());

    Configuration configuration = config("my-repo");
    Map<String, Object> attributes = newHashMap();

    Map<String, Object> apt = newHashMap();
    apt.put("distribution", distribution);
    apt.put("flat", flat);
    attributes.put("apt", apt);
    NestedAttributesMap aptNested = new NestedAttributesMap("apt", apt);
    when(configuration.attributes("apt")).thenReturn(aptNested);

    Map<String, Object> aptSigning = newHashMap();
    aptSigning.put("keypair", keypair);
    aptSigning.put("passphrase", passphrase);
    attributes.put("aptSigning", aptSigning);
    NestedAttributesMap aptSigningNested = new NestedAttributesMap("aptSigning", aptSigning);
    when(configuration.attributes("aptSigning")).thenReturn(aptSigningNested);

    repository.init(configuration);
    return repository;
  }
}
