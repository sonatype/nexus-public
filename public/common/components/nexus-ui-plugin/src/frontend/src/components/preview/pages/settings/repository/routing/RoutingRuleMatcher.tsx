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

import React, { useCallback, useMemo, useState } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { 
  Plus, 
  Trash2, 
  AlertCircle, 
  CheckCircle, 
  XCircle,
  FileCode,
  ArrowRight,
  ArrowLeft,
  Search,
  Ban,
  Check,
} from 'lucide-react';

import { SettingsButton } from '../../../../shared/form';
import { RoutingRuleMatcherProps, } from './types';

import './RoutingRuleMatcher.scss';

/**
 * Pattern preset types for quick regex templates
 */
type PatternPreset = 'regex' | 'starts-with' | 'ends-with' | 'contains';

interface PatternPresetOption {
  value: PatternPreset;
  label: string;
  icon: React.ReactNode;
  description: string;
  toRegex: (value: string) => string;
  placeholder: string;
}

const PATTERN_PRESETS: PatternPresetOption[] = [
  {
    value: 'regex',
    label: 'Regex',
    icon: <FileCode size={14} />,
    description: 'Custom regular expression',
    toRegex: (value) => value,
    placeholder: 'e.g., .*-sources\\.jar',
  },
  {
    value: 'starts-with',
    label: 'Starts with',
    icon: <ArrowRight size={14} />,
    description: 'Path starts with this prefix',
    toRegex: (value) => `^${escapeRegex(value)}.*`,
    placeholder: 'e.g., /org/apache/',
  },
  {
    value: 'ends-with',
    label: 'Ends with',
    icon: <ArrowLeft size={14} />,
    description: 'Path ends with this suffix',
    toRegex: (value) => `.*${escapeRegex(value)}$`,
    placeholder: 'e.g., .jar',
  },
  {
    value: 'contains',
    label: 'Contains',
    icon: <Search size={14} />,
    description: 'Path contains this text',
    toRegex: (value) => `.*${escapeRegex(value)}.*`,
    placeholder: 'e.g., sources',
  },
];

/**
 * Escape special regex characters in a string
 */
