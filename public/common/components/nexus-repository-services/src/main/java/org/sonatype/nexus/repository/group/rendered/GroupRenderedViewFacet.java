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
package org.sonatype.nexus.repository.group.rendered;

import java.time.Duration;
import java.util.Optional;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;

/**
 * Cache-first short-circuit contract for group rendered metadata views.
 * <p>
 * Format adapters implement this interface to provide optimized access to cached
 * rendered metadata without member dispatch when the cache is fresh.
 * <p>
 * Consulted by a format handler BEFORE member dispatch; if a fresh view is
 * available, it is returned directly, avoiding the expensive merge operation.
 *
 * Marked {@link Facet.Exposed} so {@code repository.optionalFacet(GroupRenderedViewFacet.class)}
 * resolves an attached adapter. Without it {@code FacetLookup} indexes nothing under this type and
 * {@code GroupRenderedViewCleanupTask} silently finds no repositories at all, for any format.
 *
 * @see RenderedViewFingerprint
 * @see GroupRenderedViewSupport
 */
@Facet.Exposed
public interface GroupRenderedViewFacet
    extends Facet
{
  /**
   * Returns the materialised, still-fresh rendered view for this request as a flat byte
   * copy, or empty if there is no fresh view (miss/stale/disabled) and the caller must
   * fall through to the legacy dispatch-and-merge path.
   *
   * @param context the request context
   * @return the cached content if available and fresh, or empty otherwise
   */
  Optional<Content> getRenderedView(Context context);

  /**
   * Prune rendered-view assets that have not been downloaded within the given duration.
   * <p>
   * Bounded storage maintenance: assets older than the horizon are deleted; a later
   * request rebuilds them as a miss. This is not a correctness event.
   *
   * @param unusedFor assets not downloaded within this duration are eligible for deletion
   * @return the number of assets pruned
   */
  int pruneRenderedViews(Duration unusedFor);
}
