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
import { Box, Flex, Text, Badge, Tooltip } from '@radix-ui/themes';
import { ExternalLink, Globe, HardDrive, FolderSync } from 'lucide-react';
import { StatusBadge, FormatBadge, TypeBadge } from '../../../../shared';
import { Repository, RepositoryFormData } from './types';

import './RepositoryForm.scss';

interface RepositorySummaryProps {
  repository: Repository;
  formData: RepositoryFormData;
  onNavigateToTab: (tab: string) => void;
}

export function RepositorySummary({ repository, formData, onNavigateToTab }: RepositorySummaryProps) {
  const isOnline = repository.status?.online ?? repository.online ?? true;
  const blobStoreName = formData.storage?.blobStoreName || '-';
  const remoteUrl = formData.proxy?.remoteUrl;
  const memberCount = formData.group?.memberNames?.length || 0;

  return (
    <Box className="repository-summary">
      <Box className="repository-summary__grid">
        {/* Status Section */}
        <SummaryItem 
          label="Status" 
          value={
            <Flex align="center" gap="2">
              <StatusBadge status={isOnline ? 'online' : 'offline'} size="small" />
              <Text size="2">{repository.status?.description || (isOnline ? 'Online' : 'Offline')}</Text>
            </Flex>
          }
        />

        {/* Format & Type */}
        <SummaryItem 
          label="Format" 
          value={<FormatBadge format={repository.format} />} 
        />
        <SummaryItem 
          label="Type" 
          value={<TypeBadge type={repository.type as any} />} 
        />

        {/* URL Section */}
        <SummaryItem 
          label="URL" 
          value={
            <Flex align="center" gap="2">
              <Text size="2" className="repository-summary__url">{repository.url}</Text>
              <Tooltip content="Open in new tab">
                <a href={repository.url} target="_blank" rel="noopener noreferrer" className="repository-summary__link">
                  <ExternalLink size={14} />
                </a>
              </Tooltip>
            </Flex>
          }
        />

        {/* Storage Section */}
        <SummaryItem 
          label="Blob Store" 
          value={
            <Flex align="center" gap="2">
              <HardDrive size={14} className="repository-summary__icon" />
              <Text size="2">{blobStoreName}</Text>
            </Flex>
          }
          onClick={() => onNavigateToTab('settings')}
        />

        {/* Proxy-specific: Remote URL */}
        {remoteUrl && (
          <SummaryItem 
            label="Remote Storage" 
            value={
              <Flex align="center" gap="2">
                <Globe size={14} className="repository-summary__icon" />
                <Text size="2" className="repository-summary__url">{remoteUrl}</Text>
              </Flex>
            }
            onClick={() => onNavigateToTab('settings')}
          />
        )}

        {/* Group-specific: Members */}
        {repository.type === 'group' && (
          <SummaryItem 
            label="Group Members" 
            value={
              <Flex align="center" gap="2">
                <FolderSync size={14} className="repository-summary__icon" />
                <Text size="2">{memberCount} member repositories</Text>
              </Flex>
            }
            onClick={() => onNavigateToTab('settings')}
          />
        )}

        {/* Cleanup Policies */}
        {formData.cleanup?.policyNames && formData.cleanup.policyNames.length > 0 && (
          <SummaryItem 
            label="Cleanup Policies" 
            value={
              <Flex gap="1" wrap="wrap">
                {formData.cleanup.policyNames.map(policy => (
                  <Badge key={policy} size="1" color="blue" variant="soft">{policy}</Badge>
                ))}
              </Flex>
            }
            onClick={() => onNavigateToTab('settings')}
          />
        )}
      </Box>
    </Box>
  );
}

interface SummaryItemProps {
  label: string;
  value: React.ReactNode;
  onClick?: () => void;
}

function SummaryItem({ label, value, onClick }: SummaryItemProps) {
  return (
    <Box 
      className={`repository-summary__item ${onClick ? 'repository-summary__item--clickable' : ''}`}
      onClick={onClick}
    >
      <Text size="1" color="gray" className="repository-summary__label">{label}</Text>
      <Box className="repository-summary__value">
        {typeof value === 'string' ? <Text size="2">{value}</Text> : value}
      </Box>
    </Box>
  );
}
