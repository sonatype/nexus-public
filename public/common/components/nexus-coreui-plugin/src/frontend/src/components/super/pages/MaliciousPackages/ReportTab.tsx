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
import { Box, Callout, Card, Flex, Heading, Text } from '@radix-ui/themes';
import { FileBarChart, Info } from 'lucide-react';

export function ReportTab(): React.ReactElement {
  return (
    <Card size="2">
      <Flex direction="column" gap="4">
        <Flex align="center" gap="2">
          <FileBarChart size={20} />
          <Heading size="4">Incident Reporting</Heading>
        </Flex>

        <Callout.Root color="blue">
          <Callout.Icon>
            <Info size={16} />
          </Callout.Icon>
          <Callout.Text>
            This module is under development and will be available in a future release.
          </Callout.Text>
        </Callout.Root>

        <Text size="2">
          The Report tab will provide comprehensive incident reporting capabilities for malicious
          package events, designed for management visibility and compliance documentation.
        </Text>

        <Box>
          <Heading size="3" mb="2">Planned Capabilities</Heading>
          <Flex direction="column" gap="2">
            <Text size="2">
              <Text weight="bold">Incident Summary</Text> — Timeline of malicious package detection,
              triage, and remediation with resolution timestamps and duration metrics.
            </Text>
            <Text size="2">
              <Text weight="bold">Blast Radius Analysis</Text> — Which repositories were affected,
              which components were compromised, and which users downloaded malicious packages.
            </Text>
            <Text size="2">
              <Text weight="bold">Hardening Report</Text> — Protection status before and after the
              incident: which repos gained Firewall quarantine, RHC monitoring, and automated
              malicious package cleanup tasks.
            </Text>
            <Text size="2">
              <Text weight="bold">Lessons Learned</Text> — Corrective actions taken, policy changes
              made, and recommendations for preventing future incidents.
            </Text>
            <Text size="2">
              <Text weight="bold">Audit Trail</Text> — Leverages audit events specific to malicious
              package discovery and remediation for detailed compliance documentation.
            </Text>
          </Flex>
        </Box>
      </Flex>
    </Card>
  );
}
