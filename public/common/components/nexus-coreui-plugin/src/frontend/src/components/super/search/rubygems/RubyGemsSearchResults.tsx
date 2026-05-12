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
import { Box, Flex, Text, Table, Badge, Spinner, Callout } from '@radix-ui/themes';
import { Gem, AlertCircle, ChevronRight } from 'lucide-react';
import type { RubyGemsResult } from './rubygems.types';

export interface RubyGemsSearchResultsProps {
  results: readonly RubyGemsResult[];
  loading: boolean;
  error?: string;
  totalCount: number;
  onSelect: (id: string) => void;
}

export function RubyGemsSearchResults({
  results,
  loading,
  error,
  totalCount,
  onSelect,
}: RubyGemsSearchResultsProps): JSX.Element {
  if (loading && results.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="3">
          <Spinner size="3" />
          <Text color="gray">Searching Ruby gems...</Text>
        </Flex>
      </Flex>
    );
  }

  if (error) {
    return (
      <Callout.Root color="red">
        <Callout.Icon><AlertCircle size={16} /></Callout.Icon>
        <Callout.Text>{error}</Callout.Text>
      </Callout.Root>
    );
  }

  if (results.length === 0) {
    return (
      <Flex justify="center" align="center" p="6">
        <Flex direction="column" align="center" gap="2">
          <Gem size={32} />
          <Text color="gray">No gems found</Text>
          <Text size="1" color="gray">Try adjusting your search criteria</Text>
        </Flex>
      </Flex>
    );
  }

  return (
    <Box>
      <Flex justify="between" align="center" mb="3">
        <Text size="2" color="gray">Showing {results.length} of {totalCount} gems</Text>
      </Flex>

      <Table.Root>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeaderCell>Gem</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Latest Version</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Platform</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Authors</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell>Repository</Table.ColumnHeaderCell>
            <Table.ColumnHeaderCell></Table.ColumnHeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {results.map((result) => (
            <Table.Row key={result.id} onClick={() => onSelect(result.id)} style={{ cursor: 'pointer' }}>
              <Table.Cell>
                <Flex direction="column" gap="1">
                  <Flex align="center" gap="2">
                    <Gem size={16} />
                    <Text weight="medium">{result.name}</Text>
                  </Flex>
                  {result.summary && (
                    <Text size="1" color="gray" style={{ maxWidth: '400px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {result.summary}
                    </Text>
                  )}
                </Flex>
              </Table.Cell>
              <Table.Cell>
                <Badge color="ruby" variant="soft">{result.latestVersion}</Badge>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">{result.platform || 'ruby'}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">{result.authors || '-'}</Text>
              </Table.Cell>
              <Table.Cell>
                <Text size="2" color="gray">{result.repository}</Text>
              </Table.Cell>
              <Table.Cell><ChevronRight size={16} color="var(--gray-8)" /></Table.Cell>
            </Table.Row>
          ))}
        </Table.Body>
      </Table.Root>
    </Box>
  );
}

export default RubyGemsSearchResults;
