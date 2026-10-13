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
package org.sonatype.nexus.common.app.softdelete;

/**
 * Abstraction for recording and removing entries in the trash index.
 *
 * Implemented by {@code TrashIndexStore} in the private soft-delete plugin.
 * Public-module services depend on this interface so they can update the trash
 * index without a compile-time dependency on the private module.
 */
public interface TrashIndexRecorder
{
  void record(SoftDeletedEntityInfo info);

  void remove(String entityType, String entityId);

  /**
   * Removes <em>all</em> trash index asset entries whose {@code parent_component_id} matches the given
   * component identifier, regardless of their {@code deleted_on} timestamp. Used when a component is
   * soft-deleted so every child asset (including any deleted individually beforehand, with an earlier
   * timestamp) is subsumed under the component's single Trash entry -- the assets remain visible by expanding
   * the component, and no earlier-deleted asset is left showing both at the top level and as a child.
   */
  void removeByParentComponent(String parentComponentId);

  /**
   * Removes a trash index entry for an asset that was silently un-deleted
   * by a {@code createAsset} upsert (e.g., metadata rebuild after component restore).
   * Called from {@code AssetStore.createAsset()} to prevent stale trash index entries.
   */
  void removeAssetByPath(String repositoryName, String path);

  /**
   * Removes all trash index entries (soft-deleted components and assets) belonging to the given
   * repository. Called when a repository is purged: its content is physically deleted by the purge, so
   * any individually soft-deleted children would otherwise remain as un-restorable ghosts in the Trash view.
   */
  void removeByRepositoryName(String repositoryName);

  /**
   * Removes the trash index entry for a component that was silently un-deleted by a {@code createComponent}
   * upsert (re-upload). Called from {@code ContentFacetSupport} so the revived component no longer shows as a
   * ghost in Trash. The implementation encodes {@code repositoryName:componentExternalId} into the identifier.
   *
   * @return the number of trash index rows removed (0 when the component had no entry, e.g. a brand-new or
   *         never-trashed component). Callers can use a zero result to skip further reconciliation work.
   */
  int removeComponentByExternalId(String repositoryName, String componentExternalId);

  /**
   * Re-records a still-soft-deleted child asset as its own trash index entry after its parent component was
   * revived by a re-upload. The component's single "umbrella" entry no longer represents the child, so each
   * sibling that was not part of the re-upload gets an independent entry to stay restorable/purgeable. The
   * implementation encodes the asset and parent-component identifiers; the insert is an upsert, so recording
   * an entry that already exists is a harmless no-op.
   */
  void resurfaceSoftDeletedChildAsset(SoftDeletedAssetRef asset);
}
