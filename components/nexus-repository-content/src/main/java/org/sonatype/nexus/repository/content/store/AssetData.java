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
package org.sonatype.nexus.repository.content.store;

import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.ContinuationAware;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.content.Component;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Optional.ofNullable;

/**
 * {@link Asset} data backed by the content data store.
 *
 * @since 3.20
 */
public class AssetData
    extends AbstractRepositoryContent
    implements Asset, ContinuationAware
{
  Integer assetId; // NOSONAR: internal id

  private String path;

  @Nullable
  private Integer componentId;

  @Nullable
  private Component component;

  @Nullable
  private Integer assetBlobId;

  @Nullable
  private AssetBlob assetBlob;

  @Nullable
  private DateTime lastDownloaded;

  // Asset API

  @Override
  public String path() {
    return path;
  }

  @Override
  public Optional<Component> component() {
    return ofNullable(getComponent()); // trigger lazy-loading by calling getter
  }

  @Override
  public Optional<AssetBlob> blob() {
    return ofNullable(getAssetBlob()); // trigger lazy-loading by calling getter
  }

  @Override
  public Optional<DateTime> lastDownloaded() {
    return ofNullable(lastDownloaded);
  }

  // MyBatis setters + validation

  /**
   * Sets the internal asset id.
   */
  public void setAssetId(final int assetId) {
    this.assetId = assetId;
  }

  /**
   * Sets the asset path.
   */
  public void setPath(final String path) {
    this.path = checkNotNull(path);
  }

  /**
   * Sets the (optional) owning component.
   */
  public void setComponent(@Nullable final Component component) {
    if (component != null) {
      ComponentData componentData = (ComponentData) component;
      checkState(componentData.componentId != null, "Add Component to content store before attaching it to Asset");
      this.componentId = componentData.componentId;
    }
    else {
      this.componentId = null;
    }
    this.component = component;
  }

  /**
   * Sets the (optional) blob attached to this asset.
   */
  public void setAssetBlob(@Nullable final AssetBlob assetBlob) {
    if (assetBlob != null) {
      AssetBlobData assetBlobData = (AssetBlobData) assetBlob;
      checkState(assetBlobData.assetBlobId != null, "Add AssetBlob to content store before attaching it to Asset");
      this.assetBlobId = assetBlobData.assetBlobId;
    }
    else {
      this.assetBlobId = null;
    }
    this.assetBlob = assetBlob;
  }

  /**
   * Sets the (optional) last downloaded time.
   */
  public void setLastDownloaded(@Nullable final DateTime lastDownloaded) {
    this.lastDownloaded = lastDownloaded;
  }

  // Getters to support lazy-loading (MyBatis will intercept them)

  @Nullable
  protected Component getComponent() {
    return component;
  }

  @Nullable
  protected AssetBlob getAssetBlob() {
    return assetBlob;
  }

  // ContinuationAware

  @Override
  public String nextContinuationToken() {
    return Integer.toString(assetId);
  }
}
