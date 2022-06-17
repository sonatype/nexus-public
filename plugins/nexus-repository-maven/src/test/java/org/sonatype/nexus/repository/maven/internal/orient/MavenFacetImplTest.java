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
package org.sonatype.nexus.repository.maven.internal.orient;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.validation.MavenMetadataContentValidator;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.TransactionModule;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.TypeLiteral;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;

import static com.google.inject.name.Names.named;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.cache.CacheInfo.CACHE;
import static org.sonatype.nexus.repository.cache.CacheInfo.CACHE_TOKEN;
import static org.sonatype.nexus.repository.cache.CacheInfo.INVALIDATED;
import static org.sonatype.nexus.repository.cache.CacheInfo.LAST_VERIFIED;
import static org.sonatype.nexus.repository.maven.internal.Attributes.AssetKind.ARTIFACT;
import static org.sonatype.nexus.repository.maven.internal.Attributes.AssetKind.REPOSITORY_METADATA;
import static org.sonatype.nexus.repository.storage.Asset.CHECKSUM;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.view.Content.CONTENT;
import static org.sonatype.nexus.repository.view.Content.P_ETAG;
import static org.sonatype.nexus.repository.view.Content.P_LAST_MODIFIED;

public class MavenFacetImplTest
    extends TestSupport
{
  @Mock
  private MavenMetadataContentValidator mavenMetadataContentValidator;

  @Mock
  private OrientMetadataRebuilder metadataRebuilder;

  @Mock
  private StorageTx storageTx;

  @Mock
  private Repository repository;

  @Mock
  private StorageFacet storageFacet;

  @Mock
  private EventManager eventManager;

  @Spy
  private Maven2MavenPathParser maven2MavenPathParser = new Maven2MavenPathParser();

  private MavenFacetImpl underTest;

  @Captor
  ArgumentCaptor<Map<String, String>> headersCaptor;

  @Captor
  ArgumentCaptor<DateTime> blobCreatedCaptor;

  @Before
  public void setup() throws Exception {
    underTest = Guice.createInjector(new TransactionModule(), new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(EventManager.class).toInstance(eventManager);
        bind(MavenMetadataContentValidator.class).toInstance(mavenMetadataContentValidator);
        bind(OrientMetadataRebuilder.class).toInstance(metadataRebuilder);
        bindConstant().annotatedWith(named("${nexus.maven.metadata.validation.enabled:-true}")).to(true);
        bind(new TypeLiteral<Map<String, MavenPathParser>>() { })
            .toInstance(ImmutableMap.of(Maven2Format.NAME, maven2MavenPathParser));
      }
    }).getInstance(MavenFacetImpl.class);

    underTest.attach(repository);
    when(repository.getType()).thenReturn(new HostedType());
    when(repository.facet(StorageFacet.class)).thenReturn(storageFacet);
    when(storageFacet.txSupplier()).thenReturn(() -> storageTx);

    UnitOfWork.begin(() -> storageTx);
  }

  @After
  public void cleanup() {
    UnitOfWork.end();
  }

  @Test
  public void testGet_expiredMetadata() throws Exception {
    String path = "/org/sonatype/nexus/nexus-base/3.19.0-SNAPSHOT/maven-metadata.xml";
    Asset asset = createMetadataAsset(path, INVALIDATED);
    Blob blob = mock(Blob.class);
    when(blob.getInputStream())
        .thenReturn(getClass().getResourceAsStream("/org/sonatype/nexus/repository/maven/gavMetadata.xml"));
    when(storageTx.findAssetWithProperty(anyString(), anyString(), nullable(Bucket.class))).thenReturn(asset);
    when(storageTx.requireBlob(any())).thenReturn(blob);
    MavenPath mavenPath = maven2MavenPathParser.parsePath(path);
    Content content = underTest.get(mavenPath);

    assertThat(content, not(nullValue()));
    assertThat(content.getContentType(), is(TEXT_XML));
    verify(metadataRebuilder)
        .refreshInTransaction(any(), eq(false), eq(false), eq("org.sonatype.nexus"), eq("nexus-base"),
            eq("3.19.0-SNAPSHOT"));
  }

  @Test
  public void testGet_currentMetadata() throws Exception {
    String path = "/org/sonatype/nexus/nexus-base/3.19.0-SNAPSHOT/maven-metadata.xml";
    Asset asset = createMetadataAsset(path, "valid");
    Blob blob = mock(Blob.class);
    when(blob.getInputStream())
        .thenReturn(getClass().getResourceAsStream("/org/sonatype/nexus/repository/maven/gavMetadata.xml"));
    when(storageTx.findAssetWithProperty(anyString(), anyString(), nullable(Bucket.class))).thenReturn(asset);
    when(storageTx.requireBlob(any())).thenReturn(blob);
    MavenPath mavenPath = maven2MavenPathParser.parsePath(path);
    Content content = underTest.get(mavenPath);

    assertThat(content, not(nullValue()));
    assertThat(content.getContentType(), is(TEXT_XML));
    verify(storageTx, never()).deleteAsset(any());
    verify(metadataRebuilder, never())
        .rebuildInTransaction(any(), eq(false), eq(false), eq("org.sonatype.nexus"), eq("nexus-base"),
            eq("3.19.0-SNAPSHOT"));
  }

  @Test
  public void putSetsBlobHeadersFromAttributesMap() throws Exception {
    Configuration configuration = mock(Configuration.class);
    ConfigurationFacet configurationFacet = mock(ConfigurationFacet.class);
    TempBlob tempBlob = mock(TempBlob.class);
    MavenPath path = mock(MavenPath.class);
    Asset asset = mock(Asset.class);
    AssetBlob assetBlob = mock(AssetBlob.class);
    Blob blob = mock(Blob.class);
    Format format = mock(Format.class);

    when(path.getPath()).thenReturn("some/path");
    when(repository.facet(ConfigurationFacet.class)).thenReturn(configurationFacet);
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn(Maven2Format.NAME);
    when(storageTx.createAsset(any(), (Format) any())).thenReturn(asset);
    doReturn(true).when(maven2MavenPathParser).isRepositoryMetadata(any());
    when(asset.formatAttributes()).thenReturn(new NestedAttributesMap());
    when(asset.attributes()).thenReturn(new NestedAttributesMap());
    when(storageTx.createBlob(any(), any(), any(), any(), anyBoolean())).thenReturn(assetBlob);
    when(assetBlob.getBlob()).thenReturn(blob);

    AttributesMap contentAttributes = new AttributesMap();
    DateTime uploadedDate = new DateTime().minusDays(1);
    String uploadedBy = "some_user";
    String uploadedByIp = "127.0.0.1";
    contentAttributes.set(AssetEntityAdapter.P_CREATED_BY, uploadedBy);
    contentAttributes.set(AssetEntityAdapter.P_CREATED_BY_IP, uploadedByIp);
    contentAttributes.set(AssetEntityAdapter.P_BLOB_CREATED, uploadedDate);

    underTest.doInit(configuration);
    underTest.doPut(path, tempBlob, ARTIFACT.name(), contentAttributes);

    verify(storageTx).createBlob(any(), any(), headersCaptor.capture(), any(), anyBoolean());
    verify(asset).blobCreated(blobCreatedCaptor.capture());
    assertEquals(headersCaptor.getValue().get(BlobStore.CREATED_BY_HEADER), uploadedBy);
    assertEquals(headersCaptor.getValue().get(BlobStore.CREATED_BY_IP_HEADER), uploadedByIp);
    assertEquals(blobCreatedCaptor.getValue(), uploadedDate);
  }

  private Asset createMetadataAsset(final String name, final String cacheToken) {
    Asset asset = new Asset();
    asset.contentType(TEXT_XML);
    asset.name(name);
    asset.format(Maven2Format.NAME);
    asset.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
    asset.formatAttributes().set(P_ASSET_KIND, REPOSITORY_METADATA.name());
    asset.attributes().child(CHECKSUM).set(HashType.SHA1.getExt(),
        HashAlgorithm.SHA1.function().hashString("foobar", StandardCharsets.UTF_8).toString());
    asset.attributes().child(CHECKSUM).set(HashType.SHA256.getExt(),
        HashAlgorithm.SHA256.function().hashString("foobar", StandardCharsets.UTF_8).toString());
    asset.attributes().child(CHECKSUM).set(HashType.SHA512.getExt(),
        HashAlgorithm.SHA512.function().hashString("foobar", StandardCharsets.UTF_8).toString());
    asset.attributes().child(CHECKSUM).set(HashType.MD5.getExt(),
        HashAlgorithm.MD5.function().hashString("foobar", StandardCharsets.UTF_8).toString());
    asset.attributes().child(CONTENT).set(P_LAST_MODIFIED, new Date());
    asset.attributes().child(CONTENT).set(P_ETAG, "ETAG");
    asset.attributes().child(CACHE).set(LAST_VERIFIED, new Date());
    asset.attributes().child(CACHE).set(CACHE_TOKEN, cacheToken);
    asset.blobRef(new BlobRef("node", "store", "blobid"));
    return asset;
  }
}
