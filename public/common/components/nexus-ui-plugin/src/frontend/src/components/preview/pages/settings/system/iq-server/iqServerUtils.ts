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

import { IqServerConfiguration, IqValidationErrors, PASSWORD_PLACEHOLDER } from './types';

/** validateIqConfig doesn't read `properties`, so it accepts either the wire shape
 *  (IqServerConfiguration, string properties) or the form shape (IqServerFormData,
 *  array properties) — both are structurally assignable once `properties` is omitted. */
type IqConfigForValidation = Omit<IqServerConfiguration, 'properties'>;

/**
 * Validates if a string is a valid URL
 */
export function isValidUrl(url: string): boolean {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
}

/**
 * Validates IQ Server configuration and returns validation errors
 */
export function validateIqConfig(config: IqConfigForValidation, pristineConfig: IqConfigForValidation): IqValidationErrors {
  const errors: IqValidationErrors = {};

  if (!config.url?.trim()) {
    errors.url = 'IQ Server URL is required';
  } else if (!isValidUrl(config.url)) {
    errors.url = 'Please enter a valid URL';
  }

  if (!config.authenticationType) {
    errors.authenticationType = 'Authentication method is required';
  }

  if (config.authenticationType === 'USER') {
    if (!config.username?.trim()) {
      errors.username = 'Username is required';
    }

    // Password is required for new configs or when URL changes
    const urlChanged = pristineConfig.url && pristineConfig.url !== config.url;
    const isPlaceholder = config.password === PASSWORD_PLACEHOLDER;

    if (!config.password?.trim() && !isPlaceholder) {
      errors.password = 'Password is required';
    } else if (urlChanged && isPlaceholder) {
      errors.password = 'Password is required when changing the URL';
    }
  }

  if (config.timeoutSeconds !== null && (config.timeoutSeconds < 1 || config.timeoutSeconds > 3600)) {
    errors.timeoutSeconds = 'Timeout must be between 1 and 3600 seconds';
  }

  return errors;
}

/**
 * Parses the verification reason into application names or a status message.
 * Backend returns either comma-separated app names or messages like "No applications configured yet."
 */
export function parseApplicationReason(reason: string): { isList: boolean; items: string[] } {
  const trimmed = reason.trim();
  const appsPrefix = 'Applications: ';
  if (trimmed.includes(appsPrefix)) {
    const after = trimmed.split(appsPrefix)[1]?.trim() ?? '';
    const items = after.split(',').map((s) => s.trim()).filter(Boolean);
    return { isList: items.length > 0, items };
  }
  if (
    trimmed.toLowerCase().startsWith('no applications') ||
    (trimmed.startsWith('Connection successful') && !trimmed.includes(','))
  ) {
    return { isList: false, items: [trimmed] };
  }
  const items = trimmed.split(',').map((s) => s.trim()).filter(Boolean);
  return { isList: items.length > 0, items };
}

/** "Connected v1.2.3" -> " (v1.2.3)"; no match -> "". Mirrors the inline regex used today. */
export function parseVersion(reason?: string): string {
  const m = reason?.match(/v?(\d+\.\d+(?:\.\d+)?)/);
  return m ? ` (v${m[1]})` : '';
}
