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
 * REST API Error Handler
 *
 * Parses REST API error responses and provides detailed, actionable error messages.
 * Replaces generic "Unknown error" messages with specific, helpful information.
 *
 * Usage:
 *   try {
 *     await Axios.post('/api/endpoint', data);
 *   } catch (err) {
 *     const apiError = parseApiError(err);
 *     setError(apiError.message);
 *     setFieldErrors(apiError.fieldErrors);
 *   }
 */

import { AxiosError } from 'axios';
import { ApiError, FieldError, RestErrorResponse } from './types';

/**
 * Parse any error into a structured ApiError
 */
export function parseApiError(error: unknown): ApiError {
  // Handle Axios errors
  if (isAxiosError(error)) {
    return parseAxiosError(error);
  }

  // Handle Error objects
  if (error instanceof Error) {
    return {
      status: 0,
      message: error.message || 'An unexpected error occurred',
      originalError: error,
    };
  }

  // Handle string errors
  if (typeof error === 'string') {
    return {
      status: 0,
      message: error,
    };
  }

  // Unknown error type
  return {
    status: 0,
    message: 'An unexpected error occurred',
    originalError: error,
  };
}

/**
 * Parse Axios error response
 */
function parseAxiosError(error: AxiosError<RestErrorResponse>): ApiError {
  const status = error.response?.status || 0;
  const data = error.response?.data;

  // Extract request ID from headers if available
  const requestId = error.response?.headers?.['x-request-id'] as string | undefined;

  // No response (network error, timeout, etc.)
  if (!error.response) {
    if (error.code === 'ECONNABORTED') {
      return {
        status: 0,
        message: 'Request timed out. Please check your connection and try again.',
        code: 'TIMEOUT',
        requestId,
        originalError: error,
      };
    }
    return {
      status: 0,
      message: 'Unable to connect to server. Please check your network connection.',
      code: 'NETWORK_ERROR',
      requestId,
      originalError: error,
    };
  }

  // Parse response based on status code
  switch (status) {
    case 400:
      return parseBadRequest(data, requestId, error);

    case 401:
      return {
        status: 401,
        message: 'Your session has expired. Please log in again.',
        code: 'UNAUTHORIZED',
        requestId,
        originalError: error,
      };

    case 403:
      return {
        status: 403,
        message: data?.message || 'You do not have permission to perform this action. Contact your administrator if you need access.',
        code: 'FORBIDDEN',
        requestId,
        originalError: error,
      };

    case 404:
      return {
        status: 404,
        message: data?.message || 'The requested resource was not found.',
        code: 'NOT_FOUND',
        requestId,
        originalError: error,
      };

    case 409:
      return {
        status: 409,
        message: data?.message || 'This resource already exists or conflicts with another resource.',
        code: 'CONFLICT',
        requestId,
        originalError: error,
      };

    case 422:
      return parseBadRequest(data, requestId, error);

    case 500:
    case 502:
    case 503:
    case 504: {
      // Extract backend error message if available (can be string or object with message)
      let backendMessage: string | undefined;
      if (typeof data === 'string' && data.trim()) {
        backendMessage = data;
      } else if (data?.message) {
        backendMessage = data.message;
      }
      
      const message = backendMessage 
        ? backendMessage 
        : `Server error (${status}). Please try again later.`;
      
      return {
        status,
        message: requestId ? `${message} Reference: ${requestId}` : message,
        code: 'SERVER_ERROR',
        requestId,
        originalError: error,
      };
    }

    default:
      return {
        status,
        message: data?.message || 'Unable to complete request. Please try again.',
        requestId,
        originalError: error,
      };
  }
}

/**
 * Parse 400 Bad Request errors (validation errors)
 */
