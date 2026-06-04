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

import React, { useCallback, useState, useMemo, useRef, useEffect } from 'react';
import { Box, Flex, Text, TextField, Select } from '@radix-ui/themes';
import { Plus, Trash2, AlertCircle, AlertTriangle, Info, HelpCircle, ChevronDown, ChevronRight } from 'lucide-react';
import { SettingsButton } from '../../../../shared/form';

import {
  JDBC_PARAMETERS_CONFIG,
  getParameterDefinition,
  getParameterDescription,
  isKnownParameter,
  validateParameterValue,
  getAllowedValues,
  getParametersByCategory,
  getCategoryDisplayName,
  JdbcParameterDefinition,
} from './jdbcParameters.config';

import './JdbcParameterEditor.scss';

/**
 * Single JDBC parameter with metadata
 */
export interface JdbcParameter {
  id: string;
  name: string;
  value: string;
  isDefault: boolean;
  isCustom: boolean;
  /** Whether the parameter has been touched/edited by user */
  touched?: boolean;
}

/**
 * Validation result for a single parameter
 */
export interface ParameterValidation {
  id: string;
  error?: string;
  warning?: string;
}

/**
 * Props for JdbcParameterEditor
 */
export interface JdbcParameterEditorProps {
  parameters: JdbcParameter[];
  onChange: (parameters: JdbcParameter[]) => void;
  onReset?: () => void;
  disabled?: boolean;
  validations?: ParameterValidation[];
  /** Force show all validation errors (e.g., on form submit attempt) */
  showAllValidation?: boolean;
}

/**
 * Generate unique ID for new parameters
 */
