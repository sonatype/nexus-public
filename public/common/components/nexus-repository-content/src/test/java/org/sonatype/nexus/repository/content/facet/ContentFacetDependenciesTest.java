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
package org.sonatype.nexus.repository.content.facet;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.move.RepositoryMoveService;
import org.sonatype.nexus.repository.search.normalize.VersionNormalizerService;
import org.sonatype.nexus.repository.storage.BlobMetadataStorage;
import org.sonatype.nexus.validation.ConstraintViolationFactory;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ContentFacetDependenciesTest
    extends TestSupport
{
  @Mock
  private BlobStoreManager blobStoreManager;

  @Mock
  private DataSessionSupplier dataSessionSupplier;

  @Mock
  private ConstraintViolationFactory constraintViolationFactory;

  @Mock
  private org.sonatype.nexus.security.ClientInfoProvider clientInfoProvider;

  @Mock
  private NodeAccess nodeAccess;

  @Mock
  private AssetBlobValidators assetBlobValidators;

  @Mock
  private BlobMetadataStorage blobMetadataStorage;

  @Mock
  private VersionNormalizerService versionNormalizerService;

  @Mock
  private RepositoryMoveService moveService;

  @Test
  public void testConstructorWithAllDependencies() {
    ContentFacetDependencies underTest = new ContentFacetDependencies(
        blobStoreManager, dataSessionSupplier, constraintViolationFactory,
        clientInfoProvider, nodeAccess, assetBlobValidators, blobMetadataStorage,
        versionNormalizerService, moveService);

    assertThat(underTest.getBlobStoreManager(), is(blobStoreManager));
    assertThat(underTest.getDataSessionSupplier(), is(dataSessionSupplier));
    assertThat(underTest.getConstraintViolationFactory(), is(constraintViolationFactory));
    assertThat(underTest.getClientInfoProvider(), is(clientInfoProvider));
    assertThat(underTest.getNodeAccess(), is(nodeAccess));
    assertThat(underTest.getAssetBlobValidators(), is(assetBlobValidators));
    assertThat(underTest.getBlobMetadataStorage(), is(blobMetadataStorage));
    assertThat(underTest.getVersionNormalizerService(), is(versionNormalizerService));
    assertThat(underTest.getMoveService().isPresent(), is(true));
    assertThat(underTest.getMoveService().get(), is(moveService));
  }

  @Test
  public void testConstructorWithNullMoveService() {
    ContentFacetDependencies underTest = new ContentFacetDependencies(
        blobStoreManager, dataSessionSupplier, constraintViolationFactory,
        clientInfoProvider, nodeAccess, assetBlobValidators, blobMetadataStorage,
        versionNormalizerService, null);

    assertThat(underTest.getMoveService().isPresent(), is(false));
  }
}
