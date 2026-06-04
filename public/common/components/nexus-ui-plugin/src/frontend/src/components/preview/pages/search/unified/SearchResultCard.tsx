/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React, { memo } from 'react';
import { Badge, Box, Card, Flex, Text } from '@radix-ui/themes';
import type { SearchResult } from './unified.types';
import { getFormatLogo } from './formatLogos';
import { DeepResearchLink } from '../../../shared/DeepResearchLink';

/**
 * Props for SearchResultCard component.
 */
export interface SearchResultCardProps {
  /** The search result to display */
  result: SearchResult;
  /** Callback when card is clicked */
  onClick: () => void;
}

function arePropsEqual(
  prevProps: SearchResultCardProps,
  nextProps: SearchResultCardProps
): boolean {
  return (
    prevProps.result.id === nextProps.result.id &&
    prevProps.result.version === nextProps.result.version &&
    prevProps.onClick === nextProps.onClick
  );
}

function formatDate(dateString?: string): string {
  if (!dateString) return '—';
  try {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  } catch {
    return '—';
  }
}

export const SearchResultCard = memo(function SearchResultCard({
  result,
  onClick,
}: SearchResultCardProps): JSX.Element {
  const formatLogo = getFormatLogo(result.format);

  const handleKeyDown = (event: React.KeyboardEvent): void => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onClick();
    }
  };

  return (
    <Card
      size="1"
      style={{ cursor: 'pointer' }}
      onClick={onClick}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="button"
      aria-label={`View details for ${result.name}`}
    >
      <Box py="0">
        <Flex
          direction={{ initial: 'column', md: 'row' }}
          align={{ initial: 'start', md: 'center' }}
          justify="start"
          gap="4"
        >
          {/* Logo: left aligned, vertically centered */}
          {formatLogo && (
            <Flex
              align="center"
              justify="center"
              style={{
                width: 32,
                height: 32,
                flexShrink: 0,
                borderRadius: 4,
                overflow: 'hidden',
              }}
            >
              <img
                src={formatLogo}
                alt=""
                aria-hidden
                style={{ width: 32, height: 32, objectFit: 'contain' }}
              />
            </Flex>
          )}

          {/* Package info - matches ux-lab ComponentResultCard layout and padding */}
          <Flex
            direction="column"
            gap="2"
            flexGrow="1"
            flexShrink="1"
            minWidth="0"
            p="3"
            justify="between"
          >
            {/* Header row: name + version (left), Last update (right) */}
            <Flex
              align="center"
              justify="between"
              gap="2"
              wrap="wrap"
              style={{ width: '100%' }}
            >
              <Flex align="center" gap="2">
                <Text
                  size="3"
                  weight="bold"
                  truncate={{ initial: true, xs: false }}
                  style={{ wordBreak: 'break-word' }}
                >
                  {result.name} {result.version}
                </Text>
                {result.version && (
                  <Box onClick={(e) => e.stopPropagation()}>
                    <DeepResearchLink
                      ecosystem={result.format}
                      packageName={result.format === 'maven2' && result.group ? `${result.group}:${result.name}` : result.name}
                      version={result.version}
                      iconOnly
                      size="1"
                      referrer="search-results"
                    />
                  </Box>
                )}
              </Flex>
              <Text size="1" color="gray" style={{ flexShrink: 0 }}>
                <Text weight="bold" as="span">Last update: </Text>
                {formatDate(result.lastUpdated)}
              </Text>
            </Flex>

            {/* Labels with tags: Ecosystem, Group, Repository */}
            <Flex align="center" gap="2" wrap="wrap">
              <Flex align="center" gap="1">
                <Text size="1" color="gray">Ecosystem:</Text>
                <Badge color="gray" variant="soft" size="1">
                  {result.format}
                </Badge>
              </Flex>
              {result.group && (
                <Flex align="center" gap="1">
                  <Text size="1" color="gray">Group:</Text>
                  <Badge color="gray" variant="soft" size="1">
                    {result.group}
                  </Badge>
                </Flex>
              )}
              <Flex align="center" gap="1">
                <Text size="1" color="gray">Repository:</Text>
                <Text size="1" color="gray">{result.repository}</Text>
              </Flex>
            </Flex>
          </Flex>
        </Flex>
      </Box>
    </Card>
  );
}, arePropsEqual);

export default SearchResultCard;
