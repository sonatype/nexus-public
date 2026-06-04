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

import React, { useState, useMemo, useCallback } from 'react';
import PropTypes from 'prop-types';
import { Box, Flex, IconButton, Text, TextField } from '@radix-ui/themes';
import { Search, ChevronRight, ChevronLeft, ChevronsRight, ChevronsLeft } from 'lucide-react';

import './SettingsTransferList.scss';

/**
 * SettingsTransferList - Dual-list selector for assigning items (roles, privileges, etc.)
 * 
 * @example
 * <SettingsTransferList
 *   name="roles"
 *   label="Granted Roles"
 *   availableItems={allRoles}
 *   selectedItems={grantedRoles}
 *   onChange={setGrantedRoles}
 *   availableLabel="Available Roles"
 *   selectedLabel="Granted Roles"
 *   getItemId={(item) => item.id}
 *   getItemLabel={(item) => item.name}
 * />
 */
/** Sanitize ID for use in data-testid (alphanumeric, hyphen, underscore only) */
function sanitizeTestId(id) {
  return String(id).replace(/[^a-zA-Z0-9-_]/g, '-');
}

export function SettingsTransferList({
  name: _name,
  label,
  availableItems = [],
  selectedItems = [],
  onChange,
  availableLabel = 'Available',
  selectedLabel = 'Selected',
  getItemId = (item) => item.id ?? item,
  getItemLabel = (item) => item.name ?? item.label ?? item,
  helpText = '',
  disabled = false,
  className = '',
  /** Optional testId for E2E; when provided, adds data-testid to root, search inputs, lists, and items */
  testId,
  /** Called when user clicks/selects an item (single click) - used for sidecar inspection */
  onItemSelect,
}) {
  const [availableSearch, setAvailableSearch] = useState('');
  const [selectedSearch, setSelectedSearch] = useState('');
  const [availableSelection, setAvailableSelection] = useState([]);
  const [selectedSelection, setSelectedSelection] = useState([]);

  // Filter available items (excluding selected)
  const selectedIds = useMemo(
    () => new Set(selectedItems.map(getItemId)),
    [selectedItems, getItemId]
  );

  const filteredAvailable = useMemo(() => {
    return availableItems
      .filter((item) => !selectedIds.has(getItemId(item)))
      .filter((item) => {
        const searchLower = availableSearch.toLowerCase();
        const label = getItemLabel(item);
        return !searchLower || label.toLowerCase().includes(searchLower);
      });
  }, [availableItems, selectedIds, availableSearch, getItemId, getItemLabel]);

  const filteredSelected = useMemo(() => {
    return selectedItems.filter((item) => {
      const searchLower = selectedSearch.toLowerCase();
      const label = getItemLabel(item);
      return !searchLower || label.toLowerCase().includes(searchLower);
    });
  }, [selectedItems, selectedSearch, getItemLabel]);

  // Handlers
  const handleAvailableClick = useCallback((item, e) => {
    e.stopPropagation();
    e.preventDefault();
    const id = getItemId(item);
    // Single click without modifier: notify parent for sidecar inspection (both Available and Granted)
    if (!e.ctrlKey && !e.metaKey && !e.shiftKey && onItemSelect) {
      onItemSelect(item, false);
    }
    if (e.ctrlKey || e.metaKey) {
      setAvailableSelection((prev) =>
        prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
      );
    } else if (e.shiftKey && availableSelection.length > 0) {
      // Shift-click range selection
      const lastId = availableSelection[availableSelection.length - 1];
      const lastIdx = filteredAvailable.findIndex((x) => getItemId(x) === lastId);
      const currentIdx = filteredAvailable.findIndex((x) => getItemId(x) === id);
      const start = Math.min(lastIdx, currentIdx);
      const end = Math.max(lastIdx, currentIdx);
      const range = filteredAvailable.slice(start, end + 1).map(getItemId);
      setAvailableSelection((prev) => [...new Set([...prev, ...range])]);
    } else {
      setAvailableSelection([id]);
    }
  }, [availableSelection, filteredAvailable, getItemId, onItemSelect]);

  const handleSelectedClick = useCallback((item, e) => {
    e.stopPropagation();
    e.preventDefault();
    const id = getItemId(item);
    // Single click without modifier: notify parent for sidecar inspection
    if (!e.ctrlKey && !e.metaKey && !e.shiftKey && onItemSelect) {
      onItemSelect(item, true);
    }
    if (e.ctrlKey || e.metaKey) {
      setSelectedSelection((prev) =>
        prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
      );
    } else if (e.shiftKey && selectedSelection.length > 0) {
      const lastId = selectedSelection[selectedSelection.length - 1];
      const lastIdx = filteredSelected.findIndex((x) => getItemId(x) === lastId);
      const currentIdx = filteredSelected.findIndex((x) => getItemId(x) === id);
      const start = Math.min(lastIdx, currentIdx);
      const end = Math.max(lastIdx, currentIdx);
      const range = filteredSelected.slice(start, end + 1).map(getItemId);
      setSelectedSelection((prev) => [...new Set([...prev, ...range])]);
    } else {
      setSelectedSelection([id]);
    }
  }, [selectedSelection, filteredSelected, getItemId, onItemSelect]);

  const moveToSelected = useCallback(() => {
    if (disabled || availableSelection.length === 0) return;
    const itemsToMove = availableItems.filter((item) =>
      availableSelection.includes(getItemId(item))
    );
    onChange([...selectedItems, ...itemsToMove]);
    setAvailableSelection([]);
  }, [availableItems, availableSelection, selectedItems, onChange, disabled, getItemId]);

  const moveToAvailable = useCallback(() => {
    if (disabled || selectedSelection.length === 0) return;
    onChange(selectedItems.filter((item) => !selectedSelection.includes(getItemId(item))));
    setSelectedSelection([]);
  }, [selectedItems, selectedSelection, onChange, disabled, getItemId]);

  const moveAllToSelected = useCallback(() => {
    if (disabled) return;
    const filtered = availableItems.filter((item) => !selectedIds.has(getItemId(item)));
    onChange([...selectedItems, ...filtered]);
    setAvailableSelection([]);
  }, [availableItems, selectedItems, selectedIds, onChange, disabled, getItemId]);

  const moveAllToAvailable = useCallback(() => {
    if (disabled) return;
    onChange([]);
    setSelectedSelection([]);
  }, [onChange, disabled]);

  const handleDoubleClick = useCallback((item, isSelected) => {
    if (disabled) return;
    const id = getItemId(item);
    if (isSelected) {
      onChange(selectedItems.filter((x) => getItemId(x) !== id));
      setSelectedSelection([]);
    } else {
      onChange([...selectedItems, item]);
      setAvailableSelection([]);
    }
  }, [selectedItems, onChange, disabled, getItemId]);

  return (
    <Box
      className={`settings-transfer-list ${disabled ? 'settings-transfer-list--disabled' : ''} ${className}`.trim()}
      data-testid={testId || undefined}
    >
      {label && (
        <Text as="label" size="2" weight="medium" className="settings-transfer-list__label">
          {label}
        </Text>
      )}
      
      <Flex className="settings-transfer-list__container">
        {/* Available List */}
        <Box className="settings-transfer-list__panel">
          <Flex justify="between" align="center" className="settings-transfer-list__header">
            <Text size="1" weight="medium">{availableLabel}</Text>
            <Text size="1" className="settings-transfer-list__count">
              {filteredAvailable.length} {filteredAvailable.length === 1 ? 'item' : 'items'}
            </Text>
          </Flex>
          <Box className="settings-transfer-list__search">
            <TextField.Root
              placeholder="Filter..."
              value={availableSearch}
              onChange={(e) => setAvailableSearch(e.target.value)}
              disabled={disabled}
              size="1"
              data-testid={testId ? `${testId}-available-search` : undefined}
            >
              <TextField.Slot>
                <Search size={14} />
              </TextField.Slot>
            </TextField.Root>
          </Box>
          <div
            className="settings-transfer-list__items"
            role="listbox"
            aria-label={availableLabel}
            data-testid={testId ? `${testId}-available-list` : undefined}
          >
            {filteredAvailable.map((item) => {
              const id = getItemId(item);
              const isHighlighted = availableSelection.includes(id);
              return (
                <div
                  key={id}
                  role="option"
                  aria-selected={isHighlighted}
                  tabIndex={0}
                  onClick={(e) => handleAvailableClick(item, e)}
                  onDoubleClick={() => handleDoubleClick(item, false)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleDoubleClick(item, false);
                  }}
                  className={`settings-transfer-list__item ${isHighlighted ? 'settings-transfer-list__item--selected' : ''}`}
                  data-testid={testId ? `${testId}-available-item-${sanitizeTestId(id)}` : undefined}
                >
                  {getItemLabel(item)}
                </div>
              );
            })}
            {filteredAvailable.length === 0 && (
              <Box className="settings-transfer-list__empty">
                <Text size="1" color="gray">{availableSearch ? 'No matches' : 'No items available'}</Text>
              </Box>
            )}
          </div>
        </Box>

        {/* Controls */}
        <Flex direction="column" align="center" justify="center" className="settings-transfer-list__controls">
          <IconButton
            type="button"
            variant="ghost"
            color="gray"
            size="1"
            onClick={moveAllToSelected}
            disabled={disabled || filteredAvailable.length === 0}
            aria-label="Move all to selected"
            title="Move all"
          >
            <ChevronsRight size={16} />
          </IconButton>
          <IconButton
            type="button"
            variant="ghost"
            color="gray"
            size="1"
            onClick={moveToSelected}
            disabled={disabled || availableSelection.length === 0}
            aria-label="Move selected to right"
            title="Move selected"
          >
            <ChevronRight size={16} />
          </IconButton>
          <IconButton
            type="button"
            variant="ghost"
            color="gray"
            size="1"
            onClick={moveToAvailable}
            disabled={disabled || selectedSelection.length === 0}
            aria-label="Move selected to left"
            title="Remove selected"
          >
            <ChevronLeft size={16} />
          </IconButton>
          <IconButton
            type="button"
            variant="ghost"
            color="gray"
            size="1"
            onClick={moveAllToAvailable}
            disabled={disabled || selectedItems.length === 0}
            aria-label="Move all to available"
            title="Remove all"
          >
            <ChevronsLeft size={16} />
          </IconButton>
        </Flex>

        {/* Selected List */}
        <Box className="settings-transfer-list__panel">
          <Flex justify="between" align="center" className="settings-transfer-list__header">
            <Text size="1" weight="medium">{selectedLabel}</Text>
            <Text size="1" className="settings-transfer-list__count">
              {selectedItems.length} {selectedItems.length === 1 ? 'item' : 'items'}
            </Text>
          </Flex>
          <Box className="settings-transfer-list__search">
            <TextField.Root
              placeholder="Filter..."
              value={selectedSearch}
              onChange={(e) => setSelectedSearch(e.target.value)}
              disabled={disabled}
              size="1"
              data-testid={testId ? `${testId}-selected-search` : undefined}
            >
              <TextField.Slot>
                <Search size={14} />
              </TextField.Slot>
            </TextField.Root>
          </Box>
          <div
            className="settings-transfer-list__items"
            role="listbox"
            aria-label={selectedLabel}
            data-testid={testId ? `${testId}-selected-list` : undefined}
          >
            {filteredSelected.map((item) => {
              const id = getItemId(item);
              const isHighlighted = selectedSelection.includes(id);
              return (
                <div
                  key={id}
                  role="option"
                  aria-selected={isHighlighted}
                  tabIndex={0}
                  onClick={(e) => handleSelectedClick(item, e)}
                  onDoubleClick={() => handleDoubleClick(item, true)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleDoubleClick(item, true);
                  }}
                  className={`settings-transfer-list__item ${isHighlighted ? 'settings-transfer-list__item--selected' : ''}`}
                  data-testid={testId ? `${testId}-selected-item-${sanitizeTestId(id)}` : undefined}
                >
                  {getItemLabel(item)}
                </div>
              );
            })}
            {filteredSelected.length === 0 && (
              <Box className="settings-transfer-list__empty">
                <Text size="1" color="gray">{selectedSearch ? 'No matches' : 'No items selected'}</Text>
              </Box>
            )}
          </div>
        </Box>
      </Flex>

      {helpText && (
        <Text as="p" size="1" className="settings-transfer-list__help">
          {helpText}
        </Text>
      )}
    </Box>
  );
}

SettingsTransferList.propTypes = {
  /** Field name */
  name: PropTypes.string.isRequired,
  /** Label displayed above the component */
  label: PropTypes.string,
  /** All available items to choose from */
  availableItems: PropTypes.array,
  /** Currently selected items */
  selectedItems: PropTypes.array,
  /** Called when selection changes (receives new selected array) */
  onChange: PropTypes.func,
  /** Label for available items list */
  availableLabel: PropTypes.string,
  /** Label for selected items list */
  selectedLabel: PropTypes.string,
  /** Function to get unique ID from item */
  getItemId: PropTypes.func,
  /** Function to get display label from item */
  getItemLabel: PropTypes.func,
  /** Help text displayed below */
  helpText: PropTypes.string,
  /** Disable the component */
  disabled: PropTypes.bool,
  /** Additional CSS class */
  className: PropTypes.string,
  /** Optional testId for E2E; adds data-testid to root, search inputs, lists, and items */
  testId: PropTypes.string,
  /** Called when user clicks an item (single click, no modifier). Args: (item, isSelected). Use for sidecar inspection. */
  onItemSelect: PropTypes.func,
};

export default SettingsTransferList;

