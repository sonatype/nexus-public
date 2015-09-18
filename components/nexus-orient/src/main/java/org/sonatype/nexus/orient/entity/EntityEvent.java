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
package org.sonatype.nexus.orient.entity;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.entity.EntityVersion;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Entity event.
 *
 * @since 3.0
 */
@SuppressWarnings("unchecked")
public abstract class EntityEvent
{
  private final EntityMetadata metadata;

  private volatile Entity entity;

  public EntityEvent(final EntityMetadata metadata) {
    this.metadata = checkNotNull(metadata);
  }

  public EntityId getId() {
    return metadata.getId();
  }

  public EntityVersion getVersion() {
    return metadata.getVersion();
  }

  /**
   * @return attached entity type, if it exists
   */
  @Nullable
  public Class<? extends Entity> getEntityType() {
    if (metadata instanceof AttachedEntityMetadata) {
      return ((AttachedEntityMetadata) metadata).getOwner().getEntityType();
    }
    return null;
  }

  /**
   * @return attached entity, if it exists
   */
  @Nullable
  public <T extends Entity> T getEntity() {
    // can be expensive depending on the entity, so use lazy evaluation
    if (entity == null && metadata instanceof AttachedEntityMetadata) {
      synchronized (this) {
        if (entity == null) {
          final AttachedEntityMetadata attachedMetadata = (AttachedEntityMetadata) metadata;
          final EntityAdapter<Entity> owner = attachedMetadata.getOwner();

          final Entity newEntity = owner.newEntity();
          newEntity.setEntityMetadata(metadata);
          try {
            owner.readFields(attachedMetadata.getDocument(), newEntity);
          }
          catch (Exception e) {
            throw Throwables.propagate(e);
          }

          entity = newEntity;
        }
      }
    }
    return (T) entity;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "metadata=" + metadata +
        '}';
  }
}
