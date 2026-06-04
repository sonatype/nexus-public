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
package org.sonatype.nexus.repository.raw;

import java.util.Arrays;
import java.util.function.Consumer;

import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl;
import org.sonatype.nexus.repository.raw.internal.RawFormat;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static com.google.common.collect.Maps.newHashMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RawRepositoryAdapterTest
{
  private RawRepositoryAdapter adapter;

  @Mock
  private RoutingRuleStore routingRuleStore;

  @BeforeEach
  void setup() {
    adapter = new RawRepositoryAdapter(routingRuleStore, true);
    BaseUrlHolder.set("http://nexus-url", "");
  }

  @Test
  void testAdapt_groupRepository() throws Exception {
    // No raw specific props so simple smoke test
    Repository repository = createRepository(new GroupType(), configuration -> {
      configuration.attributes("group").set("memberNames", Arrays.asList("a", "b"));
      configuration.attributes("raw").set("contentDisposition", ContentDisposition.ATTACHMENT.toString());
    });

    RawGroupApiRepository groupRepository = (RawGroupApiRepository) adapter.adapt(repository);
    assertRepository(groupRepository, "group", true);
    assertThat(groupRepository.getRaw().getContentDisposition(), is("ATTACHMENT"));
  }

  @Test
  void testAdapt_hostedRepository() throws Exception {
    Repository repository = createRepository(new HostedType(), ContentDisposition.INLINE);

    RawHostedApiRepository hostedRepository = (RawHostedApiRepository) adapter.adapt(repository);
    assertRepository(hostedRepository, "hosted", true);
    assertThat(hostedRepository.getRaw().getContentDisposition(), is("INLINE"));
    // Check fields are populated, actual values validated with SimpleApiRepositoryAdapterTest
    assertThat(hostedRepository.getCleanup(), nullValue());
    assertThat(hostedRepository.getStorage(), notNullValue());
  }

  @Test
  void testAdapt_proxyRepository() throws Exception {
    Repository repository = createRepository(new ProxyType(), ContentDisposition.INLINE);

    RawProxyApiRepository proxyRepository = (RawProxyApiRepository) adapter.adapt(repository);
    assertRepository(proxyRepository, "proxy", true);
    assertThat(proxyRepository.getRaw().getContentDisposition(), is("INLINE"));
    // Check fields are populated, actual values validated with SimpleApiRepositoryAdapterTest
    assertThat(proxyRepository.getCleanup(), nullValue());
    assertThat(proxyRepository.getHttpClient(), notNullValue());
    assertThat(proxyRepository.getNegativeCache(), notNullValue());
    assertThat(proxyRepository.getProxy(), notNullValue());
    assertThat(proxyRepository.getStorage(), notNullValue());
  }

  @Test
  void testAdapt_proxyRepository_withQueryParamsForwardingEnabled() throws Exception {
    Repository repository = createRepository(new ProxyType(), configuration -> {
      NestedAttributesMap raw = new NestedAttributesMap("raw", newHashMap());
      raw.set("contentDisposition", ContentDisposition.INLINE.toString());
      raw.set("forwardQueryParameters", true);
      raw.set("excludedQueryParameters", Arrays.asList("api_key", "token"));
      when(configuration.attributes("raw")).thenReturn(raw);
    });

    RawProxyApiRepository proxyRepository = (RawProxyApiRepository) adapter.adapt(repository);
    assertRepository(proxyRepository, "proxy", true);
    assertThat(proxyRepository.getRaw().getForwardQueryParameters(), is(true));
    assertThat(proxyRepository.getRaw().getExcludedQueryParameters(), is(Arrays.asList("api_key", "token")));
  }

  @Test
  void testAdapt_proxyRepository_withQueryParamsForwardingDisabled() throws Exception {
    Repository repository = createRepository(new ProxyType(), configuration -> {
      NestedAttributesMap raw = new NestedAttributesMap("raw", newHashMap());
      raw.set("contentDisposition", ContentDisposition.INLINE.toString());
      raw.set("forwardQueryParameters", false);
      raw.set("excludedQueryParameters", Arrays.asList());
      when(configuration.attributes("raw")).thenReturn(raw);
    });

    RawProxyApiRepository proxyRepository = (RawProxyApiRepository) adapter.adapt(repository);
    assertRepository(proxyRepository, "proxy", true);
    assertThat(proxyRepository.getRaw().getForwardQueryParameters(), is(false));
    assertThat(proxyRepository.getRaw().getExcludedQueryParameters(), is(Arrays.asList()));
  }

  @Test
  void testAdapt_proxyRepository_withoutQueryParamsFields() throws Exception {
    // Test backward compatibility - fields not present in config should default gracefully
    Repository repository = createRepository(new ProxyType(), configuration -> {
      NestedAttributesMap raw = new NestedAttributesMap("raw", newHashMap());
      raw.set("contentDisposition", ContentDisposition.INLINE.toString());
      // forwardQueryParameters and excludedQueryParameters not set
      when(configuration.attributes("raw")).thenReturn(raw);
    });

    RawProxyApiRepository proxyRepository = (RawProxyApiRepository) adapter.adapt(repository);
    assertRepository(proxyRepository, "proxy", true);
    // Should handle missing fields gracefully (null or default values)
    // Exact behavior depends on RawAttributes implementation
  }

  private static void assertRepository(
      final AbstractApiRepository repository,
      final String type,
      final Boolean online)
  {
    assertThat(repository.getFormat(), is("raw"));
    assertThat(repository.getName(), is("my-repo"));
    assertThat(repository.getOnline(), is(online));
    assertThat(repository.getType(), is(type));
    assertThat(repository.getUrl(), is(BaseUrlHolder.get() + "/repository/my-repo"));
  }

  private static Configuration config(final String repositoryName) {
    Configuration configuration = mock(Configuration.class);
    when(configuration.isOnline()).thenReturn(true);
    when(configuration.getRepositoryName()).thenReturn(repositoryName);
    when(configuration.attributes(any())).thenReturn(new NestedAttributesMap("dummy", newHashMap()));
    return configuration;
  }

  private static Repository createRepository(final Type type, final Consumer<Configuration> mutator) throws Exception {
    Repository repository = new RepositoryImpl(Mockito.mock(EventManager.class), type, new RawFormat());
    Configuration configuration = config("my-repo");
    mutator.accept(configuration);
    repository.init(configuration);
    return repository;
  }

  private static Repository createRepository(
      final Type type,
      final ContentDisposition contentDisposition) throws Exception
  {
    Repository repository = new RepositoryImpl(Mockito.mock(EventManager.class), type, new RawFormat());

    Configuration configuration = config("my-repo");
    NestedAttributesMap raw = new NestedAttributesMap("raw", newHashMap());
    raw.set("contentDisposition", contentDisposition.toString());
    when(configuration.attributes("raw")).thenReturn(raw);
    repository.init(configuration);
    return repository;
  }

  @Test
  void testAdapt_hostedRepository_normalizesNullContentDispositionToInline() throws Exception {
    Repository repository = createRepositoryWithNullContentDisposition(new HostedType());

    RawHostedApiRepository hostedRepository = (RawHostedApiRepository) adapter.adapt(repository);
    assertThat(hostedRepository.getRaw().getContentDisposition(), is("INLINE"));
  }

  @Test
  void testAdapt_proxyRepository_normalizesNullContentDispositionToInline() throws Exception {
    Repository repository = createRepositoryWithNullContentDisposition(new ProxyType());

    RawProxyApiRepository proxyRepository = (RawProxyApiRepository) adapter.adapt(repository);
    assertThat(proxyRepository.getRaw().getContentDisposition(), is("INLINE"));
  }

  @Test
  void testAdapt_groupRepository_normalizesNullContentDispositionToInline() throws Exception {
    Repository repository = createRepositoryWithNullContentDisposition(new GroupType());

    RawGroupApiRepository groupRepository = (RawGroupApiRepository) adapter.adapt(repository);
    assertThat(groupRepository.getRaw().getContentDisposition(), is("INLINE"));
  }

  private static Repository createRepositoryWithNullContentDisposition(final Type type) throws Exception {
    Repository repository = new RepositoryImpl(Mockito.mock(EventManager.class), type, new RawFormat());

    Configuration configuration = config("my-repo");
    NestedAttributesMap raw = new NestedAttributesMap("raw", newHashMap());
    // Intentionally NOT setting contentDisposition - it will be null
    when(configuration.attributes("raw")).thenReturn(raw);
    repository.init(configuration);
    return repository;
  }
}
