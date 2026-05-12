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
import { 
  Box, 
  Flex, 
  TextField, 
  Button, 
  Text, 
  Checkbox,
  Select,
} from '@radix-ui/themes';
import { Search, X } from 'lucide-react';

import type { NuGetSearchFilters as FilterValues } from './nuget.types';

interface NuGetSearchFiltersProps {
  values: FilterValues;
  onChange: (values: FilterValues) => void;
  onSearch: () => void;
  onClear: () => void;
  loading?: boolean;
}

const ANY_FRAMEWORK = '__any__';

const TARGET_FRAMEWORKS = [
  { value: ANY_FRAMEWORK, label: 'Any Framework' },
  { value: 'net8.0', label: '.NET 8.0' },
  { value: 'net7.0', label: '.NET 7.0' },
  { value: 'net6.0', label: '.NET 6.0' },
  { value: 'netstandard2.1', label: '.NET Standard 2.1' },
  { value: 'netstandard2.0', label: '.NET Standard 2.0' },
  { value: 'net48', label: '.NET Framework 4.8' },
];

/**
 * NuGet-specific search filters.
 */
export function NuGetSearchFilters({
  values,
  onChange,
  onSearch,
  onClear,
  loading = false,
}: NuGetSearchFiltersProps): JSX.Element {
  
  const handleKeyDown = (e: React.KeyboardEvent): void => {
    if (e.key === 'Enter') {
      onSearch();
    }
  };

  const hasFilters = Boolean(
    values.packageId || values.version || values.prerelease || values.targetFramework
  );

  return (
    <Box className="nuget-search-filters" p="4" style={{ backgroundColor: 'var(--gray-2)', borderRadius: 'var(--radius-3)' }}>
      <Flex direction="column" gap="3">
        <Text size="2" weight="medium">Filters</Text>
        
        <Flex gap="3" wrap="wrap">
          {/* Package ID */}
          <Box style={{ flex: '1 1 200px' }}>
            <Text as="label" size="1" color="gray" mb="1">
              Package ID
            </Text>
            <TextField.Root
              placeholder="e.g., Newtonsoft.Json"
              value={values.packageId ?? ''}
              onChange={(e) => onChange({ ...values, packageId: e.target.value || undefined })}
              onKeyDown={handleKeyDown}
              disabled={loading}
            />
          </Box>

          {/* Version */}
          <Box style={{ flex: '1 1 150px' }}>
            <Text as="label" size="1" color="gray" mb="1">
              Version
            </Text>
            <TextField.Root
              placeholder="e.g., 13.0.0"
              value={values.version ?? ''}
              onChange={(e) => onChange({ ...values, version: e.target.value || undefined })}
              onKeyDown={handleKeyDown}
              disabled={loading}
            />
          </Box>

          {/* Target Framework */}
          <Box style={{ flex: '1 1 180px' }}>
            <Text as="label" size="1" color="gray" mb="1">
              Target Framework
            </Text>
            <Select.Root
              value={values.targetFramework ?? ANY_FRAMEWORK}
              onValueChange={(value) => onChange({ 
                ...values, 
                targetFramework: value === ANY_FRAMEWORK ? undefined : value 
              })}
              disabled={loading}
            >
              <Select.Trigger placeholder="Select framework" />
              <Select.Content>
                {TARGET_FRAMEWORKS.map((fw) => (
                  <Select.Item key={fw.value} value={fw.value}>
                    {fw.label}
                  </Select.Item>
                ))}
              </Select.Content>
            </Select.Root>
          </Box>
        </Flex>

        <Flex justify="between" align="center">
          {/* Prerelease checkbox */}
          <Flex align="center" gap="2">
            <Checkbox
              checked={values.prerelease ?? false}
              onCheckedChange={(checked) => 
                onChange({ ...values, prerelease: checked === true ? true : undefined })
              }
              disabled={loading}
            />
            <Text size="2">Include prerelease versions</Text>
          </Flex>

          {/* Buttons */}
          <Flex gap="2">
            {hasFilters && (
              <Button variant="ghost" onClick={onClear} disabled={loading}>
                <X size={14} />
                Clear
              </Button>
            )}
            <Button onClick={onSearch} disabled={loading}>
              <Search size={14} />
              Search
            </Button>
          </Flex>
        </Flex>
      </Flex>
    </Box>
  );
}

export default NuGetSearchFilters;

