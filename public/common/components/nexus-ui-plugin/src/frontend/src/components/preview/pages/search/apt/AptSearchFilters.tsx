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
import { X, ChevronDown, ChevronUp } from 'lucide-react';
import type { AptSearchFilters as FilterValues } from './apt.types';

export interface AptSearchFiltersProps {
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

const ANY_VALUE = '__any__';

/** Common architectures */
const ARCHITECTURES = [
  { value: ANY_VALUE, label: 'Any Architecture' },
  { value: 'amd64', label: 'amd64 (64-bit x86)' },
  { value: 'arm64', label: 'arm64 (64-bit ARM)' },
  { value: 'i386', label: 'i386 (32-bit x86)' },
  { value: 'armhf', label: 'armhf (ARM hard float)' },
  { value: 'all', label: 'all (Architecture independent)' },
];

/** Common distributions */
const DISTRIBUTIONS = [
  { value: ANY_VALUE, label: 'Any Distribution' },
  { value: 'bookworm', label: 'Debian 12 (bookworm)' },
  { value: 'bullseye', label: 'Debian 11 (bullseye)' },
  { value: 'buster', label: 'Debian 10 (buster)' },
  { value: 'jammy', label: 'Ubuntu 22.04 (jammy)' },
  { value: 'focal', label: 'Ubuntu 20.04 (focal)' },
  { value: 'noble', label: 'Ubuntu 24.04 (noble)' },
];

/** Common components */
const COMPONENTS = [
  { value: ANY_VALUE, label: 'Any Component' },
  { value: 'main', label: 'main (Free software)' },
  { value: 'contrib', label: 'contrib (Free but depends on non-free)' },
  { value: 'non-free', label: 'non-free (Non-free software)' },
  { value: 'restricted', label: 'restricted (Ubuntu)' },
  { value: 'universe', label: 'universe (Ubuntu)' },
  { value: 'multiverse', label: 'multiverse (Ubuntu)' },
];

/**
 * Filter controls for Apt package search.
 */
export function AptSearchFilters({
  values,
  onChange,
  onSearch,
  onClear,
  loading = false,
}: AptSearchFiltersProps): JSX.Element {
  const [showAdvanced, setShowAdvanced] = useState(false);

  const handleKeyDown = (event: React.KeyboardEvent): void => {
    if (event.key === 'Enter') {
      onSearch();
    }
  };

  const hasFilters = values.name || values.version || values.architecture || values.distribution || values.component;
  const hasAdvancedFilters = values.architecture || values.distribution || values.component;

  return (
    <Box p="4" style={{ backgroundColor: 'var(--gray-2)', borderRadius: 'var(--radius-3)' }}>
      <Flex direction="column" gap="3">
        {/* Primary Filters Row */}
        <Flex gap="3" wrap="wrap">
          <Box style={{ flex: '2 1 250px' }}>
            <Text as="label" size="1" color="gray" mb="1">
              Package Name
            </Text>
            <TextField.Root
              placeholder="e.g., nginx, curl, git"
              value={values.name ?? ''}
              onChange={(e) => onChange({ ...values, name: e.target.value || undefined })}
              onKeyDown={handleKeyDown}
              disabled={loading}
            />
          </Box>

          <Box style={{ flex: '1 1 150px' }}>
            <Text as="label" size="1" color="gray" mb="1">
              Version
            </Text>
            <TextField.Root
              placeholder="e.g., 1.24.0-1"
              value={values.version ?? ''}
              onChange={(e) => onChange({ ...values, version: e.target.value || undefined })}
              onKeyDown={handleKeyDown}
              disabled={loading}
            />
          </Box>
        </Flex>

        {/* Advanced Filters Toggle */}
        <Button
          variant="ghost"
          size="1"
          onClick={() => setShowAdvanced(!showAdvanced)}
          style={{ alignSelf: 'flex-start' }}
        >
          {showAdvanced ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
          {showAdvanced ? 'Hide' : 'Show'} Advanced Filters
          {hasAdvancedFilters && (
            <Text size="1" color="blue" ml="1">(Active)</Text>
          )}
        </Button>

        {/* Advanced Filters */}
        {showAdvanced && (
          <Flex gap="3" wrap="wrap">
            <Box style={{ flex: '1 1 180px' }}>
              <Text as="label" size="1" color="gray" mb="1">
                Architecture
              </Text>
              <Select.Root
                value={values.architecture ?? ANY_VALUE}
                onValueChange={(value) => onChange({
                  ...values,
                  architecture: value === ANY_VALUE ? undefined : value,
                })}
                disabled={loading}
              >
                <Select.Trigger placeholder="Select architecture" />
                <Select.Content>
                  {ARCHITECTURES.map((arch) => (
                    <Select.Item key={arch.value} value={arch.value}>
                      {arch.label}
                    </Select.Item>
                  ))}
                </Select.Content>
              </Select.Root>
            </Box>

            <Box style={{ flex: '1 1 180px' }}>
              <Text as="label" size="1" color="gray" mb="1">
                Distribution
              </Text>
              <Select.Root
                value={values.distribution ?? ANY_VALUE}
                onValueChange={(value) => onChange({
                  ...values,
                  distribution: value === ANY_VALUE ? undefined : value,
                })}
                disabled={loading}
              >
                <Select.Trigger placeholder="Select distribution" />
                <Select.Content>
                  {DISTRIBUTIONS.map((dist) => (
                    <Select.Item key={dist.value} value={dist.value}>
                      {dist.label}
                    </Select.Item>
                  ))}
                </Select.Content>
              </Select.Root>
            </Box>

            <Box style={{ flex: '1 1 180px' }}>
              <Text as="label" size="1" color="gray" mb="1">
                Component
              </Text>
              <Select.Root
                value={values.component ?? ANY_VALUE}
                onValueChange={(value) => onChange({
                  ...values,
                  component: value === ANY_VALUE ? undefined : value,
                })}
                disabled={loading}
              >
                <Select.Trigger placeholder="Select component" />
                <Select.Content>
                  {COMPONENTS.map((comp) => (
                    <Select.Item key={comp.value} value={comp.value}>
                      {comp.label}
                    </Select.Item>
                  ))}
                </Select.Content>
              </Select.Root>
            </Box>
          </Flex>
        )}

        {/* Action Buttons */}
        {hasFilters && (
          <Flex justify="end">
            <Button variant="ghost" onClick={onClear} disabled={loading}>
              <X size={14} />
              Clear Filters
            </Button>
          </Flex>
        )}
      </Flex>
    </Box>
  );
}

export default AptSearchFilters;
