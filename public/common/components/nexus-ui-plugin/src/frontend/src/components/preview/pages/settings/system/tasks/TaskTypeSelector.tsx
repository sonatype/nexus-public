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

import React, { useMemo, useState, useEffect } from 'react';
import { Box, Badge, Flex, Text, Heading } from '@radix-ui/themes';
import {
  Search, ChevronRight, Loader2, AlertCircle,
  Settings, Database, Trash2, HeartPulse, Tag, MoreHorizontal, ListTodo
} from 'lucide-react';

import { SettingsCombobox } from '../../../../shared/form';
import { ExtJS } from '../../../../../../interface/ExtJS';
import { restClient } from '../../../../../../interface/api';
import { TaskType, TaskTypeSelectorProps } from './types';
import { TASK_FIELD_UI } from './taskFieldMetadata';
import { getTaskTypeDescription, getTaskTypeCategory } from './taskTypeDescriptions';
import { FormatIcon } from '../../repository/repositories/components/FormatIcon';

import './TaskTypeSelector.scss';
import '../../repository/repositories/RepositoryTypeSelector.scss';

/**
 * Icon mapping for non-format task categories
 */
const CATEGORY_ICONS: Record<string, React.ComponentType<{size?: number; className?: string}>> = {
  Admin: Settings,
  Repository: Database,
  Cleanup: Trash2,
  'Health Check': HeartPulse,
  Tags: Tag,
  Other: MoreHorizontal,
};

/**
 * Map of category names to FormatIcon identifiers
 */
const CATEGORY_FORMAT_MAP: Record<string, string> = {
  Maven: 'maven2',
  Docker: 'docker',
  npm: 'npm',
  Yum: 'yum',
  APT: 'apt',
  Helm: 'helm',
  R: 'r',
  PyPI: 'pypi',
  Conda: 'conda',
  RubyGems: 'rubygems',
  Go: 'go',
  P2: 'p2',
  Conan: 'conan',
  NuGet: 'nuget',
  CocoaPods: 'cocoapods',
  Raw: 'raw',
};

/**
 * TaskTypeSelector - Multi-stage wizard for selecting task category and type.
 */
