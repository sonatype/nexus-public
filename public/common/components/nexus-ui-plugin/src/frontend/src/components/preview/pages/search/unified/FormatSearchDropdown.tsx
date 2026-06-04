/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import React, { useCallback, useMemo, useState } from 'react';
import { Box, DropdownMenu, TextField } from '@radix-ui/themes';
import { ChevronDown, Search } from 'lucide-react';

import type { SearchFormat } from './unified.types';
import { FORMATS, FORMAT_ORDER } from './searchFilters';

export interface FormatSearchDropdownProps {
  value: SearchFormat | '';
  onChange: (value: SearchFormat | '') => void;
  availableFormats?: Set<string>;
  disabled?: boolean;
  placeholder?: string;
}

/**
 * Format dropdown with search input inside the menu.
 * Mirrors RepositorySearchDropdown pattern.
 */
export function FormatSearchDropdown({
  value,
  onChange,
  availableFormats,
  disabled = false,
  placeholder = 'All formats',
}: FormatSearchDropdownProps): JSX.Element {
  const [searchQuery, setSearchQuery] = useState('');
  const [open, setOpen] = useState(false);

  const displayValue = value ? FORMATS[value].label : placeholder;

  const formatOptions = useMemo(() => {
    return FORMAT_ORDER.filter((fmt) => {
      if (!availableFormats || availableFormats.size === 0) return true;
      const apiFormat = FORMATS[fmt].apiFormat.toLowerCase();
      return availableFormats.has(apiFormat);
    });
  }, [availableFormats]);

  const filteredFormats = useMemo(() => {
    const query = searchQuery.toLowerCase().trim();
    if (!query) return formatOptions;
    return formatOptions.filter((fmt) =>
      FORMATS[fmt].label.toLowerCase().includes(query),
    );
  }, [formatOptions, searchQuery]);

  const handleSelect = useCallback(
    (fmt: SearchFormat | '') => {
      onChange(fmt);
      setSearchQuery('');
      setOpen(false);
    },
    [onChange],
  );

  const handleOpenChange = useCallback((next: boolean) => {
    setOpen(next);
    if (!next) setSearchQuery('');
  }, []);

  return (
    <DropdownMenu.Root open={open} onOpenChange={handleOpenChange}>
      <DropdownMenu.Trigger>
        <button
          type="button"
          disabled={disabled}
          className="repository-search-dropdown__trigger"
          aria-haspopup="listbox"
          aria-expanded={open}
          aria-label={`Format: ${displayValue}`}
          data-testid="format-dropdown-trigger"
        >
          <span className="repository-search-dropdown__trigger-value">
            {displayValue}
          </span>
          <ChevronDown
            size={14}
            className="repository-search-dropdown__trigger-chevron"
            aria-hidden
          />
        </button>
      </DropdownMenu.Trigger>

      <DropdownMenu.Content
        align="start"
        side="bottom"
        sideOffset={4}
        avoidCollisions={false}
        className="repository-dropdown-content"
        style={{ minWidth: 280, maxWidth: 400 }}
        onCloseAutoFocus={(e) => e.preventDefault()}
      >
        <Box p="2" style={{ borderBottom: '1px solid var(--gray-6)' }}>
          <TextField.Root
            placeholder="Search formats..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={(e) => e.stopPropagation()}
            size="2"
            autoFocus
          >
            <TextField.Slot>
              <Search size={14} />
            </TextField.Slot>
          </TextField.Root>
        </Box>

        <Box style={{ maxHeight: 320, overflowY: 'auto' }}>
          <DropdownMenu.Item onClick={() => handleSelect('')}>
            All formats
          </DropdownMenu.Item>
          {filteredFormats.map((fmt) => (
            <DropdownMenu.Item key={fmt} onClick={() => handleSelect(fmt)}>
              {FORMATS[fmt].label}
            </DropdownMenu.Item>
          ))}
          {filteredFormats.length === 0 && searchQuery && (
            <Box p="4" style={{ textAlign: 'center', color: 'var(--gray-11)' }}>
              No formats matching &quot;{searchQuery}&quot;
            </Box>
          )}
        </Box>
      </DropdownMenu.Content>
    </DropdownMenu.Root>
  );
}