function generateId(): string {
  return `param-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * Autocomplete dropdown for parameter names
 */
function ParameterNameAutocomplete({
  value,
  onChange,
  disabled,
  existingNames,
  hasError,
}: {
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  existingNames: string[];
  hasError?: boolean;
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [filter, setFilter] = useState(value);
  const [dropdownPosition, setDropdownPosition] = useState({ top: 0, left: 0, width: 0 });
  const inputRef = useRef<HTMLInputElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  // Filter suggestions based on input
  const suggestions = useMemo(() => {
    const lowerFilter = filter.toLowerCase();
    return JDBC_PARAMETERS_CONFIG
      .filter(p => {
        // Filter by search text
        const matchesFilter = !lowerFilter || 
          p.name.toLowerCase().includes(lowerFilter) ||
          p.description.toLowerCase().includes(lowerFilter);
        // Exclude already used parameters
        const notUsed = !existingNames.some(n => n.toLowerCase() === p.name.toLowerCase());
        return matchesFilter && notUsed;
      })
      .slice(0, 10); // Limit to 10 suggestions
  }, [filter, existingNames]);

  // Update dropdown position when input is focused
  const updateDropdownPosition = useCallback(() => {
    if (containerRef.current) {
      const rect = containerRef.current.getBoundingClientRect();
      setDropdownPosition({
        top: rect.bottom + 4,
        left: rect.left,
        width: Math.max(rect.width, 300),
      });
    }
  }, []);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Update position on scroll/resize
  useEffect(() => {
    if (isOpen) {
      updateDropdownPosition();
      window.addEventListener('scroll', updateDropdownPosition, true);
      window.addEventListener('resize', updateDropdownPosition);
      return () => {
        window.removeEventListener('scroll', updateDropdownPosition, true);
        window.removeEventListener('resize', updateDropdownPosition);
      };
    }
  }, [isOpen, updateDropdownPosition]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    setFilter(newValue);
    onChange(newValue);
    setIsOpen(true);
  };

  const handleFocus = () => {
    updateDropdownPosition();
    setIsOpen(true);
  };

  const handleSelect = (paramName: string) => {
    onChange(paramName);
    setFilter(paramName);
    setIsOpen(false);
    inputRef.current?.blur();
  };

  return (
    <Box className="jdbc-autocomplete" ref={containerRef}>
      <TextField.Root
        ref={inputRef}
        value={value}
        onChange={handleInputChange}
        onFocus={handleFocus}
        placeholder="Type or select parameter..."
        disabled={disabled}
        className={hasError ? 'jdbc-parameter-row__input--error' : ''}
      />
      
      {isOpen && suggestions.length > 0 && !disabled && (
        <Box 
          className="jdbc-autocomplete__dropdown"
          style={{
            top: dropdownPosition.top,
            left: dropdownPosition.left,
            width: dropdownPosition.width,
          }}
        >
          {suggestions.map((param) => (
            <Box
              key={param.name}
              className="jdbc-autocomplete__option"
              onClick={() => handleSelect(param.name)}
            >
              <Text size="2" weight="medium">{param.name}</Text>
              <Text size="1" color="gray">{param.description}</Text>
            </Box>
          ))}
        </Box>
      )}
    </Box>
  );
}

/**
 * Value input with dropdown for enum/boolean types
 */
function ParameterValueInput({
  parameterName,
  value,
  onChange,
  disabled,
  hasError,
}: {
  parameterName: string;
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  hasError?: boolean;
}) {
  const allowedValues = getAllowedValues(parameterName);
  const definition = getParameterDefinition(parameterName);

  // Show dropdown for enum/boolean types
  if (allowedValues && allowedValues.length > 0) {
    return (
      <Select.Root value={value} onValueChange={onChange} disabled={disabled}>
        <Select.Trigger 
          placeholder="Select value..." 
          className={hasError ? 'jdbc-parameter-row__input--error' : ''}
        />
        <Select.Content>
          {allowedValues.map((v) => (
            <Select.Item key={v} value={v}>
              {v}
            </Select.Item>
          ))}
        </Select.Content>
      </Select.Root>
    );
  }

  // Regular text input for other types
  const placeholder = definition?.type === 'number' 
    ? `Enter number${definition.unit ? ` (${definition.unit})` : ''}...`
    : 'Enter value...';

  return (
    <TextField.Root
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      disabled={disabled}
      type={definition?.type === 'number' ? 'number' : 'text'}
      className={hasError ? 'jdbc-parameter-row__input--error' : ''}
    />
  );
}

/**
 * Single parameter row component
 */
function ParameterRow({
  parameter,
  validation,
  existingNames,
  onChange,
  onRemove,
  disabled,
  showValidation,
}: {
  parameter: JdbcParameter;
  validation?: ParameterValidation;
  existingNames: string[];
  onChange: (updated: JdbcParameter) => void;
  onRemove: () => void;
  disabled?: boolean;
  showValidation?: boolean;
}) {
  const description = getParameterDescription(parameter.name);
  const definition = getParameterDefinition(parameter.name);
  const isUnknown = parameter.name && !isKnownParameter(parameter.name);
  const isReadOnly = parameter.isDefault && !parameter.isCustom;

  // Only show validation errors after save attempt (showValidation is true)
  // This provides better UX - users see asterisks for required fields,
  // and errors only appear when they try to save with invalid data
  const shouldShowErrors = showValidation;

  // Validate value against definition
  const valueError = parameter.name && parameter.value 
    ? validateParameterValue(parameter.name, parameter.value)
    : undefined;

  // Simple handlers - no need to track touched state
  const handleNameChange = (name: string) => {
    onChange({ ...parameter, name });
  };

  const handleValueChange = (value: string) => {
    onChange({ ...parameter, value });
  };

  return (
    <Box className="jdbc-parameter-row">
      <Flex gap="3" align="start" className="jdbc-parameter-row__fields">
        {/* Parameter Name with Autocomplete */}
        <Box className="jdbc-parameter-row__name">
          {isReadOnly ? (
            <TextField.Root
              value={parameter.name}
              disabled={true}
            />
          ) : (
            <ParameterNameAutocomplete
              value={parameter.name}
              onChange={handleNameChange}
              disabled={disabled}
              existingNames={existingNames.filter(n => n !== parameter.name)}
              hasError={shouldShowErrors && !!validation?.error}
            />
          )}
          {shouldShowErrors && validation?.error && (
            <Flex align="center" gap="1" className="jdbc-parameter-row__error">
              <AlertCircle size={12} />
              <Text size="1">{validation.error}</Text>
            </Flex>
          )}
        </Box>

        {/* Parameter Value with Dropdown for enums */}
        <Box className="jdbc-parameter-row__value">
          <ParameterValueInput
            parameterName={parameter.name}
            value={parameter.value}
            onChange={handleValueChange}
            disabled={disabled || isReadOnly}
            hasError={shouldShowErrors && !!valueError}
          />
          {shouldShowErrors && valueError && (
            <Flex align="center" gap="1" className="jdbc-parameter-row__error">
              <AlertCircle size={12} />
              <Text size="1">{valueError}</Text>
            </Flex>
          )}
        </Box>

        {/* Source Badge */}
        <Box className="jdbc-parameter-row__badge">
          {parameter.isDefault && !parameter.isCustom && (
            <Text size="1" className="jdbc-parameter-row__badge--default">Default</Text>
          )}
          {parameter.isCustom && (
            <Text size="1" className="jdbc-parameter-row__badge--custom">Custom</Text>
          )}
        </Box>

        {/* Remove Button */}
        {!isReadOnly && (
          <SettingsButton
            variant="ghost"
            onClick={onRemove}
            disabled={disabled}
            className="jdbc-parameter-row__remove"
            aria-label="Remove parameter"
            icon={Trash2}
          />
        )}
      </Flex>

      {/* Description and Warnings */}
      <Box className="jdbc-parameter-row__meta">
        {parameter.name && (
          <Flex align="center" gap="1" className="jdbc-parameter-row__description">
            <Info size={12} />
            <Text size="1" color="gray">{description}</Text>
            {definition?.type && (
              <Text size="1" className="jdbc-parameter-row__type">
                ({definition.type})
              </Text>
            )}
          </Flex>
        )}
        {isUnknown && !validation?.error && (
          <Flex align="center" gap="1" className="jdbc-parameter-row__warning">
            <AlertTriangle size={12} />
            <Text size="1">Unknown parameter - verify this is correct for your database</Text>
          </Flex>
        )}
        {validation?.warning && (
          <Flex align="center" gap="1" className="jdbc-parameter-row__warning">
            <AlertTriangle size={12} />
            <Text size="1">{validation.warning}</Text>
          </Flex>
        )}
      </Box>
    </Box>
  );
}

/**
 * CommonParametersHelp - Collapsible section showing available parameters by category
 */
function CommonParametersHelp() {
  const [isExpanded, setIsExpanded] = useState(false);
  const categories: JdbcParameterDefinition['category'][] = ['connection', 'timeout', 'ssl', 'performance', 'other'];

  return (
    <Box className="jdbc-parameter-editor__help">
      <Flex
        align="center"
        gap="2"
        className="jdbc-parameter-editor__help-header"
        onClick={() => setIsExpanded(!isExpanded)}
        style={{ cursor: 'pointer' }}
      >
        {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
        <HelpCircle size={16} />
        <Text size="2" weight="medium">
          Available JDBC Parameters ({JDBC_PARAMETERS_CONFIG.length} parameters)
        </Text>
      </Flex>
      
      {isExpanded && (
        <Box className="jdbc-parameter-editor__help-content">
          <Text size="2" color="gray" className="jdbc-parameter-editor__help-intro">
            Start typing in the parameter name field to see suggestions, or browse the list below.
            Parameters with dropdowns will show allowed values automatically.
          </Text>
          
          {categories.map((category) => {
            const params = getParametersByCategory(category);
            if (params.length === 0) return null;
            
            return (
              <Box key={category} className="jdbc-parameter-editor__help-category">
                <Text size="2" weight="medium" className="jdbc-parameter-editor__help-category-title">
                  {getCategoryDisplayName(category)}
                </Text>
                <Box className="jdbc-parameter-editor__help-list">
                  {params.map((param) => (
                    <Flex key={param.name} className="jdbc-parameter-editor__help-item" gap="2">
                      <Text size="2" weight="medium" className="jdbc-parameter-editor__help-name">
                        {param.name}
                      </Text>
                      <Text size="1" color="gray" className="jdbc-parameter-editor__help-desc">
                        {param.description}
                        {param.allowedValues && (
                          <span className="jdbc-parameter-editor__help-values">
                            {' '}[{param.allowedValues.join(' | ')}]
                          </span>
                        )}
                      </Text>
                    </Flex>
                  ))}
                </Box>
              </Box>
            );
          })}
        </Box>
      )}
    </Box>
  );
}

/**
 * JdbcParameterEditor - Key-value editor for JDBC parameters with autocomplete and validation
 */
export function JdbcParameterEditor({
  parameters,
  onChange,
  onReset,
  disabled = false,
  validations = [],
  showAllValidation = false,
}: JdbcParameterEditorProps) {
  // Get all existing parameter names for duplicate detection
  const existingNames = useMemo(() => parameters.map(p => p.name), [parameters]);

  const handleParameterChange = useCallback((index: number, updated: JdbcParameter) => {
    const newParams = [...parameters];
    newParams[index] = { ...updated, isCustom: true };
    onChange(newParams);
  }, [parameters, onChange]);

  const handleRemove = useCallback((index: number) => {
    const newParams = parameters.filter((_, i) => i !== index);
    onChange(newParams);
  }, [parameters, onChange]);

  const handleAdd = useCallback(() => {
    const newParam: JdbcParameter = {
      id: generateId(),
      name: '',
      value: '',
      isDefault: false,
      isCustom: true,
      touched: false,
    };
    onChange([...parameters, newParam]);
  }, [parameters, onChange]);

  const getValidation = (id: string) => validations.find(v => v.id === id);

  const hasCustomParameters = parameters.some(p => p.isCustom);

  return (
    <Box className="jdbc-parameter-editor">
      {/* Help Section - Available Parameters */}
      <CommonParametersHelp />

      {/* Parameter List */}
      {parameters.length > 0 ? (
        <Box className="jdbc-parameter-editor__list">
          {/* Header */}
          <Flex className="jdbc-parameter-editor__header" gap="3">
            <Text size="2" weight="medium" className="jdbc-parameter-editor__header-name">
              Parameter Name <Text as="span" color="red">*</Text>
            </Text>
            <Text size="2" weight="medium" className="jdbc-parameter-editor__header-value">
              Value <Text as="span" color="red">*</Text>
            </Text>
            <Text size="2" weight="medium" className="jdbc-parameter-editor__header-source">
              Source
            </Text>
            <Box className="jdbc-parameter-editor__header-actions" />
          </Flex>

          {/* Rows */}
          {parameters.map((param, index) => (
            <ParameterRow
              key={param.id}
              parameter={param}
              validation={getValidation(param.id)}
              existingNames={existingNames}
              onChange={(updated) => handleParameterChange(index, updated)}
              onRemove={() => handleRemove(index)}
              disabled={disabled}
              showValidation={showAllValidation}
            />
          ))}
        </Box>
      ) : (
        <Box className="jdbc-parameter-editor__empty">
          <Text size="2" color="gray">No advanced parameters configured</Text>
        </Box>
      )}

      {/* Actions */}
      <Flex gap="3" className="jdbc-parameter-editor__actions">
        <SettingsButton
          variant="secondary"
          onClick={handleAdd}
          disabled={disabled}
          icon={Plus}
        >
          Add Parameter
        </SettingsButton>

        {onReset && hasCustomParameters && (
          <SettingsButton
            variant="ghost"
            onClick={onReset}
            disabled={disabled}
          >
            Reset to Defaults
          </SettingsButton>
        )}
      </Flex>
    </Box>
  );
}

// Re-export for use in types.ts
export { isKnownParameter };

export default JdbcParameterEditor;
