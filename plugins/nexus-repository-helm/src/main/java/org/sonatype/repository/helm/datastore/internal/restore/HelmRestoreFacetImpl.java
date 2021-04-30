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
package org.sonatype.repository.helm.datastore.internal.restore;

import java.io.IOException;

import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.repository.helm.datastore.HelmRestoreFacet;
import org.sonatype.repository.helm.datastore.internal.HelmContentFacet;
import org.sonatype.repository.helm.internal.AssetKind;

import static org.sonatype.nexus.blobstore.api.BlobStore.CONTENT_TYPE_HEADER;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PACKAGE;
import static org.sonatype.repository.helm.internal.AssetKind.HELM_PROVENANCE;

/**
 * @since 3.next
 */
@Named
public class HelmRestoreFacetImpl
    extends FacetSupport
    implements HelmRestoreFacet
{
  private HelmContentFacet helmContentFacet;

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    helmContentFacet = facet(HelmContentFacet.class);
  }

  @Override
  public void restore(final Blob blob, final String path) throws IOException {
    AssetKind assetKind = AssetKind.getAssetKindByFileName(path);
    if (assetKind != HELM_PACKAGE && assetKind != HELM_PROVENANCE) {
      throw new IllegalArgumentException("Unsupported assetKind: " + assetKind);
    }
    helmContentFacet.putComponent(path,
        new Content(new BlobPayload(blob, blob.getHeaders().get(CONTENT_TYPE_HEADER))), assetKind);
  }

  @Override
  public boolean isRestorable(final String name) {
    return AssetKind.getAssetKindByFileName(name) != AssetKind.HELM_INDEX;
  }
}
