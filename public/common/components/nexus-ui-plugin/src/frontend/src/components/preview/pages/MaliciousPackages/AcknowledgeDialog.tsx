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

import React, { useState, useCallback } from 'react';
import { Dialog, Flex, Text, Button, Select, Box } from '@radix-ui/themes';

import { MaliciousFinding } from './types';

export type AcknowledgeDuration = '1d' | '30d' | '90d' | '1y' | 'forever';

const DURATION_OPTIONS: Array<{ value: AcknowledgeDuration; label: string; description: string }> = [
  { value: '1d', label: '1 day', description: "I'm dealing with this tomorrow" },
  { value: '30d', label: '30 days', description: 'Planned maintenance window' },
  { value: '90d', label: '90 days', description: 'Waiting on vendor fix or internal review' },
  { value: '1y', label: '1 year', description: 'Accepted risk for this cycle' },
  { value: 'forever', label: 'Forever', description: 'False positive or permanently accepted' },
];

interface AcknowledgeDialogProps {
  open: boolean;
  finding: MaliciousFinding | null;
  onConfirm: (reason: string, duration: AcknowledgeDuration) => void;
  onCancel: () => void;
}

export function AcknowledgeDialog({
  open,
  finding,
  onConfirm,
  onCancel,
}: AcknowledgeDialogProps): React.ReactElement {
  const [reason, setReason] = useState('');
  const [duration, setDuration] = useState<AcknowledgeDuration>('30d');

  const handleConfirm = useCallback(() => {
    onConfirm(reason, duration);
    setReason('');
    setDuration('30d');
  }, [reason, duration, onConfirm]);

  const handleCancel = useCallback(() => {
    onCancel();
    setReason('');
    setDuration('30d');
  }, [onCancel]);

  const displayName = finding?.componentName ?? finding?.path ?? 'Unknown';
  const selectedOption = DURATION_OPTIONS.find((o) => o.value === duration);

  return (
    <Dialog.Root open={open} onOpenChange={(isOpen) => !isOpen && handleCancel()}>
      <Dialog.Content aria-describedby={undefined} style={{ maxWidth: 480 }}>
        <Dialog.Title>
          Acknowledge &amp; Accept — {displayName}
        </Dialog.Title>

        <Flex direction="column" gap="3" mt="3">
          <Text as="label" size="2" weight="medium" htmlFor="acknowledge-reason">
            Reason (required)
          </Text>
          <textarea
            id="acknowledge-reason"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Explain why this finding is being accepted..."
            rows={4}
            style={{
              width: '100%',
              padding: '8px 12px',
              borderRadius: 'var(--radius-2)',
              border: '1px solid var(--gray-6)',
              fontFamily: 'inherit',
              fontSize: 'var(--font-size-2)',
              resize: 'vertical',
            }}
          />

          <Box>
            <Text as="label" size="2" weight="medium" htmlFor="acknowledge-duration">
              Accept risk for
            </Text>
            <Select.Root value={duration} onValueChange={(v: string) => setDuration(v as AcknowledgeDuration)}>
              <Select.Trigger id="acknowledge-duration" style={{ width: '100%', marginTop: 4 }} />
              <Select.Content>
                {DURATION_OPTIONS.map((opt) => (
                  <Select.Item key={opt.value} value={opt.value}>
                    {opt.label}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
            {selectedOption && (
              <Text size="1" color="gray" mt="1">{selectedOption.description}</Text>
            )}
          </Box>
        </Flex>

        <Flex gap="3" mt="4" justify="end">
          <Button variant="outline" onClick={handleCancel}>
            Cancel
          </Button>
          <Button
            variant="solid"
            color="red"
            disabled={reason.trim().length === 0}
            onClick={handleConfirm}
          >
            Acknowledge &amp; Accept
          </Button>
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}
