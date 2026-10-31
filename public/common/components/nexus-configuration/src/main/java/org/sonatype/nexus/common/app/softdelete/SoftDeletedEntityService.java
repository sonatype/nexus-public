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

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Service interface for managing soft-deleted entities of a specific type.
 *
 * Each entity type (repository, tag, user, etc.) provides its own implementation
 * that handles the specifics of soft-deletion, restoration, and permanent purge.
 */
public interface SoftDeletedEntityService
{
  String getEntityType();

  void softDelete(String identifier, SoftDeleteContext context);

  void restore(String identifier);

  void purge(String identifier);

  int purgeExpired(int retentionDays);

  /**
   * Purge expired entities, draining fully but cooperating with cancellation.
   *
   * <p>
   * Large entity types (component, asset) override this to drain in bounded per-batch transactions,
   * checking {@code cancelled} between batches. Small types (repository, tag, user) operate on tiny
   * tables and may ignore cancellation via this default. (NEXUS-49147 / NEXUS-51733)
   * </p>
   *
   * @param retentionDays retention window in days
   * @param cancelled supplies {@code true} when the running task has been cancelled
   * @return the number of entities permanently removed
   */
  default int purgeExpired(int retentionDays, BooleanSupplier cancelled) {
    return purgeExpired(retentionDays);
  }

  Optional<SoftDeletedEntityInfo> getDeletedEntity(String identifier);

  RestoreValidation validateRestore(String identifier);

  /**
   * Lists the soft-deleted children of a soft-deleted entity, for the expandable Trash hierarchy.
   *
   * <p>
   * Only meaningful for container entities -- currently a soft-deleted component's soft-deleted assets
   * (the assets that were cascade-collapsed into the component's single Trash entry). The default returns
   * an empty list so leaf/unsupported types (asset, and repository in v1) contribute no children. Returned
   * identifiers are encoded exactly like top-level entries, so they are directly consumable by
   * {@link #restore(String)} / {@link #purge(String)} and the bulk endpoints (NEXUS-49147).
   * </p>
   *
   * @param identifier the encoded identifier of the parent soft-deleted entity
   * @return the parent's soft-deleted children as {@link SoftDeletedEntityInfo}
   */
  default List<SoftDeletedEntityInfo> browseChildren(String identifier) {
    return List.of();
  }
}
