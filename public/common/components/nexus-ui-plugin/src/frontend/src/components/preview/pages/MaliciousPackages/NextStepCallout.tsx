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
import { Callout, Button, Flex, Text } from '@radix-ui/themes';
import { AlertTriangle } from 'lucide-react';

import type { NistPhase } from './types';

interface NextStepCalloutProps {
  phase: NistPhase | null;
  pendingCount: number;
  unprotectedRepoCount: number;
  onAction: () => void;
  onSkip: () => void;
}

interface PhaseConfig {
  message: string;
  actionLabel: string;
  skipLabel?: string;
}

function getPhaseConfig(
  phase: NistPhase,
  pendingCount: number,
  unprotectedRepoCount: number
): PhaseConfig {
  switch (phase) {
    case 'ALERT':
      return {
        message: 'Run Malicious Packages tasks to identify which components are malicious.',
        actionLabel: 'Run Scans',
        skipLabel: 'Skip',
      };
    case 'TRIAGE':
      return {
        message: `${pendingCount} repos still have unscanned malware. Run scans to identify all components.`,
        actionLabel: 'Run Scans',
        skipLabel: 'Skip',
      };
    case 'CONTAINMENT':
      return {
        message: `${unprotectedRepoCount} repos lack quarantine protection. Enable quarantine to block new malicious packages.`,
        actionLabel: 'Configure in Protect',
        skipLabel: 'Skip',
      };
    case 'ERADICATION':
      return {
        message: 'Delete or acknowledge each finding below.',
        actionLabel: 'Delete All Pending',
      };
    case 'RECOVERY':
      return {
        message: 'All findings resolved. Re-scan affected repos to verify clean.',
        actionLabel: 'Re-Scan',
        skipLabel: 'Skip Verification',
      };
    case 'POST_INCIDENT':
      return {
        message: 'All clear. Export your report for your security team.',
        actionLabel: 'Export CSV',
      };
  }
}

export function NextStepCallout({
  phase,
  pendingCount,
  unprotectedRepoCount,
  onAction,
  onSkip,
}: NextStepCalloutProps): React.ReactElement | null {
  if (phase === null) return null;

  const config = getPhaseConfig(phase, pendingCount, unprotectedRepoCount);

  return (
    <Callout.Root color="blue">
      <Callout.Icon>
        <AlertTriangle size={16} />
      </Callout.Icon>
      <Flex align="center" justify="between" width="100%" gap="3" wrap="wrap">
        <Text size="2">{config.message}</Text>
        <Flex gap="2" shrink="0">
          <Button size="1" variant="solid" onClick={onAction}>
            {config.actionLabel}
          </Button>
          {config.skipLabel && (
            <Button size="1" variant="outline" onClick={onSkip}>
              {config.skipLabel}
            </Button>
          )}
        </Flex>
      </Flex>
    </Callout.Root>
  );
}
