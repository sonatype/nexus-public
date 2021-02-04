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
package org.sonatype.nexus.repository.content.npm.internal;

import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

public class DataStoreNpmAuditTarballFacetTest
    extends TestSupport
{
  @Mock
  private Repository repository;

  @Mock
  private Context context;

  @Mock
  private ProxyFacet proxyFacet;

  @Mock
  private Content content;

  @Mock
  private FluentAsset anAsset;

  @Mock
  private AssetBlob assetBlob;

  private DataStoreNpmAuditTarballFacet underTest = new DataStoreNpmAuditTarballFacet(1);

  @Before
  public void setup() throws Exception {
    when(repository.facet(ProxyFacet.class)).thenReturn(proxyFacet);
    when(proxyFacet.get(context)).thenReturn(content);
    AttributesMap contentAttributes = new AttributesMap();
    contentAttributes.set(Asset.class, anAsset);
    when(content.getAttributes()).thenReturn(contentAttributes);
  }

  @Test
  public void componentHashsumShouldBeEmpty() throws Exception {
    when(anAsset.blob()).thenReturn(empty());

    Optional<String> componentHashsum = underTest.getComponentHashsumForProxyRepo(repository, context);

    assertThat(componentHashsum, is(empty()));
  }

  @Test
  public void componentHashsumShouldReturnHashsum() throws Exception {
    String aHash = "abcd1234";
    when(anAsset.blob()).thenReturn(of(assetBlob));
    when(assetBlob.checksums()).thenReturn(singletonMap(SHA1.name(), aHash));

    Optional<String> componentHashsum = underTest.getComponentHashsumForProxyRepo(repository, context);

    assertThat(componentHashsum, is(of(aHash)));
  }
}
