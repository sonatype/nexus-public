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

import React, { useState, useCallback } from 'react';
import { Box, Flex, TextField, Button, Text } from '@radix-ui/themes';
import { X, ChevronDown, ChevronUp } from 'lucide-react';
import type { PyPISearchFilters as FilterValues } from './pypi.types';

export interface PyPISearchFiltersProps {
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

/**
 * Filter controls for PyPI package search.
 */
export function PyPISearchFilters({
  values,
  onChange,
  onSearch,
  onClear,
  loading = false,
}: PyPISearchFiltersProps): JSX.Element {
  const [showAdvanced, setShowAdvanced] = useState(false);

  const handleInputChange = useCallback((field: keyof FilterValues) => (
    event: React.ChangeEvent<HTMLInputElement>
  ): void => {
    onChange({
      ...values,
      [field]: event.target.value || undefined,
    });
  }, [values, onChange]);

  const handleKeyDown = useCallback((event: React.KeyboardEvent): void => {
    if (event.key === 'Enter') {
      onSearch();
    }
  }, [onSearch]);

  const hasAdvancedFilters = Boolean(values.summary || values.keywords || values.classifiers);

  return (
    <Box p="4" style={{ backgroundColor: 'var(--gray-2)', borderRadius: 'var(--radius-3)' }}>
      <Flex direction="column" gap="3">
        <Flex justify="between" align="center">
          <Text size="2" weight="medium">Additional Filters</Text>
          <Button variant="ghost" size="1" onClick={() => setShowAdvanced(!showAdvanced)}>
            {showAdvanced ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
            {showAdvanced ? 'Hide' : 'Show'}
          </Button>
        </Flex>

        {showAdvanced && (
          <Flex direction="column" gap="3">
            <Flex gap="3" wrap="wrap">
              <Box style={{ flex: '1 1 150px' }}>
                <Text as="label" size="1" color="gray" mb="1">
                  Version
                </Text>
                <TextField.Root
                  placeholder="e.g., 2.31.0"
                  value={values.version ?? ''}
                  onChange={handleInputChange('version')}
                  onKeyDown={handleKeyDown}
                  disabled={loading}
                />
              </Box>

              <Box style={{ flex: '1 1 200px' }}>
                <Text as="label" size="1" color="gray" mb="1">
                  Summary
                </Text>
                <TextField.Root
                  placeholder="Short description text"
                  value={values.summary ?? ''}
                  onChange={handleInputChange('summary')}
                  onKeyDown={handleKeyDown}
                  disabled={loading}
                />
              </Box>

              <Box style={{ flex: '1 1 200px' }}>
                <Text as="label" size="1" color="gray" mb="1">
                  Keywords
                </Text>
                <TextField.Root
                  placeholder="e.g., http, testing, web"
                  value={values.keywords ?? ''}
                  onChange={handleInputChange('keywords')}
                  onKeyDown={handleKeyDown}
                  disabled={loading}
                />
              </Box>
            </Flex>

            <Box>
              <Text as="label" size="1" color="gray" mb="1">
                Classifiers
              </Text>
              <TextField.Root
                placeholder="e.g., Development Status :: 5 - Production/Stable"
                value={values.classifiers ?? ''}
                onChange={handleInputChange('classifiers')}
                onKeyDown={handleKeyDown}
                disabled={loading}
              />
            </Box>

            <Flex justify="end" gap="2">
              {hasAdvancedFilters && (
                <Button variant="ghost" onClick={onClear} disabled={loading}>
                  <X size={14} />
                  Clear Filters
                </Button>
              )}
            </Flex>
          </Flex>
        )}
      </Flex>
    </Box>
  );
}

export default PyPISearchFilters;
