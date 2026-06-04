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

export interface RepositorySearchDropdownProps {
  value: string;
  onChange: (value: string) => void;
  repositories: readonly string[];
  disabled?: boolean;
  placeholder?: string;
}

/**
 * Repository dropdown with search input inside the menu.
 * Uses DropdownMenu (portal) so content is not clipped by parent overflow.
 */
export function RepositorySearchDropdown({
  value,
  onChange,
  repositories = [],
  disabled = false,
  placeholder = 'All repositories',
}: RepositorySearchDropdownProps): JSX.Element {
  const [searchQuery, setSearchQuery] = useState('');
  const [open, setOpen] = useState(false);

  const displayValue = value || placeholder;

  const filteredRepos = useMemo(() => {
    const query = searchQuery.toLowerCase().trim();
    const sorted = [...repositories].sort();
    if (!query) return sorted;
    return sorted.filter((repo) => repo.toLowerCase().includes(query));
  }, [repositories, searchQuery]);

  const handleSelect = useCallback(
    (repo: string) => {
      onChange(repo);
      setSearchQuery('');
      setOpen(false);
    },
    [onChange]
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
          aria-label={`Repository: ${displayValue}`}
          data-testid="repository-dropdown-trigger"
        >
          <span className="repository-search-dropdown__trigger-value">{displayValue}</span>
          <ChevronDown size={14} className="repository-search-dropdown__trigger-chevron" aria-hidden />
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
            placeholder="Search repositories..."
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
            All repositories
          </DropdownMenu.Item>
          {filteredRepos.map((repo) => (
            <DropdownMenu.Item key={repo} onClick={() => handleSelect(repo)}>
              {repo}
            </DropdownMenu.Item>
          ))}
          {filteredRepos.length === 0 && searchQuery && (
            <Box p="4" style={{ textAlign: 'center', color: 'var(--gray-11)' }}>
              No repositories matching &quot;{searchQuery}&quot;
            </Box>
          )}
        </Box>
      </DropdownMenu.Content>
    </DropdownMenu.Root>
  );
}
