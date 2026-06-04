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
import { Box, Flex, Text, Callout, Tooltip, IconButton } from '@radix-ui/themes';
import { Info, FolderTree, ExternalLink } from 'lucide-react';
import { LoadingState, ErrorState, EntityTable } from '../../../../shared';
import { useRepositoryTree } from './useRepositoryTree';

export interface RepositoryGroupUsageTabProps {
  repositoryName: string;
}

/**
 * RepositoryGroupUsageTab displays which group repositories include this repository as a member.
 */
export function RepositoryGroupUsageTab({ repositoryName }: RepositoryGroupUsageTabProps) {
  const { usages, loading, error, refresh } = useRepositoryTree(repositoryName);

  if (loading) return <LoadingState message="Checking group membership..." />;
  if (error) return <ErrorState message={error} onRetry={refresh} />;

  const columns = [
    {
      id: 'name',
      header: 'Group Repository',
      accessor: (name: string) => (
        <Flex align="center" justify="between" style={{ width: '100%' }}>
          <Flex align="center" gap="2">
            <FolderTree size={16} color="var(--blue-9)" />
            <Text weight="medium">{name}</Text>
          </Flex>
          <Tooltip content="View Structure">
            <IconButton 
              variant="ghost" 
              size="1" 
              onClick={() => window.location.hash = `preview/admin/repository/repositories/${encodeURIComponent(name)}/profile?tab=structure`}
            >
              <ExternalLink size={14} />
            </IconButton>
          </Tooltip>
        </Flex>
      ),
      sortable: true,
    }
  ];

  return (
    <Box p="4" className="repository-usage-tab">
      <Callout.Root color="blue" mb="4" size="1">
        <Callout.Icon>
          <Info size={16} />
        </Callout.Icon>
        <Callout.Text>
          Showing group repositories that include <strong>{repositoryName}</strong> as a member.
        </Callout.Text>
      </Callout.Root>

      {usages.length > 0 ? (
        <EntityTable 
          data={usages}
          columns={columns}
          getRowKey={(name) => name}
          testId="repository-usage-table"
        />
      ) : (
        <Flex 
          direction="column" 
          align="center" 
          justify="center" 
          p="6" 
          style={{ background: 'var(--gray-2)', borderRadius: 'var(--radius-3)' }}
        >
          <Text color="gray" size="2">This repository is not a member of any groups.</Text>
        </Flex>
      )}
    </Box>
  );
}

export default RepositoryGroupUsageTab;
