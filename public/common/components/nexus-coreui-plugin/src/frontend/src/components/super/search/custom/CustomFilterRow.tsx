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
import { Flex, TextField, Select, IconButton, Tooltip } from '@radix-ui/themes';
import { X } from 'lucide-react';
import type { CustomFilter, FilterField, FilterOperator } from './custom.types';
import {
  FILTER_FIELD_OPTIONS,
  FILTER_OPERATOR_OPTIONS,
} from './custom.types';

export interface CustomFilterRowProps {
  filter: CustomFilter;
  onUpdate: (id: string, updates: Partial<CustomFilter>) => void;
  onRemove: (id: string) => void;
  isOnlyFilter?: boolean;
  disabled?: boolean;
}

export function CustomFilterRow({
  filter,
  onUpdate,
  onRemove,
  isOnlyFilter = false,
  disabled = false,
}: CustomFilterRowProps): JSX.Element {
  const handleFieldChange = (value: string): void => {
    onUpdate(filter.id, { field: value as FilterField });
  };

  const handleOperatorChange = (value: string): void => {
    onUpdate(filter.id, { operator: value as FilterOperator });
  };

  const handleValueChange = (e: React.ChangeEvent<HTMLInputElement>): void => {
    onUpdate(filter.id, { value: e.target.value });
  };

  const handleRemove = (): void => {
    onRemove(filter.id);
  };

  const currentFieldOption = FILTER_FIELD_OPTIONS.find(
    (opt) => opt.value === filter.field
  );
  const placeholder = currentFieldOption?.placeholder || 'Enter value...';

  return (
    <Flex gap="2" align="center">
      <Select.Root value={filter.field} onValueChange={handleFieldChange} disabled={disabled}>
        <Select.Trigger style={{ minWidth: '140px' }} />
        <Select.Content>
          {FILTER_FIELD_OPTIONS.map((opt) => (
            <Select.Item key={opt.value} value={opt.value}>
              {opt.label}
            </Select.Item>
          ))}
        </Select.Content>
      </Select.Root>

      <Select.Root value={filter.operator} onValueChange={handleOperatorChange} disabled={disabled}>
        <Select.Trigger style={{ minWidth: '120px' }} />
        <Select.Content>
          {FILTER_OPERATOR_OPTIONS.map((opt) => (
            <Select.Item key={opt.value} value={opt.value}>
              {opt.label}
            </Select.Item>
          ))}
        </Select.Content>
      </Select.Root>

      <TextField.Root
        style={{ flex: 1 }}
        value={filter.value}
        onChange={handleValueChange}
        placeholder={placeholder}
        disabled={disabled}
      />

      <Tooltip content={isOnlyFilter ? 'At least one filter is required' : 'Remove filter'}>
        <IconButton
          variant="ghost"
          color="gray"
          onClick={handleRemove}
          disabled={disabled || isOnlyFilter}
          aria-label="Remove filter"
        >
          <X size={16} />
        </IconButton>
      </Tooltip>
    </Flex>
  );
}

export default CustomFilterRow;