export function TaskTypeSelector({
  taskTypes,
  onSelect,
  loading = false,
  error = null,
  mode,
  selectedCategory: initialCategory,
  onCategorySelect,
  onSelectionChange,
}: TaskTypeSelectorProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(initialCategory || null);
  const [selectedType, setSelectedType] = useState<TaskType | null>(null);

  // Sync internal state with prop
  useEffect(() => {
    if (initialCategory !== undefined) {
      setSelectedCategory(initialCategory);
      if (!initialCategory) {
        setSelectedType(null);
      }
    }
  }, [initialCategory]);

  const effectiveMode = mode || (selectedCategory ? 'type' : 'category');

  // Unique list of categories
  const availableCategories = useMemo(() => {
    const categories = new Set(taskTypes.map(t => getTaskTypeCategory(t.id)));
    let result = Array.from(categories).sort((a, b) => a.localeCompare(b));

    if (searchTerm && effectiveMode === 'category') {
      const q = searchTerm.toLowerCase();
      result = result.filter(cat => 
        cat.toLowerCase().includes(q) ||
        taskTypes.some(t => 
          getTaskTypeCategory(t.id) === cat && 
          (t.name.toLowerCase().includes(q) || getTaskTypeDescription(t.id).toLowerCase().includes(q))
        )
      );
    }
    return result;
  }, [taskTypes, searchTerm, effectiveMode]);

  // Types available for the selected category
  const availableTypesForCategory = useMemo(() => {
    if (!selectedCategory) return [];
    let result = taskTypes.filter(t => getTaskTypeCategory(t.id) === selectedCategory);

    if (searchTerm && effectiveMode === 'type') {
      const q = searchTerm.toLowerCase();
      result = result.filter(t => 
        t.name.toLowerCase().includes(q) || 
        getTaskTypeDescription(t.id).toLowerCase().includes(q)
      );
    }

    return result.sort((a, b) => a.name.localeCompare(b.name));
  }, [selectedCategory, taskTypes, searchTerm, effectiveMode]);

  const handleCategoryClick = (category: string) => {
    setSelectedCategory(category);
    setSelectedType(null);
    if (onCategorySelect) onCategorySelect(category);
    if (onSelectionChange) {
      onSelectionChange(true, null); // Selected category, but no type yet
    }
  };

  const handleTypeClick = (type: TaskType) => {
    setSelectedType(type);
    if (onSelectionChange) onSelectionChange(true, type);
    // If not controlled by wizard (e.g. legacy mode), advance now
    if (!mode) {
      onSelect(type);
    }
  };

  if (loading) {
    return (
      <Flex align="center" justify="center" p="8">
        <Loader2 className="animate-spin" />
        <Text ml="2">Loading task types...</Text>
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

  // --- STAGE 2: SELECT TASK TYPE ---
  if (effectiveMode === 'type' && selectedCategory) {
    return (
      <Box className="task-type-selector">
        <Box mb="6">
          {!mode && (
            <button type="button" 
              onClick={() => {
                setSelectedCategory(null);
                if (onCategorySelect) onCategorySelect(null);
                if (onSelectionChange) onSelectionChange(false, null);
              }} 
              className="repository-type-selector__back-link"
            >
              &larr; Back to categories
            </button>
          )}
          <Flex align="center" gap="3" mt={mode ? '0' : '2'}>
            {CATEGORY_FORMAT_MAP[selectedCategory] ? (
              <FormatIcon format={CATEGORY_FORMAT_MAP[selectedCategory]} size={48} />
            ) : (
              <Box className="repository-type-selector__type-icon repository-type-selector__type-icon--hosted">
                {React.createElement(CATEGORY_ICONS[selectedCategory] || ListTodo, { size: 24 })}
              </Box>
            )}
            <Box>
              <Heading size="5">{selectedCategory}</Heading>
              <Text size="2" color="gray">Available tasks for {selectedCategory}</Text>
            </Box>
          </Flex>
        </Box>

        <Box className="repository-type-selector__search">
          <div className="repository-type-selector__search-wrapper">
            <Search size={16} className="repository-type-selector__search-icon" />
            <input
              type="text"
              placeholder={`Search ${selectedCategory} tasks...`}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="repository-type-selector__search-input"
              autoFocus
              autoComplete="off"
            />
          </div>
        </Box>

        <Box className="repository-type-selector__grid repository-type-selector__grid--types">
          {availableTypesForCategory.map(type => {
            const isSelected = selectedType?.id === type.id;
            const description = getTaskTypeDescription(type.id);
            
            return (
              <button type="button"
                key={type.id}
                className={`repository-type-selector__card ${isSelected ? 'repository-type-selector__card--selected' : ''}`}
                onClick={() => handleTypeClick(type)}
              >
                <div style={{ display: 'flex', alignItems: 'start', gap: '16px' }}>
                  <Box style={{ flex: 1 }}>
                    <Flex align="center" gap="2" mb="1">
                      <Text weight="bold" size="3">
                        {type.name}
                      </Text>
                      {type.exposed === false && (
                        <Badge size="1" color="purple" variant="soft">PRO</Badge>
                      )}
                    </Flex>
                    <Text size="2" color="gray" style={{ lineHeight: '1.4', display: 'block' }}>
                      {description}
                    </Text>
                  </Box>
                </div>
              </button>
            );
          })}
        </Box>
      </Box>
    );
  }

  // --- STAGE 1: SELECT CATEGORY ---
  return (
    <Box className="task-type-selector">
      <Box className="repository-type-selector__search">
        <div className="repository-type-selector__search-wrapper">
          <Search size={16} className="repository-type-selector__search-icon" />
          <input
            type="text"
            placeholder="Search task category..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="repository-type-selector__search-input"
            autoFocus
            autoComplete="off"
          />
        </div>
        <Text size="2" color="gray">
          {availableCategories.length} categories available
        </Text>
      </Box>

      <Box className="repository-type-selector__grid">
        {availableCategories.map(category => (
          <button type="button"
            key={category}
            className={`repository-type-selector__format-card ${selectedCategory === category ? 'repository-type-selector__format-card--selected' : ''}`}
            onClick={() => handleCategoryClick(category)}
          >
            {CATEGORY_FORMAT_MAP[category] ? (
              <FormatIcon format={CATEGORY_FORMAT_MAP[category]} size={48} />
            ) : (
              <div className="repository-type-selector__type-icon repository-type-selector__type-icon--hosted" style={{ marginBottom: '8px' }}>
                {React.createElement(CATEGORY_ICONS[category] || ListTodo, { size: 24 })}
              </div>
            )}
            <Text weight="medium" style={{ textAlign: 'center' }}>
              {category}
            </Text>
          </button>
        ))}
      </Box>
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
}

function humanizeFieldId(id: string): string {
  return id.replace(/([A-Z])/g, ' $1').replace(/^./, (s) => s.toUpperCase()).trim();
}

export function DynamicFormField({
  field,
  value,
  onChange,
  error,
  disabled = false,
  repoOptions = [],
  blobStoreOptions = [],
}: DynamicFormFieldProps) {
  const handleChange = (newValue: string) => {
    onChange(newValue);
  };

  // Apply UI overrides from FIELD_UI map (the single source of truth)
  const ui = TASK_FIELD_UI[field.id];
  const displayLabel = ui?.label || (field.label && field.label !== field.id ? field.label : humanizeFieldId(field.id)).replace(/\s*\*\s*$/, '');
  const isRequired = true; // All task configuration fields are required
  const fieldType = ui?.type || field.type || 'string';
  const helpText = ui?.helpText || '';
  const placeholder = ui?.placeholder || '';

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
            />
            <Text size="2">{displayLabel}</Text>
          </label>
          {helpText && (
            <Text size="1" className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" className="dynamic-field__error">{error}</Text>
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
          />
          {helpText && (
            <Text size="1" className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" className="dynamic-field__error">{error}</Text>
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
          />
          {helpText && (
            <Text size="1" className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" className="dynamic-field__error">{error}</Text>
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
          />
          {helpText && (
            <Text size="1" className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" className="dynamic-field__error">{error}</Text>
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
          />
          {helpText && (
            <Text size="1" className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" className="dynamic-field__error">{error}</Text>
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
          />
          {helpText && (
            <Text size="1" className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" className="dynamic-field__error">{error}</Text>
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
            placeholder="Select repository..."
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
      // Smart detection: fields with "repository" in the ID should render as
      // repository autocomplete, even when the backend types them as "string"
      const isRepoField = field.id.toLowerCase().includes('repository');
      const isBlobStoreField = field.id.toLowerCase().includes('blobstore');

      if (isRepoField) {
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
          />
          {helpText && (
            <Text size="1" className="dynamic-field__help">{helpText}</Text>
          )}
          {error && (
            <Text size="1" className="dynamic-field__error">{error}</Text>
          )}
        </Box>
      );
    }
  }
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

export function DynamicFormFields({
  taskType,
  values,
  onChange,
  errors = {},
  disabled = false,
}: DynamicFormFieldsProps) {
  // Load reference data for combobox options
  const [repoOptions, setRepoOptions] = useState<{value: string; label: string}[]>([]);
  const [blobStoreOptions, setBlobStoreOptions] = useState<{value: string; label: string}[]>([]);

  useEffect(() => {
    restClient.get<{name: string}[]>('/service/rest/v1/repositories')
      .then((repos) => {
        if (Array.isArray(repos)) {
          setRepoOptions(repos.map((r) => ({ value: r.name, label: r.name })));
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
  }, []);

  if (!taskType?.formFields || taskType.formFields.length === 0) {
    return null;
  }

  return (
    <Flex direction="column" gap="3" className="dynamic-form-fields">
      {taskType.formFields.map((field) => (
        <DynamicFormField
          key={field.id}
          field={field}
          value={values[field.id] || ''}
          onChange={(value) => onChange(field.id, value)}
          error={errors[field.id]}
          disabled={disabled}
          repoOptions={repoOptions}
          blobStoreOptions={blobStoreOptions}
        />
      ))}
    </Flex>
  );
}

export default TaskTypeSelector;
