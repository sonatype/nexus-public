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

import React, { useMemo } from 'react';
import { Dialog, Flex, Spinner, Text } from '@radix-ui/themes';
import { useChangeHistory } from './useChangeHistory';

const VALUE_LABEL: Record<string, string> = {
  none: 'No Protection',
  audit: 'Audit',
  quarantine: 'Quarantine',
  enabled: 'Enabled',
  disabled: 'Disabled',
};

function labelFor(v: string | undefined) {
  if (!v) return '—';
  return VALUE_LABEL[v] ?? v;
}

function formatWhen(iso: string | null | undefined, initiator: string | null | undefined) {
  if (!iso) return initiator ? `${initiator}` : 'Unknown time';
  try {
    const d = new Date(iso);
    const fmt = new Intl.DateTimeFormat(undefined, {
      dateStyle: 'medium',
      timeStyle: 'short',
      timeZoneName: 'short',
    });
    return `${fmt.format(d)}${initiator ? ` · ${initiator}` : ''}`;
  } catch {
    return iso;
  }
}

export interface ProtectChangeHistoryModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  domain: string;
  context: string;
  /** Health Check modal: show last analyzed from RHC API */
  lastAnalyzedMs?: number | null;
}

export default function ProtectChangeHistoryModal({
  open,
  onOpenChange,
  title,
  domain,
  context,
  lastAnalyzedMs,
}: ProtectChangeHistoryModalProps) {
  const { events, totalCount, loading, error } = useChangeHistory(domain, context, open);

  const displayEvents = useMemo(() => events.slice(0, 3), [events]);

  const lines = useMemo(() => {
    return displayEvents.map((ev) => {
      const attrs = ev.attributes ?? {};
      const from = labelFor(String(attrs.from ?? ''));
      const to = labelFor(String(attrs.to ?? ''));
      return `Set to ${to} from ${from}`;
    });
  }, [displayEvents]);

  const lastAnalyzedLine = useMemo(() => {
    if (lastAnalyzedMs === undefined) return null;
    if (lastAnalyzedMs == null) return 'Last analyzed: Never';
    try {
      const fmt = new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
        timeZoneName: 'short',
      });
      return `Last analyzed: ${fmt.format(new Date(lastAnalyzedMs))}`;
    } catch {
      return 'Last analyzed: Never';
    }
  }, [lastAnalyzedMs]);

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Content style={{ maxWidth: 440 }} aria-describedby={undefined}>
        <Dialog.Title>{title}</Dialog.Title>
        <Text size="1" color="gray">
          Up to 3 most recent changes (newest first).
        </Text>
        <Flex direction="column" gap="3" mt="2">
          {lastAnalyzedLine && (
            <Text size="2" color="gray">
              {lastAnalyzedLine}
            </Text>
          )}
          {loading ? (
            <Flex justify="center" p="4">
              <Spinner size="2" />
            </Flex>
          ) : error ? (
            <Text size="2" color="red">
              Unavailable
            </Text>
          ) : lines.length === 0 ? (
            <Text size="2" color="gray">
              Unavailable
            </Text>
          ) : (
            <ul style={{ margin: 0, paddingLeft: '1.1rem' }}>
              {lines.map((line, i) => (
                <li key={i}>
                  <Text size="2">{line}</Text>
                  <Text size="1" color="gray" as="div">
                    {formatWhen(displayEvents[i]?.timestamp ?? null, displayEvents[i]?.initiator ?? null)}
                  </Text>
                </li>
              ))}
            </ul>
          )}
          {totalCount > 3 && (
            <Text size="1" color="gray">
              View full history in the Audit module.
            </Text>
          )}
        </Flex>
      </Dialog.Content>
    </Dialog.Root>
  );
}
