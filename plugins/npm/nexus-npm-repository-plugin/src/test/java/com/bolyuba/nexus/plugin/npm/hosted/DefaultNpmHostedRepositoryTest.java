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

import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.sisu.goodies.testsupport.TestSupport;

import com.bolyuba.nexus.plugin.npm.service.MetadataServiceFactory;
import com.bolyuba.nexus.plugin.npm.service.NpmBlob;
import com.bolyuba.nexus.plugin.npm.service.PackageRequest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

public class DefaultNpmHostedRepositoryTest
    extends TestSupport
{
  DefaultNpmHostedRepository underTest;

  @Mock
  ContentClass contentClass;

  @Mock
  NpmHostedRepositoryConfigurator npmHostedRepositoryConfigurator;

  @Mock
  MetadataServiceFactory metadataServiceFactory;

  @Mock
  PackageRequest packageRequest;

  @Mock
  NpmBlob attachment;

  @Before
  public void setUp() throws Exception {
    underTest = new DefaultNpmHostedRepository(contentClass, npmHostedRepositoryConfigurator, metadataServiceFactory);
  }

  @Test
  public void testGetStorageRequestPathScoped() {
    when(packageRequest.getPath()).thenReturn("/content/repositories/npm-hosted1/@sonatype/npm-test");
    when(packageRequest.isScoped()).thenReturn(true);

    String scopedAttachmentName = "@sonatype/npm-test-1.1.19.tgz";
    when(attachment.getName()).thenReturn(scopedAttachmentName);

    String expected = "/content/repositories/npm-hosted1/@sonatype/npm-test/-/npm-test-1.1.19.tgz";

    MatcherAssert.assertThat(underTest.getStorageRequestPath(packageRequest, attachment), Matchers.is(expected));
  }


  @Test
  public void testGetStorageRequestPathNotScoped() {
    when(packageRequest.getPath()).thenReturn("/content/repositories/npm-hosted1/npm-test");
    when(packageRequest.isScoped()).thenReturn(false);

    String scopedAttachmentName = "npm-test-1.1.19.tgz";
    when(attachment.getName()).thenReturn(scopedAttachmentName);

    String expected = "/content/repositories/npm-hosted1/npm-test/-/npm-test-1.1.19.tgz";

    MatcherAssert.assertThat(underTest.getStorageRequestPath(packageRequest, attachment), Matchers.is(expected));
  }

}
