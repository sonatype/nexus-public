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

import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Box, Badge, Flex, RadioGroup, Table, Text } from '@radix-ui/themes';
import { Search, Loader2, AlertCircle, ArrowUp, ArrowDown, ChevronRight } from 'lucide-react';
import classnames from 'classnames';

import { SettingsCombobox, SettingsTransferList, SettingsAlert } from '../../../../shared/form';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { restClient } from '../../../../../../interface/api';
import { TaskType, TaskTypeSelectorProps } from './types';
import {
  TASK_TYPE_REPO_FILTERS,
  TASK_TYPE_BLOBSTORE_FILTERS,
  TaskBlobstoreFilter,
  isMultiRepoTask,
  resolveTaskFieldMeta,
  resolveDefaultScope,
  mdyToIso,
  isoToMdy,
  ALL_BLOB_STORES,
} from './taskFieldMetadata';
import { buildRepoQuery, deriveAllEntries, queryFromStaticFilter } from './taskRepoQuery';
import { humanizePropertyKey } from './useTasksApi';
import { getTaskTypeDescription, getTaskTypeCategory } from './taskTypeDescriptions';
import { TASK_TYPE_SELECTOR, DYNAMIC_FORM_FIELDS } from './TaskStrings';
import { PlanInformationWidget } from './PlanInformationWidget';
import { ReadOnlySelectedList } from './ReadOnlySelectedList';

import './TaskTypeSelector.scss';
import '../../../../shared/EntityTable/EntityTable.scss';

interface TaskTypeRow {
  id: string;
  name: string;
  category: string;
  description: string;
  exposed: boolean;
  original: TaskType;
}

/**
 * TaskTypeSelector - Flat table listing all available task types.
 */
export function TaskTypeSelector({
  taskTypes,
  onSelect,
  loading = false,
  error = null,
  selectedType,
}: TaskTypeSelectorProps) {
  const [searchTerm, setSearchTerm] = useState('');
  type SortableColumn = 'name' | 'category';
  const [sortBy, setSortBy] = useState<SortableColumn>('name');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');

  const rows: TaskTypeRow[] = useMemo(() => {
    let result = taskTypes.map(t => ({
      id: t.id,
      name: t.name,
      category: getTaskTypeCategory(t.id),
      description: getTaskTypeDescription(t.id),
      exposed: t.exposed,
      original: t,
    }));

    if (searchTerm) {
      const q = searchTerm.toLowerCase();
      result = result.filter(r =>
        r.name.toLowerCase().includes(q) ||
        r.category.toLowerCase().includes(q) ||
        r.description.toLowerCase().includes(q)
      );
    }

    result.sort((a, b) => {
      const cmp = a[sortBy].localeCompare(b[sortBy]);
      return sortDirection === 'asc' ? cmp : -cmp;
    });

    return result;
  }, [taskTypes, searchTerm, sortBy, sortDirection]);

  const handleSort = (column: SortableColumn) => {
    if (sortBy === column) {
      setSortDirection(prev => prev === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortDirection('asc');
    }
  };

  const handleRowClick = (row: TaskTypeRow) => {
    onSelect(row.original);
  };

  const renderSortIcon = (column: SortableColumn) => {
    if (sortBy !== column) return null;
    return sortDirection === 'asc'
      ? <ArrowUp size={14} aria-hidden="true" />
      : <ArrowDown size={14} aria-hidden="true" />;
  };

  if (loading) {
    return (
      <Flex align="center" justify="center" p="8">
        <Loader2 className="animate-spin" />
        <Text ml="2">{TASK_TYPE_SELECTOR.LOADING_MESSAGE}</Text>
      </Flex>
    );
  }

  if (error) {
    return (
      <Box className="task-type-selector__error">
        <AlertCircle size={20} />
        <Text size="2">{error}</Text>
      </Box>
    );
  }

  return (
    <Box className="task-type-selector">
      <Flex className="task-type-selector__search" align="center" justify="between">
        <div className="task-type-selector__search-wrapper">
          <Search size={16} className="task-type-selector__search-icon" />
          <input
            type="text"
            placeholder={TASK_TYPE_SELECTOR.FILTER_PLACEHOLDER}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="task-type-selector__search-input"
            autoComplete="off"
            data-testid="task-type-filter"
          />
        </div>
        <Text size="2" color="gray" className="task-type-selector__count">
          {TASK_TYPE_SELECTOR.COUNT(rows.length)}
        </Text>
      </Flex>

      {rows.length === 0 ? (
        <Flex justify="center" p="6">
          <Text color="gray">{TASK_TYPE_SELECTOR.EMPTY_FILTER}</Text>
        </Flex>
      ) : (
        <Table.Root className="entity-table" aria-label="Task types">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeaderCell
                className="entity-table__header entity-table__header--sortable entity-table__header--name"
                onClick={() => handleSort('name')}
              >
                <Flex align="center" gap="1">
                  <Text size="2" weight="medium">{TASK_TYPE_SELECTOR.COLUMN_NAME}</Text>
                  {renderSortIcon('name')}
                </Flex>
              </Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell
                className="entity-table__header entity-table__header--sortable entity-table__header--category"
                onClick={() => handleSort('category')}
              >
                <Flex align="center" gap="1">
                  <Text size="2" weight="medium">{TASK_TYPE_SELECTOR.COLUMN_CATEGORY}</Text>
                  {renderSortIcon('category')}
                </Flex>
              </Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell className="entity-table__header">
                <Text size="2" weight="medium">{TASK_TYPE_SELECTOR.COLUMN_DESCRIPTION}</Text>
              </Table.ColumnHeaderCell>
              <Table.ColumnHeaderCell className="entity-table__header--arrow" />
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {rows.map((row) => {
              const isSelected = selectedType?.id === row.id;
              return (
                <Table.Row
                  key={row.id}
                  className={classnames('entity-table__row', 'entity-table__row--clickable', {
                    'entity-table__row--selected': isSelected,
                  })}
                  onClick={() => handleRowClick(row)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      handleRowClick(row);
                    }
                  }}
                  tabIndex={0}
                  aria-selected={isSelected}
                  aria-label={`Select ${row.name}`}
                  data-testid={`task-type-row-${row.id}`}
                >
                  <Table.Cell>
                    <Flex align="center" gap="2">
                      <Text weight="medium" size="2">{row.name}</Text>
                      {row.exposed === false && (
                        <Badge size="1" color="purple" variant="soft">PRO</Badge>
                      )}
                    </Flex>
                  </Table.Cell>
                  <Table.Cell>{row.category}</Table.Cell>
                  <Table.Cell>
                    <Text size="2" color="gray" className="task-type-selector__description-cell">
                      {row.description}
                    </Text>
                  </Table.Cell>
                  <Table.Cell className="entity-table__cell entity-table__cell--arrow">
                    <ChevronRight size={16} className="entity-table__arrow" aria-hidden="true" />
                  </Table.Cell>
                </Table.Row>
              );
            })}
          </Table.Body>
        </Table.Root>
      )}
    </Box>
  );
}