function escapeRegex(str: string): string {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

/**
 * Check if a path matches a regex pattern
 */
function testPathMatch(pattern: string, path: string): boolean | null {
  if (!pattern.trim() || !path.trim()) return null;
  try {
    const regex = new RegExp(pattern);
    return regex.test(path);
  } catch {
    return null;
  }
}

/**
 * Validate a regex pattern and return error message if invalid
 */
function validateRegex(pattern: string): string | null {
  if (!pattern.trim()) return null;
  try {
    new RegExp(pattern);
    return null;
  } catch (e: any) {
    return e.message || 'Invalid regex pattern';
  }
}

/**
 * Check if a regex pattern is valid
 */
function isValidRegex(pattern: string): boolean {
  return validateRegex(pattern) === null;
}

/**
 * Generate a human-readable description of a regex pattern
 */
function describePattern(pattern: string): string {
  if (!pattern.trim()) return '';
  
  // Check for common patterns
  if (pattern.startsWith('^') && pattern.endsWith('.*')) {
    const prefix = pattern.slice(1, -2).replace(/\\\./g, '.').replace(/\\\//g, '/');
    return `starts with "${prefix}"`;
  }
  if (pattern.startsWith('.*') && pattern.endsWith('$')) {
    const suffix = pattern.slice(2, -1).replace(/\\\./g, '.').replace(/\\\//g, '/');
    return `ends with "${suffix}"`;
  }
  if (pattern.startsWith('.*') && pattern.endsWith('.*')) {
    const middle = pattern.slice(2, -2).replace(/\\\./g, '.').replace(/\\\//g, '/');
    return `contains "${middle}"`;
  }
  
  // Fallback to showing the pattern
  return `matches "${pattern}"`;
}

/**
 * Detect which preset a pattern was likely created from
 */
function detectPresetFromPattern(pattern: string): PatternPreset {
  if (!pattern.trim()) return 'regex';
  
  // Check for starts-with pattern: ^escaped-text.*
  if (pattern.startsWith('^') && pattern.endsWith('.*') && !pattern.includes('|')) {
    return 'starts-with';
  }
  
  // Check for ends-with pattern: .*escaped-text$
  if (pattern.startsWith('.*') && pattern.endsWith('$') && !pattern.includes('|')) {
    return 'ends-with';
  }
  
  // Check for contains pattern: .*escaped-text.*
  if (pattern.startsWith('.*') && pattern.endsWith('.*') && pattern !== '.*.*') {
    return 'contains';
  }
  
  return 'regex';
}

/**
 * Get the appropriate placeholder for a preset type
 */
function getPlaceholderForPreset(preset: PatternPreset): string {
  const presetOption = PATTERN_PRESETS.find(p => p.value === preset);
  return presetOption?.placeholder || PATTERN_PRESETS[0].placeholder;
}

/**
 * RoutingRuleMatcher - Enhanced matcher management with pattern presets,
 * inline validation, and real-time feedback
 */
export function RoutingRuleMatcher({
  matchers,
  onChange,
  error,
  disabled = false,
  testPath = '',
  testMode,
  onTest,
}: RoutingRuleMatcherProps) {
  const [presetMenuOpen, setPresetMenuOpen] = useState(false);
  const [localTestPath, setLocalTestPath] = useState(testPath);
  // Track which preset was used for each matcher index (for placeholder display)
  const [matcherPresets, setMatcherPresets] = useState<PatternPreset[]>(() => 
    matchers.map(m => detectPresetFromPattern(m))
  );

  // Sync local test path with prop
  React.useEffect(() => {
    setLocalTestPath(testPath);
  }, [testPath]);

  // Sync matcher presets when matchers change externally (e.g., editing existing rule)
  React.useEffect(() => {
    if (matchers.length !== matcherPresets.length) {
      setMatcherPresets(matchers.map(m => detectPresetFromPattern(m)));
    }
  }, [matchers.length, matcherPresets.length, matchers.map]);

  const handleMatcherChange = useCallback((index: number, value: string) => {
    const newMatchers = [...matchers];
    newMatchers[index] = value;
    onChange(newMatchers);
  }, [matchers, onChange]);

  const handleAddMatcher = useCallback((preset: PatternPreset = 'regex') => {
    onChange([...matchers, '']);
    setMatcherPresets(prev => [...prev, preset]);
    setPresetMenuOpen(false);
  }, [matchers, onChange]);

  const handleRemoveMatcher = useCallback((index: number) => {
    if (matchers.length <= 1) return;
    const newMatchers = matchers.filter((_, i) => i !== index);
    onChange(newMatchers);
    setMatcherPresets(prev => prev.filter((_, i) => i !== index));
  }, [matchers, onChange]);

  const handleTestPathChange = useCallback((value: string) => {
    setLocalTestPath(value);
    if (onTest) {
      onTest(value);
    }
  }, [onTest]);

  // Calculate validation state for each matcher
  const matcherValidation = useMemo(() => {
    return matchers.map(matcher => ({
      error: validateRegex(matcher),
      isValid: isValidRegex(matcher),
      matchResult: localTestPath.trim() && matcher.trim() 
        ? (isValidRegex(matcher) ? testPathMatch(matcher, localTestPath) : null)
        : null,
    }));
  }, [matchers, localTestPath]);

  // Check if any matcher is invalid (blocks save)
  const _hasInvalidMatchers = useMemo(() => {
    return matchers.some((m, i) => m.trim() && !matcherValidation[i].isValid);
  }, [matchers, matcherValidation]);

  // Calculate overall rule outcome for test path
  const ruleOutcome = useMemo(() => {
    if (!localTestPath.trim() || !testMode) return null;

    const hasValidMatcher = matchers.some((m, i) => m.trim() && matcherValidation[i].isValid);
    if (!hasValidMatcher) return null;

    const anyMatch = matchers.some(
      (m, i) => m.trim() && matcherValidation[i]?.isValid && matcherValidation[i]?.matchResult === true
    );

    if (testMode === 'BLOCK') {
      return anyMatch ? 'blocked' : 'allowed';
    } else {
      return anyMatch ? 'allowed' : 'blocked';
    }
  }, [matchers, matcherValidation, localTestPath, testMode]);

  // Generate rule summary
  const ruleSummary = useMemo(() => {
    const validPatterns = matchers.filter(m => m.trim() && isValidRegex(m));
    if (validPatterns.length === 0) return null;

    return validPatterns.map(p => describePattern(p));
  }, [matchers]);

  // Get mode-specific helper text
  const getModeHelperText = () => {
    if (!testMode) return null;
    if (testMode === 'BLOCK') {
      return 'If any matcher matches the request path, the request will be blocked.';
    }
    return 'If any matcher matches the request path, the request will be allowed.';
  };

  return (
    <Box className="routing-rule-matcher">
      {/* Header with Add Matcher dropdown */}
      <Flex justify="between" align="center" className="routing-rule-matcher__header">
        <Box>
          <Text size="2" weight="medium" className="routing-rule-matcher__label">
            Matchers
            <span className="routing-rule-matcher__required">*</span>
          </Text>
          <Text size="1" className="routing-rule-matcher__help">
            {getModeHelperText() || 'Define patterns to match request paths. Matchers use regular expressions.'}
          </Text>
        </Box>
        
        {/* Preset dropdown */}
        <Box className="routing-rule-matcher__preset-dropdown">
          <SettingsButton
            variant="ghost"
            onClick={() => setPresetMenuOpen(!presetMenuOpen)}
            disabled={disabled}
            className="routing-rule-matcher__add-btn"
            icon={Plus}
          >
            Add Matcher
          </SettingsButton>
          
          {presetMenuOpen && (
            <Box className="routing-rule-matcher__preset-menu">
              {PATTERN_PRESETS.map(preset => (
                <button
                  key={preset.value}
                  type="button"
                  className="routing-rule-matcher__preset-item"
                  onClick={() => handleAddMatcher(preset.value)}
                >
                  <span className="routing-rule-matcher__preset-icon">{preset.icon}</span>
                  <span className="routing-rule-matcher__preset-content">
                    <span className="routing-rule-matcher__preset-label">{preset.label}</span>
                    <span className="routing-rule-matcher__preset-desc">{preset.description}</span>
                  </span>
                </button>
              ))}
            </Box>
          )}
        </Box>
      </Flex>

      {/* Click outside to close menu */}
      {presetMenuOpen && (
        <Box 
          className="routing-rule-matcher__backdrop" 
          onClick={() => setPresetMenuOpen(false)} 
        />
      )}

      {/* Matcher List */}
      <Box className="routing-rule-matcher__list">
        {matchers.map((matcher, index) => {
          const validation = matcherValidation[index];
          const hasMatch = validation.matchResult === true;
          const hasNoMatch = validation.matchResult === false;
          const isInvalid = !validation.isValid && matcher.trim();

          return (
            <Box key={index} className="routing-rule-matcher__row-container">
              <Flex
                align="center"
                gap="2"
                className={`routing-rule-matcher__row ${isInvalid ? 'routing-rule-matcher__row--invalid' : ''} ${hasMatch ? 'routing-rule-matcher__row--match' : ''}`}
              >
                {/* Row number */}
                <Text size="1" className="routing-rule-matcher__row-number">
                  {index + 1}
                </Text>

                {/* Regex input */}
                <Box className="routing-rule-matcher__input-wrapper">
                  <input
                    type="text"
                    value={matcher}
                    onChange={(e) => handleMatcherChange(index, e.target.value)}
                    placeholder={getPlaceholderForPreset(matcherPresets[index] || 'regex')}
                    disabled={disabled}
                    className={`routing-rule-matcher__input ${isInvalid ? 'routing-rule-matcher__input--invalid' : ''}`}
                  />
                </Box>

                {/* Match indicator */}
                <Box className="routing-rule-matcher__status">
                  {localTestPath.trim() && matcher.trim() && (
                    <>
                      {hasMatch && (
                        <Flex align="center" gap="1" className="routing-rule-matcher__status-badge routing-rule-matcher__status-badge--match">
                          <CheckCircle size={14} />
                          <span>Matches</span>
                        </Flex>
                      )}
                      {hasNoMatch && (
                        <Flex align="center" gap="1" className="routing-rule-matcher__status-badge routing-rule-matcher__status-badge--no-match">
                          <XCircle size={14} />
                          <span>No match</span>
                        </Flex>
                      )}
                      {isInvalid && (
                        <Flex align="center" gap="1" className="routing-rule-matcher__status-badge routing-rule-matcher__status-badge--invalid">
                          <AlertCircle size={14} />
                          <span>Invalid</span>
                        </Flex>
                      )}
                    </>
                  )}
                </Box>

                {/* Remove button */}
                <SettingsButton
                  variant="ghost"
                  onClick={() => handleRemoveMatcher(index)}
                  disabled={disabled || matchers.length <= 1}
                  className="routing-rule-matcher__remove-btn"
                  aria-label={`Remove matcher ${index + 1}`}
                  icon={Trash2}
                />
              </Flex>

              {/* Inline validation error */}
              {isInvalid && validation.error && (
                <Flex align="center" gap="1" className="routing-rule-matcher__inline-error">
                  <AlertCircle size={12} />
                  <Text size="1">{validation.error}</Text>
                </Flex>
              )}
            </Box>
          );
        })}
      </Box>

      {/* Global error (from parent) */}
      {error && (
        <Flex align="center" gap="1" className="routing-rule-matcher__error">
          <AlertCircle size={14} />
          <Text size="1">{error}</Text>
        </Flex>
      )}

      {/* Test Path Section - Client-side regex testing (no API call).
          When the user enters a path, matchers are evaluated in real time and the outcome
          (BLOCKED/ALLOWED) is shown. For API-based testing across all repositories, use
          RoutingRulePreview component (Test button on Routing Rules page). */}
      {onTest && (
        <Box className="routing-rule-matcher__test">
          <Text size="2" weight="medium" className="routing-rule-matcher__test-label">
            Test Path
          </Text>
          <Text size="1" className="routing-rule-matcher__test-help">
            Enter a request path to see which matchers would match and the resulting action.
          </Text>
          <Box className="routing-rule-matcher__test-input-wrapper">
            <Search size={16} className="routing-rule-matcher__test-search-icon" />
            <input
              type="text"
              value={localTestPath}
              onChange={(e) => handleTestPathChange(e.target.value)}
              placeholder="/com/example/artifact-1.0-sources.jar"
              disabled={disabled}
              className="routing-rule-matcher__test-input"
            />
          </Box>

          {/* Rule outcome */}
          {localTestPath.trim() && testMode && (
            <Box className={`routing-rule-matcher__outcome routing-rule-matcher__outcome--${ruleOutcome || 'neutral'}`}>
              {ruleOutcome === 'blocked' && (
                <Flex align="center" gap="2">
                  <Ban size={18} className="routing-rule-matcher__outcome-icon" />
                  <Box>
                    <Text size="2" weight="medium">This request would be BLOCKED</Text>
                    <Text size="1" className="routing-rule-matcher__outcome-detail">
                      {testMode === 'BLOCK' ? 'A matcher matched the path' : 'No matcher matched the path'}
                    </Text>
                  </Box>
                </Flex>
              )}
              {ruleOutcome === 'allowed' && (
                <Flex align="center" gap="2">
                  <Check size={18} className="routing-rule-matcher__outcome-icon" />
                  <Box>
                    <Text size="2" weight="medium">This request would be ALLOWED</Text>
                    <Text size="1" className="routing-rule-matcher__outcome-detail">
                      {testMode === 'ALLOW' ? 'A matcher matched the path' : 'No matcher matched the path'}
                    </Text>
                  </Box>
                </Flex>
              )}
              {!ruleOutcome && (
                <Flex align="center" gap="2">
                  <AlertCircle size={18} className="routing-rule-matcher__outcome-icon" />
                  <Box>
                    <Text size="2" weight="medium">This rule does not apply to this request</Text>
                    <Text size="1" className="routing-rule-matcher__outcome-detail">
                      No valid matchers defined
                    </Text>
                  </Box>
                </Flex>
              )}
            </Box>
          )}
        </Box>
      )}

      {/* Rule Summary - human readable */}
      {ruleSummary && ruleSummary.length > 0 && testMode && (
        <Box className="routing-rule-matcher__summary">
          <Text size="2" weight="medium" className="routing-rule-matcher__summary-title">
            Rule Summary
          </Text>
          <Box className="routing-rule-matcher__summary-content">
            <Text size="2">
              <strong>Mode:</strong> {testMode === 'BLOCK' ? 'Block' : 'Allow'}
            </Text>
            <Text size="2" className="routing-rule-matcher__summary-matchers">
              <strong>Matches requests where path:</strong>
            </Text>
            <ul className="routing-rule-matcher__summary-list">
              {ruleSummary.map((desc, i) => (
                <li key={i}>
                  {i > 0 && <span className="routing-rule-matcher__summary-or">OR</span>}
                  {desc}
                </li>
              ))}
            </ul>
          </Box>
        </Box>
      )}
    </Box>
  );
}

export default RoutingRuleMatcher;
