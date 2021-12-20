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
package com.bolyuba.nexus.plugin.npm.hosted;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.sisu.goodies.testsupport.TestSupport;

import com.bolyuba.nexus.plugin.npm.service.HostedMetadataService;
import org.sonatype.nexus.proxy.attributes.Attributes;
import org.sonatype.nexus.proxy.item.StorageFileItem;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RecreateMetadataWalkerProcessorTest
    extends TestSupport
{
  RecreateMetadataWalkerProcessor underTest;

  @Mock
  HostedMetadataService hostedMetadataService;

  @Before
  public void setUp() throws Exception {
    underTest = new RecreateMetadataWalkerProcessor(hostedMetadataService);
  }

  @Test
  public void testRepairVersionJson() {
    Map<String, Object> versionJson = new HashMap<String, Object>();
    versionJson.put("version", "1.0.0-SNAPSHOT");

    StorageFileItem file = mock(StorageFileItem.class);
    Attributes attributes = mock(Attributes.class);
    when(attributes.get(StorageFileItem.DIGEST_SHA1_KEY)).thenReturn("e8d7d44af51c63a40f39fe749e6db49535d769da");
    when(file.getRepositoryItemAttributes()).thenReturn(attributes);

    underTest.repairVersionJson(versionJson, file);

    assertThat(versionJson.get("version"), is("1.0.0-SNAPSHOT"));
    Map<String, Object> dist = (Map<String, Object>) versionJson.get("dist");
    assertThat(dist.get("tarball"), is("generated-on-request"));
    assertThat(dist.get("shasum"), is("e8d7d44af51c63a40f39fe749e6db49535d769da"));
  }

  @Test
  public void testRepairVersionJson_withBuildMetadataVersion() {
    Map<String, Object> versionJson = new HashMap<>();
    versionJson.put("version", "1.0.0-SNAPSHOT+abc123");

    StorageFileItem file = mock(StorageFileItem.class);
    Attributes attributes = mock(Attributes.class);
    when(attributes.get(StorageFileItem.DIGEST_SHA1_KEY)).thenReturn("e8d7d44af51c63a40f39fe749e6db49535d769da");
    when(file.getRepositoryItemAttributes()).thenReturn(attributes);

    underTest.repairVersionJson(versionJson, file);

    assertThat(versionJson.get("version"), is("1.0.0-SNAPSHOT"));
  }
}
