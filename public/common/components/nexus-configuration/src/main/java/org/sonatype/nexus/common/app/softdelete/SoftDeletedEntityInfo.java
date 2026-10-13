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

import java.time.OffsetDateTime;
import java.util.Map;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Information about a soft-deleted entity, used for browse/display in the UI and REST API.
 */
public class SoftDeletedEntityInfo
{
  private final String entityType;

  private final String identifier;

  private final String displayName;

  private final OffsetDateTime deletedOn;

  private final String deletedBy;

  @Nullable
  private final Map<String, Object> metadata;

  public SoftDeletedEntityInfo(
      final String entityType,
      final String identifier,
      final String displayName,
      final OffsetDateTime deletedOn,
      final String deletedBy,
      @Nullable final Map<String, Object> metadata)
  {
    this.entityType = requireNonNull(entityType);
    this.identifier = requireNonNull(identifier);
    this.displayName = requireNonNull(displayName);
    this.deletedOn = requireNonNull(deletedOn);
    this.deletedBy = requireNonNull(deletedBy);
    this.metadata = metadata;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getIdentifier() {
    return identifier;
  }

  public String getDisplayName() {
    return displayName;
  }

  public OffsetDateTime getDeletedOn() {
    return deletedOn;
  }

  public String getDeletedBy() {
    return deletedBy;
  }

  @Nullable
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  @Override
  public String toString() {
    return "SoftDeletedEntityInfo{entityType='" + entityType + "', identifier='" + identifier +
        "', deletedOn=" + deletedOn + ", deletedBy='" + deletedBy + "'}";
  }
}
