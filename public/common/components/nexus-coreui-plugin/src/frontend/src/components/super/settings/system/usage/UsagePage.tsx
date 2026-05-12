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
import { Box, Heading, Text } from '@radix-ui/themes';
import { BarChart3 } from 'lucide-react';
import { HistoricalUsage, historicalUsageColumns } from '@sonatype/nexus-ui-plugin';

const COLUMNS = [
  historicalUsageColumns.metricDateMonth,
  historicalUsageColumns.peakComponents,
  historicalUsageColumns.percentageChangeComponent,
  historicalUsageColumns.totalRequests,
  historicalUsageColumns.percentageChangeRequests,
];

export default function UsagePage() {
  return (
    <Box p="5">
      <Box mb="5">
        <Heading size="5" mb="1">
          <BarChart3 size={20} style={{ verticalAlign: 'text-bottom', marginRight: 8 }} />
          Usage
        </Heading>
        <Text size="2" color="gray">
          Historical usage metrics for this Nexus Repository instance.
        </Text>
      </Box>
      <HistoricalUsage columns={COLUMNS} />
    </Box>
  );
}