/**
 * DynamicFormField - Renders a form field based on its type definition
 */
interface RepoOption {
  value: string;
  label: string;
  format?: string;
  type?: string;
}

interface DynamicFormFieldProps {
  field: NonNullable<TaskType['formFields']>[0];
  value: string;
  onChange: (value: string) => void;
  error?: string;
  disabled?: boolean;
  repoOptions?: RepoOption[];
  blobStoreOptions?: {value: string; label: string}[];
  showLabelInInput?: boolean;
  loadingRepoOptions?: boolean;
  /** Render the repository field as a multi-select transfer list (ItemselectFormField tasks). */
  multiSelect?: boolean;
  /** Owning task type id, so field UI metadata can be resolved with per-task overrides. */
  taskTypeId?: string;
}

export function DynamicFormField({
  field,
  value,
  onChange,
  error,
  disabled = false,
  repoOptions = [],
  blobStoreOptions = [],
  showLabelInInput = false,
  loadingRepoOptions = false,
  multiSelect = false,
  taskTypeId,
}: DynamicFormFieldProps) {
  // Apply UI overrides, preferring per-task-type metadata over the shared field-id default.
  const ui = resolveTaskFieldMeta(taskTypeId, field.id);
  const displayLabel = ui?.label || (field.label && field.label !== field.id ? field.label : humanizePropertyKey(field.id)).replace(/\s*\*\s*$/, '');
  const isRequired = Boolean(field.required);
  const fieldType = ui?.type || field.type || 'string';
  const helpText = ui?.helpText || field.helpText || '';
  const placeholder = ui?.placeholder || '';
  const helpId = helpText ? `dynamic-field-help-${field.id}` : undefined;
  const errorId = error ? `dynamic-field-error-${field.id}` : undefined;
  const describedBy = [errorId, helpId].filter(Boolean).join(' ') || undefined;

  // Render based on resolved field type (FIELD_UI overrides field.type)
  switch (fieldType) {
    case 'alertBanner':
      // Display-only banner (AlertBannerFormField). Copy is hard-coded in the field metadata
      // because the REST template does not carry it. No value/onChange participation.
      return (
        <SettingsAlert type={ui?.bannerVariant ?? 'info'}>{ui?.bannerText}</SettingsAlert>
      );

    case 'staticInfo': {
      // StaticInfoFormField → a read-only section header + help paragraph. Display-only: no value
      // or onChange participation. The descriptor uses a different label per edition for the same
      // field id (e.g. planOptionsLabelId), so prefer cloudLabel on the cloud edition.
      const isCloud = ExtJS.state()?.getValue('isCloud', false) === true;
      const sectionLabel = isCloud && ui?.cloudLabel ? ui.cloudLabel : displayLabel;
      return (
        <Box className="dynamic-field dynamic-field--static-info">
          {sectionLabel && (
            <Text as="div" size="3" weight="bold" className="dynamic-field__section-header">
              {sectionLabel}
            </Text>
          )}
          {helpText && (
            <Text as="p" size="2" color="gray" id={helpId} className="dynamic-field__help">
              {helpText}
            </Text>
          )}
        </Box>
      );
    }

    case 'planInformation':
      // PlanInformationFormField → read-only aggregate of /v1/plan. Display-only.
      return <PlanInformationWidget />;

    case 'taskScope': {
      // TaskScopeFormField → radio group toggling the Duration vs Start/End Dates inputs.
      // Radix RadioGroup provides roving-tabindex arrow-key navigation and focus management.
      const scopeValue = value || resolveDefaultScope(taskTypeId);
      const labelId = `dynamic-field-label-${field.id}`;
      return (
        <Box className="dynamic-field">
          {displayLabel && (
            <Text as="div" size="2" weight="medium" id={labelId} className="dynamic-field__label">
              {displayLabel}
            </Text>
          )}
          <RadioGroup.Root
            value={scopeValue}
            onValueChange={onChange}
            name={field.id}
            data-testid={`input-${field.id}`}
            aria-labelledby={displayLabel ? labelId : undefined}
            aria-describedby={helpId}
            disabled={disabled || field.disabled || field.readOnly || ui?.readOnly === true}
          >
            <Flex direction="column" gap="2" mt="1">
              <Text as="label" size="2" style={{ cursor: 'pointer' }}>
                <Flex align="center" gap="2">
                  <RadioGroup.Item value="duration" data-testid={`input-${field.id}-duration`} />
                  Duration
                </Flex>
              </Text>
              <Text as="label" size="2" style={{ cursor: 'pointer' }}>
                <Flex align="center" gap="2">
                  <RadioGroup.Item value="dates" data-testid={`input-${field.id}-dates`} />
                  Start/End Dates
                </Flex>
              </Text>
            </Flex>
          </RadioGroup.Root>
          {helpText && (
            <Text size="1" id={helpId} className="dynamic-field__help">{helpText}</Text>
          )}
        </Box>
      );
    }

    case 'itemselect': {
      // Read-only (Execute Data Repair Plan, Classic renderExecutePlanFields parity): show the stored
      // selection only — no Available column, no transfer controls, no editing.
      if (ui?.readOnly) {
        // For read-only display, preserve the "(All Blob Stores)" sentinel as a visible item rather
        // than stripping it (parseItemselectValues drops it, which is correct for the editable list
        // but wrong here — Classic shows "(All Blob Stores)" when no specific store is selected).
        const readOnlyValues = value ? value.split(',').map((v) => v.trim()).filter(Boolean) : [];
        return (
          <ReadOnlySelectedList
            label={displayLabel}
            helpText={helpText}
            values={readOnlyValues}
            emptyText={ui?.selectedEmptyText}
            testId={`input-${field.id}`}
          />
        );
      }
      // ItemselectFormField → dual-list transfer selector with a comma-separated string value
      // (the backend valueAsString contract). Blob-store selectors use blobStoreOptions; repository
      // selectors use repoOptions (already built/filtered by the parent DynamicFormFields).
      const isBlobStore = field.id === 'blobstoreName';
      const options = isBlobStore ? blobStoreOptions : repoOptions;
      // For the blob-store selector, "(All Blob Stores)" is a sentinel meaning "no specific selection"
      // (matches Classic withSelectionPlaceholderText(ENTRY_ALL_BLOB_STORES)). It is a default marker,
      // not a selectable item — so parseItemselectValues drops it, leaving the Selected column empty.
      const selectedValues = parseItemselectValues(value);
      const optionsByValue = new Map(options.map((option) => [option.value, option]));
      const selectedItems = selectedValues.map((v) =>
        optionsByValue.get(v) ?? {
          value: v,
          label: v === DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_VALUE
            ? DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL
            : v,
        }
      );
      return (
        <Box className="dynamic-field">
          <SettingsTransferList
            name={field.id}
            label={displayLabel}
            availableItems={options}
            selectedItems={selectedItems}
            onChange={(items: {value: string; label: string}[]) => {
              const joined = items.map((item) => item.value).join(',');
              // An empty blob-store selection reverts to the "(All Blob Stores)" sentinel so the
              // round-trip matches Classic (which defaults the field to that value).
              onChange(isBlobStore && joined === '' ? ALL_BLOB_STORES : joined);
            }}
            getItemId={(item: {value: string}) => item.value}
            getItemLabel={(item: {label: string}) => item.label}
            availableLabel="Available"
            selectedLabel="Selected"
            selectedEmptyText={ui?.selectedEmptyText}
            helpText={helpText}
            required={isRequired}
            error={error}
            disabled={disabled || field.disabled || field.readOnly}
            testId={`input-${field.id}`}
          />
        </Box>
      );
    }

    case 'checkbox':
      return (
        <Box className="dynamic-field">
          <label className="dynamic-field__checkbox">
            <input autoComplete="off"
              name={field.id}
              data-testid={`input-${field.id}`}
              type="checkbox"
              checked={value === 'true'}
              onChange={(e) => onChange(e.target.checked ? 'true' : 'false')}
              disabled={disabled || field.disabled || field.readOnly}
              aria-invalid={!!error}
              aria-describedby={describedBy}
            />
            <Text size="2">{displayLabel}</Text>
          </label>
          {helpText && (
            <Text size="1" id={helpId} className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" id={errorId} role="alert" className="dynamic-field__error">{error}</Text>
          )}
        </Box>
      );

    case 'password':
      return (
        <Box className="dynamic-field">
          <label className="dynamic-field__label">
            {displayLabel}
            {isRequired && <span className="dynamic-field__required">*</span>}
          </label>
          <input autoComplete="off"
            name={field.id}
            data-testid={`input-${field.id}`}
            type="password"
            value={value || ''}
            onChange={(e) => onChange(e.target.value)}
            placeholder={placeholder}
            disabled={disabled || field.disabled || field.readOnly}
            className="dynamic-field__input"
            aria-required={isRequired}
            aria-invalid={!!error}
            aria-describedby={describedBy}
          />
          {helpText && (
            <Text size="1" id={helpId} className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" id={errorId} role="alert" className="dynamic-field__error">{error}</Text>
          )}
        </Box>
      );

    case 'number':
      return (
        <Box className="dynamic-field">
          <label className="dynamic-field__label">
            {displayLabel}
            {isRequired && <span className="dynamic-field__required">*</span>}
          </label>
          <input autoComplete="off"
            name={field.id}
            data-testid={`input-${field.id}`}
            type="number"
            value={value || ''}
            onChange={(e) => onChange(e.target.value)}
            placeholder={placeholder}
            disabled={disabled || field.disabled || field.readOnly}
            min={ui?.min ?? (field.minValue !== undefined ? Number(field.minValue) : field.attributes?.minValue as number)}
            max={ui?.max ?? (field.maxValue !== undefined ? Number(field.maxValue) : field.attributes?.maxValue as number)}
            className="dynamic-field__input"
            aria-required={isRequired}
            aria-invalid={!!error}
            aria-describedby={describedBy}
          />
          {helpText && (
            <Text size="1" id={helpId} className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" id={errorId} role="alert" className="dynamic-field__error">{error}</Text>
          )}
        </Box>
      );

    case 'text':
      return (
        <Box className="dynamic-field">
          <label className="dynamic-field__label">
            {displayLabel}
            {isRequired && <span className="dynamic-field__required">*</span>}
          </label>
          <textarea
            name={field.id}
            data-testid={`input-${field.id}`}
            value={value || ''}
            onChange={(e) => onChange(e.target.value)}
            placeholder={placeholder}
            disabled={disabled || field.disabled || field.readOnly}
            className="dynamic-field__textarea"
            rows={ui?.rows ?? 4}
            aria-required={isRequired}
            aria-invalid={!!error}
            aria-describedby={describedBy}
          />
          {helpText && (
            <Text size="1" id={helpId} className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" id={errorId} role="alert" className="dynamic-field__error">{error}</Text>
          )}
        </Box>
      );

    case 'date':
      // <input type="date"> uses YYYY-MM-DD, but task date properties round-trip as the Classic
      // m/d/Y string. Convert on display and on change so Preview/Classic stay byte-compatible.
      return (
        <Box className="dynamic-field">
          <label className="dynamic-field__label">
            {displayLabel}
            {isRequired && <span className="dynamic-field__required">*</span>}
          </label>
          <input autoComplete="off"
            name={field.id}
            data-testid={`input-${field.id}`}
            type="date"
            value={mdyToIso(value)}
            onChange={(e) => onChange(isoToMdy(e.target.value))}
            disabled={disabled || field.disabled || field.readOnly || ui?.readOnly === true}
            className="dynamic-field__input"
            aria-required={isRequired}
            aria-invalid={!!error}
            aria-describedby={describedBy}
          />
          {helpText && (
            <Text size="1" id={helpId} className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" id={errorId} role="alert" className="dynamic-field__error">{error}</Text>
          )}
        </Box>
      );

    case 'url':
      return (
        <Box className="dynamic-field">
          <label className="dynamic-field__label">
            {displayLabel}
            {isRequired && <span className="dynamic-field__required">*</span>}
          </label>
          <input autoComplete="off"
            name={field.id}
            data-testid={`input-${field.id}`}
            type="url"
            value={value || ''}
            onChange={(e) => onChange(e.target.value)}
            placeholder={field.initialValue?.toString() || 'https://'}
            disabled={disabled || field.disabled || field.readOnly}
            className="dynamic-field__input"
            aria-required={isRequired}
            aria-invalid={!!error}
            aria-describedby={describedBy}
          />
          {helpText && (
            <Text size="1" id={helpId} className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" id={errorId} role="alert" className="dynamic-field__error">{error}</Text>
          )}
        </Box>
      );

    case 'repo':
    case 'repo-or-group': {
      // Multi-repository selectors (ItemselectFormField tasks) render a dual-list transfer
      // selector. The value is a comma-separated list of repository names (the backend's
      // valueAsString contract); split for display, join on change. The "*" (All
      // Repositories) entry and format/type filtering already come from `repoOptions`,
      // which the parent built via TASK_TYPE_REPO_FILTERS.
      if (multiSelect) {
        const selectedValues = value ? value.split(',').map((v) => v.trim()).filter(Boolean) : [];
        const optionsByValue = new Map(repoOptions.map((option) => [option.value, option]));
        const selectedItems = selectedValues.map((v) =>
          // Fall back to the friendly label for the "*" sentinel even when repoOptions has not
          // loaded yet (initial edit render), so the user never sees a raw "*" in the list.
          optionsByValue.get(v) ?? {
            value: v,
            label: v === DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_VALUE
              ? DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL
              : v,
          }
        );
        return (
          <Box className="dynamic-field">
            <SettingsTransferList
              name={field.id}
              label={displayLabel}
              availableItems={repoOptions}
              selectedItems={selectedItems}
              onChange={(items: {value: string; label: string}[]) =>
                onChange(items.map((item) => item.value).join(','))
              }
              getItemId={(item: {value: string}) => item.value}
              getItemLabel={(item: {label: string}) => item.label}
              availableLabel="Available"
              selectedLabel="Selected"
              helpText={helpText}
              required={isRequired}
              error={error}
              disabled={disabled || field.disabled || field.readOnly}
              testId={`input-${field.id}`}
            />
          </Box>
        );
      }
      // repoOptions is the final, already-filtered list supplied by the parent (built from the
      // descriptor's storeApi/storeFilters via the internal endpoint). Render it verbatim.
      return (
        <Box className="dynamic-field">
          <SettingsCombobox
            name={field.id}
            label={displayLabel}
            value={value || ''}
            onChange={onChange}
            options={repoOptions.map(({value: v, label: l}) => ({value: v, label: l}))}
            helpText={helpText}
            required={isRequired}
            disabled={disabled || field.disabled || field.readOnly}
            loading={loadingRepoOptions}
            placeholder="Select repository..."
            showLabelForValue={showLabelInInput}
          />
        </Box>
      );
    }

    case 'blobstore':
      return (
        <Box className="dynamic-field">
          <SettingsCombobox
            name={field.id}
            label={displayLabel}
            value={value || ''}
            onChange={onChange}
            options={blobStoreOptions}
            helpText={helpText}
            required={isRequired}
            disabled={disabled || field.disabled || field.readOnly}
            placeholder="Select blob store..."
          />
        </Box>
      );

    // Default to string input for string, combobox, and unknown types
    default: {
      // Smart detection: fields with "repository"/"blobstore" in the ID render as a
      // typed combobox — but ONLY when there's no explicit metadata. Once metadata
      // declares a type, that wins (e.g. external.metadata.repository.format is a
      // free-form text input despite the id containing "repository").
      const isRepoField = !ui && field.id.toLowerCase().includes('repository');
      const isBlobStoreField = !ui && field.id.toLowerCase().includes('blobstore');

      if (isRepoField) {
        // Smart-detected repo field: repoOptions is the final list supplied by the parent
        // (built from the descriptor's storeApi/storeFilters). Render it verbatim.
        return (
          <Box className="dynamic-field">
            <SettingsCombobox
              name={field.id}
              label={displayLabel}
              value={value || ''}
              onChange={onChange}
              options={repoOptions.map(({value: v, label: l}) => ({value: v, label: l}))}
              helpText={helpText}
              required={isRequired}
              disabled={disabled || field.disabled || field.readOnly}
              placeholder="Select repository..."
            />
          </Box>
        );
      }

      if (isBlobStoreField) {
        return (
          <Box className="dynamic-field">
            <SettingsCombobox
              name={field.id}
              label={displayLabel}
              value={value || ''}
              onChange={onChange}
              options={blobStoreOptions}
              helpText={helpText}
              required={isRequired}
              disabled={disabled || field.disabled || field.readOnly}
              placeholder="Select blob store..."
            />
          </Box>
        );
      }

      return (
        <Box className="dynamic-field">
          <label className="dynamic-field__label">
            {displayLabel}
            {isRequired && <span className="dynamic-field__required">*</span>}
          </label>
          <input autoComplete="off"
            name={field.id}
            data-testid={`input-${field.id}`}
            type="text"
            value={value || ''}
            onChange={(e) => onChange(e.target.value)}
            placeholder={placeholder}
            disabled={disabled || field.disabled || field.readOnly}
            className="dynamic-field__input"
            aria-required={isRequired}
            aria-invalid={!!error}
            aria-describedby={describedBy}
          />
          {helpText && (
            <Text size="1" id={helpId} className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" id={errorId} role="alert" className="dynamic-field__error">{error}</Text>
          )}
        </Box>
      );
    }
  }
}

