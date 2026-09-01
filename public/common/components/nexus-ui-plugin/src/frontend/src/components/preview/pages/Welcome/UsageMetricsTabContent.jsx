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
import React from 'react';
import {Box, Flex} from '@radix-ui/themes';
// UsageCenter replaces InstanceTotalsPanel for self-hosted (NEXUS-53863): matches classic layout,
// no Storage/Egress charts, handles both Pro and CE editions internally
import UsageCenter from './UsageCenter';
import {CloudUsageCenterPanel} from './CloudUsageCenterPanel';
import {useUsageMetricsTabData} from './useUsageMetricsTabData';
import CELimitsAlerts from './CELimitsAlerts';
import MalwareBanner from '../../shared/security/MalwareBanner';

export default function UsageMetricsTabContent() {
  const {isCloud, monthlyMetrics} = useUsageMetricsTabData();

  return (
    <Box pt="4">
      <Flex direction="column" gap="4">
        <CELimitsAlerts />
        <MalwareBanner />
      </Flex>
      <Box mt="4">
        {isCloud ? (
          <CloudUsageCenterPanel monthlyMetrics={monthlyMetrics} />
        ) : (
          <UsageCenter />
        )}
      </Box>
    </Box>
  );
}
