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
package org.sonatype.nexus.repository.rest.cma;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.AssetXO;
import org.sonatype.nexus.repository.rest.api.AssetXODescriptor;
import org.sonatype.nexus.repository.storage.Asset;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.orientechnologies.orient.core.id.ORID;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

public class AssetXOBuilderTest
    extends TestSupport
{
  @Mock
  Repository repository;

  @Mock
  Asset assetOne;

  @Mock
  ORID assetOneORID;

  @Mock
  EntityMetadata assetOneEntityMetadata;

  @Mock
  EntityId assetOneEntityId;

  @Mock
  AssetXODescriptor assetDescriptor;

  private static final long blobCreatedTimestamp = 1604707200000L;

  private static final long blobUpdatedTimestamp = blobCreatedTimestamp + (60 * 60 * 1000); // 1 hour later

  private static final long lastDownloadedTimestamp = blobUpdatedTimestamp + (60 * 60 * 1000);

  private static final Map<String, Object> checksum =
      Maps.newHashMap(ImmutableMap.of(HashAlgorithm.SHA1.name(), "87acec17cd9dcd20a716cc2cf67417b71c8a7016"));

  @Before
  public void setup() {
    when(assetOne.name()).thenReturn("nameOne");
    when(assetOne.getEntityMetadata()).thenReturn(assetOneEntityMetadata);
    when(assetOneORID.toString()).thenReturn("assetOneORID");

    when(assetOneEntityMetadata.getId()).thenReturn(assetOneEntityId);
    when(assetOneEntityId.getValue()).thenReturn("assetOne");

    when(repository.getName()).thenReturn("maven-releases");
    when(repository.getUrl()).thenReturn("http://localhost:8081/repository/maven-releases");
    when(repository.getFormat()).thenReturn(new Format("maven2") { });

    when(assetDescriptor.listExposedAttributeKeys()).thenReturn(Stream.of("Key1", "Key3").collect(Collectors.toSet()));
  }

  private void setupAttributes(boolean includeFormatAttributes) {
    NestedAttributesMap attributes = new NestedAttributesMap("attributes", new HashMap<>());
    attributes.set(Asset.CHECKSUM, checksum);
    if (includeFormatAttributes) {
      Map<String, Object> formatAttributes = Maps.newHashMap(
          ImmutableMap.of("Key1", "Value1", "Key2", "Value2", "Key3", "Value3"));
      attributes.set(repository.getFormat().getValue(), formatAttributes);
    }
    when(assetOne.attributes()).thenReturn(attributes);
  }

  @Test
  public void fromAsset() {
    setupAttributes(false);

    when(assetOne.blobCreated()).thenReturn(new DateTime(blobCreatedTimestamp));
    when(assetOne.blobUpdated()).thenReturn(new DateTime(blobUpdatedTimestamp));
    when(assetOne.lastDownloaded()).thenReturn(new DateTime(lastDownloadedTimestamp));
    when(assetOne.createdBy()).thenReturn("admin");
    when(assetOne.createdByIp()).thenReturn("10.0.0.1");
    when(assetOne.size()).thenReturn(2345L);

    AssetXO assetXO = AssetXOBuilder.fromAsset(assetOne, repository, null);

    assertThat(assetXO.getId(), notNullValue());
    assertThat(assetXO.getPath(), is("nameOne"));
    assertThat(assetXO.getDownloadUrl(), is("http://localhost:8081/repository/maven-releases/nameOne"));
    assertThat(assetXO.getChecksum(), is(checksum));
    assertThat(assetXO.getFormat(), is(repository.getFormat().getValue()));
    assertThat(assetXO.getLastModified(), is(new Date(blobUpdatedTimestamp)));
    assertThat(assetXO.getAttributes().get("blobCreated"), is(new Date(blobCreatedTimestamp)));
    assertThat(assetXO.getUploader(), is("admin"));
    assertThat(assetXO.getUploaderIp(), is("10.0.0.1"));
    assertThat(assetXO.getFileSize(), is(2345L));
  }

  @Test
  public void fromAssetMissingDates() {
    setupAttributes(false);

    AssetXO assetXO = AssetXOBuilder.fromAsset(assetOne, repository, null);

    assertThat(assetXO.getId(), notNullValue());
    assertThat(assetXO.getPath(), is("nameOne"));
    assertThat(assetXO.getDownloadUrl(), is("http://localhost:8081/repository/maven-releases/nameOne"));
    assertThat(assetXO.getChecksum(), is(checksum));
    assertThat(assetXO.getFormat(), is(repository.getFormat().getValue()));
    assertThat(assetXO.getLastModified(), nullValue());
  }

  @Test
  public void fromAssetExtraDatesNull() {
    setupAttributes(false);

    AssetXO assetXO = AssetXOBuilder.fromAsset(assetOne, repository, null);

    assertThat(assetXO.getFormat(), is(repository.getFormat().getValue()));
    assertThat(assetXO.getAttributes().get("blobCreated"), nullValue());
    assertThat(assetXO.getAttributes().get("LastDownloaded"), nullValue());
  }

  @Test
  public void fromAssetExtraDatesPopulated() {
    setupAttributes(false);

    AssetXO assetXO = AssetXOBuilder.fromAsset(assetOne, repository, null);

    assertThat(assetXO.getFormat(), is(repository.getFormat().getValue()));
    assertThat(assetXO.getAttributes().get("blobCreated"), nullValue());
    assertThat(assetXO.getAttributes().get("LastDownloaded"), nullValue());
  }

  @Test
  public void fromAssetNoFormatAttributes() {
    setupAttributes(false);

    AssetXO assetXO = AssetXOBuilder.fromAsset(assetOne, repository, null);

    assertThat(assetXO.getFormat(), is(repository.getFormat().getValue()));
    assertThat(assetXO.getAttributes().containsKey(assetXO.getFormat()), is(false));
  }

  @Test
  public void fromAssetFormatAttributesNotExposed() {
    setupAttributes(true);

    AssetXO assetXO = AssetXOBuilder.fromAsset(assetOne, repository, null);

    assertThat(assetXO.getFormat(), is(repository.getFormat().getValue()));
    assertThat(assetXO.getAttributes().containsKey(assetXO.getFormat()), is(false));
  }

  @Test
  public void fromAssetFormatAttributesExposed() {
    setupAttributes(true);

    AssetXO assetXO = AssetXOBuilder.fromAsset(assetOne, repository,
        Maps.newHashMap(ImmutableMap.of(repository.getFormat().getValue(), assetDescriptor)));

    assertThat(assetXO.getFormat(), is(repository.getFormat().getValue()));
    assertThat(assetXO.getAttributes().containsKey(assetXO.getFormat()), is(true));
    Map<String, Object> attributeMap = (Map<String, Object>)assetXO.getAttributes().get(assetXO.getFormat());
    assertThat(attributeMap.size(), is(2));
    assertThat(attributeMap.get("Key1"), is("Value1"));
    assertThat(attributeMap.get("Key3"), is("Value3"));
  }
}