const ALL_BLOB_STORES_OPTION = {
  value: DYNAMIC_FORM_FIELDS.ALL_BLOB_STORES_VALUE,
  label: DYNAMIC_FORM_FIELDS.ALL_BLOB_STORES_LABEL,
};

// Matches BlobStoreGroup.TYPE in the Java backend; used to drop group blobstores from
// dropdowns for descriptors that call readNoneGroupEntriesIncludingEntryForAll.
// This string is stable but should be kept in sync with the Java constant if it ever changes.
const BLOB_STORE_GROUP_TYPE = 'Group';

interface BlobStoreOption {
  value: string;
  label: string;
  type?: string;
}

function matchesBlobstoreFilter(
  option: BlobStoreOption,
  filter?: TaskBlobstoreFilter,
): boolean {
  if (!filter) return true;
  if (filter.excludeGroups && option.type === BLOB_STORE_GROUP_TYPE) return false;
  return true;
}

/**
 * Split a comma-separated itemselect value into trimmed, non-empty repository/blob-store names,
 * dropping the "(All Blob Stores)" sentinel (which represents "no specific selection").
 */
function parseItemselectValues(value: string | undefined): string[] {
  if (!value) return [];
  return value
    .split(',')
    .map((v) => v.trim())
    .filter((v) => v !== '' && v !== ALL_BLOB_STORES);
}

