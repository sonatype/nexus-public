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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.view.Content;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * The resultant asset paths and associated component id with a component upload
 *
 * @since 3.10
 */
public class UploadResponse
{
  private final List<String> assetPaths;

  private final List<EntityId> componentIds;

  public UploadResponse(final Asset asset) {
    checkNotNull(asset);
    this.assetPaths = singletonList(asset.name());
    this.componentIds = Optional.of(asset)
        .map(Asset::componentId)
        .map(Collections::singletonList)
        .orElse(emptyList());
  }

  public UploadResponse(final EntityId entityId, final List<String> assetPaths) {
    this.componentIds = ImmutableList.of(checkNotNull(entityId));
    this.assetPaths = checkNotNull(assetPaths);
  }

  public UploadResponse(final Content content, final List<String> assetPaths) {
    this.componentIds = extractComponentIds(ImmutableList.of(checkNotNull(content)));
    this.assetPaths = checkNotNull(assetPaths);
  }

  public UploadResponse(final Collection<Content> contents, final List<String> assetPaths) {
    this.componentIds = extractComponentIds(checkNotNull(contents));
    this.assetPaths = checkNotNull(assetPaths);
  }

  @Nullable
  public EntityId getComponentId() {
    return componentIds.stream().findFirst().orElse(null);
  }

  public List<EntityId> getComponentIds() {
    return componentIds;
  }

  public List<String> getAssetPaths() {
    return assetPaths;
  }

  private static List<EntityId> extractComponentIds(final Collection<Content> contents) {
    return contents.stream()
        .map(UploadResponse::extractComponentId)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Nullable
  private static EntityId extractComponentId(final Content content) {
    Optional<Asset> asset = Optional.ofNullable(content.getAttributes().get(Asset.class));
    return asset.map(Asset::componentId).orElse(null);
  }
}
