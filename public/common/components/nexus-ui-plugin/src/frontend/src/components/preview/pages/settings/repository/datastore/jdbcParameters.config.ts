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

/**
 * JDBC Parameter Type
 */
export type JdbcParameterType = 'string' | 'number' | 'boolean' | 'enum';

/**
 * JDBC Parameter Definition
 */
export interface JdbcParameterDefinition {
  /** Parameter name (case-sensitive) */
  name: string;
  /** Human-readable description */
  description: string;
  /** Value type */
  type: JdbcParameterType;
  /** Allowed values for enum type */
  allowedValues?: string[];
  /** Default value (informational) */
  defaultValue?: string;
  /** Minimum value for number type */
  min?: number;
  /** Maximum value for number type */
  max?: number;
  /** Unit label (e.g., 'ms', 'seconds') */
  unit?: string;
  /** Category for grouping in UI */
  category: 'connection' | 'timeout' | 'ssl' | 'performance' | 'other';
}

/**
 * Known JDBC Parameters Configuration
 *
 * Nexus's DataStore only supports PostgreSQL and H2. H2 connection settings use an
 * entirely different (SET-style, uppercase) syntax than JDBC Properties, so this catalog
 * is PostgreSQL-only (pgjdbc property names) — see the help panel caption in
 * JdbcParameterEditor.tsx. It's deliberately curated to the parameters admins most
 * commonly need, not the full pgjdbc property list; unknown parameters aren't blocked,
 * just unassisted (no autocomplete/dropdown/validated range).
 *
 * To add a new parameter:
 * 1. Add it to the appropriate category section
 * 2. Specify the type and validation rules
 * 3. Include allowed values for enum types
 */
export const JDBC_PARAMETERS_CONFIG: JdbcParameterDefinition[] = [
  // ===================
  // Connection Parameters
  // ===================
  {
    name: 'ApplicationName',
    description: 'Application name to identify this connection in database logs',
    type: 'string',
    category: 'connection',
    defaultValue: 'Nexus Repository',
  },
  {
    name: 'currentSchema',
    description: 'Default schema to use for unqualified table names',
    type: 'string',
    category: 'connection',
  },
  {
    name: 'readOnly',
    description: 'Set connection to read-only mode',
    type: 'boolean',
    allowedValues: ['true', 'false'],
    category: 'connection',
    defaultValue: 'false',
  },

  // ===================
  // Timeout Parameters
  // ===================
  {
    name: 'connectTimeout',
    description: 'Time to wait when establishing a connection',
    type: 'number',
    min: 0,
    max: 600,
    unit: 'seconds',
    category: 'timeout',
    defaultValue: '10',
  },
  {
    name: 'socketTimeout',
    description: 'Time to wait for socket read operations',
    type: 'number',
    min: 0,
    max: 600,
    unit: 'seconds',
    category: 'timeout',
    defaultValue: '0',
  },
  {
    name: 'loginTimeout',
    description: 'Time to wait for login to complete',
    type: 'number',
    min: 0,
    max: 600,
    unit: 'seconds',
    category: 'timeout',
    defaultValue: '0',
  },

  // ===================
  // SSL/TLS Parameters
  // ===================
  {
    name: 'ssl',
    description: 'Enable SSL/TLS encrypted connection',
    type: 'boolean',
    allowedValues: ['true', 'false'],
    category: 'ssl',
    defaultValue: 'false',
  },
  {
    name: 'sslmode',
    description: 'SSL connection mode',
    type: 'enum',
    allowedValues: ['disable', 'allow', 'prefer', 'require', 'verify-ca', 'verify-full'],
    category: 'ssl',
    defaultValue: 'prefer',
  },

  // ===================
  // Performance Parameters
  // ===================
  {
    name: 'reWriteBatchedInserts',
    description: 'Rewrite batched INSERT statements for better performance',
    type: 'boolean',
    allowedValues: ['true', 'false'],
    category: 'performance',
    defaultValue: 'false',
  },

  // ===================
  // Other Parameters
  // ===================
  {
    name: 'assumeMinServerVersion',
    description: 'Assume minimum server version for feature compatibility',
    type: 'string',
    category: 'other',
  },
];

/**
 * Get parameter definition by name
 */
export function getParameterDefinition(name: string): JdbcParameterDefinition | undefined {
  return JDBC_PARAMETERS_CONFIG.find(p => p.name.toLowerCase() === name.toLowerCase());
}

/**
 * Get all parameter names for autocomplete
 */
export function getAllParameterNames(): string[] {
  return JDBC_PARAMETERS_CONFIG.map(p => p.name);
}

/**
 * Get parameters by category
 */
export function getParametersByCategory(category: JdbcParameterDefinition['category']): JdbcParameterDefinition[] {
  return JDBC_PARAMETERS_CONFIG.filter(p => p.category === category);
}

/**
 * Get parameter description
 */
export function getParameterDescription(name: string): string {
  const def = getParameterDefinition(name);
  if (!def) return 'Unknown parameter - not in standard JDBC parameter list';
  
  let desc = def.description;
  if (def.unit) desc += ` (${def.unit})`;
  if (def.defaultValue) desc += `. Default: ${def.defaultValue}`;
  return desc;
}

/**
 * Check if parameter name is known
 */
export function isKnownParameter(name: string): boolean {
  return JDBC_PARAMETERS_CONFIG.some(p => p.name.toLowerCase() === name.toLowerCase());
}

/**
 * Validate parameter value based on its definition
 */
export function validateParameterValue(name: string, value: string): string | undefined {
  const def = getParameterDefinition(name);
  
  // Unknown parameters - just warn, don't block
  if (!def) {
    return undefined;
  }

  // Empty value is handled separately
  if (!value.trim()) {
    return undefined;
  }

  switch (def.type) {
    case 'boolean':
      if (def.allowedValues && !def.allowedValues.includes(value.toLowerCase())) {
        return `Must be one of: ${def.allowedValues.join(', ')}`;
      }
      break;

    case 'number': {
      const num = parseFloat(value);
      if (Number.isNaN(num)) {
        return 'Must be a valid number';
      }
      if (def.min !== undefined && num < def.min) {
        return `Must be at least ${def.min}${def.unit ? ` ${def.unit}` : ''}`;
      }
      if (def.max !== undefined && num > def.max) {
        return `Must be at most ${def.max}${def.unit ? ` ${def.unit}` : ''}`;
      }
      break;
    }

    case 'enum':
      if (def.allowedValues && !def.allowedValues.includes(value)) {
        return `Must be one of: ${def.allowedValues.join(', ')}`;
      }
      break;

    case 'string':
      // No validation for generic strings
      break;
  }

  return undefined;
}

/**
 * Get allowed values for a parameter (for dropdowns)
 */
export function getAllowedValues(name: string): string[] | undefined {
  const def = getParameterDefinition(name);
  return def?.allowedValues;
}

/**
 * Get category display name
 */
export function getCategoryDisplayName(category: JdbcParameterDefinition['category']): string {
  const names: Record<JdbcParameterDefinition['category'], string> = {
    connection: 'Connection',
    timeout: 'Timeouts',
    ssl: 'SSL/TLS',
    performance: 'Performance',
    other: 'Other',
  };
  return names[category];
}


