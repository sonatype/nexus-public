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

import org.sonatype.nexus.common.entity.ContinuationTokenHelper;
import org.sonatype.nexus.common.entity.DetachedEntityId;
import org.sonatype.nexus.common.entity.Entity;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.common.entity.EntityHelper.id;

/**
 * Orient based implementation of {@link ContinuationTokenHelper}
 *
 * @since 3.7
 */
public abstract class OrientContinuationTokenHelper
    implements ContinuationTokenHelper
{
  private final EntityAdapter<?> entityAdapter;

  public OrientContinuationTokenHelper(final EntityAdapter<?> entityAdapter) {
    this.entityAdapter = checkNotNull(entityAdapter);
  }

  @Nullable
  @Override
  public String getIdFromToken(final String continuationToken) {
    try {
      return continuationToken != null ?
          entityAdapter.recordIdentity(new DetachedEntityId(continuationToken)).toString() : null;
    }
    catch (IllegalArgumentException e) {
      throw new ContinuationTokenException(
          format("Caught exception parsing id from continuation token '%s'", continuationToken), e);
    }
  }

  @Override
  public String getTokenFromId(final Entity entity) {
    return id(entity).getValue();
  }
}
