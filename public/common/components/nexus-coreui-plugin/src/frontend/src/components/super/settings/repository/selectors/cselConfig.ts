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
 * CSEL (Content Selector Expression Language) Configuration
 *
 * This static configuration file defines the attributes, operators, and examples
 * available for CSEL expressions. It is UI-owned and versioned with the frontend.
 *
 * NO BACKEND CHANGES REQUIRED - This is purely frontend configuration.
 */

export interface CSELAttribute {
  name: string;
  description: string;
  operators: string[];
  formatSpecific?: string; // e.g., "maven2 only"
}

export interface CSELOperator {
  symbol: string;
  description: string;
  isLogical?: boolean;
}

export interface CSELExample {
  label: string;
  expression: string;
  description?: string;
}

export interface CSELConfig {
  attributes: CSELAttribute[];
  operators: CSELOperator[];
  logicalOperators: CSELOperator[];
  examples: CSELExample[];
}

/**
 * Static CSEL configuration
 */
export const CSEL_CONFIG: CSELConfig = {
  attributes: [
    {
      name: 'format',
      description: 'Repository format (e.g., maven2, npm, docker)',
      operators: ['==', '!=', '=~'],
    },
    {
      name: 'path',
      description: 'Asset path within the repository',
      operators: ['==', '!=', '=~', '=^'],
    },
    {
      name: 'coordinate.repositoryName',
      description: 'Name of the repository',
      operators: ['==', '!='],
    },
    {
      name: 'coordinate.groupId',
      description: 'Maven group ID',
      operators: ['==', '!=', '=~'],
      formatSpecific: 'maven2 only',
    },
    {
      name: 'coordinate.artifactId',
      description: 'Maven artifact ID',
      operators: ['==', '!=', '=~'],
      formatSpecific: 'maven2 only',
    },
    {
      name: 'coordinate.version',
      description: 'Component version',
      operators: ['==', '!=', '=~'],
    },
    {
      name: 'coordinate.extension',
      description: 'File extension (e.g., jar, pom)',
      operators: ['==', '!=', '=~'],
      formatSpecific: 'maven2 only',
    },
    {
      name: 'coordinate.classifier',
      description: 'Maven classifier (e.g., sources, javadoc)',
      operators: ['==', '!=', '=~'],
      formatSpecific: 'maven2 only',
    },
  ],

  operators: [
    { symbol: '==', description: 'Equals' },
    { symbol: '!=', description: 'Not equals' },
    { symbol: '=~', description: 'Regular expression match' },
    { symbol: '=^', description: 'Starts with' },
  ],

  logicalOperators: [
    { symbol: 'and', description: 'Logical AND', isLogical: true },
    { symbol: 'or', description: 'Logical OR', isLogical: true },
    { symbol: 'not', description: 'Logical NOT', isLogical: true },
  ],

  examples: [
    // Simple format selectors - safe to use
    {
      label: 'Raw format',
      expression: 'format == "raw"',
      description: 'Matches all content in raw repositories',
    },
    {
      label: 'Maven format',
      expression: 'format == "maven2"',
      description: 'Matches all Maven artifacts',
    },
    {
      label: 'npm format',
      expression: 'format == "npm"',
      description: 'Matches all npm packages',
    },
    {
      label: 'Docker format',
      expression: 'format == "docker"',
      description: 'Matches all Docker images',
    },
    // Path-based selectors - work with any format
    {
      label: 'Path starts with /org',
      expression: 'path =^ "/org"',
      description: 'Matches content with paths starting with /org',
    },
    // Combined expressions (from Default UI documentation)
    {
      label: 'Maven + path filter',
      expression: 'format == "maven2" and path =^ "/org"',
      description: 'Matches Maven artifacts with paths starting with /org',
    },
  ],
};

/**
 * Get attribute by name
 */
export function getAttributeByName(name: string): CSELAttribute | undefined {
  return CSEL_CONFIG.attributes.find((attr) => attr.name === name);
}

/**
 * Get all attribute names
 */
export function getAttributeNames(): string[] {
  return CSEL_CONFIG.attributes.map((attr) => attr.name);
}

/**
 * Get operators for a specific attribute
 */
export function getOperatorsForAttribute(attributeName: string): string[] {
  const attribute = getAttributeByName(attributeName);
  return attribute?.operators || CSEL_CONFIG.operators.map((op) => op.symbol);
}

/**
 * Check if an attribute is known
 */
export function isKnownAttribute(name: string): boolean {
  return CSEL_CONFIG.attributes.some((attr) => attr.name === name);
}

/**
 * Check if an operator is valid
 */
export function isValidOperator(symbol: string): boolean {
  return (
    CSEL_CONFIG.operators.some((op) => op.symbol === symbol) ||
    CSEL_CONFIG.logicalOperators.some((op) => op.symbol === symbol)
  );
}

/**
 * Check if operator is valid for attribute
 */
export function isOperatorValidForAttribute(attributeName: string, operator: string): boolean {
  const attribute = getAttributeByName(attributeName);
  if (!attribute) return true; // Unknown attributes allow any operator
  return attribute.operators.includes(operator);
}

