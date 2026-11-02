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

import java.util.Map;

/**
 * Read-path SPI for point-in-time trash (Trash) state, keyed by {@code format::entityType}.
 *
 * <p>
 * Lives in {@code nexus-repository-content} so the analytics marker (in {@code nexus-analytics-common}) can consume it
 * without depending on {@code nexus-trash-plugin}, which owns the trash-index store that implements it. The
 * implementation is optional and wired at runtime by Spring; when absent (e.g. Community Edition), the marker reports
 * empty maps.
 * </p>
 *
 * <p>
 * Keys are of the form {@code format::entityType} (e.g. {@code maven2::COMPONENT}), where {@code entityType} is one of
 * the {@link TrashEntityType} constants. Maps are sparse: only observed combinations appear.
 * </p>
 */
public interface TrashPendingMetricsProvider
{
  /**
   * Returns the current count of pending (trashed, not yet purged) entities, grouped by
   * {@code format::entityType}.
   */
  Map<String, Long> pendingCountByFormatAndEntityType();
}
