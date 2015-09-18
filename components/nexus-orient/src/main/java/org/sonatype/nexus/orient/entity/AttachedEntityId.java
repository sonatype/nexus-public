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

import javax.annotation.Nonnull;

import org.sonatype.nexus.common.entity.EntityId;

import com.orientechnologies.orient.core.id.ORID;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Attached {@link EntityId}.
 *
 * @since 3.0
 */
public class AttachedEntityId
    implements EntityId
{
  private final EntityAdapter owner;

  private final ORID identity;

  /**
   * Cached encoded value of identity.
   */
  private volatile String encoded;

  public AttachedEntityId(final EntityAdapter owner, final ORID identity) {
    this.owner = checkNotNull(owner);
    this.identity = checkNotNull(identity);
  }

  public ORID getIdentity() {
    return identity;
  }

  @Override
  @Nonnull
  public String getValue() {
    if (encoded == null) {
      checkState(!identity.isTemporary(), "attempted use of temporary/uncommitted document id");
      encoded = owner.getRecordIdObfuscator().encode(owner.getSchemaType(), identity);
    }
    return encoded;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    else if (o instanceof AttachedEntityId) {
      AttachedEntityId that = (AttachedEntityId)o;
      return identity.equals(that.identity);
    }
    else if (o instanceof EntityId) {
      EntityId that = (EntityId)o;
      return getValue().equals(that.getValue());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return getValue().hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + '{' +
        owner.getSchemaType() + "->" +
        identity +
        '}';
  }
}
