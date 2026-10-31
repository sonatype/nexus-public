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
package org.sonatype.nexus.repository.content.maintenance;

import java.util.Set;
import java.util.stream.Stream;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;

/**
 * @since 3.24
 */
@Facet.Exposed
public interface ContentMaintenanceFacet
    extends Facet
{
  /**
   * Deletes a component from the repository; the format may decide to trigger additional deletes/updates.
   *
   * @return paths(s) of deleted asset(s)
   */
  Set<String> deleteComponent(Component component);

  /**
   * Deletes an asset from the repository; the format may decide to trigger additional deletes/updates.
   *
   * @return path(s) of deleted asset(s)
   */
  Set<String> deleteAsset(Asset asset);

  /**
   * Delete a batch of components.
   *
   * @param components the components to delete
   * @return number of components purged
   * @since 3.29
   */
  int deleteComponents(Stream<FluentComponent> components);

  /**
   * Validates whether {@code asset} may be deleted, throwing if a referential-integrity constraint
   * forbids it. Called by the soft-delete path before an asset is moved to Trash so that soft-delete
   * honors the same guards as hard-delete (e.g. Docker/OCI refuse deletion of a blob still referenced
   * by a live manifest — NEXUS-54127). Hard-delete formats enforce the same rule inside
   * {@link #deleteAsset(Asset)}.
   *
   * <p>
   * The default implementation is a no-op (no format-specific constraint). Formats override to enforce
   * their referential-integrity rules; implementations should throw a
   * {@code jakarta.ws.rs.WebApplicationException} (HTTP 409) or equivalent when deletion is refused.
   * </p>
   *
   * @param asset the asset being (soft-)deleted
   */
  default void validateAssetDeletable(final Asset asset) {
    // no-op by default; format-specific facets override to enforce referential integrity
  }

  /**
   * Whether soft-deleting {@code asset} should also soft-delete its parent component.
   *
   * <p>
   * This mirrors the hard-delete {@link #deleteAsset(Asset)} semantics so that soft-delete and
   * hard-delete stay consistent. Formats that treat a component as a single logical asset cascade
   * the delete to the component; formats whose components hold multiple independently-deletable
   * assets (e.g. Swift's manifest/archive/metadata) must not, otherwise the component and its
   * surviving sibling assets would be left in an inconsistent state (component in Trash while its
   * other assets remain live).
   * </p>
   *
   * <p>
   * The default implementation returns {@code false} (delete only the asset). {@code SingleAsset}
   * and {@code LastAsset} maintenance facets override this to match their {@code deleteAsset}
   * behavior.
   * </p>
   *
   * @param asset the asset being soft-deleted
   * @param lastRemainingAsset whether {@code asset} was the component's last remaining
   *          (non-soft-deleted) asset
   * @return {@code true} to cascade the soft-delete to the parent component
   */
  default boolean cascadeSoftDeleteToComponent(final Asset asset, final boolean lastRemainingAsset) {
    return false;
  }

  /**
   * Called after a component has been soft-deleted. Formats should override to update
   * aggregate metadata (e.g., Maven's {@code maven-metadata.xml}, npm packument).
   *
   * <p>
   * <strong>Idempotency contract:</strong> When the last asset of a component is soft-deleted,
   * both {@link #afterAssetSoftDeleted} and this method may fire for the same logical delete
   * operation. Implementations must be idempotent — applying the same metadata update twice
   * (e.g., removing the same version entry from a packument) must produce the same result
   * as applying it once, with no errors or side effects.
   * </p>
   *
   * The default implementation is a no-op.
   */
  default void afterComponentSoftDeleted(Component component) {
    // no-op by default; format-specific facets override as needed
  }

  /**
   * Called after a soft-deleted component has been restored. Formats should override to
   * rebuild aggregate metadata that now includes the restored component.
   *
   * <p>
   * <strong>Idempotency contract:</strong> When the last asset of a component is restored,
   * both {@link #afterAssetRestored} and {@link #afterComponentRestored} may fire for the
   * same logical restore. Implementations must be idempotent.
   * </p>
   *
   * The default implementation is a no-op.
   */
  default void afterComponentRestored(Component component) {
    // no-op by default; format-specific facets override as needed
  }

  /**
   * Called after an individual asset has been soft-deleted. Formats should override to update
   * aggregate metadata (e.g., npm packument, Yum repodata, APT package index).
   *
   * <p>
   * <strong>Idempotency contract:</strong> When the last asset of a component is soft-deleted,
   * both this method and {@link #afterComponentSoftDeleted} may fire for the same logical delete.
   * Implementations must be idempotent — applying the same metadata update twice must produce
   * the same result as applying it once. For example, removing a version entry from an npm
   * packument or marking Yum metadata as stale should be safe to call repeatedly.
   * </p>
   *
   * The default implementation is a no-op.
   */
  default void afterAssetSoftDeleted(Asset asset) {
    // no-op by default; format-specific facets override as needed
  }

  /**
   * Called after a soft-deleted asset has been restored. Formats should override to rebuild
   * aggregate metadata that now includes the restored asset.
   *
   * <p>
   * <strong>Idempotency contract:</strong> When the last asset of a component is restored,
   * both this method and {@link #afterComponentRestored} may fire for the same logical restore.
   * Implementations must be idempotent.
   * </p>
   *
   * The default implementation is a no-op.
   */
  default void afterAssetRestored(Asset asset) {
    // no-op by default; format-specific facets override as needed
  }
}