/**
 * DynamicFormFields - Renders all form fields for a task type
 */
interface DynamicFormFieldsProps {
  taskType: TaskType | null | undefined;
  values: Record<string, string>;
  onChange: (fieldId: string, value: string) => void;
  errors?: Record<string, string>;
  disabled?: boolean;
}

/**
 * A repository *option* field is one whose dropdown/transfer list is populated with repositories:
 * an explicit `repo` type, a non-blobstore `itemselect`, or an unmapped field whose id names a
 * repository. Blob-store selectors and non-repo fields are excluded. Used to decide which fields
 * need a per-field repository fetch.
 */
function isRepoOptionField(
  meta: { type?: string } | undefined,
  field: { id: string; type?: string },
): boolean {
  const t = meta?.type ?? field.type;
  if (t === 'repo') return true;
  if (t === 'itemselect') return field.id !== 'blobstoreName';
  const id = field.id.toLowerCase();
  return !meta && id.includes('repository') && !id.includes('blobstore');
}

export function DynamicFormFields({
  taskType,
  values,
  onChange,
  errors = {},
  disabled = false,
}: DynamicFormFieldsProps) {
  // Load reference data for combobox options
  const [repoOptions, setRepoOptions] = useState<RepoOption[]>([]);
  const [blobStoreOptions, setBlobStoreOptions] = useState<BlobStoreOption[]>([]);
  // Repository options per field id, fetched from the internal endpoint using the descriptor's
  // storeApi + storeFilters (see repoFetchFields). `undefined` for a field id means "still
  // loading"; `[]` means loaded-empty. The endpoint returns the already-filtered list plus any
  // (All)/(All <format>) synthetic entries, so it is rendered verbatim.
  const [repoOptionsByField, setRepoOptionsByField] = useState<Record<string, RepoOption[]>>({});

  // Cache for repo→assigned-blobstore lookups so we don't re-fetch on every render
  const repoBlobstoreCacheRef = useRef<Record<string, string>>({});
  const [repoBlobstoreMap, setRepoBlobstoreMap] = useState<Record<string, string>>({});

  // Fields that have a repo dependency (e.g. moveTargetBlobstore depends on moveRepositoryName)
  const repoDependencyFields = useMemo(() => {
    if (!taskType?.formFields) return [];
    return taskType.formFields
      .filter(f => resolveTaskFieldMeta(taskType.id, f.id)?.dependsOnRepo)
      .map(f => ({ fieldId: f.id, dependsOnField: resolveTaskFieldMeta(taskType.id, f.id)!.dependsOnRepo! }));
  }, [taskType?.id, taskType?.formFields]);

  // True when this task has a blob-store selector that filters the repository list by the
  // selected blob stores (Data Repair Plan, self-hosted). The repo list from /v1/repositories
  // lacks blobStoreName, so we additionally fetch /details which carries it.
  const needsBlobstoreFilter = useMemo(() => {
    if (!taskType?.formFields) return false;
    return taskType.formFields.some((f) => resolveTaskFieldMeta(taskType.id, f.id)?.filterByBlobstore);
  }, [taskType?.id, taskType?.formFields]);

  // name -> assigned blobStoreName for every repository, used to filter the repository selector
  // by the currently-selected blob stores (mirrors Classic filterRepositoryBySelectedBlobstore).
  const [repoBlobstoreDetails, setRepoBlobstoreDetails] = useState<{name: string; blobStoreName: string}[]>([]);

  // Re-fetch whenever task type changes so the list reflects repos created after initial load.
  // Fetches all repos with format/type so per-task-descriptor filters and visibleForRepoTypes
  // can be applied client-side without per-repo round-trips.
  useEffect(() => {
    restClient.get<{name: string; format?: string; type?: string}[]>('/service/rest/v1/repositories')
      .then((repos) => {
        if (Array.isArray(repos)) {
          setRepoOptions(
            repos
              .slice()
              .sort((a, b) => a.name.localeCompare(b.name))
              .map((r) => ({ value: r.name, label: r.name, format: r.format, type: r.type }))
          );
        }
      })
      .catch((err) => console.warn('Failed to load reference data:', err));

    if (!ExtJS.state()?.getValue('isCloud', false)) {
      // Fetch type alongside name so per-task filters (e.g. excludeGroups for
      // readNoneGroupEntriesIncludingEntryForAll) can be applied client-side.
      restClient.get<{name: string; type?: string}[]>('/service/rest/v1/blobstores')
        .then((stores) => {
          if (Array.isArray(stores)) {
            setBlobStoreOptions(
              stores.map((s) => ({ value: s.name, label: s.name, type: s.type })),
            );
          }
        })
        .catch((err) => console.warn('Failed to load reference data:', err));
    }
  }, [taskType?.id]);

  // Currently-selected repository name, used to drive visibleForRepoTypes field visibility
  const selectedRepoName = values.repositoryName ?? '';

  // Repository fields that need an option fetch: every repo-option field EXCEPT the Data Repair
  // Plan's blob-store-filtered repositoryName (handled at render time) and read-only itemselect
  // fields (their value is derived from the stored plan, not fetched). Each field's query is built
  // from the descriptor's storeApi + storeFilters; the static map is a fallback for templates that
  // omit storeApi (OSS/older builds).
  const repoFetchFields = useMemo(() => {
    if (!taskType?.id || !taskType.formFields) return [];
    const taskId = taskType.id;
    return taskType.formFields
      .filter((f) => {
        const meta = resolveTaskFieldMeta(taskId, f.id);
        if (meta?.readOnly) return false;
        if (needsBlobstoreFilter && f.id === 'repositoryName') return false;
        return isRepoOptionField(meta, f);
      })
      .map((f) => {
        // Descriptor is authoritative: storeApi ships even when there are no storeFilters.
        if (f.storeApi != null) {
          return { fieldId: f.id, query: buildRepoQuery(f.storeApi, f.storeFilters) };
        }
        // Legacy fallback (template without storeApi — OSS/older builds): synthesize from the
        // static map, honouring the field's UI-metadata defaults so an unmapped repository field
        // still behaves as before — includeFormatEntries => (All)/(All <format>) entries;
        // allowAll => an (All Repositories) entry.
        const staticFilter = TASK_TYPE_REPO_FILTERS[taskId]?.[f.id];
        let query = queryFromStaticFilter(staticFilter);
        if (!staticFilter && !query) {
          const meta = resolveTaskFieldMeta(taskId, f.id);
          if (meta?.includeFormatEntries) {
            query = 'withAll=true&withFormats=true';
          }
          else if (meta?.allowAll) {
            query = 'withAll=true';
          }
        }
        return { fieldId: f.id, query };
      });
  }, [taskType?.id, taskType?.formFields, needsBlobstoreFilter]);

  // Reset + fetch when the task type changes. Gated on taskType?.id only (repoFetchFields derives
  // from the same id + static metadata); a re-mount with the same id must not clear the map and
  // flash empty. The `cancelled` flag drops late handlers after a real switch or unmount.
  useEffect(() => {
    setRepoOptionsByField({});
    if (repoFetchFields.length === 0) return;
    let cancelled = false;
    repoFetchFields.forEach(({ fieldId, query }) => {
      restClient.get<{ id?: string; name?: string }[]>(
        `/service/rest/internal/ui/repositories${query ? `?${query}` : ''}`
      )
        .then((repos) => {
          if (cancelled || !Array.isArray(repos)) return;
          const options: RepoOption[] = repos
            .map((r) => ({ value: String(r.id ?? r.name ?? ''), label: String(r.name ?? r.id ?? '') }))
            .filter((o) => o.value);
          setRepoOptionsByField((prev) => ({ ...prev, [fieldId]: options }));
        })
        .catch((err) => { if (!cancelled) console.warn('Failed to load repository options:', err); });
    });
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [taskType?.id]);

  // Fetch the blobstore assigned to each selected dependency repo so we can filter it out
  useEffect(() => {
    repoDependencyFields.forEach(({ dependsOnField }) => {
      const repoName = values[dependsOnField];
      if (!repoName || repoName in repoBlobstoreCacheRef.current) return;

      repoBlobstoreCacheRef.current[repoName] = ''; // mark in-flight to prevent duplicate requests
      restClient.get<{ storage?: { blobStoreName?: string } }>(
        `/service/rest/internal/ui/repositories/repository/${encodeURIComponent(repoName)}`
      )
        .then((repo) => {
          const blobstore = repo?.storage?.blobStoreName;
          repoBlobstoreCacheRef.current[repoName] = blobstore ?? '';
          setRepoBlobstoreMap(prev => ({ ...prev, [repoName]: blobstore ?? '' }));
        })
        .catch(() => {
          delete repoBlobstoreCacheRef.current[repoName]; // allow retry on next render
        });
    });
  }, [repoDependencyFields, values]);

  // Load every repository's assigned blob store (name + blobStoreName) when this task filters the
  // repository list by blob store. /v1/repositories omits blobStoreName, so use the internal
  // /details endpoint. Cloud has no blob-store selector, so this never runs there.
  useEffect(() => {
    if (!needsBlobstoreFilter) {
      setRepoBlobstoreDetails([]);
      return;
    }
    let cancelled = false;
    restClient.get<{name: string; blobStoreName?: string}[]>('/service/rest/internal/ui/repositories/details')
      .then((repos) => {
        if (cancelled || !Array.isArray(repos)) return;
        setRepoBlobstoreDetails(
          repos
            .slice()
            .sort((a, b) => a.name.localeCompare(b.name))
            .map((r) => ({ name: r.name, blobStoreName: r.blobStoreName ?? '' }))
        );
      })
      .catch((err) => { if (!cancelled) console.warn('Failed to load repository blob-store details:', err); });
    return () => { cancelled = true; };
  }, [needsBlobstoreFilter, taskType?.id]);

  // When the blob-store selection changes, drop any already-selected repositories whose assigned
  // blob store is no longer in the selection (Classic filterRepositoryBySelectedBlobstore also
  // re-filters the selected side, not just the Available column). An empty blob-store selection
  // means "all blob stores", so nothing is pruned. The pruned value converges (a second pass
  // changes nothing), so this is safe even though `onChange` (whose identity changes with form
  // state) is a dependency — a redundant run is a cheap no-op.
  const selectedBlobstoreValue = values['blobstoreName'];
  const selectedRepoValue = values['repositoryName'];
  useEffect(() => {
    if (!needsBlobstoreFilter || repoBlobstoreDetails.length === 0) return;
    const selectedBlobstores = parseItemselectValues(selectedBlobstoreValue);
    if (selectedBlobstores.length === 0) return;
    const currentRepos = parseItemselectValues(selectedRepoValue);
    if (currentRepos.length === 0) return;
    const allowed = new Set(
      repoBlobstoreDetails
        .filter((r) => selectedBlobstores.includes(r.blobStoreName))
        .map((r) => r.name)
    );
    const pruned = currentRepos.filter((name) => allowed.has(name));
    if (pruned.length !== currentRepos.length) {
      onChange('repositoryName', pruned.join(','));
    }
  }, [needsBlobstoreFilter, repoBlobstoreDetails, selectedBlobstoreValue, selectedRepoValue, onChange]);

  const sortedFields = useMemo(
    () => [...(taskType?.formFields ?? [])].sort((a, b) => {
      const orderA = resolveTaskFieldMeta(taskType?.id, a.id)?.order ?? Infinity;
      const orderB = resolveTaskFieldMeta(taskType?.id, b.id)?.order ?? Infinity;
      return orderA - orderB;
    }),
    [taskType?.id, taskType?.formFields]
  );

  if (!taskType?.formFields || taskType.formFields.length === 0) {
    return null;
  }

  // Per-task descriptor override map (formats/types/includeAll keyed by task ID + field ID)
  // beats the field-id-only allowAll default in TASK_FIELD_UI for the repositoryName field.
  const taskRepoFilters = TASK_TYPE_REPO_FILTERS[taskType.id];
  // Per-task blobstore override map — mirrors taskRepoFilters but for blobstore selectors.
  // Encodes the differences between coreui_Blobstore.read / readWithAll /
  // readNoneGroupEntriesIncludingEntryForAll because the REST templates endpoint drops
  // that distinction.
  const taskBlobstoreFilters = TASK_TYPE_BLOBSTORE_FILTERS[taskType.id];

  return (
    <Flex direction="column" gap="3" className="dynamic-form-fields">
      {sortedFields.map((field) => {
        const meta = resolveTaskFieldMeta(taskType.id, field.id);

        if (meta?.hidden) return null;

        // Task-scope conditional visibility (Data Repair Plan): a field tied to a scope renders
        // only while that scope is active. Inactive-side values stay in form state (so they
        // reappear on switch-back) and are stripped at serialize time.
        if (meta?.scope && (values['taskScope'] || resolveDefaultScope(taskType.id)) !== meta.scope) {
          return null;
        }

        // Conditional visibility based on selected repository type (e.g. APT checkboxes).
        // Show the field when: no repo selected, "All Repositories" (*), repo not yet loaded,
        // or the known type is in the field's visibleForRepoTypes list.
        if (meta?.visibleForRepoTypes && selectedRepoName && selectedRepoName !== '*') {
          const knownType = repoOptions.find(r => r.value === selectedRepoName)?.type;
          if (knownType && !meta.visibleForRepoTypes.includes(knownType as 'hosted' | 'proxy' | 'group')) {
            return null;
          }
        }

        // Repository options come from the per-field descriptor-driven fetch (repoOptionsByField),
        // rendered verbatim — the endpoint already applied every filter and prepended the
        // (All)/(All <format>) entries. The Data Repair Plan is the one exception: its list is
        // filtered by the selected blob store at runtime (a Classic listener, not a storeFilter).
        let fieldRepoOptions: {value: string; label: string}[];
        if (needsBlobstoreFilter && field.id === 'repositoryName') {
          const selectedBlobstores = parseItemselectValues(values['blobstoreName']);
          const filtered = selectedBlobstores.length === 0
            ? repoBlobstoreDetails
            : repoBlobstoreDetails.filter((r) => selectedBlobstores.includes(r.blobStoreName));
          fieldRepoOptions = filtered.map((r) => ({ value: r.name, label: r.name }));
        }
        else {
          fieldRepoOptions = repoOptionsByField[field.id] ?? [];
        }

        // Dependency: disable this field until its referenced repo field has a value,
        // and filter out the blobstore already assigned to the selected repo.
        const depField = meta?.dependsOnRepo;
        const depRepoValue = depField ? (values[depField] || '') : '';
        const isDisabledByDependency = !!depField && !depRepoValue;
        const assignedBlobstore = depRepoValue ? repoBlobstoreMap[depRepoValue] : undefined;

        // Per-task descriptor override (e.g. blobstore.compact prepends "(All Blob Stores)",
        // blobstore.metrics.reconcile additionally drops Group-type blobstores).
        const blobstoreFilter = taskBlobstoreFilters?.[field.id];
        // Combined filter in single pass for performance
        const filteredBlobStores = blobStoreOptions
          .filter((opt) => matchesBlobstoreFilter(opt, blobstoreFilter) && (!assignedBlobstore || opt.value !== assignedBlobstore));
        const fieldBlobStoreOptions = blobstoreFilter?.includeAll
          ? [ALL_BLOB_STORES_OPTION, ...filteredBlobStores]
          : filteredBlobStores;

        // Show the option label (not the raw id) whenever the list carries synthetic entries whose
        // id differs from the label — the "(All Repositories)"/"(All <format>)" entries. Derived
        // from storeApi; falls back to the static map / allowAll default for legacy templates.
        const { withAll, withFormats } = deriveAllEntries(field.storeApi);
        const showRepoLabel = withAll || withFormats
          || (field.storeApi == null && (taskRepoFilters?.[field.id]?.includeAll === true
            || (taskRepoFilters?.[field.id] === undefined && meta?.allowAll === true)));

        return (
          <DynamicFormField
            key={field.id}
            field={field}
            value={values[field.id] || ''}
            onChange={(value) => onChange(field.id, value)}
            error={errors[field.id]}
            disabled={disabled || isDisabledByDependency}
            repoOptions={fieldRepoOptions}
            blobStoreOptions={fieldBlobStoreOptions}
            showLabelInInput={showRepoLabel}
            loadingRepoOptions={
              isRepoOptionField(meta, field)
              && !(needsBlobstoreFilter && field.id === 'repositoryName')
              && repoOptionsByField[field.id] === undefined
            }
            // ItemselectFormField selectors (per-task `multiSelect` override) or the shared
            // multi-repo repositoryName tasks render a dual-list transfer selector.
            multiSelect={!!meta?.multiSelect || (field.id === 'repositoryName' && isMultiRepoTask(taskType.id))}
            taskTypeId={taskType.id}
          />
        );
      })}
    </Flex>
  );
}
