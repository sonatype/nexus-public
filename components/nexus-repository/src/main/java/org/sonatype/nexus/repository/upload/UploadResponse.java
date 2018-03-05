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
package org.sonatype.nexus.repository.upload;

import java.util.List;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.view.Content;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.*;

/**
 * The resultant asset paths and associated component id with a component upload
 *
 * @since 3.next
 */
public final class UploadResponse
{
  private final List<String> assetPaths;

  private final EntityId componentId;

  public UploadResponse(final Asset asset) {
    checkNotNull(asset);
    this.assetPaths = singletonList(asset.name());
    this.componentId = asset.componentId();
  }

  public UploadResponse(final EntityId entityId, final List<String> assetPaths) {
    this.componentId = checkNotNull(entityId);
    this.assetPaths = checkNotNull(assetPaths);
  }

  public UploadResponse(final Content content, final List<String> assetPaths) {
    this.componentId = extractComponentId(checkNotNull(content));
    this.assetPaths = checkNotNull(assetPaths);
  }

  public EntityId getComponentId() {
    return componentId;
  }

  public List<String> getAssetPaths() {
    return assetPaths;
  }

  private EntityId extractComponentId(final Content content) {
    Asset asset = content.getAttributes().get(Asset.class);
    return asset.componentId();
  }
}
