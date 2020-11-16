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

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link EntityId} backed by a {@link UUID}.
 *
 * @since 3.19
 */
public class EntityUUID
    implements EntityId
{
  @JsonProperty("value")
  private final UUID id;

  public EntityUUID() {
    this.id = UUID.randomUUID();
  }

  public EntityUUID(final UUID id) {
    this.id = checkNotNull(id);
  }

  public UUID uuid() {
    return id;
  }

  @Override
  public String getValue() {
    return id.toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof EntityUUID) {
      return id.equals(((EntityUUID) o).id);
    }
    if (o instanceof EntityId) {
      return getValue().equals(((EntityId) o).getValue());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getValue().hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "id='" + id + '\'' +
        '}';
  }
}
