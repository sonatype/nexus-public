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

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Box, Badge, Flex, Text, ScrollArea } from '@radix-ui/themes';
import {
  Search, ChevronRight, Loader2, AlertCircle, Puzzle,
  FileSearch, Globe, Users, Cloud, UserCheck, Shield, HeartPulse, Clock,
  Link, Server, Megaphone, Scissors, KeyRound, Calendar, HardDrive,
  Palette, Settings, ArrowUpCircle, Webhook, GitBranch,
} from 'lucide-react';
import DOMPurify from 'dompurify';
import { CapabilityType } from './types';
import { useCapabilitiesApi } from './useCapabilitiesApi';

const MULTI_INSTANCE_TYPES = new Set(['webhook.global', 'webhook.repository']);

const TYPE_ICONS: Record<string, React.ComponentType<{size?: number; className?: string}>> = {
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
  OutreachManagementCapability: Megaphone,
  outreach: Megaphone,
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

function getTypeIcon(typeId: string) {
  return TYPE_ICONS[typeId] || Puzzle;
}

import './CapabilityTypeSelector.scss';

interface CapabilityTypeSelectorProps {
  onSelect: (type: CapabilityType) => void;
  selectedTypeId?: string | null;
}

/**
 * CapabilityTypeSelector - Grid display for selecting a capability type
 */
export function CapabilityTypeSelector({ onSelect, selectedTypeId }: CapabilityTypeSelectorProps) {
  const { fetchCapabilityTypes, fetchCapabilities } = useCapabilitiesApi();
  const [types, setTypes] = useState<CapabilityType[]>([]);
  const [existingTypes, setExistingTypes] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      setError(null);
      try {
        const [typeData, capData] = await Promise.all([
          fetchCapabilityTypes(),
          fetchCapabilities(),
        ]);
        typeData.sort((a, b) => a.name.localeCompare(b.name));
        setTypes(typeData);
        setExistingTypes(new Set(capData.map((c) => c.typeId)));
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load capability types');
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [fetchCapabilityTypes, fetchCapabilities]);

  const isCreatable = useCallback((type: CapabilityType): boolean => {
    if (MULTI_INSTANCE_TYPES.has(type.id)) return true;
    return !existingTypes.has(type.id);
  }, [existingTypes]);

  const filteredTypes = useMemo(() => {
    let result = types;
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      result = result.filter(
        (type) =>
          type.name.toLowerCase().includes(term) ||
          type.about?.toLowerCase().includes(term)
      );
    }
    return [...result].sort((a, b) => {
      const aCreatable = isCreatable(a);
      const bCreatable = isCreatable(b);
      if (aCreatable !== bCreatable) return aCreatable ? -1 : 1;
      return a.name.localeCompare(b.name);
    });
  }, [types, searchTerm, isCreatable]);

  if (loading) {
    return (
      <Flex align="center" justify="center" className="capability-type-selector__loading">
        <Loader2 size={24} className="capability-type-selector__spinner" />
        <Text size="2">Loading capability types...</Text>
      </Flex>
    );
  }

  if (error) {
    return (
      <Box className="capability-type-selector__error">
        <AlertCircle size={20} />
        <Text size="2">{error}</Text>
      </Box>
    );
  }

  return (
    <Box className="capability-type-selector">
      {/* Search */}
      <Box className="capability-type-selector__search">
        <Box className="capability-type-selector__search-wrapper">
          <Search size={16} className="capability-type-selector__search-icon" />
          <input
            type="text"
            placeholder="Filter capability types..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="capability-type-selector__search-input"
            autoFocus
            autoComplete="off"
          />
        </Box>
        <Text size="2" className="capability-type-selector__count" data-testid="type-selector-count">
          {filteredTypes.filter(isCreatable).length} of {filteredTypes.length} types available
        </Text>
      </Box>

      {/* Type Grid */}
      <Box className="capability-type-selector__grid-wrapper">
        {filteredTypes.length === 0 ? (
          <Box className="capability-type-selector__empty">
            <Text size="2">No capability types match your filter</Text>
          </Box>
        ) : (
          <Box className="capability-type-selector__grid">
            {filteredTypes.map((type) => {
              const canCreate = isCreatable(type);
              const isSelected = selectedTypeId === type.id;
              return (
                <button
                  key={type.id}
                  type="button"
                  className={`capability-type-selector__card ${!canCreate ? 'capability-type-selector__card--disabled' : ''} ${isSelected ? 'capability-type-selector__card--selected' : ''}`}
                  onClick={canCreate ? () => onSelect(type) : undefined}
                  disabled={!canCreate}
                  data-testid={`type-card-${type.id}`}
                >
                  <Flex align="start" gap="3">
                    <Box className="capability-type-selector__icon">
                      {React.createElement(getTypeIcon(type.id), {size: 20})}
                    </Box>
                    <Box className="capability-type-selector__content">
                      <Flex align="center" gap="2">
                        <Text weight="medium" className="capability-type-selector__name">
                          {type.name}
                        </Text>
                        {!canCreate && (
                          <Badge size="1" color="gray" variant="soft">Already configured</Badge>
                        )}
                      </Flex>
                      {type.about && (
                        <Text
                          size="1"
                          className="capability-type-selector__about"
                          dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(type.about) }}
                        />
                      )}
                    </Box>
                    {canCreate && <ChevronRight size={16} className="capability-type-selector__chevron" />}
                  </Flex>
                </button>
              );
            })}
          </Box>
        )}
      </Box>
    </Box>
  );
}

export default CapabilityTypeSelector;


