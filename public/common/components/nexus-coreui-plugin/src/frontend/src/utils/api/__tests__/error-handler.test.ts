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

import { AxiosError, AxiosHeaders } from 'axios';
import {
  parseApiError,
  getErrorMessage,
  getFieldError,
  hasFieldErrors,
  fieldErrorsToMap,
  isAuthError,
  isPermissionError,
  isNotFoundError,
  isConflictError,
  isValidationError,
  isServerError,
  isNetworkError,
} from '../error-handler';
import { ApiError } from '../types';

/**
 * Create a mock Axios error for testing
 */
function createAxiosError(
  status: number | undefined,
  data?: unknown,
  code?: string,
  headers?: Record<string, string>
): AxiosError {
  const error = new Error('Request failed') as AxiosError;
  error.isAxiosError = true;
  error.code = code;

  if (status !== undefined) {
    error.response = {
      status,
      data,
      statusText: 'Error',
      headers: headers || {},
      config: { headers: new AxiosHeaders() },
    };
  }

  return error;
}

describe('error-handler', () => {
  describe('parseApiError', () => {
    describe('with Axios errors', () => {
      it('handles timeout errors (ECONNABORTED)', () => {
        const error = createAxiosError(undefined, undefined, 'ECONNABORTED');

        const result = parseApiError(error);

        expect(result.status).toBe(0);
        expect(result.code).toBe('TIMEOUT');
        expect(result.message).toContain('timed out');
      });

      it('handles network errors (no response)', () => {
        const error = createAxiosError(undefined);

        const result = parseApiError(error);

        expect(result.status).toBe(0);
        expect(result.code).toBe('NETWORK_ERROR');
        expect(result.message).toContain('Unable to connect');
      });

      it('handles 401 Unauthorized', () => {
        const error = createAxiosError(401);

        const result = parseApiError(error);

        expect(result.status).toBe(401);
        expect(result.code).toBe('UNAUTHORIZED');
        expect(result.message).toContain('session has expired');
      });

      it('handles 403 Forbidden with custom message', () => {
        const error = createAxiosError(403, { message: 'Access denied to repository' });

        const result = parseApiError(error);

        expect(result.status).toBe(403);
        expect(result.code).toBe('FORBIDDEN');
        expect(result.message).toBe('Access denied to repository');
      });

      it('handles 403 Forbidden with default message', () => {
        const error = createAxiosError(403);

        const result = parseApiError(error);

        expect(result.status).toBe(403);
        expect(result.message).toContain('do not have permission');
      });

      it('handles 404 Not Found with custom message', () => {
        const error = createAxiosError(404, { message: 'Role not found: admin' });

        const result = parseApiError(error);

        expect(result.status).toBe(404);
        expect(result.code).toBe('NOT_FOUND');
        expect(result.message).toBe('Role not found: admin');
      });

      it('handles 404 Not Found with default message', () => {
        const error = createAxiosError(404);

        const result = parseApiError(error);

        expect(result.status).toBe(404);
        expect(result.message).toContain('not found');
      });

      it('handles 409 Conflict with custom message', () => {
        const error = createAxiosError(409, { message: 'Role "admin" already exists' });

        const result = parseApiError(error);

        expect(result.status).toBe(409);
        expect(result.code).toBe('CONFLICT');
        expect(result.message).toBe('Role "admin" already exists');
      });

      it('handles 409 Conflict with default message', () => {
        const error = createAxiosError(409);

        const result = parseApiError(error);

        expect(result.status).toBe(409);
        expect(result.message).toContain('already exists');
      });

      it('handles 500 Server Error with backend message', () => {
        const error = createAxiosError(500, { message: 'Database connection failed' });

        const result = parseApiError(error);

        expect(result.status).toBe(500);
        expect(result.code).toBe('SERVER_ERROR');
        expect(result.message).toBe('Database connection failed');
      });

      it('handles 500 Server Error with string data', () => {
        const error = createAxiosError(500, 'Internal server error');

        const result = parseApiError(error);

        expect(result.status).toBe(500);
        expect(result.message).toBe('Internal server error');
      });

      it('handles 500 Server Error with default message', () => {
        const error = createAxiosError(500);

        const result = parseApiError(error);

        expect(result.status).toBe(500);
        expect(result.message).toContain('Server error');
        expect(result.message).toContain('500');
      });

      it('handles 502 Bad Gateway', () => {
        const error = createAxiosError(502);

        const result = parseApiError(error);

        expect(result.status).toBe(502);
        expect(result.code).toBe('SERVER_ERROR');
      });

      it('handles 503 Service Unavailable', () => {
        const error = createAxiosError(503, { message: 'Service maintenance' });

        const result = parseApiError(error);

        expect(result.status).toBe(503);
        expect(result.message).toBe('Service maintenance');
      });

      it('handles 504 Gateway Timeout', () => {
        const error = createAxiosError(504);

        const result = parseApiError(error);

        expect(result.status).toBe(504);
        expect(result.code).toBe('SERVER_ERROR');
      });

      it('includes request ID in server error message', () => {
        const error = createAxiosError(500, undefined, undefined, {
          'x-request-id': 'req-12345',
        });

        const result = parseApiError(error);

        expect(result.requestId).toBe('req-12345');
        expect(result.message).toContain('req-12345');
      });

      it('handles unknown status codes', () => {
        const error = createAxiosError(418, { message: "I'm a teapot" });

        const result = parseApiError(error);

        expect(result.status).toBe(418);
        expect(result.message).toBe("I'm a teapot");
      });

      it('handles unknown status with default message', () => {
        const error = createAxiosError(418);

        const result = parseApiError(error);

        expect(result.status).toBe(418);
        expect(result.message).toContain('Unable to complete request');
      });
    });

    describe('with 400 Bad Request (validation errors)', () => {
      it('parses Nexus array format validation errors', () => {
        const data = [
          { id: 'PARAMETER path', message: 'Path is required' },
          { id: 'PARAMETER name', message: 'Name cannot be empty' },
        ];
        const error = createAxiosError(400, data);

        const result = parseApiError(error);

        expect(result.status).toBe(400);
        expect(result.code).toBe('VALIDATION_ERROR');
        expect(result.fieldErrors).toHaveLength(2);
        expect(result.fieldErrors?.[0]).toEqual({
          field: 'path',
          message: 'Path is required',
        });
        expect(result.fieldErrors?.[1]).toEqual({
          field: 'name',
          message: 'Name cannot be empty',
        });
        expect(result.message).toContain('path: Path is required');
      });

      it('parses Nexus array format with wildcard id (*)', () => {
        const data = [{ id: '*', message: 'A file blob store must have a unique path' }];
        const error = createAxiosError(400, data);

        const result = parseApiError(error);

        expect(result.message).toBe('A file blob store must have a unique path');
        expect(result.fieldErrors).toBeUndefined();
      });

      it('parses Hibernate Validator format with errors array', () => {
        const data = {
          errors: [
            { field: 'name', message: 'Name is required' },
            { id: 'description', message: 'Description too long' },
          ],
        };
        const error = createAxiosError(400, data);

        const result = parseApiError(error);

        expect(result.fieldErrors).toHaveLength(2);
        expect(result.fieldErrors?.[0].field).toBe('name');
        expect(result.fieldErrors?.[1].field).toBe('description');
      });

      it('extracts field from message pattern', () => {
        const data = { message: "Field 'username' is required" };
        const error = createAxiosError(400, data);

        const result = parseApiError(error);

        expect(result.fieldErrors).toHaveLength(1);
        expect(result.fieldErrors?.[0].field).toBe('username');
      });

      it('handles single field error message', () => {
        const data = {
          errors: [{ field: 'email', message: 'Invalid email format' }],
        };
        const error = createAxiosError(400, data);

        const result = parseApiError(error);

        expect(result.message).toBe('Invalid email format');
      });

      it('handles multiple field errors with count', () => {
        const data = {
          errors: [
            { field: 'name' },
            { field: 'email' },
            { field: 'password' },
          ],
        };
        const error = createAxiosError(400, data);

        const result = parseApiError(error);

        expect(result.message).toContain('3 validation errors');
      });

      it('provides default message when no details available', () => {
        const error = createAxiosError(400);

        const result = parseApiError(error);

        expect(result.message).toBe('Invalid request. Please check your input.');
      });

      it('handles 422 Unprocessable Entity same as 400', () => {
        const data = [{ id: 'PARAMETER format', message: 'Invalid format' }];
        const error = createAxiosError(422, data);

        const result = parseApiError(error);

        expect(result.status).toBe(400); // Normalized to 400
        expect(result.code).toBe('VALIDATION_ERROR');
        expect(result.fieldErrors).toHaveLength(1);
      });
    });

    describe('with non-Axios errors', () => {
      it('handles Error objects', () => {
        const error = new Error('Something went wrong');

        const result = parseApiError(error);

        expect(result.status).toBe(0);
        expect(result.message).toBe('Something went wrong');
        expect(result.originalError).toBe(error);
      });

      it('handles Error with empty message', () => {
        const error = new Error('');

        const result = parseApiError(error);

        expect(result.message).toBe('An unexpected error occurred');
      });

      it('handles string errors', () => {
        const result = parseApiError('Network failure');

        expect(result.status).toBe(0);
        expect(result.message).toBe('Network failure');
      });

      it('handles unknown error types', () => {
        const result = parseApiError({ unknown: 'structure' });

        expect(result.status).toBe(0);
        expect(result.message).toBe('An unexpected error occurred');
      });

      it('handles null', () => {
        const result = parseApiError(null);

        expect(result.status).toBe(0);
        expect(result.message).toBe('An unexpected error occurred');
      });

      it('handles undefined', () => {
        const result = parseApiError(undefined);

        expect(result.status).toBe(0);
        expect(result.message).toBe('An unexpected error occurred');
      });
    });
  });

  describe('getErrorMessage', () => {
    it('returns the message from ApiError', () => {
      const error: ApiError = {
        status: 400,
        message: 'Validation failed',
      };

      expect(getErrorMessage(error)).toBe('Validation failed');
    });
  });

  describe('getFieldError', () => {
    it('returns field-specific error message', () => {
      const error: ApiError = {
        status: 400,
        message: 'Validation error',
        fieldErrors: [
          { field: 'name', message: 'Name is required' },
          { field: 'email', message: 'Invalid email' },
        ],
      };

      expect(getFieldError(error, 'name')).toBe('Name is required');
      expect(getFieldError(error, 'email')).toBe('Invalid email');
    });

    it('returns undefined for missing field', () => {
      const error: ApiError = {
        status: 400,
        message: 'Validation error',
        fieldErrors: [{ field: 'name', message: 'Name is required' }],
      };

      expect(getFieldError(error, 'password')).toBeUndefined();
    });

    it('returns undefined when no field errors', () => {
      const error: ApiError = {
        status: 500,
        message: 'Server error',
      };

      expect(getFieldError(error, 'name')).toBeUndefined();
    });
  });

  describe('hasFieldErrors', () => {
    it('returns true when field errors exist', () => {
      const error: ApiError = {
        status: 400,
        message: 'Validation error',
        fieldErrors: [{ field: 'name', message: 'Required' }],
      };

      expect(hasFieldErrors(error)).toBe(true);
    });

    it('returns false when no field errors', () => {
      const error: ApiError = {
        status: 400,
        message: 'Validation error',
      };

      expect(hasFieldErrors(error)).toBe(false);
    });

    it('returns false when field errors array is empty', () => {
      const error: ApiError = {
        status: 400,
        message: 'Validation error',
        fieldErrors: [],
      };

      expect(hasFieldErrors(error)).toBe(false);
    });
  });

  describe('fieldErrorsToMap', () => {
    it('converts field errors to a map', () => {
      const error: ApiError = {
        status: 400,
        message: 'Validation error',
        fieldErrors: [
          { field: 'name', message: 'Required' },
          { field: 'email', message: 'Invalid format' },
        ],
      };

      const map = fieldErrorsToMap(error);

      expect(map).toEqual({
        name: 'Required',
        email: 'Invalid format',
      });
    });

    it('returns empty object when no field errors', () => {
      const error: ApiError = {
        status: 500,
        message: 'Server error',
      };

      expect(fieldErrorsToMap(error)).toEqual({});
    });
  });

  describe('error type checks', () => {
    describe('isAuthError', () => {
      it('returns true for 401', () => {
        expect(isAuthError({ status: 401, message: '' })).toBe(true);
      });

      it('returns false for other status', () => {
        expect(isAuthError({ status: 403, message: '' })).toBe(false);
      });
    });

    describe('isPermissionError', () => {
      it('returns true for 403', () => {
        expect(isPermissionError({ status: 403, message: '' })).toBe(true);
      });

      it('returns false for other status', () => {
        expect(isPermissionError({ status: 401, message: '' })).toBe(false);
      });
    });

    describe('isNotFoundError', () => {
      it('returns true for 404', () => {
        expect(isNotFoundError({ status: 404, message: '' })).toBe(true);
      });

      it('returns false for other status', () => {
        expect(isNotFoundError({ status: 400, message: '' })).toBe(false);
      });
    });

    describe('isConflictError', () => {
      it('returns true for 409', () => {
        expect(isConflictError({ status: 409, message: '' })).toBe(true);
      });

      it('returns false for other status', () => {
        expect(isConflictError({ status: 400, message: '' })).toBe(false);
      });
    });

    describe('isValidationError', () => {
      it('returns true for 400', () => {
        expect(isValidationError({ status: 400, message: '' })).toBe(true);
      });

      it('returns true for 422', () => {
        expect(isValidationError({ status: 422, message: '' })).toBe(true);
      });

      it('returns false for other status', () => {
        expect(isValidationError({ status: 500, message: '' })).toBe(false);
      });
    });

    describe('isServerError', () => {
      it('returns true for 500+', () => {
        expect(isServerError({ status: 500, message: '' })).toBe(true);
        expect(isServerError({ status: 502, message: '' })).toBe(true);
        expect(isServerError({ status: 503, message: '' })).toBe(true);
        expect(isServerError({ status: 504, message: '' })).toBe(true);
      });

      it('returns false for client errors', () => {
        expect(isServerError({ status: 400, message: '' })).toBe(false);
        expect(isServerError({ status: 404, message: '' })).toBe(false);
      });
    });

    describe('isNetworkError', () => {
      it('returns true for status 0', () => {
        expect(isNetworkError({ status: 0, message: '' })).toBe(true);
      });

      it('returns false for non-zero status', () => {
        expect(isNetworkError({ status: 500, message: '' })).toBe(false);
      });
    });
  });
});
