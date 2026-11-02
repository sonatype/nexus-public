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

import { ExtJS } from '../../../../interface/ExtJS';

/**
 * Whether the backend reports repository size and asset metrics as available.
 *
 * The backend resolves four conditions in RepositoryMetricsFeatureCheck.isRepositorySizeSupported()
 * — the feature flag AND a repository-metrics licence AND Postgres AND a finished CopyBlobSizeTask —
 * and publishes the single resolved answer as the `nexus.repository.size` state key
 * (RepositoryMetricsStateContributor). Read that rather than re-deriving any of the four conditions
 * client-side, so this cannot drift when a fifth is added.
 *
 * Compared against {@code true} explicitly rather than coerced: the state contributor lives in a
 * Pro-only plugin, so in Community Edition the key is absent entirely and the read yields undefined.
 * The REST bootstrap path also copies state values through without type coercion, so a non-boolean
 * must not read as supported.
 */
export function useIsRepositorySizeSupported(): boolean {
  return ExtJS.useState?.(() => ExtJS.state()?.getValue?.('nexus.repository.size', false)) === true;
}
