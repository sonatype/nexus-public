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
package org.sonatype.nexus.repository.rest.api;

import java.util.Arrays;
import java.util.function.Function;

import org.sonatype.goodies.common.Time;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientConnectionAttributes;
import org.sonatype.nexus.repository.rest.api.model.SimpleApiGroupRepository;
import org.sonatype.nexus.repository.rest.api.model.SimpleApiHostedRepository;
import org.sonatype.nexus.repository.rest.api.model.SimpleApiProxyRepository;
import org.sonatype.nexus.repository.routing.RoutingRule;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.storage.StorageFacetConstants;
import org.sonatype.nexus.repository.storage.WritePolicy;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class SimpleApiRepositoryAdapterTest
    extends TestSupport
{
  private static final String ROUTING_RULE_NAME = "block-the-things";

  private SimpleApiRepositoryAdapter underTest;

  @Mock
  private RoutingRuleStore routingRuleStore;

  private Configuration configuration = new Configuration();

  @Before
  public void setup() {
    underTest = new SimpleApiRepositoryAdapter(routingRuleStore);
    BaseUrlHolder.set("http://nexus-url");

    configuration.setOnline(true);
  }

  @Test
  public void testAdapt_groupRepository() throws Exception {
    Repository repository = createRepository(new GroupType());
    repository.getConfiguration().attributes("group").set("memberNames", Arrays.asList("a", "b"));

    SimpleApiGroupRepository groupRepository = (SimpleApiGroupRepository) underTest.adapt(repository);
    assertRepository(groupRepository, "group", true);
    assertThat(groupRepository.getGroup().getMemberNames(), contains("a", "b"));
    assertThat(groupRepository.getStorage().getStrictContentTypeValidation(), is(false));

    setStorageAttributes(repository, "default", /* non-default */ true, null);
    groupRepository = (SimpleApiGroupRepository) underTest.adapt(repository);
    assertThat(groupRepository.getStorage().getBlobStoreName(), is("default"));
    assertThat(groupRepository.getStorage().getStrictContentTypeValidation(), is(true));
  }

  @Test
  public void testAdapt_hostedRepository() throws Exception {
    Repository repository = createRepository(new HostedType());

    SimpleApiHostedRepository hostedRepository = (SimpleApiHostedRepository) underTest.adapt(repository);
    assertRepository(hostedRepository, "hosted", true);
    assertThat(hostedRepository.getStorage().getStrictContentTypeValidation(), is(false));
    assertThat(hostedRepository.getStorage().getWritePolicy(), is("ALLOW"));

    // set some values
    setStorageAttributes(repository, "default", /* non-default */ true, WritePolicy.DENY);
    hostedRepository = (SimpleApiHostedRepository) underTest.adapt(repository);

    assertThat(hostedRepository.getStorage().getBlobStoreName(), is("default"));
    assertThat(hostedRepository.getStorage().getStrictContentTypeValidation(), is(true));
    assertThat(hostedRepository.getStorage().getWritePolicy(), is("DENY"));
  }

  @Test
  public void testAdapt_hostedRepositoryCleanup() throws Exception {
    Repository repository = createRepository(new HostedType());

    testCleanup(repository, repo -> ((SimpleApiHostedRepository) repo).getCleanup());
  }

  @Test
  public void testAdapt_proxyRepository() throws Exception {
    Repository repository = createRepository(new ProxyType());

    NestedAttributesMap proxy = repository.getConfiguration().attributes("proxy");
    proxy.set("remoteUrl", "https://repo1.maven.org/maven2/");

    SimpleApiProxyRepository proxyRepository = (SimpleApiProxyRepository) underTest.adapt(repository);
    assertRepository(proxyRepository, "proxy", true);

    assertThat(proxyRepository.getProxy().getContentMaxAge(), is(1440));
    assertThat(proxyRepository.getProxy().getMetadataMaxAge(), is(1440));
    assertThat(proxyRepository.getProxy().getRemoteUrl(), is("https://repo1.maven.org/maven2/"));
    assertThat(proxyRepository.getHttpClient().getAutoBlock(), is(false));
    assertThat(proxyRepository.getHttpClient().getBlocked(), is(false));

    // Test specified values
    proxy.set("contentMaxAge", 1000.0); // specifically a double here to ensure exceptions not thrown
    proxy.set("metadataMaxAge", 1000.0); // specifically a double here to ensure exceptions not thrown

    NestedAttributesMap httpclient = repository.getConfiguration().attributes("httpclient");
    httpclient.set("autoBlock", true);
    httpclient.set("blocked", true);

    proxyRepository = (SimpleApiProxyRepository) underTest.adapt(repository);

    assertThat(proxyRepository.getProxy().getContentMaxAge(), is(1000));
    assertThat(proxyRepository.getProxy().getMetadataMaxAge(), is(1000));
    assertThat(proxyRepository.getHttpClient().getAutoBlock(), is(true));
    assertThat(proxyRepository.getHttpClient().getBlocked(), is(true));
  }

  @Test
  public void testAdapt_proxyRepository_negativeCache() throws Exception {
    Repository repository = createRepository(new ProxyType());

    SimpleApiProxyRepository proxyRepository = (SimpleApiProxyRepository) underTest.adapt(repository);
    assertThat(proxyRepository.getNegativeCache().getEnabled(), is(true));
    assertThat(proxyRepository.getNegativeCache().getTimeToLive(), is(Time.hours(24).toMinutesI()));

    // Test specified values
    NestedAttributesMap negativeCache = repository.getConfiguration().attributes("negativeCache");
    negativeCache.set("enabled", false);
    negativeCache.set("timeToLive", 23.0); // specifically a double here to ensure exceptions not thrown

    proxyRepository = (SimpleApiProxyRepository) underTest.adapt(repository);
    assertThat(proxyRepository.getNegativeCache().getEnabled(), is(false));
    assertThat(proxyRepository.getNegativeCache().getTimeToLive(), is(23));
  }

  @Test
  public void testAdapt_proxyRepositoryRoutingRule() throws Exception {
    Repository repository = createRepository(new ProxyType());
    EntityId entityId = Mockito.mock(EntityId.class);
    repository.getConfiguration().setRoutingRuleId(entityId);

    SimpleApiProxyRepository proxyRepository = (SimpleApiProxyRepository) underTest.adapt(repository);
    assertThat(proxyRepository.getRoutingRuleName(), nullValue());

    when(routingRuleStore.getById(any())).thenReturn(new RoutingRule(ROUTING_RULE_NAME, null, null, null));

    proxyRepository = (SimpleApiProxyRepository) underTest.adapt(repository);
    assertThat(proxyRepository.getRoutingRuleName(), is(ROUTING_RULE_NAME));
  }

  @Test
  public void testAdapt_proxyRepositoryStorageAttributes() throws Exception {
    Repository repository = createRepository(new ProxyType());
    setStorageAttributes(repository, "default", /* non-default */ true, null);

    SimpleApiProxyRepository proxyRepository = (SimpleApiProxyRepository) underTest.adapt(repository);

    assertThat(proxyRepository.getStorage().getBlobStoreName(), is("default"));
    assertThat(proxyRepository.getStorage().getStrictContentTypeValidation(), is(true));
  }

  @Test
  public void testAdapt_proxyRepositoryCleanup() throws Exception {
    Repository repository = createRepository(new ProxyType());

    testCleanup(repository, repo -> ((SimpleApiProxyRepository) repo).getCleanup());
  }

  @Test
  public void testAdapt_proxyRepositoryAuth() throws Exception {
    Repository repository = createRepository(new ProxyType());

    // defaults
    SimpleApiProxyRepository proxyRepository = (SimpleApiProxyRepository) underTest.adapt(repository);
    assertThat(proxyRepository.getHttpClient().getAuthentication(), nullValue());

    // username
    NestedAttributesMap httpclient = repository.getConfiguration().attributes("httpclient").child("authentication");
    httpclient.set("type", "username");
    httpclient.set("username", "jsmith");
    httpclient.set("password", "p4ssw0rd");

    proxyRepository = (SimpleApiProxyRepository) underTest.adapt(repository);
    assertThat(proxyRepository.getHttpClient().getAuthentication().getType(), is("username"));
    assertThat(proxyRepository.getHttpClient().getAuthentication().getUsername(), is("jsmith"));
    assertThat(proxyRepository.getHttpClient().getAuthentication().getPassword(), nullValue());
    assertThat(proxyRepository.getHttpClient().getAuthentication().getNtlmDomain(), nullValue());
    assertThat(proxyRepository.getHttpClient().getAuthentication().getNtlmHost(), nullValue());

    // ntlm
    httpclient.set("type", "ntlm");
    httpclient.set("username", "jsmith");
    httpclient.set("password", "p4ssw0rd");
    httpclient.set("ntlmDomain", "sona");
    httpclient.set("ntlmHost", "sonatype.com");

    proxyRepository = (SimpleApiProxyRepository) underTest.adapt(repository);
    assertThat(proxyRepository.getHttpClient().getAuthentication().getType(), is("ntlm"));
    assertThat(proxyRepository.getHttpClient().getAuthentication().getUsername(), is("jsmith"));
    assertThat(proxyRepository.getHttpClient().getAuthentication().getPassword(), nullValue());
    assertThat(proxyRepository.getHttpClient().getAuthentication().getNtlmDomain(), is("sona"));
    assertThat(proxyRepository.getHttpClient().getAuthentication().getNtlmHost(), is("sonatype.com"));
  }

  @Test
  public void testAdapt_proxyRepositoryConnection() throws Exception {
    Repository repository = createRepository(new ProxyType());

    // defaults
    SimpleApiProxyRepository proxyRepository = (SimpleApiProxyRepository) underTest.adapt(repository);
    assertThat(proxyRepository.getHttpClient().getConnection(), nullValue());

    // test empty connection
    NestedAttributesMap connection = repository.getConfiguration().attributes("httpclient").child("connection");
    proxyRepository = (SimpleApiProxyRepository) underTest.adapt(repository);
    assertConnection(proxyRepository.getHttpClient().getConnection(), /* circular redirects */ false,
        /* cookies */ false, /* retries */ null, /* timeout */ null, null);

    // populate values
    connection.set("enableCircularRedirects", true);
    connection.set("enableCookies", true);
    connection.set("retries", 9.0);
    connection.set("timeout", 7.0);
    connection.set("userAgentSuffix", "hi-yall");

    proxyRepository = (SimpleApiProxyRepository) underTest.adapt(repository);
    assertConnection(proxyRepository.getHttpClient().getConnection(), /* circular redirects */ true, /* cookies */ true,
        /* retries */ 9, /* timeout */ 7, "hi-yall");
  }

  private static void assertConnection(
      final HttpClientConnectionAttributes connection,
      final Boolean enableCircularRedirects,
      final Boolean enableCookies,
      final Integer retries,
      final Integer timeout,
      final String uaSuffix)
  {
    assertThat(connection.getEnableCircularRedirects(), is(enableCircularRedirects));
    assertThat(connection.getEnableCookies(), is(enableCookies));
    assertThat(connection.getRetries(), is(retries));
    assertThat(connection.getTimeout(), is(timeout));
    assertThat(connection.getUserAgentSuffix(), is(uaSuffix));
  }

  private static Repository createRepository(final Type type) throws Exception {
    Repository repository = new RepositoryImpl(Mockito.mock(EventManager.class), type, new Format("test-format")
    {
    });
    repository.init(config("my-repo"));
    return repository;
  }

  private static Configuration config(final String repositoryName) {
    Configuration configuration = new Configuration();
    configuration.setOnline(true);
    configuration.setRepositoryName(repositoryName);
    return configuration;
  }

  private static void assertRepository(
      final AbstractApiRepository repository,
      final String type,
      final Boolean online)
  {
    assertThat(repository.getFormat(), is("test-format"));
    assertThat(repository.getName(), is("my-repo"));
    assertThat(repository.getOnline(), is(online));
    assertThat(repository.getType(), is(type));
    assertThat(repository.getUrl(), is(BaseUrlHolder.get() + "/repository/my-repo"));
  }

  private static void setStorageAttributes(
      final Repository repository,
      final String blobStoreName,
      final Boolean strictContentTypeValidation,
      final WritePolicy writePolicy)
  {
    NestedAttributesMap storage = repository.getConfiguration().attributes(StorageFacetConstants.STORAGE);
    storage.set(StorageFacetConstants.BLOB_STORE_NAME, blobStoreName);
    if (strictContentTypeValidation != null) {
      storage.set(StorageFacetConstants.STRICT_CONTENT_TYPE_VALIDATION, strictContentTypeValidation);
    }
    if (writePolicy != null) {
      storage.set(StorageFacetConstants.WRITE_POLICY, writePolicy);
    }
  }

  private void testCleanup(
      final Repository repository,
      final Function<AbstractApiRepository, CleanupPolicyAttributes> cleanupFn) throws Exception
  {
    AbstractApiRepository restRepository = underTest.adapt(repository);
    assertThat(cleanupFn.apply(restRepository).getPolicyNames(), empty());

    NestedAttributesMap storage = repository.getConfiguration().attributes("cleanup");
    storage.set("policyName", Arrays.asList("policy-a"));

    restRepository = underTest.adapt(repository);
    assertThat(cleanupFn.apply(restRepository).getPolicyNames(), contains("policy-a"));
  }

}
