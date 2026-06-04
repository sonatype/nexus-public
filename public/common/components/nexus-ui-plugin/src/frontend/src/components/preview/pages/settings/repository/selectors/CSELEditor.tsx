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

import React, { useState, useRef, useCallback, useEffect, useMemo } from 'react';
import { Box, Text, Flex, ScrollArea } from '@radix-ui/themes';
import { AlertCircle, AlertTriangle, Check } from 'lucide-react';

import {
  CSEL_CONFIG,
  getAttributeByName,
  getOperatorsForAttribute,
} from './cselConfig';
import {
  validateCSEL,
  getCursorContext,
  ValidationResult,
  ValidationMessage,
} from './cselValidator';

import './CSELEditor.scss';

interface AutocompleteSuggestion {
  value: string;
  label: string;
  description?: string;
  category: 'attribute' | 'operator' | 'logical' | 'value';
}

interface CSELEditorProps {
  value: string;
  onChange: (value: string) => void;
  onValidationChange?: (result: ValidationResult) => void;
  placeholder?: string;
  disabled?: boolean;
  rows?: number;
}

/**
 * CSELEditor - Enhanced textarea with autocomplete and inline validation
 *
 * Features:
 * - Context-aware autocomplete for attributes and operators
 * - Real-time inline validation with errors and warnings
 * - Keyboard navigation for suggestions
 * - Preserves exact CSEL syntax
 */
