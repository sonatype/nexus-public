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
package org.sonatype.nexus.repository.maven.rest;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationStore;
import org.sonatype.nexus.repository.config.internal.ConfigurationData;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.ContentDisposition;
import org.sonatype.nexus.repository.maven.api.MavenAttributes;
import org.sonatype.nexus.repository.rest.api.model.NegativeCacheAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyAttributes;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.sonatype.nexus.repository.Repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MavenProxyRepositoryApiRequestToConfigurationConverter}.
 */
@ExtendWith(MockitoExtension.class)
class MavenProxyRepositoryApiRequestToConfigurationConverterTest
{
  @Mock
  private ConfigurationStore configurationStore;

  @Mock
  private RoutingRuleStore routingRuleStore;

  @Mock
  private RepositoryManager repositoryManager;

  private MavenProxyRepositoryApiRequestToConfigurationConverter underTest;

  @BeforeEach
  void setUp() {
    when(configurationStore.newConfiguration()).thenReturn(new ConfigurationData());
    underTest = new MavenProxyRepositoryApiRequestToConfigurationConverter(routingRuleStore);
    underTest.setConfigurationStore(configurationStore);
    underTest.setRepositoryManager(repositoryManager);
  }

  @Test
  void testConvert_defaultsContentDispositionToAttachment() {
    MavenProxyRepositoryApiRequest request = createRequest(null);
    when(repositoryManager.get("maven-proxy")).thenReturn(null);

    Configuration config = underTest.convert(request);

    assertThat(config, notNullValue());
    String contentDisposition = config.attributes("maven").get("contentDisposition", String.class);
    assertThat(contentDisposition, is(ContentDisposition.ATTACHMENT.name()));
  }

  @Test
  void testConvert_preservesExplicitContentDisposition() {
    MavenProxyRepositoryApiRequest request = createRequest(ContentDisposition.INLINE.name());

    Configuration config = underTest.convert(request);

    assertThat(config, notNullValue());
    String contentDisposition = config.attributes("maven").get("contentDisposition", String.class);
    assertThat(contentDisposition, is(ContentDisposition.INLINE.name()));
  }

  @Test
  void testConvert_preservesExistingAttachmentOnUpdate() {
    MavenProxyRepositoryApiRequest request = createRequest(null);
    mockExistingRepository("maven-proxy", "maven", ContentDisposition.ATTACHMENT.name());

    Configuration config = underTest.convert(request);

    assertThat(config, notNullValue());
    String contentDisposition = config.attributes("maven").get("contentDisposition", String.class);
    assertThat(contentDisposition, is(ContentDisposition.ATTACHMENT.name()));
  }

  @Test
  void testConvert_preservesExistingInlineOnUpdate() {
    MavenProxyRepositoryApiRequest request = createRequest(null);
    mockExistingRepository("maven-proxy", "maven", ContentDisposition.INLINE.name());

    Configuration config = underTest.convert(request);

    assertThat(config, notNullValue());
    String contentDisposition = config.attributes("maven").get("contentDisposition", String.class);
    assertThat(contentDisposition, is(ContentDisposition.INLINE.name()));
  }

  @Test
  void testConvert_preservesNullOnUpdateForLegacyRepo() {
    // Request with null contentDisposition (update scenario for legacy repo)
    MavenProxyRepositoryApiRequest request = createRequest(null);
    mockLegacyRepository("maven-proxy");

    Configuration config = underTest.convert(request);

    assertThat(config, notNullValue());
    String contentDisposition = config.attributes("maven").get("contentDisposition", String.class);
    // Legacy repo with null should preserve null for backward compatibility
    assertThat(contentDisposition, nullValue());
  }

  private void mockExistingRepository(String name, String formatKey, String contentDisposition) {
    Configuration existingConfig = new ConfigurationData();
    existingConfig.setRepositoryName(name);
    existingConfig.attributes(formatKey).set("contentDisposition", contentDisposition);

    Repository repo = mock(Repository.class);
    when(repo.getConfiguration()).thenReturn(existingConfig);
    when(repositoryManager.get(name)).thenReturn(repo);
  }

  private void mockLegacyRepository(String name) {
    Configuration existingConfig = new ConfigurationData();
    existingConfig.setRepositoryName(name);
    // No contentDisposition set (legacy repo)

    Repository repo = mock(Repository.class);
    when(repo.getConfiguration()).thenReturn(existingConfig);
    when(repositoryManager.get(name)).thenReturn(repo);
  }

  private MavenProxyRepositoryApiRequest createRequest(String contentDisposition) {
    MavenAttributes maven = new MavenAttributes("RELEASE", "STRICT", contentDisposition);
    StorageAttributes storage = new StorageAttributes("default", true);
    ProxyAttributes proxy = new ProxyAttributes("https://repo.maven.apache.org/maven2", 1440, 1440, false);
    NegativeCacheAttributes negativeCache = new NegativeCacheAttributes(false, 1440);
    HttpClientAttributesWithPreemptiveAuth httpClient = new HttpClientAttributesWithPreemptiveAuth(
        false, true, null, null);

    return new MavenProxyRepositoryApiRequest(
        "maven-proxy",
        true,
        storage,
        null,
        proxy,
        negativeCache,
        httpClient,
        null,
        maven,
        null);
  }
}
