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
package org.sonatype.repository.helm.datastore.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.repository.helm.datastore.internal.restore.HelmRestoreFacetImpl;
import org.sonatype.repository.helm.internal.AssetKind;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;

/**
 * @since 3.next
 */
public class HelmRestoreFacetImplTest
    extends TestSupport
{
  private static final String PACKAGE_PATH = "mongodb-7.8.10.tgz";

  private static final String PROVENANCE_PATH = "mongodb-7.8.10.tgz.prov";

  private static final String INDEX_PATH = "index.yaml";

  @Mock
  private HelmRestoreFacetImpl restoreFacet;

  @Mock
  private HelmContentFacet helmContentFacet;

  @Mock
  Blob packageBlob;

  @Mock
  Blob provenanceBlob;

  @Mock
  Blob indexBlob;

  @Before
  public void setUp() throws IOException {
    Whitebox.setInternalState(restoreFacet, "helmContentFacet", helmContentFacet);
    doCallRealMethod().when(restoreFacet).isRestorable(any());
    doCallRealMethod().when(restoreFacet).restore(any(), any());

    Map<String, String> packageBlobProperties = new HashMap<>();
    packageBlobProperties.put(CONTENT_TYPE_HEADER, "some-content-type");
    when(packageBlob.getHeaders()).thenReturn(packageBlobProperties);

    Map<String, String> provenanceBlobProperties = new HashMap<>();
    provenanceBlobProperties.put(CONTENT_TYPE_HEADER, "some-content-type");
    when(provenanceBlob.getHeaders()).thenReturn(provenanceBlobProperties);

    Map<String, String> indexBlobProperties = new HashMap<>();
    packageBlobProperties.put(CONTENT_TYPE_HEADER, "some-content-type");
    when(indexBlob.getHeaders()).thenReturn(indexBlobProperties);
  }

  @Test
  public void testIsRestorable() {
    assertTrue(restoreFacet.isRestorable(PACKAGE_PATH));
    assertTrue(restoreFacet.isRestorable(PROVENANCE_PATH));
  }

  @Test
  public void testIsNotRestorable() {
    assertFalse(restoreFacet.isRestorable(INDEX_PATH));
  }

  @Test
  public void testRestorePackage() throws IOException {
    restoreFacet.restore(packageBlob, PACKAGE_PATH);
    verify(helmContentFacet).putComponent(eq(PACKAGE_PATH), any(), eq(AssetKind.HELM_PACKAGE));
  }

  @Test
  public void testRestoreProvenance() throws IOException {
    restoreFacet.restore(provenanceBlob, PROVENANCE_PATH);
    verify(helmContentFacet).putComponent(eq(PROVENANCE_PATH), any(), eq(AssetKind.HELM_PROVENANCE));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNotRestoreIndex() throws IOException {
    restoreFacet.restore(indexBlob, INDEX_PATH);
  }
}
