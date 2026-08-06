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
import { Box, Card, Flex, Text, Heading, Button } from '@radix-ui/themes';
import { Server, Loader2, AlertCircle, CheckCircle, Archive } from 'lucide-react';
import { ExtJS } from '../../../../../../interface/ExtJS';

import { NodeInfo, SUPPORT_ZIP_API } from './types';

interface SupportZipNodeCardProps {
  node: NodeInfo;
  isBlobStoreConfigured: boolean;
  onGenerate: (node: NodeInfo) => void;
  disabled?: boolean;
}

function formatTimestamp(value?: number): string | null {
  if (!value) return null;
  try {
    return new Date(value).toLocaleString();
  } catch {
    return null;
  }
}

export function SupportZipNodeCard({
  node,
  isBlobStoreConfigured,
  onGenerate,
  disabled = false,
}: SupportZipNodeCardProps): React.ReactElement {
  const isNodeActive = node.status !== 'NODE_UNAVAILABLE';
  const zipCreating = node.status === 'CREATING';
  const generateDisabled = disabled || !isNodeActive || !isBlobStoreConfigured || zipCreating;
  const lastUpdated = formatTimestamp(node.lastUpdated);

  const renderHeader = () => {
    const StatusIcon = isNodeActive ? CheckCircle : AlertCircle;
    const iconColor = isNodeActive ? 'var(--green-9)' : 'var(--red-9)';
    return (
      <Flex align="center" gap="2">
        <Server size={18} aria-hidden="true" />
        <Heading as="h4" size="3" weight="medium" style={{ flex: 1 }}>
          {node.hostname || node.nodeId}
        </Heading>
        <StatusIcon size={16} aria-hidden="true" style={{ color: iconColor }} />
      </Flex>
    );
  };

  const renderBody = () => {
    if (!isNodeActive) {
      return (
        <Text size="2" color="gray">
          Node is unavailable. Cannot create ZIP.
        </Text>
      );
    }
    if (!isBlobStoreConfigured) {
      return (
        <Text size="2" color="gray">
          No blob store configured for this cluster. Configure a blob store to generate support ZIPs.
        </Text>
      );
    }
    switch (node.status) {
      case 'CREATING':
        return (
          <Flex align="center" gap="2" aria-live="polite" aria-busy="true">
            <Loader2 size={16} aria-hidden="true" />
            <Text size="2">Creating ZIP...</Text>
          </Flex>
        );
      case 'COMPLETED':
        return (
          <Flex direction="column" gap="1">
            <a
              href={ExtJS.urlOf(SUPPORT_ZIP_API.DOWNLOAD(node.blobRef ?? ''))}
              download
              aria-label={`Download ZIP for ${node.hostname || node.nodeId}`}
              data-testid={`support-zip-node-card-download-${node.nodeId}`}
            >
              Download ZIP
            </a>
            {lastUpdated && (
              <Text size="1" color="gray">
                Generated: {lastUpdated}
              </Text>
            )}
          </Flex>
        );
      case 'FAILED':
        return (
          <Flex align="center" gap="2">
            <AlertCircle size={16} aria-hidden="true" style={{ color: 'var(--red-9)' }} />
            <Text size="2" color="red">
              Generation failed.
            </Text>
          </Flex>
        );
      default:
        return (
          <Text size="2" color="gray">
            No ZIP created yet.
          </Text>
        );
    }
  };

  const buttonLabel = node.status === 'FAILED' ? 'Retry' : 'Generate new ZIP';

  return (
    <Card data-testid={`support-zip-node-card-${node.nodeId}`}>
      <Flex direction="column" gap="3">
        {renderHeader()}
        <Box>{renderBody()}</Box>
        <Flex justify="end">
          <Button
            type="button"
            variant="solid"
            disabled={generateDisabled}
            onClick={() => onGenerate(node)}
            data-testid={`support-zip-node-card-generate-${node.nodeId}`}
            data-analytics-id="nxrm-support-zip-create-node"
          >
            <Archive size={16} aria-hidden="true" />
            {buttonLabel}
          </Button>
        </Flex>
      </Flex>
    </Card>
  );
}

export default SupportZipNodeCard;
