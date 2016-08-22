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

import java.util.Optional;

import javax.annotation.Nonnull;

import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.DetachedEntityMetadata;
import org.sonatype.nexus.common.entity.DetachedEntityVersion;
import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.common.entity.EntityVersion;

import com.google.common.base.Throwables;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Attached {@link EntityMetadata}.
 *
 * @since 3.0
 */
public class AttachedEntityMetadata
    implements EntityMetadata
{
  private final EntityAdapter owner;

  private final ODocument document;

  private final EntityId id;

  private final EntityVersion version;

  public AttachedEntityMetadata(final EntityAdapter owner, final ODocument document) {
    this.owner = checkNotNull(owner);
    this.document = checkNotNull(document);
    this.id = new AttachedEntityId(owner, document.getIdentity());
    this.version = new AttachedEntityVersion(owner, document.getVersion());
  }

  public EntityAdapter getOwner() {
    return owner;
  }

  public ODocument getDocument() {
    return document;
  }

  @Override
  @Nonnull
  public EntityId getId() {
    return id;
  }

  @Override
  @Nonnull
  public EntityVersion getVersion() {
    return version;
  }

  /**
   * Returns detached entity metadata.
   */
  public DetachedEntityMetadata detach() {
    return new DetachedEntityMetadata(
        new DetachedEntityId(id.getValue()),
        new DetachedEntityVersion(version.getValue())
    );
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "schema=" + owner.getTypeName() +
        ", document=" + safeDocumentToString() +
        '}';
  }

  private String safeDocumentToString() {
    try {
      return document.toString();
    }
    catch (Exception e) { // NOSONAR
      return document.getIdentity().toString();
    }
  }

  @Override
  public <T> Optional<Class<T>> getEntityType() {
    return Optional.of(getOwner().getEntityType());
  }

  @Override
  public <T extends Entity> Optional<T> getEntity() {
    final Entity newEntity = getOwner().newEntity();
    newEntity.setEntityMetadata(this);
    try {
      getOwner().readFields(getDocument(), newEntity);
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }

    return Optional.of((T) newEntity);
  }
}
