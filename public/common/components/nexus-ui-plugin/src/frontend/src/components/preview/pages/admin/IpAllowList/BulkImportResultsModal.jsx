/*
 * Copyright (c) 2008-present Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import React from 'react';
import { Dialog, Button, Flex, Box, Text, Table, Badge, IconButton } from '@radix-ui/themes';
import { Check, AlertTriangle, XCircle, Database, X } from 'lucide-react';

export function BulkImportResultsModal({ isOpen, onClose, results }) {
  const { totalRows, addedCount, skippedCount, rejectedCount, skipped, rejected } = results;

  const hasIssues = skippedCount > 0 || rejectedCount > 0;

  const issueRows = [
    ...((skipped || []).map(item => ({ ...item, status: 'skipped', ip: item.ip, reason: item.reason, row: item.row }))),
    ...((rejected || []).map(item => ({ ...item, status: 'rejected', ip: item.ip || 'Invalid row', reason: item.reason, row: item.row }))),
  ].sort((a, b) => (a.row || 0) - (b.row || 0));

  const stats = [
    {
      label: 'Total records',
      value: totalRows,
      icon: <Database size={14} />,
      iconColor: 'var(--gray-9)',
      labelColor: 'var(--gray-11)',
      countColor: 'var(--gray-12)',
    },
    {
      label: 'Will import',
      value: addedCount,
      icon: <Check size={14} strokeWidth={3} />,
      iconColor: 'var(--green-9)',
      labelColor: 'var(--green-11)',
      countColor: 'var(--gray-12)',
    },
    {
      label: 'Will be skipped',
      value: skippedCount,
      icon: <AlertTriangle size={14} />,
      iconColor: 'var(--amber-9)',
      labelColor: 'var(--amber-11)',
      countColor: 'var(--gray-12)',
    },
    {
      label: 'Errors',
      value: rejectedCount,
      icon: <XCircle size={14} />,
      iconColor: 'var(--red-9)',
      labelColor: 'var(--red-11)',
      countColor: 'var(--gray-12)',
    },
  ];

  return (
    <Dialog.Root open={isOpen} onOpenChange={onClose}>
      <Dialog.Content
        maxWidth="780px"
        className="nxrm-ip-allowlist__modal-content nxrm-ip-allowlist__modal-content--wide"
        data-testid="import-results-modal"
      >
        <Flex align="start" justify="between" mb="4">
          <Dialog.Title size="5" weight="bold">Import Results</Dialog.Title>
          <Dialog.Close>
            <IconButton variant="ghost" size="1">
              <X size={18} />
            </IconButton>
          </Dialog.Close>
        </Flex>

        <Dialog.Description style={{ display: 'none' }}>
          Summary of the bulk IP address import operation
        </Dialog.Description>

        <Box style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>

          {/* Stat tiles */}
          <Flex
            style={{
              border: '1px solid var(--gray-4)',
              borderRadius: '8px',
              overflow: 'hidden',
            }}
          >
            {stats.map((stat, i) => (
              <Box
                key={stat.label}
                style={{
                  flex: 1,
                  padding: '16px 20px',
                  borderRight: i < stats.length - 1 ? '1px solid var(--gray-4)' : 'none',
                  backgroundColor: 'var(--color-panel-solid, white)',
                }}
              >
                <Flex align="center" gap="2" style={{ marginBottom: '8px' }}>
                  <span style={{
                    color: stat.iconColor,
                    display: 'flex',
                    alignItems: 'center',
                  }}>
                    {stat.icon}
                  </span>
                  <Text size="2" style={{ color: stat.labelColor, fontWeight: 500 }}>
                    {stat.label}
                  </Text>
                </Flex>
                <Text size="7" weight="bold" style={{ color: stat.countColor, lineHeight: 1 }}>
                  {stat.value}
                </Text>
              </Box>
            ))}
          </Flex>

          {/* Table or success message */}
          {hasIssues ? (
            <Box>
              <Text size="2" color="gray" style={{ display: 'block', marginBottom: '10px' }}>
                The following rows were not imported. Review the reasons below.
              </Text>
              <Box className="nxrm-ip-allowlist__results-table-container">
                <Table.Root className="nxrm-ip-allowlist__results-table" variant="surface">
                  <Table.Header>
                    <Table.Row>
                      <Table.ColumnHeaderCell width="80px">Line</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell>Status</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell>IP Address</Table.ColumnHeaderCell>
                      <Table.ColumnHeaderCell>Reason</Table.ColumnHeaderCell>
                    </Table.Row>
                  </Table.Header>
                  <Table.Body>
                    {issueRows.map((item, idx) => (
                      <Table.Row key={idx}>
                        <Table.Cell>
                          <Text size="2" color="gray">{item.row}</Text>
                        </Table.Cell>
                        <Table.Cell>
                          {item.status === 'skipped' ? (
                            <Badge color="orange" variant="soft" radius="full">Skipped</Badge>
                          ) : (
                            <Badge color="red" variant="soft" radius="full">Error</Badge>
                          )}
                        </Table.Cell>
                        <Table.Cell>
                          <Text size="2" weight="medium" style={{ fontFamily: 'monospace' }}>
                            {item.ip}
                          </Text>
                        </Table.Cell>
                        <Table.Cell>
                          <Text size="2" color="gray">{item.reason}</Text>
                        </Table.Cell>
                      </Table.Row>
                    ))}
                  </Table.Body>
                </Table.Root>
              </Box>
            </Box>
          ) : (
            <Flex
              align="center"
              gap="2"
              p="3"
              style={{
                backgroundColor: 'var(--green-2)',
                borderRadius: '8px',
                border: '1px solid var(--green-6)',
              }}
            >
              <Check size={18} strokeWidth={3} style={{ color: 'var(--green-9)', flexShrink: 0 }} />
              <Text size="2" style={{ color: 'var(--green-11)' }}>
                All {addedCount} {addedCount === 1 ? 'entry was' : 'entries were'} imported successfully.
              </Text>
            </Flex>
          )}

          {/* Actions */}
          <Flex gap="3" justify="end">
            <Button variant="solid" onClick={onClose} data-testid="import-results-close-button">
              Close
            </Button>
          </Flex>
        </Box>
      </Dialog.Content>
    </Dialog.Root>
  );
}
