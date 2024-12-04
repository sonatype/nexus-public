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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.BaseUrlHolder;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl;
import org.sonatype.nexus.repository.rest.api.SimpleApiRepositoryAdapterTest.SimpleConfiguration;
import org.sonatype.nexus.repository.search.AssetSearchResult;
import org.sonatype.nexus.repository.types.HostedType;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class AssetXOTest
    extends TestSupport
{

  @Before
  public void setup() {
    BaseUrlHolder.set("https://nexus-url", "");
  }

  @Test
  @Parameters({
      "hosted, /path/to/resource, /hosted/path/to/resource",
      "hosted, path/to/resource, /hosted/path/to/resource"
  })
  public void testFrom(String repositoryName, String path, String expectedUrl) throws Exception {
    Repository repository = createRepository(new HostedType(), repositoryName);
    AssetSearchResult assetSearchResult = Mockito.mock(AssetSearchResult.class);
    when(assetSearchResult.getPath()).thenReturn(path);
    when(assetSearchResult.getId()).thenReturn("resource-id");
    when(assetSearchResult.getFormat()).thenReturn("test-format");
    AssetXO assetXO = AssetXO.from(assetSearchResult, repository, null);
    Assert.assertTrue(assetXO.getDownloadUrl().contains(expectedUrl));
  }

  private static Repository createRepository(final Type type, String repositoryName) throws Exception {
    Repository repository = new RepositoryImpl(
        Mockito.mock(EventManager.class),
        type,
        new Format("test-format")
        {
        });
    repository.init(config(repositoryName));
    return repository;
  }

  private static Configuration config(final String repositoryName) {
    Configuration configuration = new SimpleConfiguration();
    configuration.setOnline(true);
    configuration.setRepositoryName(repositoryName);
    return configuration;
  }
}
