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

import React, { useState, useEffect, useMemo } from 'react';
import { Box, Flex, Text, Table, Badge, ScrollArea, Tooltip } from '@radix-ui/themes';
import {
  Search, CheckCircle, XCircle, AlertCircle, Circle, Loader2, Pencil,
  FileSearch, Globe, Users, Cloud, UserCheck, Shield, HeartPulse, Clock,
  Link, Server, Megaphone, Scissors, KeyRound, Calendar, HardDrive,
  Palette, Settings, ArrowUpCircle, Webhook, GitBranch, Puzzle,
} from 'lucide-react';
import { Capability, CapabilityType, getStateColor, getStateName } from './types';
import { useCapabilitiesApi } from './useCapabilitiesApi';

import './CapabilitiesList.scss';

interface CapabilitiesListProps {
  onSelect: (capability: Capability) => void;
  refreshKey?: number;
}

const CAPABILITY_CATEGORIES: Record<string, string> = {
  audit: 'Audit',
  baseurl: 'Core',
  crowd: 'Security',
  customs3regions: 'Core',
  defaultrole: 'Security',
  'firewall.audit': 'Security',
  healthcheck: 'Health Check',
  'license-expiration': 'Core',
  LegacyUrlCapability: 'Core',
  'node.identity': 'Core',
  outreach: 'Core',
  OutreachManagementCapability: 'Core',
  'browse.trim': 'Repository',
  rutauth: 'Security',
  'scheduling.scheduler': 'Scheduling',
  StorageSettings: 'Core',
  'rapture.branding': 'UI',
  'rapture.settings': 'UI',
  migration: 'Core',
  'webhook.global': 'Webhook',
  'webhook.repository': 'Webhook',
};

function getCategoryForType(typeId: string): string {
  return CAPABILITY_CATEGORIES[typeId] || 'Other';
}

const TYPE_ICONS: Record<string, React.ComponentType<{ size?: number; className?: string }>> = {
  audit: FileSearch,
  baseurl: Globe,
  crowd: Users,
  customs3regions: Cloud,
  defaultrole: UserCheck,
  'firewall.audit': Shield,
  healthcheck: HeartPulse,
  'license-expiration': Clock,
  LegacyUrlCapability: Link,
  'node.identity': Server,
  outreach: Megaphone,
  OutreachManagementCapability: Megaphone,
  'browse.trim': Scissors,
  rutauth: KeyRound,
  'scheduling.scheduler': Calendar,
  StorageSettings: HardDrive,
  'rapture.branding': Palette,
  'rapture.settings': Settings,
  migration: ArrowUpCircle,
  'webhook.global': Webhook,
  'webhook.repository': GitBranch,
};

function TypeIcon({ typeId }: { typeId: string }) {
  const Icon = TYPE_ICONS[typeId] || Puzzle;
  return <Icon size={16} className="capabilities-list__category-icon" />;
}

function StateIcon({ state }: { state: string }) {
  const color = getStateColor(state as 'active' | 'disabled' | 'error' | 'passive');
  const iconProps = { size: 16, style: { color } };

  switch (state) {
    case 'active':
      return <CheckCircle {...iconProps} />;
    case 'disabled':
      return <Circle {...iconProps} />;
    case 'error':
      return <XCircle {...iconProps} />;
    case 'passive':
      return <AlertCircle {...iconProps} />;
    default:
      return <Circle {...iconProps} />;
  }
}

