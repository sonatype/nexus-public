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

import React, { useCallback } from 'react';
import { Box, Flex, Text, TextField } from '@radix-ui/themes';
import { Plus, Trash2, AlertCircle } from 'lucide-react';
import { SettingsButton } from '../../../../shared/form';

import { IqProperty, PropertyValidation } from './types';
import { generateId } from './propertyList';

import './PropertyListEditor.scss';

export interface PropertyListEditorProps {
  properties: IqProperty[];
  onChange: (properties: IqProperty[]) => void;
  onClearAll?: () => void;
  disabled?: boolean;
  validations?: PropertyValidation[];
  showAllValidation?: boolean;
}

/**
 * Parse pasted text into name/value pairs. Mirrors JdbcParameterEditor's
 * parsePastedPairs — duplicated rather than imported, since the two editors are
 * intentionally independent (no shared catalog/badge/type concerns between them).
 */
function parsePastedPairs(text: string): { name: string; value: string }[] {
  return text
    .split(/\r?\n/)
    .map((line) => {
      const [name, ...valueParts] = line.split('=');
      return { name: name?.trim() ?? '', value: valueParts.join('=').trim() };
    })
    .filter((pair) => pair.name);
}

function PropertyRow({
  property,
  validation,
  onChange,
  onRemove,
  onPasteExpand,
  disabled,
  showValidation,
}: {
  property: IqProperty;
  validation?: PropertyValidation;
  onChange: (updated: IqProperty) => void;
  onRemove: () => void;
  onPasteExpand: (pairs: { name: string; value: string }[]) => void;
  disabled?: boolean;
  showValidation?: boolean;
}) {
  const shouldShowErrors = showValidation;

  const handleNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChange({ ...property, name: e.target.value });
  };

  const handleValueChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChange({ ...property, value: e.target.value });
  };

  const handlePaste = (e: React.ClipboardEvent<HTMLInputElement>) => {
    const text = e.clipboardData.getData('text');
    if (!text.includes('=')) return;
    const pairs = parsePastedPairs(text);
    if (pairs.length === 0) return;
    e.preventDefault();
    onPasteExpand(pairs);
  };

  return (
    <Box className="property-list-row">
      <Flex gap="3" align="start" className="property-list-row__fields">
        <Box className="property-list-row__name">
          <TextField.Root
            value={property.name}
            onChange={handleNameChange}
            onPaste={handlePaste}
            placeholder="Property name..."
            disabled={disabled}
            className={shouldShowErrors && validation?.error ? 'property-list-row__input--error' : ''}
          />
          {shouldShowErrors && validation?.error && (
            <Flex align="center" gap="1" className="property-list-row__error">
              <AlertCircle size={12} />
              <Text size="1">{validation.error}</Text>
            </Flex>
          )}
        </Box>

        <Box className="property-list-row__value">
          <TextField.Root
            value={property.value}
            onChange={handleValueChange}
            placeholder="Enter value..."
            disabled={disabled}
          />
        </Box>

        <SettingsButton
          variant="ghost"
          onClick={onRemove}
          disabled={disabled}
          className="property-list-row__remove"
          aria-label="Remove property"
          icon={Trash2}
        />
      </Flex>
    </Box>
  );
}

/**
 * PropertyListEditor - Key-value editor for IQ Server "properties".
 *
 * Deliberately simpler than JdbcParameterEditor: there is no known catalog of valid IQ
 * property names anywhere in this codebase, so there's no autocomplete, no per-row
 * type/description, and no Default/Custom badge (nothing here is a "default").
 */
export function PropertyListEditor({
  properties,
  onChange,
  onClearAll,
  disabled = false,
  validations = [],
  showAllValidation = false,
}: PropertyListEditorProps) {
  const handlePropertyChange = useCallback((index: number, updated: IqProperty) => {
    const next = [...properties];
    next[index] = updated;
    onChange(next);
  }, [properties, onChange]);

  const handleRemove = useCallback((index: number) => {
    onChange(properties.filter((_, i) => i !== index));
  }, [properties, onChange]);

  // Atomically apply a pasted name=value list: the first pair replaces the pasted-into
  // row, any remaining pairs are inserted as new rows right after it — one onChange call,
  // matching JdbcParameterEditor's handlePasteExpand contract.
  const handlePasteExpand = useCallback((index: number, pairs: { name: string; value: string }[]) => {
    if (pairs.length === 0) return;
    const [first, ...rest] = pairs;

    const next = [...properties];
    next[index] = { ...next[index], name: first.name, value: first.value };
    next.splice(index + 1, 0, ...rest.map(({ name, value }) => ({ id: generateId(), name, value })));
    onChange(next);
  }, [properties, onChange]);

  const handleAdd = useCallback(() => {
    onChange([...properties, { id: generateId(), name: '', value: '' }]);
  }, [properties, onChange]);

  const getValidation = (id: string) => validations.find((v) => v.id === id);

  return (
    <Box className="property-list-editor">
      {properties.length > 0 ? (
        <Box className="property-list-editor__list">
          <Flex className="property-list-editor__header" gap="3">
            <Text size="2" weight="medium" className="property-list-editor__header-name">
              Name <Text as="span" color="red">*</Text>
            </Text>
            <Text size="2" weight="medium" className="property-list-editor__header-value">
              Value <Text as="span" color="red">*</Text>
            </Text>
            <Box className="property-list-editor__header-actions" />
          </Flex>

          {properties.map((property, index) => (
            <PropertyRow
              key={property.id}
              property={property}
              validation={getValidation(property.id)}
              onChange={(updated) => handlePropertyChange(index, updated)}
              onRemove={() => handleRemove(index)}
              onPasteExpand={(pairs) => handlePasteExpand(index, pairs)}
              disabled={disabled}
              showValidation={showAllValidation}
            />
          ))}
        </Box>
      ) : (
        <Box className="property-list-editor__empty">
          <Text size="2" color="gray">No properties configured</Text>
        </Box>
      )}

      <Flex gap="3" className="property-list-editor__actions">
        <SettingsButton variant="secondary" onClick={handleAdd} disabled={disabled} icon={Plus}>
          Add Parameter
        </SettingsButton>

        {onClearAll && properties.length > 0 && (
          <SettingsButton variant="ghost" onClick={onClearAll} disabled={disabled}>
            Clear All
          </SettingsButton>
        )}
      </Flex>
    </Box>
  );
}

export default PropertyListEditor;
