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

import React, { useEffect, useState } from 'react';
import {
  Badge,
  Box,
  Button,
  Dialog,
  Flex,
  Heading,
  Separator,
  Spinner,
  Table,
  Text,
  Tooltip,
} from '@radix-ui/themes';
import { AlertTriangle, CheckCircle, HelpCircle, XCircle } from 'lucide-react';

import type { MaliciousFinding } from './types';

export interface IdentifyTaskModalProps {
  open: boolean;
  repoName: string;
  signatureCount: number;
  onCreateAndRunAuditTask: (repoName: string) => Promise<MaliciousFinding[]>;
  onClose: () => void;
  onComplete: () => void;
}

type ScanPhase = 'idle' | 'running' | 'success' | 'failed';

function formatStartedAt(ts: number): string {
  return new Date(ts).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    second: '2-digit',
    hour12: true,
  });
}

function threatLevelBadge(threatLevel: number | null): React.ReactElement {
  if (threatLevel === null) {
    return <Badge color="gray">Unknown</Badge>;
  }
  if (threatLevel >= 9) {
    return <Badge color="red">Critical</Badge>;
  }
  if (threatLevel >= 7) {
    return <Badge color="orange">Severe</Badge>;
  }
  if (threatLevel >= 4) {
    return <Badge color="yellow">Moderate</Badge>;
  }
  return <Badge color="gray">Low</Badge>;
}

export function IdentifyTaskModal({
  open,
  repoName,
  signatureCount,
  onCreateAndRunAuditTask,
  onClose,
  onComplete,
}: IdentifyTaskModalProps): React.ReactElement {
  const [scanPhase, setScanPhase] = useState<ScanPhase>('idle');
  const [startedAt, setStartedAt] = useState<number | null>(null);
  const [findings, setFindings] = useState<MaliciousFinding[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setScanPhase('idle');
      setStartedAt(null);
      setFindings([]);
      setError(null);
      return;
    }

    let cancelled = false;
    setScanPhase('running');
    setStartedAt(Date.now());
    setFindings([]);
    setError(null);

    onCreateAndRunAuditTask(repoName)
      .then((result) => {
        if (!cancelled) {
          setFindings(result);
          setScanPhase('success');
          onComplete();
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : String(err));
          setScanPhase('failed');
        }
      });

    return () => {
      cancelled = true;
    };
  }, [open, repoName, onCreateAndRunAuditTask, onComplete]);

  const handleOpenChange = (isOpen: boolean) => {
    if (isOpen) {
      return;
    }
    if (scanPhase === 'running') {
      return;
    }
    onClose();
  };

  const handleClose = () => {
    if (scanPhase === 'running') {
      return;
    }
    onClose();
  };

  const startedLabel = startedAt != null ? formatStartedAt(startedAt) : '';
  const showRunning = scanPhase === 'running' || (open && scanPhase === 'idle');

  return (
    <Dialog.Root open={open} onOpenChange={handleOpenChange}>
      <Dialog.Content aria-describedby={undefined} style={{ maxWidth: 720 }}>
        {showRunning && (
          <>
            <Dialog.Title>Run One-Time Analysis</Dialog.Title>
            <Flex direction="column" align="center" justify="center" gap="4" py="6" px="2">
              <Spinner size="3" />
              <Text size="2" align="center">
                Scanning <Text weight="bold">{repoName}</Text>…
              </Text>
              <Text size="2" color="gray" align="center">
                {startedLabel ? `Started at ${startedLabel}` : 'Starting…'}
              </Text>
              <Text size="1" color="gray" align="center">
                Large repositories may take several minutes. Check Administration &gt; Tasks for live progress and logs.
              </Text>
              {signatureCount > 0 && (
                <Text size="1" color="gray" align="center">
                  Evaluating against {signatureCount} known signatures
                </Text>
              )}
            </Flex>
          </>
        )}

        {scanPhase === 'success' && (
          <>
            <Dialog.Title>Analysis Results</Dialog.Title>
            {findings.length === 0 && signatureCount > 0 ? (
              <Flex align="center" gap="2" mt="2">
                <AlertTriangle size={22} color="var(--orange-9)" aria-hidden />
                <Heading size="4" color="orange" weight="medium">
                  Scan complete — {repoName}
                </Heading>
              </Flex>
            ) : (
              <Flex align="center" gap="2" mt="2">
                <CheckCircle size={22} color="var(--green-9)" aria-hidden />
                <Heading size="4" weight="medium">
                  Scan complete — {repoName}
                </Heading>
              </Flex>
            )}

            <Flex direction="column" gap="4" mt="3">
              <Text size="2">
                {findings.length > 0 ? (
                  <>
                    <Text weight="bold">{findings.length}</Text> malicious{' '}
                    {findings.length === 1 ? 'package' : 'packages'} identified
                  </>
                ) : signatureCount > 0 ? (
                  'No findings were recorded despite RHC detecting signatures. This may indicate the scan did not fully execute — verify IQ Server connectivity and check Administration > Tasks for errors.'
                ) : (
                  'No malicious packages found'
                )}
              </Text>

              {findings.length > 0 && (
                <Box style={{ maxHeight: 320, overflow: 'auto' }}>
                  <Table.Root variant="surface" size="1">
                    <Table.Header>
                      <Table.Row>
                        <Table.ColumnHeaderCell>Component</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Version</Table.ColumnHeaderCell>
                        <Table.ColumnHeaderCell>Threat level</Table.ColumnHeaderCell>
                      </Table.Row>
                    </Table.Header>
                    <Table.Body>
                      {findings.map((f) => (
                        <Table.Row key={f.id}>
                          <Table.Cell>
                            <Text size="2">{f.componentName ?? '—'}</Text>
                          </Table.Cell>
                          <Table.Cell>
                            <Text size="2" color="gray">{f.componentVersion ?? '—'}</Text>
                          </Table.Cell>
                          <Table.Cell>{threatLevelBadge(f.threatLevel)}</Table.Cell>
                        </Table.Row>
                      ))}
                    </Table.Body>
                  </Table.Root>
                </Box>
              )}

              <Separator size="4" />
              <Text size="1" color="gray">
                These findings are now available in the Remediate tab.
              </Text>
              <Flex justify="end">
                <Button onClick={handleClose}>Close</Button>
              </Flex>
            </Flex>
          </>
        )}

        {scanPhase === 'failed' && (
          <>
            <Dialog.Title>Identification</Dialog.Title>
            <Flex align="center" gap="2" mt="2">
              <XCircle size={22} color="var(--red-9)" aria-hidden />
              <Heading size="4" color="red" weight="medium">
                Task failed
              </Heading>
            </Flex>
            <Flex direction="column" gap="4" mt="3">
              <Flex align="center" gap="2" wrap="wrap">
                <Text size="2" color="gray">
                  {startedLabel ? `Task ran at ${startedLabel} and failed` : 'The task failed'}
                </Text>
                {error && (
                  <Tooltip content={error}>
                    <span style={{ display: 'inline-flex', cursor: 'help' }} aria-label="Error details">
                      <HelpCircle size={18} color="var(--gray-9)" />
                    </span>
                  </Tooltip>
                )}
              </Flex>
              <Flex justify="end">
                <Button onClick={handleClose}>Close</Button>
              </Flex>
            </Flex>
          </>
        )}
      </Dialog.Content>
    </Dialog.Root>
  );
}
