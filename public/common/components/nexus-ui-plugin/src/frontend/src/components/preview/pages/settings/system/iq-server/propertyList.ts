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

import { IqProperty, PropertyValidation } from './types';

export function generateId(): string {
  return `property-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

/**
 * Parse a raw IQ Server `properties` string into rows. Only plain `name=value` lines
 * round-trip — Java `Properties.load()` syntax this doesn't attempt to preserve (comments,
 * ':' separators, line continuations, escapes) is dropped and counted in `droppedLineCount`
 * so the caller can warn the user. Splits on the first '=' only, so values may themselves
 * contain '='. A line with a name but an empty value (`foo=`) is kept, not dropped — it's
 * caught by `validateProperties`'s "Value is required" error instead.
 */
export function parsePropertiesString(raw: string): { properties: IqProperty[]; droppedLineCount: number } {
  const properties: IqProperty[] = [];
  let droppedLineCount = 0;

  const lines = (raw ?? '').split(/\r?\n/);
  for (const line of lines) {
    if (!line.trim()) continue;

    const eq = line.indexOf('=');
    const name = eq >= 0 ? line.slice(0, eq).trim() : '';
    if (eq >= 0 && name) {
      properties.push({ id: generateId(), name, value: line.slice(eq + 1).trim() });
    } else {
      droppedLineCount++;
    }
  }

  return { properties, droppedLineCount };
}

/**
 * Serialize rows back into a `name=value` newline-separated string, matching the format
 * `parsePropertiesString` reads. Rows with an empty name or value are skipped.
 */
export function serializeProperties(properties: IqProperty[]): string {
  return properties
    .filter((p) => p.name.trim() && p.value.trim())
    .map((p) => `${p.name}=${p.value}`)
    .join('\n');
}

/**
 * Validate rows: required name, required value, duplicate name (case-insensitive).
 * There's no known catalog of valid IQ property names, so unlike the JDBC parameter
 * editor there's no "unknown parameter" warning tier — only these blocking errors.
 */
export function validateProperties(properties: IqProperty[]): {
  validations: PropertyValidation[];
  hasBlockingErrors: boolean;
} {
  const validations: PropertyValidation[] = [];
  let hasBlockingErrors = false;
  const seenNames = new Set<string>();

  for (const property of properties) {
    const validation: PropertyValidation = { id: property.id };

    if (!property.name.trim()) {
      validation.error = 'Parameter name is required';
      hasBlockingErrors = true;
    } else if (!property.value.trim()) {
      validation.error = 'Value is required';
      hasBlockingErrors = true;
    } else if (seenNames.has(property.name.toLowerCase())) {
      validation.error = 'Duplicate parameter name';
      hasBlockingErrors = true;
    }

    if (property.name) {
      seenNames.add(property.name.toLowerCase());
    }

    if (validation.error) {
      validations.push(validation);
    }
  }

  return { validations, hasBlockingErrors };
}
