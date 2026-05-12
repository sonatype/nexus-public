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
import { Box, Text } from '@radix-ui/themes';

import './MetadataGrid.scss';

export interface MetadataGridItem {
  label: string;
  value: React.ReactNode;
}

export interface MetadataGridProps {
  items: MetadataGridItem[];
}

const EMPTY_VALUE = '—';

/**
 * MetadataGrid - Two-column grid for profile metadata.
 * Labels fixed width (140px), values flexible. Empty values show em dash.
 */
export function MetadataGrid({ items }: MetadataGridProps): JSX.Element {
  return (
    <Box className="metadata-grid" role="list" data-testid="metadata-grid">
      {items.map(({ label, value }, index) => (
        <Box
          key={index}
          className="metadata-grid__row"
          role="listitem"
          style={{ display: 'flex', flexDirection: 'row', alignItems: 'flex-start' }}
        >
          <Text
            size="2"
            weight="bold"
            className="metadata-grid__label"
            style={{ flexShrink: 0, width: '140px', minWidth: '140px', display: 'inline-block' }}
          >
            {label}
          </Text>
          <Box className="metadata-grid__value" style={{ flex: 1, display: 'inline-block' }}>
            {value === undefined || value === null || value === '' ? (
              <Text size="2">{EMPTY_VALUE}</Text>
            ) : typeof value === 'string' ? (
              <Text size="2">{value}</Text>
            ) : (
              value
            )}
          </Box>
        </Box>
      ))}
    </Box>
  );
}

export default MetadataGrid;
