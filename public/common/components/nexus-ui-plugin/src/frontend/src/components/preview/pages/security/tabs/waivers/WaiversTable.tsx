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
import { Badge, Checkbox, DropdownMenu, IconButton, Table, Text, Tooltip } from '@radix-ui/themes';
import { MoreHorizontal } from 'lucide-react';

import { ThreatLevelBadge } from '../../../../shared/ThreatLevelBadge';
import type { Waiver } from './waiverTypes';
import {
  NO_VALUE, WAIVER_BAND_COLOR, componentDisplay, expirationBand,
  scopeLabel, toDisplayDate, toIsoDate, upgradeLabel,
} from './waiverFormat';

export interface WaiversTableSelection {
  isSelected: (id: string) => boolean;
  toggleRow: (id: string) => void;
  toggleHeader: () => void;
  headerState: 'none' | 'some' | 'all';
}

export interface WaiversTableProps {
  waivers: Waiver[];
  /** Injected for deterministic banding in tests. */
  now?: Date;
  /** Omit to render without a checkbox column. */
  selection?: WaiversTableSelection;
  onRenew?: (waiver: Waiver) => void;
  onRevoke?: (waiver: Waiver) => void;
  /** False when the caller lacks the update privilege — see the spec's open item. */
  showActions?: boolean;
}

export function WaiversTable({
  waivers,
  now = new Date(),
  selection,
  onRenew,
  onRevoke,
  showActions = true,
}: WaiversTableProps): JSX.Element {
  const actionable = showActions && !!(onRenew || onRevoke);

  return (
    <Table.Root variant="surface" data-testid="waivers-table">
      <Table.Header>
        <Table.Row>
          {selection && (
            <Table.ColumnHeaderCell style={{ width: '44px' }}>
              <Checkbox
                className="waivers-table-checkbox"
                checked={
                  selection.headerState === 'all'
                    ? true
                    : selection.headerState === 'some'
                      ? 'indeterminate'
                      : false
                }
                onCheckedChange={selection.toggleHeader}
                aria-label="Select all waivers"
                data-testid="waivers-select-all"
              />
            </Table.ColumnHeaderCell>
          )}
          <Table.ColumnHeaderCell>Threat</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Date Created</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Expiration</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Policy</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Scope</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Components</Table.ColumnHeaderCell>
          <Table.ColumnHeaderCell>Upgrade</Table.ColumnHeaderCell>
          {/* Empty Header on the action column, per the table standard. */}
          <Table.ColumnHeaderCell align="right" />
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {waivers.map((waiver) => {
          const band = expirationBand(waiver.expiryTime, now);
          return (
            <Table.Row key={waiver.policyWaiverId} data-testid={`waiver-row-${waiver.policyWaiverId}`}>
              {selection && (
                <Table.Cell>
                  <Checkbox
                    className="waivers-table-checkbox"
                    checked={selection.isSelected(waiver.policyWaiverId)}
                    onCheckedChange={() => selection.toggleRow(waiver.policyWaiverId)}
                    aria-label={`Select waiver for ${componentDisplay(waiver)}`}
                  />
                </Table.Cell>
              )}
              <Table.Cell>
                <Tooltip content={`Threat level ${waiver.threatLevel} of 10`}>
                  <span>
                    <ThreatLevelBadge threatLevel={waiver.threatLevel} />
                  </span>
                </Tooltip>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{toDisplayDate(waiver.createTime)}</Text>
              </Table.Cell>
              <Table.Cell>
                <Badge color={WAIVER_BAND_COLOR[band]} variant="soft" size="1">
                  {waiver.expiryTime === null ? 'Never' : toIsoDate(waiver.expiryTime)}
                </Badge>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{waiver.policyName || NO_VALUE}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{scopeLabel(waiver.scopeOwnerType, waiver.scopeOwnerName)}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{componentDisplay(waiver)}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2">{upgradeLabel(waiver.componentUpgradeAvailable)}</Text>
              </Table.Cell>
              <Table.Cell align="right">
                {actionable ? (
                  <DropdownMenu.Root>
                    <DropdownMenu.Trigger>
                      <IconButton
                        variant="ghost"
                        size="1"
                        color="gray"
                        aria-label={`Actions for ${componentDisplay(waiver)}`}
                        data-testid={`waiver-actions-${waiver.policyWaiverId}`}
                      >
                        <MoreHorizontal size={16} aria-hidden />
                      </IconButton>
                    </DropdownMenu.Trigger>
                    <DropdownMenu.Content>
                      {onRenew && (
                        <DropdownMenu.Item onSelect={() => onRenew(waiver)}>Renew</DropdownMenu.Item>
                      )}
                      {onRevoke && (
                        <DropdownMenu.Item color="red" onSelect={() => onRevoke(waiver)}>
                          Revoke
                        </DropdownMenu.Item>
                      )}
                    </DropdownMenu.Content>
                  </DropdownMenu.Root>
                ) : (
                  <Text size="2" color="gray">{NO_VALUE}</Text>
                )}
              </Table.Cell>
            </Table.Row>
          );
        })}
      </Table.Body>
    </Table.Root>
  );
}
