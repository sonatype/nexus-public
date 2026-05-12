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
import { Box, Flex, Text } from '@radix-ui/themes';
import { Package } from 'lucide-react';
import type { SearchResultItem as SearchResult } from './browse.api';

export interface SearchResultItemProps {
  result: SearchResult;
  onClick: () => void;
  onMouseDown: () => void;
  isSelected?: boolean;
}

/**
 * Individual search result item displayed in the dropdown.
 */
export function SearchResultItem({
  result,
  onClick,
  onMouseDown,
  isSelected = false,
}: SearchResultItemProps): JSX.Element {
  const displayName = result.group
    ? `${result.group}:${result.name}`
    : result.name;

  const displayVersion = result.version || '';

  return (
    <Box
      className={`in-repo-search__result-item${isSelected ? ' in-repo-search__result-item--selected' : ''}`}
      onClick={onClick}
      onMouseDown={onMouseDown}
      role="option"
      aria-selected={isSelected}
      tabIndex={-1}
      p="2"
      style={{
        cursor: 'pointer',
        borderBottom: '1px solid var(--gray-4)',
      }}
    >
      <Flex align="center" gap="2">
        <Package size={16} className="in-repo-search__result-icon" aria-hidden />
        <Box style={{ flex: 1, minWidth: 0 }}>
          <Text
            size="2"
            weight="medium"
            className="in-repo-search__result-name"
            style={{
              display: 'block',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
          >
            {displayName}
          </Text>
          {displayVersion && (
            <Text
              size="1"
              color="gray"
              className="in-repo-search__result-version"
              style={{ display: 'block' }}
            >
              {displayVersion}
            </Text>
          )}
        </Box>
      </Flex>
    </Box>
  );
}

export default SearchResultItem;
