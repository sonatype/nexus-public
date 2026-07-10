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
import { Box, Badge, Flex, Table, Text } from '@radix-ui/themes';
import { Search, Loader2, AlertCircle, ArrowUp, ArrowDown, ChevronRight } from 'lucide-react';
import classnames from 'classnames';

import { SettingsCombobox } from '../../../../shared/form';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { restClient } from '../../../../../../interface/api';
import { TaskType, TaskTypeSelectorProps } from './types';
import { TASK_FIELD_UI, TASK_TYPE_REPO_FILTERS, TaskRepoFilter } from './taskFieldMetadata';
import { humanizePropertyKey } from './useTasksApi';
import { getTaskTypeDescription, getTaskTypeCategory } from './taskTypeDescriptions';
import { TASK_TYPE_SELECTOR, DYNAMIC_FORM_FIELDS } from './TaskStrings';

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
            autoFocus
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
interface DynamicFormFieldProps {
  field: NonNullable<TaskType['formFields']>[0];
  value: string;
  onChange: (value: string) => void;
  error?: string;
  disabled?: boolean;
  repoOptions?: {value: string; label: string}[];
  blobStoreOptions?: {value: string; label: string}[];
  showLabelInInput?: boolean;
  loadingRepoOptions?: boolean;
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
}: DynamicFormFieldProps) {
  const handleChange = (newValue: string) => {
    onChange(newValue);
  };

  // Apply UI overrides from FIELD_UI map (the single source of truth)
  const ui = TASK_FIELD_UI[field.id];
  const displayLabel = ui?.label || (field.label && field.label !== field.id ? field.label : humanizePropertyKey(field.id)).replace(/\s*\*\s*$/, '');
  // field.required is computed by restTemplateToTaskType (TASK_FIELD_UI metadata
  // applied — non-required fields, checkboxes, and hidden fields all yield false).
  const isRequired = field.required === true;
  const fieldType = ui?.type || field.type || 'string';
  const helpText = ui?.helpText || '';
  const placeholder = ui?.placeholder || '';
  const helpId = helpText ? `dynamic-field-help-${field.id}` : undefined;
  const errorId = error ? `dynamic-field-error-${field.id}` : undefined;
  const describedBy = [errorId, helpId].filter(Boolean).join(' ') || undefined;

  // Render based on resolved field type (FIELD_UI overrides field.type)
  switch (fieldType) {
    case 'checkbox':
      return (
        <Box className="dynamic-field">
          <label className="dynamic-field__checkbox">
            <input autoComplete="off"
              name={field.id}
              data-testid={`input-${field.id}`}
              type="checkbox"
              checked={value === 'true'}
              onChange={(e) => handleChange(e.target.checked ? 'true' : 'false')}
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
            onChange={(e) => handleChange(e.target.value)}
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
            onChange={(e) => handleChange(e.target.value)}
            placeholder={placeholder}
            disabled={disabled || field.disabled || field.readOnly}
            min={ui?.min ?? field.attributes?.minValue as number}
            max={ui?.max ?? field.attributes?.maxValue as number}
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
            onChange={(e) => handleChange(e.target.value)}
            placeholder={placeholder}
            disabled={disabled || field.disabled || field.readOnly}
            className="dynamic-field__textarea"
            rows={4}
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
            value={value || ''}
            onChange={(e) => handleChange(e.target.value)}
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
            onChange={(e) => handleChange(e.target.value)}
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
    case 'repo-or-group':
      return (
        <Box className="dynamic-field">
          <SettingsCombobox
            name={field.id}
            label={displayLabel}
            value={value || ''}
            onChange={handleChange}
            options={repoOptions}
            helpText={helpText}
            required={isRequired}
            disabled={disabled || field.disabled || field.readOnly}
            loading={loadingRepoOptions}
            placeholder="Select repository..."
            showLabelForValue={showLabelInInput}
          />
        </Box>
      );

    case 'blobstore':
      return (
        <Box className="dynamic-field">
          <SettingsCombobox
            name={field.id}
            label={displayLabel}
            value={value || ''}
            onChange={handleChange}
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
        // showLabelInInput is intentionally not forwarded here — smart-detected repo fields
        // use plain repository names as both value and label. Only fields with includeFormatEntries
        // (e.g. restrictComponentDelete) need showLabelForValue because their values are synthetic
        // format-group IDs that differ from the display label.
        return (
          <Box className="dynamic-field">
            <SettingsCombobox
              name={field.id}
              label={displayLabel}
              value={value || ''}
              onChange={handleChange}
              options={repoOptions}
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
              onChange={handleChange}
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
            onChange={(e) => handleChange(e.target.value)}
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

const ALL_REPOS_OPTION = {
  value: DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_VALUE,
  label: DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL,
};

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

interface RepoOption {
  value: string;
  label: string;
  format?: string;
  type?: string;
}

function matchesRepoFilter(repo: RepoOption, filter?: TaskRepoFilter): boolean {
  if (!filter) return true;
  if (filter.formats && filter.formats.length > 0 && !filter.formats.includes(repo.format || '')) {
    return false;
  }
  if (filter.types && filter.types.length > 0 && !filter.types.includes(repo.type || '')) {
    return false;
  }
  return true;
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
  const [blobStoreOptions, setBlobStoreOptions] = useState<{value: string; label: string}[]>([]);
  // Server-filtered repo lists keyed by field id, populated only for fields that
  // declare a `facets` filter in TASK_TYPE_REPO_FILTERS (the format/type filter
  // alone can't reproduce a facet-based descriptor filter — see TaskRepoFilter).
  const [serverFilteredRepos, setServerFilteredRepos] = useState<Record<string, RepoOption[]>>({});

  // Cache for repo→assigned-blobstore lookups so we don't re-fetch on every render
  const repoBlobstoreCacheRef = useRef<Record<string, string>>({});
  const [repoBlobstoreMap, setRepoBlobstoreMap] = useState<Record<string, string>>({});

  const [repoOptionsWithFormats, setRepoOptionsWithFormats] = useState<{value: string; label: string}[]>([]);
  const [isLoadingWithFormats, setIsLoadingWithFormats] = useState(false);

  // Fields that have a repo dependency (e.g. moveTargetBlobstore depends on moveRepositoryName)
  const repoDependencyFields = useMemo(() => {
    if (!taskType?.formFields) return [];
    return taskType.formFields
      .filter(f => TASK_FIELD_UI[f.id]?.dependsOnRepo)
      .map(f => ({ fieldId: f.id, dependsOnField: TASK_FIELD_UI[f.id]!.dependsOnRepo! }));
  }, [taskType?.formFields]);

  const needsWithFormats = useMemo(() => {
    if (!taskType?.formFields) return false;
    return taskType.formFields.some((f) => TASK_FIELD_UI[f.id]?.includeFormatEntries);
  }, [taskType?.formFields]);

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
      restClient.get<{name: string}[]>('/service/rest/v1/blobstores')
        .then((stores) => {
          if (Array.isArray(stores)) {
            setBlobStoreOptions(stores.map((s) => ({ value: s.name, label: s.name })));
          }
        })
        .catch((err) => console.warn('Failed to load reference data:', err));
    }
  }, [taskType?.id]);

  // Main's `needsWithFormats` fetch: tags.cleanup's restrictComponentDelete renders
  // `(All <format> Repositories)` entries in the dropdown.
  useEffect(() => {
    if (!needsWithFormats) {
      setRepoOptionsWithFormats([]);
      setIsLoadingWithFormats(false);
      return;
    }
    let cancelled = false;
    setIsLoadingWithFormats(true);
    restClient
      .get<{id: string; name: string}[]>('/service/rest/internal/ui/repositories?withFormats=true&withAll=true')
      .then((items) => {
        if (cancelled || !Array.isArray(items)) return;
        // Filter out any API-returned all-repos entry (id: '*') before prepending our own
        // constant, preventing duplicates. This assumes the API uses '*' as the all-repos id.
        const mapped = items
          .filter((it) => it.id !== DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_VALUE)
          .map((it) => ({value: it.id, label: it.name}));
        setRepoOptionsWithFormats([
          {value: DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_VALUE, label: DYNAMIC_FORM_FIELDS.ALL_REPOSITORIES_LABEL},
          ...mapped,
        ]);
      })
      .catch((err) => { if (!cancelled) console.warn('Failed to load with-formats repository list:', err); })
      .finally(() => { if (!cancelled) setIsLoadingWithFormats(false); });
    return () => { cancelled = true; };
  }, [needsWithFormats, taskType?.id]);

  // Currently-selected repository name, used to drive visibleForRepoTypes field visibility
  const selectedRepoName = values['repositoryName'] ?? '';

  // Fields whose descriptor uses a server-only filter (facets or Maven versionPolicies).
  // `/v1/repositories` doesn't expose either, so we hit the internal endpoint that does.
  const serverFilteredFields = useMemo(() => {
    if (!taskType?.id || !taskType.formFields) return [];
    const taskFilters = TASK_TYPE_REPO_FILTERS[taskType.id];
    if (!taskFilters) return [];
    return taskType.formFields
      .map((f) => ({ fieldId: f.id, filter: taskFilters[f.id] }))
      .filter((entry): entry is { fieldId: string; filter: TaskRepoFilter } => {
        const f = entry.filter;
        return !!f && (
          (!!f.facets && f.facets.length > 0) ||
          (!!f.versionPolicies && f.versionPolicies.length > 0)
        );
      });
  }, [taskType?.id, taskType?.formFields]);

  // Reset and re-fetch server-filtered lists when the task type changes.
  // Gate on `taskType?.id` only — the serverFilteredFields memo depends on the same
  // id, and TASK_TYPE_REPO_FILTERS is static, so a re-mount with the same id (e.g.
  // XState context hydration creating a new formFields array reference) must NOT
  // trigger setServerFilteredRepos({}) — otherwise the dropdown flashes empty until
  // the fetch resolves. Mirrors the `cancelled` pattern from the needsWithFormats
  // effect above so a real taskType switch (or unmount) drops late .then/.catch
  // handlers instead of calling setState on a stale component.
  useEffect(() => {
    setServerFilteredRepos({});
    if (serverFilteredFields.length === 0) return;

    let cancelled = false;
    serverFilteredFields.forEach(({ fieldId, filter }) => {
      const params = new URLSearchParams();
      if (filter.facets?.length) params.set('facets', filter.facets.join(','));
      if (filter.formats?.length) params.set('format', filter.formats.join(','));
      if (filter.types?.length) params.set('type', filter.types.join(','));
      if (filter.versionPolicies?.length) params.set('versionPolicies', filter.versionPolicies.join(','));

      restClient.get<{id?: string; name?: string}[]>(
        `/service/rest/internal/ui/repositories?${params.toString()}`
      )
        .then((repos) => {
          if (cancelled || !Array.isArray(repos)) return;
          const options: RepoOption[] = repos
            .map((r) => ({ value: String(r.id ?? r.name ?? ''), label: String(r.name ?? r.id ?? '') }))
            .filter((o) => o.value);
          setServerFilteredRepos((prev) => ({ ...prev, [fieldId]: options }));
        })
        .catch((err) => { if (!cancelled) console.warn('Failed to load server-filtered repositories:', err); });
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

  const sortedFields = useMemo(
    () => [...(taskType?.formFields ?? [])].sort((a, b) => {
      const orderA = TASK_FIELD_UI[a.id]?.order ?? Infinity;
      const orderB = TASK_FIELD_UI[b.id]?.order ?? Infinity;
      return orderA - orderB;
    }),
    [taskType?.formFields]
  );

  if (!taskType?.formFields || taskType.formFields.length === 0) {
    return null;
  }

  // Per-task descriptor override map (formats/types/includeAll keyed by task ID + field ID)
  // beats the field-id-only allowAll default in TASK_FIELD_UI for the repositoryName field.
  const taskRepoFilters = TASK_TYPE_REPO_FILTERS[taskType.id];

  return (
    <Flex direction="column" gap="3" className="dynamic-form-fields">
      {sortedFields.map((field) => {
        const meta = TASK_FIELD_UI[field.id];

        if (meta?.hidden) return null;

        // Conditional visibility based on selected repository type (e.g. APT checkboxes).
        // Show the field when: no repo selected, "All Repositories" (*), repo not yet loaded,
        // or the known type is in the field's visibleForRepoTypes list.
        if (meta?.visibleForRepoTypes && selectedRepoName && selectedRepoName !== '*') {
          const knownType = repoOptions.find(r => r.value === selectedRepoName)?.type;
          if (knownType && !meta.visibleForRepoTypes.includes(knownType as 'hosted' | 'proxy' | 'group')) {
            return null;
          }
        }

        // Three repo-options sources, picked per field:
        //  • includeFormatEntries (e.g. restrictComponentDelete) → withFormats API list
        //    that already contains "(All <format> repositories)" entries.
        //  • facets/versionPolicies filter → serverFilteredRepos[field.id] (the internal
        //    /repositories endpoint applies the filter server-side).
        //  • everything else → filterable client-side repo list, optionally narrowed by
        //    a per-task descriptor filter (TASK_TYPE_REPO_FILTERS) and prepended with
        //    (All Repositories).
        let fieldRepoOptions: {value: string; label: string}[];
        if (meta?.includeFormatEntries) {
          fieldRepoOptions = repoOptionsWithFormats;
        }
        else {
          const repoFilter = taskRepoFilters?.[field.id];
          const needsServerFilter = !!repoFilter && (
            (!!repoFilter.facets && repoFilter.facets.length > 0) ||
            (!!repoFilter.versionPolicies && repoFilter.versionPolicies.length > 0)
          );
          // While the server response is in flight, render an empty list rather than
          // the unfiltered set so users never see disallowed repos.
          const baseRepos = needsServerFilter
            ? (serverFilteredRepos[field.id] ?? [])
            : repoFilter
              ? repoOptions.filter((repo) => matchesRepoFilter(repo, repoFilter))
              : repoOptions;
          const includeAll = repoFilter ? repoFilter.includeAll === true : meta?.allowAll === true;
          fieldRepoOptions = includeAll
            ? [ALL_REPOS_OPTION, ...baseRepos]
            : baseRepos;
        }

        // Dependency: disable this field until its referenced repo field has a value,
        // and filter out the blobstore already assigned to the selected repo.
        const depField = meta?.dependsOnRepo;
        const depRepoValue = depField ? (values[depField] || '') : '';
        const isDisabledByDependency = !!depField && !depRepoValue;
        const assignedBlobstore = depRepoValue ? repoBlobstoreMap[depRepoValue] : undefined;
        const fieldBlobStoreOptions = assignedBlobstore
          ? blobStoreOptions.filter(opt => opt.value !== assignedBlobstore)
          : blobStoreOptions;

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
            showLabelInInput={
              !!meta?.includeFormatEntries
              || taskRepoFilters?.[field.id]?.includeAll === true
              || (taskRepoFilters?.[field.id] === undefined && meta?.allowAll === true)
            }
            loadingRepoOptions={meta?.includeFormatEntries ? isLoadingWithFormats : false}
          />
        );
      })}
    </Flex>
  );
}