export function CSELEditor({
  value,
  onChange,
  onValidationChange,
  placeholder = 'Enter CSEL expression...',
  disabled = false,
  rows = 4,
}: CSELEditorProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [suggestions, setSuggestions] = useState<AutocompleteSuggestion[]>([]);
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [cursorPosition, setCursorPosition] = useState(0);

  // Validate on value change
  const validation = useMemo(() => validateCSEL(value), [value]);

  // Notify parent of validation changes
  useEffect(() => {
    onValidationChange?.(validation);
  }, [validation, onValidationChange]);

  // Generate suggestions based on cursor context
  const updateSuggestions = useCallback(() => {
    if (!textareaRef.current || disabled) {
      setSuggestions([]);
      setShowSuggestions(false);
      return;
    }

    const pos = textareaRef.current.selectionStart;
    setCursorPosition(pos);
    const context = getCursorContext(value, pos);

    let newSuggestions: AutocompleteSuggestion[] = [];

    switch (context.context) {
      case 'start':
      case 'afterLogical':
        // Suggest attributes
        newSuggestions = CSEL_CONFIG.attributes
          .filter((attr) => {
            if (context.partialText) {
              return attr.name.toLowerCase().startsWith(context.partialText.toLowerCase());
            }
            return true;
          })
          .map((attr) => ({
            value: attr.name,
            label: attr.name,
            description: attr.description + (attr.formatSpecific ? ` (${attr.formatSpecific})` : ''),
            category: 'attribute' as const,
          }));
        break;

      case 'afterAttribute':
        // Suggest operators for the current attribute
        const operators = context.currentAttribute
          ? getOperatorsForAttribute(context.currentAttribute)
          : CSEL_CONFIG.operators.map((op) => op.symbol);

        newSuggestions = operators.map((op) => {
          const opConfig = CSEL_CONFIG.operators.find((o) => o.symbol === op);
          return {
            value: op,
            label: op,
            description: opConfig?.description || '',
            category: 'operator' as const,
          };
        });
        break;

      case 'afterOperator':
        // Suggest a value placeholder
        newSuggestions = [
          {
            value: '""',
            label: '"value"',
            description: 'Enter a quoted string value',
            category: 'value' as const,
          },
        ];
        break;

      case 'afterValue':
        // Suggest logical operators
        newSuggestions = CSEL_CONFIG.logicalOperators.map((op) => ({
          value: op.symbol,
          label: op.symbol,
          description: op.description,
          category: 'logical' as const,
        }));
        break;

      case 'insideValue':
      case 'unknown':
        // No suggestions inside values
        break;
    }

    setSuggestions(newSuggestions);
    setShowSuggestions(newSuggestions.length > 0);
    setSelectedIndex(0);
  }, [value, disabled]);

  // Handle input change
  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      onChange(e.target.value);
    },
    [onChange]
  );

  // Handle cursor movement
  const handleSelect = useCallback(() => {
    updateSuggestions();
  }, [updateSuggestions]);

  // Handle blur
  const handleBlur = useCallback(() => {
    // Delay hiding suggestions to allow click on suggestion
    setTimeout(() => {
      setShowSuggestions(false);
    }, 200);
  }, []);

  // Handle focus
  const handleFocus = useCallback(() => {
    updateSuggestions();
  }, [updateSuggestions]);

  // Insert suggestion at cursor
  const insertSuggestion = useCallback(
    (suggestion: AutocompleteSuggestion) => {
      if (!textareaRef.current) return;

      const pos = cursorPosition;
      const before = value.slice(0, pos);
      const after = value.slice(pos);

      // Check if we need to add space before
      const needsSpaceBefore = before.length > 0 && !/\s$/.test(before) && !/[(\s]$/.test(before);
      // Check if we need to add space after
      const needsSpaceAfter = suggestion.category !== 'value';

      // Handle partial text replacement
      const context = getCursorContext(value, pos);
      let insertPos = pos;
      if (context.partialText) {
        insertPos = pos - context.partialText.length;
      }

      const prefix = needsSpaceBefore ? ' ' : '';
      const suffix = needsSpaceAfter ? ' ' : '';
      let insertValue = suggestion.value;

      // For value placeholder, position cursor inside quotes
      const newValue =
        value.slice(0, insertPos) + prefix + insertValue + suffix + after;

      onChange(newValue);

      // Update cursor position
      setTimeout(() => {
        if (textareaRef.current) {
          const newPos = insertPos + prefix.length + insertValue.length + suffix.length;
          textareaRef.current.selectionStart = newPos;
          textareaRef.current.selectionEnd = newPos;
          textareaRef.current.focus();

          // If we inserted a value placeholder, position cursor between quotes
          if (suggestion.category === 'value') {
            const quotePos = insertPos + prefix.length + 1;
            textareaRef.current.selectionStart = quotePos;
            textareaRef.current.selectionEnd = quotePos;
          }
        }
      }, 0);

      setShowSuggestions(false);
    },
    [value, cursorPosition, onChange]
  );

  // Handle keyboard navigation
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
      if (!showSuggestions || suggestions.length === 0) return;

      switch (e.key) {
        case 'ArrowDown':
          e.preventDefault();
          setSelectedIndex((prev) => (prev + 1) % suggestions.length);
          break;
        case 'ArrowUp':
          e.preventDefault();
          setSelectedIndex((prev) => (prev - 1 + suggestions.length) % suggestions.length);
          break;
        case 'Enter':
          if (showSuggestions && suggestions.length > 0) {
            e.preventDefault();
            insertSuggestion(suggestions[selectedIndex]);
          }
          break;
        case 'Escape':
          e.preventDefault();
          setShowSuggestions(false);
          break;
        case 'Tab':
          if (showSuggestions && suggestions.length > 0) {
            e.preventDefault();
            insertSuggestion(suggestions[selectedIndex]);
          }
          break;
      }
    },
    [showSuggestions, suggestions, selectedIndex, insertSuggestion]
  );

  // Update suggestions when value changes
  useEffect(() => {
    const timer = setTimeout(updateSuggestions, 100);
    return () => clearTimeout(timer);
  }, [value, updateSuggestions]);

  // Get category color
  const getCategoryColor = (category: string) => {
    switch (category) {
      case 'attribute':
        return 'var(--blue-9)';
      case 'operator':
        return 'var(--orange-9)';
      case 'logical':
        return 'var(--purple-9)';
      case 'value':
        return 'var(--green-9)';
      default:
        return 'var(--gray-9)';
    }
  };

  return (
    <Box className="csel-editor">
      <Box className="csel-editor__container">
        <textarea
          ref={textareaRef}
          className={`csel-editor__textarea ${validation.hasBlockingErrors ? 'csel-editor__textarea--error' : ''}`}
          value={value}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          onSelect={handleSelect}
          onBlur={handleBlur}
          onFocus={handleFocus}
          placeholder={placeholder}
          disabled={disabled}
          rows={rows}
          spellCheck={false}
          autoComplete="off"
          data-testid="content-selector-expression"
          aria-label="Content selector expression"
        />

        {/* Autocomplete dropdown */}
        {showSuggestions && suggestions.length > 0 && (
          <Box className="csel-editor__suggestions">
            <ScrollArea style={{ maxHeight: '200px' }}>
              {suggestions.map((suggestion, index) => (
                <Box
                  key={`${suggestion.category}-${suggestion.value}`}
                  className={`csel-editor__suggestion ${index === selectedIndex ? 'csel-editor__suggestion--selected' : ''}`}
                  onClick={() => insertSuggestion(suggestion)}
                  onMouseEnter={() => setSelectedIndex(index)}
                >
                  <Flex align="center" gap="2">
                    <Text
                      size="1"
                      className="csel-editor__suggestion-category"
                      style={{ color: getCategoryColor(suggestion.category) }}
                    >
                      {suggestion.category}
                    </Text>
                    <Text size="2" weight="medium" className="csel-editor__suggestion-label">
                      {suggestion.label}
                    </Text>
                  </Flex>
                  {suggestion.description && (
                    <Text size="1" color="gray" className="csel-editor__suggestion-description">
                      {suggestion.description}
                    </Text>
                  )}
                </Box>
              ))}
            </ScrollArea>
          </Box>
        )}
      </Box>

      {/* Validation messages */}
      {validation.messages.length > 0 && (
        <Box className="csel-editor__validation">
          {validation.messages.map((msg, index) => (
            <ValidationMessageItem key={index} message={msg} />
          ))}
        </Box>
      )}

      {/* Success indicator when valid */}
      {value.trim() && validation.messages.length === 0 && (
        <Flex align="center" gap="1" className="csel-editor__valid">
          <Check size={14} />
          <Text size="1">Valid expression</Text>
        </Flex>
      )}
    </Box>
  );
}

function ValidationMessageItem({ message }: { message: ValidationMessage }) {
  const isError = message.type === 'error';
  return (
    <Flex
      align="center"
      gap="1"
      className={`csel-editor__message csel-editor__message--${message.type}`}
    >
      {isError ? <AlertCircle size={14} /> : <AlertTriangle size={14} />}
      <Text size="1">{message.message}</Text>
    </Flex>
  );
}

export default CSELEditor;


