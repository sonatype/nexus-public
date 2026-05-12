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

import React, { useState, useMemo } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Search, CheckCircle, XCircle, HelpCircle } from 'lucide-react';

import { MetricHealthListProps, formatCheckName, getHealthStatus, sortHealthChecks } from './types';

import './MetricHealthList.scss';

/**
 * MetricHealthList - List of health checks with status indicators
 */
export function MetricHealthList({
  checks,
  selectedCheck,
  onSelectCheck,
  className = '',
}: MetricHealthListProps) {
  const [filter, setFilter] = useState('');

  // Sort and filter checks
  const filteredChecks = useMemo(() => {
    const sorted = sortHealthChecks(checks);
    
    if (!filter.trim()) {
      return sorted;
    }

    const lowerFilter = filter.toLowerCase();
    return sorted.filter(
      (check) =>
        check.name.toLowerCase().includes(lowerFilter) ||
        formatCheckName(check.name).toLowerCase().includes(lowerFilter)
    );
  }, [checks, filter]);

  // Count by status
  const statusCounts = useMemo(() => {
    return checks.reduce(
      (acc, check) => {
        const status = getHealthStatus(check.result);
        acc[status]++;
        return acc;
      },
      { healthy: 0, unhealthy: 0, unknown: 0 }
    );
  }, [checks]);

  const getStatusIcon = (check: typeof checks[0]) => {
    const status = getHealthStatus(check.result);
    switch (status) {
      case 'healthy':
        return <CheckCircle size={16} className="metric-health-list__icon metric-health-list__icon--healthy" />;
      case 'unhealthy':
        return <XCircle size={16} className="metric-health-list__icon metric-health-list__icon--unhealthy" />;
      default:
        return <HelpCircle size={16} className="metric-health-list__icon metric-health-list__icon--unknown" />;
    }
  };

  return (
    <Box className={`metric-health-list ${className}`.trim()}>
      {/* Summary */}
      <Flex className="metric-health-list__summary" gap="4">
        <Flex align="center" gap="1">
          <CheckCircle size={14} className="metric-health-list__icon--healthy" />
          <Text size="1">{statusCounts.healthy} healthy</Text>
        </Flex>
        <Flex align="center" gap="1">
          <XCircle size={14} className="metric-health-list__icon--unhealthy" />
          <Text size="1">{statusCounts.unhealthy} unhealthy</Text>
        </Flex>
        {statusCounts.unknown > 0 && (
          <Flex align="center" gap="1">
            <HelpCircle size={14} className="metric-health-list__icon--unknown" />
            <Text size="1">{statusCounts.unknown} unknown</Text>
          </Flex>
        )}
      </Flex>

      {/* Search filter */}
      <Box className="metric-health-list__filter">
        <Flex align="center" gap="2">
          <Search size={14} className="metric-health-list__filter-icon" />
          <input
            type="text"
            className="metric-health-list__filter-input"
            placeholder="Filter health checks..."
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          />
        </Flex>
      </Box>

      {/* List */}
      <Box className="metric-health-list__items">
        {filteredChecks.length === 0 ? (
          <Text size="2" className="metric-health-list__empty">
            {filter ? 'No matching health checks found' : 'No health checks available'}
          </Text>
        ) : (
          filteredChecks.map((check) => (
            <button
              key={check.name}
              type="button"
              className={`metric-health-list__item ${
                selectedCheck === check.name ? 'metric-health-list__item--selected' : ''
              }`}
              onClick={() => onSelectCheck(check.name)}
              aria-selected={selectedCheck === check.name}
            >
              <Flex align="center" gap="2">
                {getStatusIcon(check)}
                <Text size="2" className="metric-health-list__name">
                  {formatCheckName(check.name)}
                </Text>
              </Flex>
            </button>
          ))
        )}
      </Box>
    </Box>
  );
}

export default MetricHealthList;


