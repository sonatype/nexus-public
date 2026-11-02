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
package org.sonatype.nexus.repository.content.trash.analytics;

/**
 * Canonical entity-type dimension values used by trash analytics keys of the form
 * {@code format::entityType} (e.g. {@code maven2::COMPONENT}).
 *
 * <p>
 * These are deliberately upper-cased and distinct from the lower-cased content {@code entity_type} column values
 * ({@code asset}/{@code component}/{@code repository}); the analytics dimension is a stable, downstream-facing
 * contract and should not be coupled to the storage-layer casing.
 * </p>
 */
public final class TrashEntityType
{
  public static final String REPOSITORY = "REPOSITORY";

  public static final String COMPONENT = "COMPONENT";

  public static final String ASSET = "ASSET";

  private TrashEntityType() {
    // constants holder
  }
}
