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

import org.sonatype.nexus.common.entity.Entity;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.common.entity.EntityMetadata;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Attached {@link Entity} helpers.
 *
 * @since 3.0
 */
public class AttachedEntityHelper
{
  private AttachedEntityHelper() {
    // empty
  }

  /**
   * Returns attached metadata of entity.
   */
  public static AttachedEntityMetadata metadata(final Entity entity) {
    EntityMetadata metadata = EntityHelper.metadata(entity);
    checkState(metadata instanceof AttachedEntityMetadata, "Entity not attached");
    return (AttachedEntityMetadata) metadata;
  }

  /**
   * Check if given entity is attached.
   */
  public static boolean isAttached(final Entity entity) {
    return EntityHelper.metadata(entity) instanceof AttachedEntityMetadata;
  }

  /**
   * Returns attached document of entity.
   */
  public static ODocument document(final Entity entity) {
    checkNotNull(entity);
    return metadata(entity).getDocument();
  }

  /**
   * Returns attached record-id of entity.
   */
  public static ORID id(final Entity entity) {
    return document(entity).getIdentity();
  }

  /**
   * Returns attached record-version of entity.
   */
  public static int version(final Entity entity) {
    return document(entity).getVersion();
  }
}