function parseBadRequest(
  data: RestErrorResponse | undefined | Array<{ id?: string; message?: string }>,
  requestId: string | undefined,
  error: AxiosError
): ApiError {
  const fieldErrors: FieldError[] = [];
  const messages: string[] = [];

  // Handle array of errors directly (Nexus validation format)
  // Example: [{"id":"PARAMETER path","message":"Path is required"}]
  // Example: [{"id":"*","message":"A file blob store must have a unique path..."}]
  if (Array.isArray(data)) {
    for (const err of data) {
      if (err.message) {
        const fieldName = err.id && err.id !== '*'
          ? err.id.replace('PARAMETER ', '')
          : null;
        // Include field name in message for clarity (e.g., "docker: must not be null")
        const displayMessage = fieldName
          ? `${fieldName}: ${err.message}`
          : err.message;
        messages.push(displayMessage);
        if (fieldName) {
          fieldErrors.push({
            field: fieldName,
            message: err.message,
          });
        }
      }
    }
  }
  // Parse nested errors array (Hibernate Validator format)
  else if (data?.errors && Array.isArray(data.errors)) {
    for (const err of data.errors) {
      if (err.field || err.id) {
        fieldErrors.push({
          field: err.field || err.id || 'unknown',
          message: err.message || 'Invalid value',
        });
        if (err.message) {
          messages.push(err.message);
        }
      }
    }
  }
  // Try to extract field from message if no explicit errors
  else if (data?.message) {
    messages.push(data.message);
    const fieldMatch = data.message.match(/(?:field|property|parameter)\s+['"]?(\w+)['"]?/i);
    if (fieldMatch) {
      fieldErrors.push({
        field: fieldMatch[1],
        message: data.message,
      });
    }
  }

  // Build user-friendly message - prefer the actual error messages from API
  let message: string;
  if (messages.length > 0) {
    message = messages.join('. ');
  } else if (fieldErrors.length > 0) {
    if (fieldErrors.length === 1) {
      message = `Validation error: ${fieldErrors[0].message}`;
    } else {
      message = `Please fix ${fieldErrors.length} validation errors`;
    }
  } else {
    message = 'Invalid request. Please check your input.';
  }

  return {
    status: 400,
    message,
    code: 'VALIDATION_ERROR',
    fieldErrors: fieldErrors.length > 0 ? fieldErrors : undefined,
    requestId,
    originalError: error,
  };
}

/**
 * Type guard for Axios errors
 */
function isAxiosError(error: unknown): error is AxiosError<RestErrorResponse> {
  return (
    typeof error === 'object' &&
    error !== null &&
    'isAxiosError' in error &&
    (error as AxiosError).isAxiosError === true
  );
}

/**
 * Get user-friendly message for display
 */
export function getErrorMessage(error: ApiError): string {
  return error.message;
}

/**
 * Get field-specific error message
 */
export function getFieldError(error: ApiError, fieldName: string): string | undefined {
  return error.fieldErrors?.find((fe) => fe.field === fieldName)?.message;
}

/**
 * Check if error has field-level validation errors
 */
export function hasFieldErrors(error: ApiError): boolean {
  return (error.fieldErrors?.length || 0) > 0;
}

/**
 * Convert field errors to a map for easy lookup
 */
export function fieldErrorsToMap(error: ApiError): Record<string, string> {
  const map: Record<string, string> = {};
  if (error.fieldErrors) {
    for (const fe of error.fieldErrors) {
      map[fe.field] = fe.message;
    }
  }
  return map;
}

/**
 * Check if error is a specific type
 */
export function isAuthError(error: ApiError): boolean {
  return error.status === 401;
}

export function isPermissionError(error: ApiError): boolean {
  return error.status === 403;
}

export function isNotFoundError(error: ApiError): boolean {
  return error.status === 404;
}

export function isConflictError(error: ApiError): boolean {
  return error.status === 409;
}

export function isValidationError(error: ApiError): boolean {
  return error.status === 400 || error.status === 422;
}

export function isServerError(error: ApiError): boolean {
  return error.status >= 500;
}

export function isNetworkError(error: ApiError): boolean {
  return error.status === 0;
}
