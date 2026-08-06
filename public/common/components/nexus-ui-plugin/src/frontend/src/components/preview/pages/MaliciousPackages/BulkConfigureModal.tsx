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

import React, { useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  Flex,
  Progress,
  RadioGroup,
  Text,
} from '@radix-ui/themes';
import { CheckCircle } from 'lucide-react';

import { MalwareRemediatorMode } from '../../shared/security/malwareRemediatorTask';
import { BulkProgress } from './useMaliciousPackagesData';

interface BulkConfigureModalProps {
  open: boolean;
  repoCount: number;
  bulkProgress: BulkProgress;
  onConfirm: (mode: MalwareRemediatorMode) => void;
  onClose: () => void;
}

export function BulkConfigureModal({
  open,
  repoCount,
  bulkProgress,
  onConfirm,
  onClose,
}: BulkConfigureModalProps): React.ReactElement {
  const [mode, setMode] = useState<'audit' | 'delete'>('audit');
  const isRunning = bulkProgress.active;
  const isDone = !bulkProgress.active && bulkProgress.completed > 0 && bulkProgress.completed === bulkProgress.total;

  return (
    <Dialog.Root open={open} onOpenChange={(v) => { if (!v && !isRunning) onClose(); }}>
      <Dialog.Content maxWidth="520px" data-testid="bulk-configure-modal">
        <Dialog.Title>Configure Automated Malware Cleanup</Dialog.Title>

        {isDone ? (
          <Flex direction="column" gap="3" py="3" align="center">
            <CheckCircle size={40} color="var(--green-9)" />
            <Text size="3" weight="medium" align="center">
              {bulkProgress.total} tasks created successfully
            </Text>
            <Text size="2" color="gray" align="center">
              First scans begin tonight. Tasks are staggered to avoid overloading your system.
            </Text>
            <Button size="2" variant="solid" onClick={onClose} mt="2">
              Done
            </Button>
          </Flex>
        ) : isRunning ? (
          <Flex direction="column" gap="3" py="3">
            <Text size="2" align="center">
              Creating tasks... {bulkProgress.completed} of {bulkProgress.total}
            </Text>
            <Progress
              value={Math.round((bulkProgress.completed / bulkProgress.total) * 100)}
              size="3"
            />
            <Text size="1" color="gray" align="center">
              Do not close this dialog while tasks are being created.
            </Text>
          </Flex>
        ) : (
          <Flex direction="column" gap="4">
            <Dialog.Description size="2" color="gray">
              You are about to create daily malware removal tasks for{' '}
              <Text weight="bold">{repoCount} repositories</Text> that don't currently have one.
            </Dialog.Description>

            <Box>
              <Text size="2" weight="medium">What happens:</Text>
              <Flex direction="column" gap="1" mt="1" ml="3">
                <Text size="2" color="gray">&bull; A daily scan task is created for each repository</Text>
                <Text size="2" color="gray">&bull; Tasks are staggered throughout the night (starting at 2:00 AM, spaced 10 min apart)</Text>
                <Text size="2" color="gray">&bull; Each task scans for known malicious packages</Text>
              </Flex>
            </Box>

            <Box>
              <Text size="2" weight="medium" mb="1">What should happen when malware is found?</Text>
              <RadioGroup.Root value={mode} onValueChange={(v: string) => setMode(v as 'audit' | 'delete')}>
                <Flex direction="column" gap="2" mt="2">
                  <Text as="label" size="2">
                    <Flex align="center" gap="2">
                      <RadioGroup.Item value="audit" />
                      <Box>
                        <Text weight="medium">Scan and Review</Text>
                        <Text size="1" color="gray"> — Identifies malware. You review and decide what to remove.</Text>
                      </Box>
                    </Flex>
                  </Text>
                  <Text as="label" size="2">
                    <Flex align="center" gap="2">
                      <RadioGroup.Item value="delete" />
                      <Box>
                        <Text weight="medium">Scan and Auto-Delete</Text>
                        <Text size="1" color="gray"> — Identifies malware and removes it automatically.</Text>
                      </Box>
                    </Flex>
                  </Text>
                </Flex>
              </RadioGroup.Root>
            </Box>

            <Flex gap="3" justify="end" mt="2">
              <Dialog.Close>
                <Button variant="soft" color="gray">Cancel</Button>
              </Dialog.Close>
              <Button variant="solid" onClick={() => onConfirm(mode)}>
                Create {repoCount} Tasks
              </Button>
            </Flex>
          </Flex>
        )}
      </Dialog.Content>
    </Dialog.Root>
  );
}
