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
// monthlyMetrics is only needed for the cloud path (CloudUsageCenterPanel).
// Self-hosted path uses UsageCenter which reads its own state internally (NEXUS-53863).
import { useMonthlyMetrics } from './dashboard';

export function useUsageMetricsTabData() {
  const isCloud: boolean = ExtJS.state().getValue('isCloud', false);
  const monthlyMetrics = useMonthlyMetrics();

  return {isCloud, monthlyMetrics};
}
