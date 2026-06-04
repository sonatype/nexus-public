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
import { Box, Flex, Text, Heading, Badge } from '@radix-ui/themes';
import { CheckCircle, XCircle, HelpCircle, Clock } from 'lucide-react';

import { MetricHealthDetailProps, formatCheckName, getHealthStatus } from './types';

import './MetricHealthDetail.scss';

/**
 * MetricHealthDetail - Detailed view of a single health check
 */
export function MetricHealthDetail({
  check,
  className = '',
}: MetricHealthDetailProps) {
  if (!check) {
    return (
      <Box className={`metric-health-detail ${className}`.trim()}>
        <Flex align="center" justify="center" className="metric-health-detail__empty">
          <Text size="2" className="metric-health-detail__empty-text">
            Select a health check to view details
          </Text>
        </Flex>
      </Box>
    );
  }

  const status = getHealthStatus(check.result);
  const { result } = check;

  const getStatusIcon = () => {
    switch (status) {
      case 'healthy':
        return <CheckCircle size={24} className="metric-health-detail__status-icon metric-health-detail__status-icon--healthy" />;
      case 'unhealthy':
        return <XCircle size={24} className="metric-health-detail__status-icon metric-health-detail__status-icon--unhealthy" />;
      default:
        return <HelpCircle size={24} className="metric-health-detail__status-icon metric-health-detail__status-icon--unknown" />;
    }
  };

  const getStatusBadge = () => {
    switch (status) {
      case 'healthy':
        return <Badge color="green" size="2">Healthy</Badge>;
      case 'unhealthy':
        return <Badge color="red" size="2">Unhealthy</Badge>;
      default:
        return <Badge color="gray" size="2">Unknown</Badge>;
    }
  };

  return (
    <Box className={`metric-health-detail ${className}`.trim()}>
      {/* Header */}
      <Flex className="metric-health-detail__header" align="center" gap="3">
        {getStatusIcon()}
        <Box>
          <Heading as="h3" size="4" weight="medium" className="metric-health-detail__title">
            {formatCheckName(check.name)}
          </Heading>
          <Text size="1" className="metric-health-detail__subtitle">
            {check.name}
          </Text>
        </Box>
        <Box style={{ marginLeft: 'auto' }}>
          {getStatusBadge()}
        </Box>
      </Flex>

      {/* Details */}
      <Box className="metric-health-detail__content">
        {/* Message */}
        {result.message && (
          <Box className="metric-health-detail__section">
            <Text size="2" weight="medium" className="metric-health-detail__label">
              Message
            </Text>
            <Box className="metric-health-detail__message">
              <Text size="2">{result.message}</Text>
            </Box>
          </Box>
        )}

        {/* Error */}
        {result.error && (
          <Box className="metric-health-detail__section">
            <Text size="2" weight="medium" className="metric-health-detail__label metric-health-detail__label--error">
              Error
            </Text>
            <Box className="metric-health-detail__error">
              {result.error.message && (
                <Text size="2" className="metric-health-detail__error-message">
                  {result.error.message}
                </Text>
              )}
              {result.error.stack && (
                <pre className="metric-health-detail__stack">
                  {result.error.stack}
                </pre>
              )}
            </Box>
          </Box>
        )}

        {/* Duration */}
        {result.duration !== undefined && (
          <Box className="metric-health-detail__section">
            <Flex align="center" gap="2">
              <Clock size={14} className="metric-health-detail__duration-icon" />
              <Text size="2" className="metric-health-detail__duration">
                Duration: {result.duration}ms
              </Text>
            </Flex>
          </Box>
        )}

        {/* Timestamp */}
        {result.timestamp && (
          <Box className="metric-health-detail__section">
            <Text size="2" className="metric-health-detail__timestamp">
              Last checked: {new Date(result.timestamp).toLocaleString()}
            </Text>
          </Box>
        )}

        {/* No additional details */}
        {!result.message && !result.error && !result.duration && (
          <Text size="2" className="metric-health-detail__no-details">
            No additional details available for this health check.
          </Text>
        )}
      </Box>
    </Box>
  );
}

export default MetricHealthDetail;


