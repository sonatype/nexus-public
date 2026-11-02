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

// JAX-RS ConstraintViolationExceptionMapper prefixes the `id` field of each
// validation error with the leaf bean's simple name (`HelperBean.`) followed by
// the property path (often `attributes.<field>` for repository config). Neither
// segment is part of the frontend form's field naming, so we strip both before
// surfacing field-level errors. (NEXUS-51599)
const HELPER_BEAN_PREFIX = 'HelperBean.';
const ATTRIBUTES_PREFIX = 'attributes.';

const NOTNULL_CONSTRAINT_KEY = 'org.sonatype.nexus.validation.constraint.notnull';

const CONSTRAINT_MESSAGES: Record<string, string> = {
  'org.sonatype.nexus.validation.constraint.name':
    'Only letters, digits, hyphens (-), underscores (_), and dots (.) are allowed and may not start with underscore or dot.',
  'org.sonatype.nexus.validation.constraint.case': 'Case must be the correct value.',
  'org.sonatype.nexus.validation.constraint.portnumber': 'Port number must be within the valid range.',
  'org.sonatype.nexus.validation.constraint.hostname': 'Hostname must be valid.',
  [NOTNULL_CONSTRAINT_KEY]: 'Must not be null.',
};

// Matches both the Sonatype constraint text and Jakarta's default @NotNull template ("must not be null"), which the v1 REST endpoint emits verbatim; case-insensitive, trailing period optional. (NEXUS-54609)
const NOTNULL_MESSAGE_PATTERN = /^must not be null\.?$/i;

function resolveConstraintMessage(message: string): string {
  const match = message.match(/^\{([^}]+)\}$/);
  if (match) {
    return CONSTRAINT_MESSAGES[match[1]] ?? message;
  }
  return message;
}

// Humanize a bean-property key for validation toasts, e.g. `blobStoreName` -> "Blob store name". (NEXUS-54609)
function humanizeFieldName(field: string): string {
  const label = field
    // last path segment only: `attributes.storage.blobStoreName` -> `blobStoreName`
    .split('.')
    .pop()!
    // split a lowercase/digit followed by an uppercase into two words
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_-]+/g, ' ')
    .trim()
    .toLowerCase();
  if (!label) {
    return field;
  }
  return label.charAt(0).toUpperCase() + label.slice(1);
}

// Prefix the field label onto a generic notnull message so the toast is actionable. (NEXUS-54609)
function contextualizeMessage(resolvedMessage: string, fieldName: string | null): string {
  if (fieldName && NOTNULL_MESSAGE_PATTERN.test(resolvedMessage.trim())) {
    return `${humanizeFieldName(fieldName)} must not be null.`;
  }
  return resolvedMessage;
}

// WebApplicationMessageException callers on the backend commonly pass a JSON string
// literal as the message (e.g. `"\"Repository not found\""`), so the parsed `message`
// field itself is wrapped in a redundant pair of literal quote characters. Strip one
// layer so toasts show the plain text instead of `"Repository not found"`.
function unwrapQuotedMessage(message: string | undefined): string | undefined {
  if (message && message.length >= 2 && message.startsWith('"') && message.endsWith('"')) {
    return message.slice(1, -1);
  }
  return message;
}

// ValidationErrorXO#toString() shape sent as text/plain by WebApplicationMessageException callers.
const VALIDATION_ERROR_XO = /^ValidationErrorXO\{id='.*', message='([\s\S]*)'\}$/;

// Strips a leaked Java Throwable#toString() FQCN prefix (e.g. "jakarta.validation.ValidationException: ", incl. bare Exception/Error/Throwable) so toasts read as the message; fresh /g literal per call avoids shared lastIndex state and collapses chained causes to the innermost message. (NEXUS-54871)
export function stripJavaExceptionPrefix(message: string): string;
export function stripJavaExceptionPrefix(message: string | undefined): string | undefined;
export function stripJavaExceptionPrefix(message: string | undefined): string | undefined {
  return message?.replace(/(?:[a-zA-Z_$][\w$]*\.)+[\w$]*(?:Exception|Error|Throwable):\s+/g, '');
}

// Reads the message from a JSON RestErrorResponse or a text/plain string body (axios leaves a non-JSON body as a string).
function extractMessage(data: RestErrorResponse | string | undefined): string | undefined {
  if (typeof data === 'string') {
    if (!data.trim()) {
      return undefined;
    }
    const match = data.match(VALIDATION_ERROR_XO);
    return stripJavaExceptionPrefix(match ? match[1] : data);
  }
  return stripJavaExceptionPrefix(data?.message);
}

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
function parseAxiosError(error: AxiosError<RestErrorResponse | string>): ApiError {
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
      return parseBadRequest(status, data, requestId, error);

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
        message: unwrapQuotedMessage(extractMessage(data)) || 'You do not have permission to perform this action. Contact your administrator if you need access.',
        code: 'FORBIDDEN',
        requestId,
        originalError: error,
      };

    case 404:
      return {
        status: 404,
        message: unwrapQuotedMessage(extractMessage(data)) || 'The requested resource was not found.',
        code: 'NOT_FOUND',
        requestId,
        originalError: error,
      };

    case 405:
      return {
        status: 405,
        message: unwrapQuotedMessage(extractMessage(data)) || 'This operation is not supported for the current resource state.',
        code: 'METHOD_NOT_ALLOWED',
        requestId,
        originalError: error,
      };

    case 409:
      return {
        status: 409,
        message: unwrapQuotedMessage(extractMessage(data)) || 'This resource already exists or conflicts with another resource.',
        code: 'CONFLICT',
        requestId,
        originalError: error,
      };

    case 422:
      return parseBadRequest(status, data, requestId, error);

    case 500:
    case 502:
    case 503:
    case 504: {
      // Extract backend error message if available (can be string or object with message)
      const backendMessage = unwrapQuotedMessage(extractMessage(data));

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
        message: unwrapQuotedMessage(extractMessage(data)) || 'Unable to complete request. Please try again.',
        requestId,
        originalError: error,
      };
  }
}

