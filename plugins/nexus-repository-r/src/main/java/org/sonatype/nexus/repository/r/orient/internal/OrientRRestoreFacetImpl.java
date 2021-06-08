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
package org.sonatype.nexus.repository.r.orient.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.inject.Named;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.r.orient.OrientRFacet;
import org.sonatype.nexus.repository.r.orient.OrientRRestoreFacet;
import org.sonatype.nexus.repository.r.internal.RAttributes;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.sonatype.nexus.repository.r.internal.util.RDescriptionUtils.extractDescriptionFromArchive;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * @since 1.1.next
 *
 * @since 3.28
 */
@Named
public class OrientRRestoreFacetImpl
    extends FacetSupport
    implements OrientRRestoreFacet
{
  @Override
  @TransactionalTouchBlob
  public void restore(final AssetBlob assetBlob, final String path) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    OrientRFacet facet = facet(OrientRFacet.class);

    Asset asset;
    if (componentRequired(path)) {
      Map<String, String> attributes;

      try (InputStream is = assetBlob.getBlob().getInputStream()) {
        attributes = extractDescriptionFromArchive(path, is);
      }

      Component component = facet.findOrCreateComponent(tx, path, attributes);
      asset = facet.findOrCreateAsset(tx, component, path, attributes);
    }
    else {
      asset = facet.findOrCreateAsset(tx, path);
    }
    tx.attachBlob(asset, assetBlob);

    Content.applyToAsset(asset, Content.maintainLastModified(asset, new AttributesMap()));
    tx.saveAsset(asset);
  }

  @Override
  @TransactionalTouchBlob
  public boolean assetExists(final String path) {
    final StorageTx tx = UnitOfWork.currentTx();
    return tx.findAssetWithProperty(P_NAME, path, tx.findBucket(getRepository())) != null;
  }

  @Override
  public boolean componentRequired(final String name) {
    return !(name.contains("/PACKAGES") || name.endsWith(".rds"));
  }

  @Override
  public Query getComponentQuery(final Map<String, String> attributes) {
    return Query.builder().where(P_NAME).eq(attributes.get(RAttributes.P_PACKAGE))
        .and(P_VERSION).eq(attributes.get(RAttributes.P_VERSION)).build();
  }

  @Override
  public Map<String, String> extractComponentAttributesFromArchive(final String filename, final InputStream is) {
    return extractDescriptionFromArchive(filename, is);
  }
}
