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

import { JdbcParameter, ParameterValidation } from './JdbcParameterEditor';
import { isKnownParameter } from './jdbcParameters.config';

/**
 * DataStore configuration data
 */
export interface DataStoreConfig {
  name: string;
  source: string;
  type: string;
  jdbcUrl: string;
  username: string;
  schema: string;
  maximumConnectionPool: number | string;
  advanced: string;
  // Additional metadata fields
  databaseType?: string;
  driverVersion?: string;
}

/**
 * Form data for updating DataStore configuration
 */
export interface DataStoreFormData {
  maximumConnectionPool: number | string;
  advanced: string;
}

/**
 * Form validation errors
 */
export interface DataStoreFormErrors {
  maximumConnectionPool?: string;
  advanced?: string;
}

/**
 * Props for DataStorePage component
 */
export interface DataStorePageProps {
  className?: string;
}

/**
 * Effective configuration entry for preview
 */
export interface EffectiveParameter {
  name: string;
  value: string;
  source: 'Default' | 'Custom';
}

/**
 * Validate connection pool value
 */
export function validateConnectionPool(value: number | string): string | undefined {
  // For string inputs, use parseFloat to properly detect decimals
  const numValue = typeof value === 'string' ? parseFloat(value) : value;
  
  if (isNaN(numValue)) {
    return 'Must be a valid number';
  }
  
  if (numValue < 1) {
    return 'Must be at least 1';
  }
  
  if (numValue > 3000) {
    return 'Must be at most 3000';
  }
  
  if (!Number.isInteger(Number(numValue))) {
    return 'Must be a whole number';
  }
  
  return undefined;
}

/**
 * Validate DataStore form
 */
export function validateDataStoreForm(data: DataStoreFormData): DataStoreFormErrors {
  const errors: DataStoreFormErrors = {};
  
  const poolError = validateConnectionPool(data.maximumConnectionPool);
  if (poolError) {
    errors.maximumConnectionPool = poolError;
  }
  
  return errors;
}

/**
 * Check if form has validation errors
 */
export function hasFormErrors(errors: DataStoreFormErrors): boolean {
  return Object.keys(errors).length > 0;
}

/**
 * Validate JDBC parameters
 * Returns blocking errors and non-blocking warnings
 */
export function validateJdbcParameters(parameters: JdbcParameter[]): {
  validations: ParameterValidation[];
  hasBlockingErrors: boolean;
} {
  const validations: ParameterValidation[] = [];
  let hasBlockingErrors = false;
  const seenNames = new Set<string>();

  for (const param of parameters) {
    const validation: ParameterValidation = { id: param.id };

    // Skip validation for default read-only parameters
    if (param.isDefault && !param.isCustom) {
      continue;
    }

    // Blocking: Empty parameter name
    if (!param.name.trim()) {
      validation.error = 'Parameter name is required';
      hasBlockingErrors = true;
    }
    // Blocking: Empty value
    else if (!param.value.trim()) {
      validation.error = 'Value is required';
      hasBlockingErrors = true;
    }
    // Blocking: Duplicate parameter name
    else if (seenNames.has(param.name.toLowerCase())) {
      validation.error = 'Duplicate parameter name';
      hasBlockingErrors = true;
    }
    // Warning only: Unknown parameter (backend *might* reject unknown JDBC properties)
    else if (!isKnownParameter(param.name)) {
      validation.warning = 'Unknown JDBC parameter - verify this is correct for your database driver. Unrecognized properties may be rejected by the server.';
    }

    if (param.name) {
      seenNames.add(param.name.toLowerCase());
    }

    if (validation.error || validation.warning) {
      validations.push(validation);
    }
  }

  return { validations, hasBlockingErrors };
}

/**
 * Parse advanced string into JdbcParameter array
 * Format: key1=value1;key2=value2 or key1=value1&key2=value2
 */
export function parseAdvancedString(advanced: string): JdbcParameter[] {
  if (!advanced || !advanced.trim()) {
    return [];
  }

  const parameters: JdbcParameter[] = [];
  // Support both semicolon and ampersand separators
  const separator = advanced.includes(';') ? ';' : '&';
  const pairs = advanced.split(separator).filter(p => p.trim());

  for (const pair of pairs) {
    const [name, ...valueParts] = pair.split('=');
    const value = valueParts.join('='); // Handle values with = in them
    
    if (name && name.trim()) {
      parameters.push({
        id: `param-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
        name: name.trim(),
        value: value?.trim() || '',
        isDefault: false,
        isCustom: true,
      });
    }
  }

  return parameters;
}

/**
 * Serialize JdbcParameter array back to advanced string
 */
export function serializeParameters(parameters: JdbcParameter[]): string {
  const customParams = parameters.filter(p => p.isCustom && p.name.trim() && p.value.trim());
  
  if (customParams.length === 0) {
    return '';
  }

  return customParams
    .map(p => `${p.name}=${p.value}`)
    .join(';');
}

/**
 * Calculate effective configuration (defaults + custom overrides)
 * Shows all parameters with a name - empty values are shown as "(not set)"
 */
export function calculateEffectiveConfig(parameters: JdbcParameter[]): EffectiveParameter[] {
  const effective = new Map<string, EffectiveParameter>();

  // Process all parameters in order - later ones override earlier ones
  for (const param of parameters) {
    // Skip parameters without a name
    if (!param.name.trim()) {
      continue;
    }

    const source = param.isCustom ? 'Custom' : 'Default';
    const displayValue = param.value.trim() || '(not set)';

    effective.set(param.name.toLowerCase(), {
      name: param.name,
      value: displayValue,
      source,
    });
  }

  return Array.from(effective.values());
}

