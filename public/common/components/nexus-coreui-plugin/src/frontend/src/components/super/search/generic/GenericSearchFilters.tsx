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

import React, { useState } from 'react';
import {
  Box,
  Flex,
  TextField,
  Button,
  Text,
  Select,
} from '@radix-ui/themes';
import { ChevronDown, ChevronUp, X } from 'lucide-react';
import type { GenericSearchFilters as FilterValues } from './generic.types';
import { FORMAT_CONFIG } from './generic.types';

export interface GenericSearchFiltersProps {
  /** Current filter values */
  values: FilterValues;
  /** Callback when filter values change */
  onChange: (values: FilterValues) => void;
  /** Callback when search is triggered */
  onSearch: () => void;
  /** Callback to clear all filters */
  onClear: () => void;
  /** Whether a search is in progress */
  loading?: boolean;
}

const ANY_FORMAT = '__any__';

/**
 * Filter controls for generic component search.
 */
export function GenericSearchFilters({
  values,
  onChange,
  onSearch,
  onClear,
  loading = false,
}: GenericSearchFiltersProps): JSX.Element {
  const [showAdvanced, setShowAdvanced] = useState(false);

  const handleKeyDown = (event: React.KeyboardEvent): void => {
    if (event.key === 'Enter') {
      onSearch();
    }
  };

  const hasFilters = values.q || values.format || values.repository || 
                     values.group || values.name || values.version;

  const hasAdvancedFilters = values.repository || values.group || 
                             values.name || values.version;

  // Format options for dropdown
  const formatOptions = [
    { value: ANY_FORMAT, label: 'All Formats' },
    ...Object.entries(FORMAT_CONFIG).map(([value, config]) => ({
      value,
      label: config.label,
    })),
  ];

  return (
    <Box p="4" style={{ backgroundColor: 'var(--gray-2)', borderRadius: 'var(--radius-3)' }}>
      <Flex direction="column" gap="3">
        {/* Format Filter */}
        <Flex gap="3" wrap="wrap" align="end">
          <Box style={{ flex: '0 0 200px' }}>
            <Text as="label" size="1" color="gray" mb="1">
              Format
            </Text>
            <Select.Root
              value={values.format || ANY_FORMAT}
              onValueChange={(value) => onChange({
                ...values,
                format: value === ANY_FORMAT ? undefined : value,
              })}
              disabled={loading}
            >
              <Select.Trigger placeholder="All Formats" />
              <Select.Content>
                {formatOptions.map((opt) => (
                  <Select.Item key={opt.value} value={opt.value}>
                    {opt.label}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Box>
        </Flex>

        {/* Advanced filters toggle */}
        <Flex justify="between" align="center">
          <Button
            variant="ghost"
            size="1"
            onClick={() => setShowAdvanced(!showAdvanced)}
          >
            {showAdvanced ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
            {showAdvanced ? 'Hide' : 'Show'} Advanced Filters
            {hasAdvancedFilters && !showAdvanced && (
              <Text size="1" color="blue" ml="1">(Active)</Text>
            )}
          </Button>

          {hasFilters && (
            <Button variant="ghost" onClick={onClear} disabled={loading}>
              <X size={14} />
              Clear Filters
            </Button>
          )}
        </Flex>

        {/* Advanced filters row */}
        {showAdvanced && (
          <Flex gap="3" wrap="wrap">
            <Box style={{ flex: '1 1 180px' }}>
              <Text as="label" size="1" color="gray" mb="1">
                Repository
              </Text>
              <TextField.Root
                placeholder="e.g., maven-central"
                value={values.repository ?? ''}
                onChange={(e) => onChange({ ...values, repository: e.target.value || undefined })}
                onKeyDown={handleKeyDown}
                disabled={loading}
              />
            </Box>

            <Box style={{ flex: '1 1 180px' }}>
              <Text as="label" size="1" color="gray" mb="1">
                Group/Namespace
              </Text>
              <TextField.Root
                placeholder="e.g., org.apache"
                value={values.group ?? ''}
                onChange={(e) => onChange({ ...values, group: e.target.value || undefined })}
                onKeyDown={handleKeyDown}
                disabled={loading}
              />
            </Box>

            <Box style={{ flex: '1 1 180px' }}>
              <Text as="label" size="1" color="gray" mb="1">
                Name
              </Text>
              <TextField.Root
                placeholder="e.g., commons-lang3"
                value={values.name ?? ''}
                onChange={(e) => onChange({ ...values, name: e.target.value || undefined })}
                onKeyDown={handleKeyDown}
                disabled={loading}
              />
            </Box>

            <Box style={{ flex: '1 1 120px' }}>
              <Text as="label" size="1" color="gray" mb="1">
                Version
              </Text>
              <TextField.Root
                placeholder="e.g., 3.12.0"
                value={values.version ?? ''}
                onChange={(e) => onChange({ ...values, version: e.target.value || undefined })}
                onKeyDown={handleKeyDown}
                disabled={loading}
              />
            </Box>
          </Flex>
        )}
      </Flex>
    </Box>
  );
}

export default GenericSearchFilters;