/**
 * Parse 400 Bad Request errors (validation errors)
 */
function parseBadRequest(
  httpStatus: number,
  data: RestErrorResponse | string | undefined | Array<{ id?: string; message?: string }>,
  requestId: string | undefined,
  error: AxiosError
): ApiError {
  const fieldErrors: FieldError[] = [];
  const messages: string[] = [];

  // Handle array of errors directly (Nexus validation format)
  // Example: [{"id":"PARAMETER path","message":"Path is required"}]
  // Example: [{"id":"*","message":"A file blob store must have a unique path..."}]
  // Example: [{"id":"HelperBean.attributes.docker.httpPort","message":"Port must be unique..."}]
  if (Array.isArray(data)) {
    for (const err of data) {
      if (err.message) {
        const resolvedMessage = resolveConstraintMessage(err.message);
        let fieldName = err.id && err.id !== '*'
          ? err.id.replace('PARAMETER ', '')
          : null;
        // Anchor the HelperBean. and attributes. strips to the start of the
        // string so an `id` that legitimately contains those tokens deeper in
        // the property path (e.g. a field literally named myHelperBean.foo)
        // isn't mangled mid-key.
        if (fieldName?.startsWith(HELPER_BEAN_PREFIX)) {
          fieldName = fieldName.slice(HELPER_BEAN_PREFIX.length);
        }
        if (fieldName?.startsWith(ATTRIBUTES_PREFIX)) {
          fieldName = fieldName.slice(ATTRIBUTES_PREFIX.length);
        }
        messages.push(contextualizeMessage(resolvedMessage, fieldName));
        if (fieldName) {
          fieldErrors.push({
            field: fieldName,
            message: resolvedMessage,
          });
        }
      }
    }
  }
  // Parse nested errors array (Hibernate Validator format)
  else if (typeof data === 'object' && data?.errors && Array.isArray(data.errors)) {
    for (const err of data.errors) {
      if (err.field || err.id) {
        const resolvedMessage = resolveConstraintMessage(err.message || 'Invalid value');
        const fieldName = err.field || err.id || 'unknown';
        fieldErrors.push({
          field: fieldName,
          message: resolvedMessage,
        });
        messages.push(contextualizeMessage(resolvedMessage, fieldName));
      }
    }
  }
  // Try to extract field from message if no explicit errors
  else if (typeof data === 'object' && data?.message) {
    messages.push(data.message);
    const fieldMatch = data.message.match(/(?:field|property|parameter)\s+['"]?(\w+)['"]?/i);
    if (fieldMatch) {
      fieldErrors.push({
        field: fieldMatch[1],
        message: data.message,
      });
    }
  }
  // text/plain WebApplicationMessageException body: unwrapped ValidationErrorXO message, else verbatim.
  else {
    const extracted = extractMessage(data);
    if (extracted) {
      messages.push(extracted);
    }
  }

  // Build user-friendly message - prefer the actual error messages from API
  let message: string;
  if (messages.length > 0) {
    // Join with ". ", collapsing the doubled period when a message already ends in one. (NEXUS-54609)
    message = messages.join('. ').replace(/\.\.\s/g, '. ');
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
    status: httpStatus,
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

// Runtime Error carrying the parsed ApiError fields. Consumers that catch this
// can branch on `status` / inspect `fieldErrors` without re-parsing.
export type ThrownApiError = Error & {
  status: number;
  code?: string;
  fieldErrors?: FieldError[];
  requestId?: string;
};

/**
 * Wrap a raw thrown value into a ThrownApiError that preserves the parsed
 * status/code/fieldErrors/requestId. Use inside catch blocks to re-throw with
 * the full parsed shape intact rather than losing it via `new Error(msg)`.
 */
export function wrapApiError(error: unknown): ThrownApiError {
  const apiError = parseApiError(error);
  const wrapped = new Error(apiError.message) as ThrownApiError;
  wrapped.status = apiError.status;
  if (apiError.code !== undefined) wrapped.code = apiError.code;
  if (apiError.fieldErrors !== undefined) wrapped.fieldErrors = apiError.fieldErrors;
  if (apiError.requestId !== undefined) wrapped.requestId = apiError.requestId;
  return wrapped;
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
