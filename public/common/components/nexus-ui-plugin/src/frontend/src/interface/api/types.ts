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
 * Common API Types
 *
 * Shared type definitions for REST API interactions.
 * Part of the ExtDirect migration - standardizes error handling.
 */

/**
 * Structured API error with detailed information
 */
export interface ApiError {
  /** HTTP status code */
  status: number;

  /** Human-readable error message */
  message: string;

  /** Error code for programmatic handling */
  code?: string;

  /** Field-level validation errors */
  fieldErrors?: FieldError[];

  /** Request ID for debugging/support */
  requestId?: string;

  /** Original error for debugging */
  originalError?: unknown;
}

/**
 * Field-level validation error
 */
export interface FieldError {
  /** Field name (e.g., "name", "actions") */
  field: string;

  /** Error message for this field */
  message: string;

  /** Rejected value (if available) */
  rejectedValue?: unknown;
}

/**
 * Standard REST API error response format
 * Based on actual Nexus REST API responses
 */
export interface RestErrorResponse {
  /** Unique error ID */
  id?: string;

  /** Error message */
  message?: string;

  /** Nested validation errors (Hibernate Validator format) */
  errors?: Array<{
    id?: string;
    message?: string;
    field?: string;
  }>;
}

/**
 * Pagination parameters for list endpoints
 */
export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}

/**
 * Paginated response wrapper
 */
export interface PaginatedResponse<T> {
  items: T[];
  totalCount: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
}

/**
 * Common API response states for hooks
 */
export interface ApiState<T> {
  data: T | null;
  loading: boolean;
  error: ApiError | null;
}

/**
 * HTTP methods supported
 */
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

/**
 * Request configuration
 */
export interface RequestConfig {
  /** Request timeout in milliseconds */
  timeout?: number;

  /** Additional headers */
  headers?: Record<string, string>;

  /** Query parameters */
  params?: Record<string, string | number | boolean>;

  /** Skip error handling (for custom handling) */
  skipErrorHandling?: boolean;
}
