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
package org.sonatype.nexus.repository.search.index;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.rest.SearchMapping;
import org.sonatype.nexus.repository.rest.SearchMappings;
import org.sonatype.nexus.repository.rest.api.RepositoryManagerRESTAdapter;
import org.sonatype.nexus.repository.search.SearchUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

public class SearchUtilsTest
    extends TestSupport
{
  static final String VALID_SHA1_ATTRIBUTE_NAME = "assets.attributes.checksum.sha1";

  static final String INVALID_SHA1_ATTRIBUTE_NAME = "asset.attributes.checksum.sha1";

  static final String SHA1_ALIAS = "sha1";

  @Mock
  RepositoryManagerRESTAdapter repositoryManagerRESTAdapter;

  SearchUtils underTest;

  @Before
  public void setup() {

    Map<String, SearchMappings> searchMappings = ImmutableMap.of(
        "default", () -> ImmutableList.of(
            new SearchMapping(SHA1_ALIAS, VALID_SHA1_ATTRIBUTE_NAME, "")
        )
    );

    underTest = new SearchUtils(repositoryManagerRESTAdapter, searchMappings);
  }

  @Test
  public void testIsAssetSearchParam_MappedAlias_Sha1() {
    assertTrue(underTest.isAssetSearchParam(SHA1_ALIAS));
  }

  @Test
  public void testIsAssetSearchParam_UnMapped_FullAssetAttributeName() {
    assertTrue(underTest.isAssetSearchParam(VALID_SHA1_ATTRIBUTE_NAME));
  }

  @Test
  public void testIsAssetSearchParam_UnMappedAlias_Returns_False() {
    assertFalse(underTest.isAssetSearchParam("new.asset"));
  }

  @Test
  public void testIsAssetSearchParam_Invalid_Full_AssetAttribute() {
    assertFalse(underTest.isAssetSearchParam(INVALID_SHA1_ATTRIBUTE_NAME));
  }

  @Test
  public void testIsFullAssetAttributeName() {
    assertTrue(underTest.isFullAssetAttributeName(VALID_SHA1_ATTRIBUTE_NAME));
  }

  @Test
  public void testIsFullAssetAttributeName_Invalid_LongForm_Attribute_ReturnsFalse() {
    assertFalse(underTest.isFullAssetAttributeName(INVALID_SHA1_ATTRIBUTE_NAME));
  }

  @Test
  public void testIsFullAssetAttributeName_MappedAlias_ReturnsFalse() {
    assertFalse(underTest.isFullAssetAttributeName(SHA1_ALIAS));
  }

  @Test
  public void testGetRepository() {
    String repositoryId = "repositoryId";
    underTest.getRepository(repositoryId);

    verify(repositoryManagerRESTAdapter).getReadableRepository(repositoryId);
  }
}
