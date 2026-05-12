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
 * Check if validation errors object has any errors
 */
export function hasValidationErrors(errors: Record<string, string | null>): boolean {
  return Object.values(errors).some((error) => error !== null && error !== undefined);
}

/**
 * Extract error message from various error formats (Axios, Error, string, etc.)
 */
export function extractErrorMessage(error: unknown): string {
  if (typeof error === 'string') {
    return error;
  }

  if (error instanceof Error) {
    return error.message;
  }

  if (typeof error === 'object' && error !== null) {
    const err = error as Record<string, unknown>;

    // Axios error format
    if (err.response && typeof err.response === 'object') {
      const response = err.response as Record<string, unknown>;
      if (response.data && typeof response.data === 'object') {
        const data = response.data as Record<string, unknown>;
        if (typeof data.message === 'string') {
          return data.message;
        }
      }
    }

    // Standard message property
    if (typeof err.message === 'string') {
      return err.message;
    }
  }

  return 'An unexpected error occurred';
}

/**
 * Convert field name to path array for Ramda functions
 * Supports dot notation: 'user.name' -> ['user', 'name']
 */
export function toPathArray(name: string): string[] {
  return name.split('.');
}
