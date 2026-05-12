/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/**
 * Auto-mock for @/utils/api that delegates to @sonatype/nexus-ui-plugin mock.
 *
 * Many test files mock @sonatype/nexus-ui-plugin (which exports restClient, urlBuilder, etc.)
 * but the source code imports these from @/utils/api. This bridge ensures the mocks
 * from @sonatype/nexus-ui-plugin are what @/utils/api resolves to in tests.
 *
 * Uses lazy getters so that test-time mocks of @sonatype/nexus-ui-plugin are picked
 * up when properties are accessed (not at module load time).
 */

const fallbackRestClient = {
  get: jest.fn().mockResolvedValue([]),
  post: jest.fn().mockResolvedValue({}),
  put: jest.fn().mockResolvedValue({}),
  delete: jest.fn().mockResolvedValue({}),
};

let _resolvedPlugin = undefined;

function p() {
  if (_resolvedPlugin !== undefined) return _resolvedPlugin;
  try {
    _resolvedPlugin = require('@sonatype/nexus-ui-plugin') || {};
  } catch {
    _resolvedPlugin = {};
  }
  return _resolvedPlugin;
}

beforeEach(() => {
  _resolvedPlugin = undefined;
});

module.exports = {
  __esModule: true,
  get restClient() { return p().restClient || fallbackRestClient; },
  get urlBuilder() { return p().urlBuilder || {}; },
  get parseApiError() {
    return p().parseApiError || jest.fn((err) => ({
      message: err?.response?.data?.message || err?.message || 'Error',
      status: err?.response?.status,
    }));
  },
  get getErrorMessage() { return p().getErrorMessage || jest.fn((err) => err?.message || 'Error'); },
  get ENDPOINTS() { return p().ENDPOINTS || {}; },
  API_BASE: '/service/rest',
  API_V1: '/service/rest/v1',
  API_INTERNAL: '/service/rest/internal',
  get API_INTERNAL_UI() { return p().API_INTERNAL_UI || '/service/rest/internal/ui'; },
  get encodeRepositoryItemId() { return p().encodeRepositoryItemId || jest.fn((id) => encodeURIComponent(id)); },
  get decodeRepositoryItemId() { return p().decodeRepositoryItemId || jest.fn((id) => decodeURIComponent(id)); },
  get isNotFoundError() { return p().isNotFoundError || jest.fn().mockReturnValue(false); },
  get isAuthError() { return p().isAuthError || jest.fn().mockReturnValue(false); },
  get isPermissionError() { return p().isPermissionError || jest.fn().mockReturnValue(false); },
  get isConflictError() { return p().isConflictError || jest.fn().mockReturnValue(false); },
  get isValidationError() { return p().isValidationError || jest.fn().mockReturnValue(false); },
  get isServerError() { return p().isServerError || jest.fn().mockReturnValue(false); },
  get isNetworkError() { return p().isNetworkError || jest.fn().mockReturnValue(false); },
  get hasFieldErrors() { return p().hasFieldErrors || jest.fn().mockReturnValue(false); },
  get fieldErrorsToMap() { return p().fieldErrorsToMap || jest.fn().mockReturnValue({}); },
  get getFieldError() { return p().getFieldError || jest.fn().mockReturnValue(null); },
  get default() { return p().restClient || fallbackRestClient; },
};
