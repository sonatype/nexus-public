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
package org.sonatype.nexus.common.entity;

import java.io.Serializable;
import java.util.Optional;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Detached {@link EntityMetadata}.
 *
 * @since 3.0
 */
public class DetachedEntityMetadata
    implements EntityMetadata, Serializable
{
  private static final long serialVersionUID = 1L;

  private final DetachedEntityId id;

  private final DetachedEntityVersion version;

  public DetachedEntityMetadata(final DetachedEntityId id, final DetachedEntityVersion version) {
    this.id = checkNotNull(id);
    this.version = checkNotNull(version);
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

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "id=" + id +
        ", version=" + version +
        '}';
  }

  @Override
  public <T> Optional<Class<T>> getEntityType() {
    return Optional.empty();
  }

  @Override
  public <T extends Entity> Optional<T> getEntity() {
    return Optional.empty();
  }
}
