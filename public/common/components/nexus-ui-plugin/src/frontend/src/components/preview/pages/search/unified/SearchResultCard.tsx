/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React, { memo } from 'react';
import { Badge, Box, Card, Flex, Text } from '@radix-ui/themes';
import { Box as FormatBoxIcon, Package } from 'lucide-react';
import type { SearchResult } from './unified.types';
import { getFormatLogo } from './formatLogos';
import { DeepResearchLink } from '../../../shared/DeepResearchLink';
import { formatDateTime } from '../../../shared';
import { guidePackageName, isGuideSupported } from '../../../../../utils/guideIntegration';
import './SearchResultCard.scss';

type BadgeColor = 'gray' | 'grass' | 'iris' | 'blue' | 'amber' | 'purple' | 'cyan' | 'orange';

/**
 * Accent color for the ecosystem format badge, keyed by API format id. A format without an entry
 * falls back to neutral gray, so an unmapped ecosystem still renders a legible badge rather than
 * an uncolored one.
 */
const FORMAT_BADGE_COLOR: Record<string, BadgeColor> = {
  npm: 'grass',
  maven: 'iris',
  maven2: 'iris',
  docker: 'blue',
  oci: 'blue',
  pypi: 'amber',
  nuget: 'purple',
  go: 'cyan',
  cargo: 'orange',
};

function formatBadgeColor(format: string): BadgeColor {
  return FORMAT_BADGE_COLOR[format.toLowerCase()] ?? 'gray';
}

/**
 * Props for SearchResultCard component.
 */
export interface SearchResultCardProps {
  /** The search result to display */
  result: SearchResult;
  /**
   * Callback when the card is selected, by click or by keyboard.
   *
   * Receives the result so callers can pass one handler shared by every card
   * rather than allocating a closure per card. A handler whose identity is
   * stable across renders is what lets `memo` skip cards the parent re-rendered
   * without changing.
   */
  onSelect: (result: SearchResult) => void;
}

export const SearchResultCard = memo(function SearchResultCard({
  result,
  onSelect,
}: SearchResultCardProps): JSX.Element {
  const formatLogo = getFormatLogo(result.format);

  const handleSelect = (): void => {
    onSelect(result);
  };

  return (
    <Card size="2" className="search-result-card" style={{ cursor: 'pointer' }}>
      <Flex
        direction={{ initial: 'column', md: 'row' }}
        align={{ initial: 'start', md: 'center' }}
        gap="4"
      >
        {/*
         * Brand logo: left, decorative (alt="", aria-hidden). The ecosystem is named in visible
         * text by the Format badge below, so the logo carries no accessible text of its own. A
         * format without a logo gets a neutral placeholder icon so the layout does not collapse.
         */}
        <Flex
          align="center"
          justify="center"
          aria-hidden
          style={{ width: 48, height: 48, flexShrink: 0 }}
        >
          {formatLogo ? (
            <img
              src={formatLogo}
              alt=""
              style={{ width: 48, height: 48, objectFit: 'contain' }}
            />
          ) : (
            <Package size={28} color="var(--gray-9)" />
          )}
        </Flex>

        {/* Middle: name/version, group (when present), repository, format */}
        <Flex direction="column" gap="1" flexGrow="1" flexShrink="1" minWidth="0">
          {/* Name (primary) + version as a distinct token */}
          <Flex align="center" gap="2" wrap="wrap">
            {/*
             * The component name is the card's accessible control. It is a real <button> — not a
             * role="button" wrapper around the whole card — so screen readers still announce the
             * group / repository / format / date below, and the Guide link is not nested inside an
             * interactive element. A full-bleed ::after overlay (see the .scss) keeps the whole card
             * clickable by mouse; keyboard users activate this button directly.
             */}
            <button
              type="button"
              className="search-result-card__title"
              onClick={handleSelect}
              aria-label={`View details for ${result.name}`}
            >
              <Text
                size="4"
                weight="bold"
                truncate={{ initial: true, sm: false }}
                style={{ wordBreak: 'break-word' }}
              >
                {result.name}
              </Text>
            </button>
            {/*
             * Version as a distinct token: separated from the name by weight, size and the badge's
             * shape/background — not by color alone — so name and version stay individually legible.
             * Guarded on result.version: formats without versions (raw/apt/yum/gitlfs) would
             * otherwise render an empty gray pill.
             */}
            {result.version && (
              <Badge color="gray" variant="soft" size="1">
                {result.version}
              </Badge>
            )}
          </Flex>

          {result.group && (
            <Text size="2" color="gray">
              <Text weight="bold" as="span">
                Group:{' '}
              </Text>
              {result.group}
            </Text>
          )}

          <Text size="2" color="gray">
            <Text weight="bold" as="span">
              Repository name:{' '}
            </Text>
            {/*
             * Plain text, not blue and not an anchor: there is no repository navigation yet, and
             * blue here would read as a link and compete with the "Research in Guide" link, which
             * is the only real link on the card. A card click still opens the detail.
             */}
            {result.repository}
          </Text>

          {/* Format: label first (left of the badge), then the colored ecosystem badge */}
          <Flex align="center" gap="2">
            <Text size="2" color="gray" weight="medium">
              Format:
            </Text>
            <Badge color={formatBadgeColor(result.format)} variant="soft" radius="full" size="1">
              <FormatBoxIcon size={12} aria-hidden />
              {result.format}
            </Badge>
          </Flex>
        </Flex>

        {/* Right column: "Last updated" at the top, the "Research in Guide" jump link pinned to the
            bottom-right. Right-aligned so both read as trailing metadata for the card. */}
        <Flex
          direction="column"
          align="end"
          justify="between"
          gap="3"
          flexShrink="0"
          style={{ alignSelf: 'stretch' }}
        >
          <Flex direction="column" align="end">
            <Text size="1" color="gray" weight="bold">
              Last updated
            </Text>
            <Text size="1" color="gray">
              {formatDateTime(result.lastUpdated)}
            </Text>
          </Flex>

          {result.version && isGuideSupported(result.format) && (
            <Box className="search-result-card__guide">
              <DeepResearchLink
                ecosystem={result.format}
                packageName={guidePackageName(result.format, result.group, result.name)}
                version={result.version}
                size="2"
                appearance="link"
                referrer="search-results"
              />
            </Box>
          )}
        </Flex>
      </Flex>
    </Card>
  );
});

export default SearchResultCard;
