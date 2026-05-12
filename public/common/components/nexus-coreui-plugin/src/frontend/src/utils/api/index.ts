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
 * REST API Utilities
 *
 * Shared utilities for REST API interactions in the Preview UI.
 * Part of the ExtDirect to REST migration.
 *
 * Usage:
 *   import { restClient, parseApiError, ENDPOINTS } from '@/utils/api';
 *
 *   try {
 *     const data = await restClient.get<MyType>(ENDPOINTS.PRIVILEGES);
 *   } catch (err) {
 *     const apiError = parseApiError(err);
 *     console.error(apiError.message);
 *     if (apiError.fieldErrors) {
 *       // Handle field-level validation errors
 *     }
 *   }
 */

// Types
export type {
  ApiError,
  FieldError,
  RestErrorResponse,
  PaginationParams,
  PaginatedResponse,
  ApiState,
  HttpMethod,
  RequestConfig,
} from './types';

// Error handling
export {
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
} from './error-handler';

// REST client
export {
  restClient,
  urlBuilder,
  API_BASE,
  API_V1,
  API_INTERNAL,
  API_INTERNAL_UI,
  ENDPOINTS,
  encodeRepositoryItemId,
  decodeRepositoryItemId,
} from './rest-client';

export { restClient as default } from './rest-client';
