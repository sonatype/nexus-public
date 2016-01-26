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
package org.sonatype.nexus.repository.view;

import java.util.Date;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.storage.Asset;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Test;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.storage.Asset.CHECKSUM;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;

/**
 * Tests {@link Content}.
 */
public class ContentTest
    extends TestSupport
{
  private static final List<HashAlgorithm> ALGORITHMS = singletonList(HashAlgorithm.SHA1);

  private NestedAttributesMap nestedAttributesMap() {
    NestedAttributesMap attributes = new NestedAttributesMap(P_ATTRIBUTES, Maps.<String, Object>newHashMap());
    attributes.child(CHECKSUM).set(
        HashAlgorithm.SHA1.name(),
        HashAlgorithm.SHA1.function().hashString("foobar", Charsets.UTF_8).toString()
    );
    return attributes;
  }

  @Test
  public void nothingToExtract() {
    final NestedAttributesMap attributes = nestedAttributesMap();
    Asset asset = mock(Asset.class);
    when(asset.attributes()).thenReturn(attributes);
    AttributesMap contentAttributes = new AttributesMap();
    Content.extractFromAsset(asset, ALGORITHMS, contentAttributes);
    assertThat(contentAttributes.contains(Content.CONTENT_LAST_MODIFIED), is(false));
    assertThat(contentAttributes.contains(Content.CONTENT_ETAG), is(false));
    assertThat(contentAttributes.contains(Content.CONTENT_HASH_CODES_MAP), is(true));
  }

  @Test
  public void lastModifiedOnlyExtract() {
    final DateTime now = DateTime.now();
    final NestedAttributesMap attributes = nestedAttributesMap();
    attributes.child(Content.CONTENT).set(Content.P_LAST_MODIFIED, now.toDate());
    Asset asset = mock(Asset.class);
    when(asset.attributes()).thenReturn(attributes);
    AttributesMap contentAttributes = new AttributesMap();
    Content.extractFromAsset(asset, ALGORITHMS, contentAttributes);
    assertThat(contentAttributes.get(Content.CONTENT_LAST_MODIFIED, DateTime.class), equalTo(now));
    assertThat(contentAttributes.contains(Content.CONTENT_ETAG), is(false));
    assertThat(contentAttributes.contains(Content.CONTENT_HASH_CODES_MAP), is(true));
  }

  @Test
  public void etagOnlyExtract() {
    final String etag = "foo-bar";
    final NestedAttributesMap attributes = nestedAttributesMap();
    attributes.child(Content.CONTENT).set(Content.P_ETAG, etag);
    Asset asset = mock(Asset.class);
    when(asset.attributes()).thenReturn(attributes);
    AttributesMap contentAttributes = new AttributesMap();
    Content.extractFromAsset(asset, ALGORITHMS, contentAttributes);
    assertThat(contentAttributes.contains(Content.CONTENT_LAST_MODIFIED), is(false));
    assertThat(contentAttributes.get(Content.CONTENT_ETAG, String.class), equalTo(etag));
    assertThat(contentAttributes.contains(Content.CONTENT_HASH_CODES_MAP), is(true));
  }

  @Test
  public void lastModifiedAndEtagExtract() {
    final DateTime now = DateTime.now();
    final String etag = "foo-bar";
    final NestedAttributesMap attributes = nestedAttributesMap();
    attributes.child(Content.CONTENT).set(Content.P_LAST_MODIFIED, now.toDate());
    attributes.child(Content.CONTENT).set(Content.P_ETAG, etag);
    Asset asset = mock(Asset.class);
    when(asset.attributes()).thenReturn(attributes);
    AttributesMap contentAttributes = new AttributesMap();
    Content.extractFromAsset(asset, ALGORITHMS, contentAttributes);
    assertThat(contentAttributes.get(Content.CONTENT_LAST_MODIFIED, DateTime.class), equalTo(now));
    assertThat(contentAttributes.get(Content.CONTENT_ETAG, String.class), equalTo(etag));
    assertThat(contentAttributes.contains(Content.CONTENT_HASH_CODES_MAP), is(true));
  }

  @Test
  public void lastModifiedAndEtagApply() {
    final DateTime now = DateTime.now();
    final String etag = "foo-bar";
    final NestedAttributesMap attributes = nestedAttributesMap();
    attributes.child(Content.CONTENT).set(Content.P_LAST_MODIFIED, now.toDate());
    attributes.child(Content.CONTENT).set(Content.P_ETAG, etag);
    Asset asset = mock(Asset.class);
    when(asset.attributes()).thenReturn(attributes);
    AttributesMap contentAttributes = new AttributesMap();
    contentAttributes.set(Content.CONTENT_LAST_MODIFIED, now);
    contentAttributes.set(Content.CONTENT_ETAG, etag);
    Content.applyToAsset(asset, contentAttributes);
    assertThat(asset.attributes().child(Content.CONTENT).get(Content.P_LAST_MODIFIED, Date.class),
        equalTo(now.toDate()));
    assertThat(asset.attributes().child(Content.CONTENT).get(Content.P_ETAG, String.class), equalTo(
        etag));
  }
}
