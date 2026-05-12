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

import React, { useState, useMemo, forwardRef } from 'react';
import { Box, Flex, Text, Heading } from '@radix-ui/themes';
import { ChevronDown, ChevronRight, Search } from 'lucide-react';

import { SystemInfoSectionProps, formatValue } from './types';

import './SystemInfoSection.scss';

/**
 * SystemInfoSection - Collapsible section displaying key-value pairs
 * Supports both controlled (open/onToggle) and uncontrolled (defaultOpen) modes.
 */
export const SystemInfoSection = forwardRef<HTMLDivElement, SystemInfoSectionProps>(function SystemInfoSection({
  title,
  data,
  defaultOpen = false,
  open,
  onToggle,
  className = '',
}, ref) {
  // Support both controlled and uncontrolled modes
  const [internalOpen, setInternalOpen] = useState(defaultOpen);
  const isControlled = open !== undefined;
  const isOpen = isControlled ? open : internalOpen;
  const [filter, setFilter] = useState('');

  // Filter entries based on search
  const entries = useMemo(() => {
    const allEntries = Object.entries(data || {});
    
    if (!filter.trim()) {
      return allEntries;
    }

    const lowerFilter = filter.toLowerCase();
    return allEntries.filter(
      ([key, value]) =>
        key.toLowerCase().includes(lowerFilter) ||
        formatValue(value).toLowerCase().includes(lowerFilter)
    );
  }, [data, filter]);

  const entryCount = Object.keys(data || {}).length;

  const handleToggle = () => {
    if (isControlled && onToggle) {
      onToggle(!isOpen);
    } else {
      setInternalOpen(!isOpen);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleToggle();
    }
  };

  return (
    <Box ref={ref} className={`system-info-section ${className}`.trim()}>
      {/* Header */}
      <Flex
        className="system-info-section__header"
        align="center"
        justify="between"
        onClick={handleToggle}
        role="button"
        tabIndex={0}
        onKeyDown={handleKeyDown}
        aria-expanded={isOpen}
      >
        <Flex align="center" gap="2">
          {isOpen ? (
            <ChevronDown size={18} className="system-info-section__chevron" />
          ) : (
            <ChevronRight size={18} className="system-info-section__chevron" />
          )}
          <Heading as="h3" size="3" weight="medium" className="system-info-section__title">
            {title}
          </Heading>
          <Text size="1" className="system-info-section__count">
            ({entryCount} {entryCount === 1 ? 'item' : 'items'})
          </Text>
        </Flex>
      </Flex>

      {/* Content */}
      {isOpen && (
        <Box className="system-info-section__content">
          {/* Search filter */}
          {entryCount > 5 && (
            <Box className="system-info-section__filter">
              <Flex align="center" gap="2">
                <Search size={14} className="system-info-section__filter-icon" />
                <input
                  type="text"
                  className="system-info-section__filter-input"
                  placeholder="Filter properties..."
                  value={filter}
                  onChange={(e) => setFilter(e.target.value)}
                  onClick={(e) => e.stopPropagation()}
                />
              </Flex>
            </Box>
          )}

          {/* Properties table */}
          <Box className="system-info-section__table">
            {entries.length === 0 ? (
              <Text size="2" className="system-info-section__empty">
                {filter ? 'No matching properties found' : 'No properties available'}
              </Text>
            ) : (
              entries.map(([key, value]) => (
                <Flex
                  key={key}
                  className="system-info-section__row"
                  align="flex-start"
                  gap="3"
                >
                  <Text size="2" weight="medium" className="system-info-section__key">
                    {key}
                  </Text>
                  <Text size="2" className="system-info-section__value">
                    {formatValue(value)}
                  </Text>
                </Flex>
              ))
            )}
          </Box>
        </Box>
      )}
    </Box>
  );
});

export default SystemInfoSection;