export function CapabilitiesList({ onSelect, refreshKey = 0 }: CapabilitiesListProps) {
  const { fetchCapabilities, fetchCapabilityTypes } = useCapabilitiesApi();
  const [capabilities, setCapabilities] = useState<Capability[]>([]);
  const [typeNameMap, setTypeNameMap] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      setError(null);
      try {
        const [capData, typeData] = await Promise.all([
          fetchCapabilities(),
          fetchCapabilityTypes(),
        ]);
        setCapabilities(capData);
        const nameMap: Record<string, string> = {};
        typeData.forEach((t) => { nameMap[t.id] = t.name; });
        setTypeNameMap(nameMap);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load capabilities');
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [fetchCapabilities, fetchCapabilityTypes, refreshKey]);

  const filteredCapabilities = useMemo(() => {
    if (!searchTerm) return capabilities;
    const term = searchTerm.toLowerCase();
    return capabilities.filter(
      (cap) => {
        const displayName = typeNameMap[cap.typeId] || cap.typeName;
        const category = getCategoryForType(cap.typeId);
        return (
          displayName.toLowerCase().includes(term) ||
          cap.description?.toLowerCase().includes(term) ||
          cap.stateDescription?.toLowerCase().includes(term) ||
          cap.notes?.toLowerCase().includes(term) ||
          cap.state.toLowerCase().includes(term) ||
          category.toLowerCase().includes(term)
        );
      }
    );
  }, [capabilities, searchTerm, typeNameMap]);

  if (loading) {
    return (
      <Flex align="center" justify="center" className="capabilities-list__loading">
        <Loader2 size={24} className="capabilities-list__spinner" />
        <Text size="2">Loading capabilities...</Text>
      </Flex>
    );
  }

  if (error) {
    return (
      <Box className="capabilities-list__error">
        <AlertCircle size={20} />
        <Text size="2">{error}</Text>
      </Box>
    );
  }

  return (
    <Box className="capabilities-list">
      <Box className="capabilities-list__search">
        <Box className="capabilities-list__search-wrapper">
          <Search size={16} className="capabilities-list__search-icon" />
          <input
            type="text"
            placeholder="Filter capabilities..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="capabilities-list__search-input"
          />
        </Box>
        <Text size="2" className="capabilities-list__count">
          {filteredCapabilities.length} of {capabilities.length} capabilities
        </Text>
      </Box>

      <Box className="capabilities-list__table-wrapper" style={{ overflow: 'auto' }}>
        <Table.Root className="capabilities-list__table" variant="surface">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeaderCell className="capabilities-list__th capabilities-list__th--type">
                Type
              </Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell className="capabilities-list__th capabilities-list__th--state">
                State
              </Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell className="capabilities-list__th capabilities-list__th--category">
                Category
              </Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell className="capabilities-list__th capabilities-list__th--description">
                Description
              </Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell className="capabilities-list__th capabilities-list__th--notes">
                Notes
              </Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell className="capabilities-list__th capabilities-list__th--action" />
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {filteredCapabilities.length === 0 ? (
              <Table.Row>
                <Table.Cell colSpan={6} className="capabilities-list__empty">
                  {searchTerm ? 'No capabilities match your filter' : 'No capabilities configured'}
                </Table.Cell>
              </Table.Row>
            ) : (
              filteredCapabilities.map((capability) => {
                // Use typeName from REST API, fall back to types lookup
                const displayName = capability.typeName || typeNameMap[capability.typeId] || capability.typeId;
                const category = getCategoryForType(capability.typeId);
                // Use description from REST API (added by dev-backend)
                const description = capability.description || capability.stateDescription || '-';
                const notes = capability.notes || '-';

                return (
                  <Table.Row
                    key={capability.id}
                    className="capabilities-list__row"
                    onClick={() => onSelect(capability)}
                    data-testid={`capability-row-${capability.id}`}
                  >
                    <Table.Cell className="capabilities-list__cell capabilities-list__cell--type">
                      <Flex align="center" gap="2">
                        <TypeIcon typeId={capability.typeId} />
                        <Text weight="medium">{displayName}</Text>
                      </Flex>
                    </Table.Cell>
                    <Table.Cell className="capabilities-list__cell capabilities-list__cell--state">
                      <Flex align="center" gap="2">
                        <StateIcon state={capability.state} />
                        <Badge
                          color={
                            capability.state === 'active' ? 'green'
                              : capability.state === 'error' ? 'red'
                              : capability.state === 'passive' ? 'orange'
                              : 'gray'
                          }
                          variant="soft"
                        >
                          {getStateName(capability.state as 'active' | 'disabled' | 'error' | 'passive')}
                        </Badge>
                      </Flex>
                    </Table.Cell>
                    <Table.Cell className="capabilities-list__cell capabilities-list__cell--category">
                      <Badge variant="outline" size="1">{category}</Badge>
                    </Table.Cell>
                    <Table.Cell className="capabilities-list__cell capabilities-list__cell--description">
                      <Tooltip content={description}>
                        <Text size="2">{description}</Text>
                      </Tooltip>
                    </Table.Cell>
                    <Table.Cell className="capabilities-list__cell capabilities-list__cell--notes">
                      {notes !== '-' ? (
                        <Tooltip content={notes}>
                          <Text size="2">{notes}</Text>
                        </Tooltip>
                      ) : (
                        <Text size="2" color="gray">-</Text>
                      )}
                    </Table.Cell>
                    <Table.Cell className="capabilities-list__cell capabilities-list__cell--action">
                      <Pencil size={16} className="capabilities-list__edit-icon" />
                    </Table.Cell>
                  </Table.Row>
                );
              })
            )}
          </Table.Body>
        </Table.Root>
      </Box>
    </Box>
  );
}

export default CapabilitiesList;
