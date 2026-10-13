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
 * Stable identifier for the cloud distribution, matching {@code NexusDistributionType.CLOUD.getId()}
 * on the backend ({@code EditionStateContributor} publishes it into Rapture state).
 */
export const CLOUD_DISTRIBUTION = 'cloud';

/**
 * Stable identifier for the self-hosted distribution, matching
 * {@code NexusDistributionType.SELF_HOSTED.getId()} on the backend
 * ({@code StatusStateContributor} publishes it into Rapture state).
 */
export const SELF_HOSTED_DISTRIBUTION = 'selfhosted';

/**
 * Rapture state key for the deployment (distribution) the running instance was packaged for.
 * Backed by {@code EditionStateContributor} on cloud builds and {@code StatusStateContributor}
 * on self-hosted builds; unlike the legacy {@code isCloud} flag, this value is present on both
 * deployments so self-hosted no longer has to infer itself from a missing value.
 */
export const DISTRIBUTION_STATE_KEY = 'distribution';

/**
 * Returns true when the running instance is a cloud deployment.
 *
 * Reads the explicit {@code distribution} value published by the backend so that both cloud and
 * self-hosted resolve to a real string (NEXUS-54785) rather than relying on the legacy
 * cloud-only {@code isCloud} boolean. When the state subsystem has not yet published a value
 * (e.g. before the REST bootstrap or ExtJS state store has resolved), the read defaults to
 * {@code SELF_HOSTED_DISTRIBUTION}, so callers see {@code false} during that brief window.
 * Callers that need a distinct loading state must gate on another signal in addition to this hook.
 *
 * Session-scoped: the value is fixed for the page lifecycle.
 */
export function useIsCloud(): boolean {
  const distribution = ExtJS.useState?.(() =>
    ExtJS.state()?.getValue?.(DISTRIBUTION_STATE_KEY, SELF_HOSTED_DISTRIBUTION) ?? SELF_HOSTED_DISTRIBUTION,
  ) ?? SELF_HOSTED_DISTRIBUTION;
  return distribution === CLOUD_DISTRIBUTION;
}
