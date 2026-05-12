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

import { humanizeHttpError } from '../useHttpForm';

jest.mock('@sonatype/nexus-ui-plugin', () => {
  const actual = jest.requireActual('@sonatype/nexus-ui-plugin');
  return {
    ...actual,
    createFormMachine: actual.createFormMachine,
    ENDPOINTS: {
      HTTP: '/service/rest/v1/http',
    },
    restClient: {
      get: jest.fn(),
      put: jest.fn(),
    },
  };
});

describe('humanizeHttpError', () => {
  it('returns friendly message for 400 status', () => {
    const err = { response: { status: 400 } };
    expect(humanizeHttpError(err)).toBe('Invalid HTTP settings. Please check your proxy host and port values.');
  });

  it('returns friendly message for 403 status', () => {
    const err = { response: { status: 403 } };
    expect(humanizeHttpError(err)).toBe('You do not have permission to modify HTTP settings. Contact your administrator.');
  });

  it('returns friendly message for 405 status', () => {
    const err = { response: { status: 405 } };
    expect(humanizeHttpError(err)).toBe('Unable to save HTTP settings. The server rejected this request.');
  });

  it('returns friendly message for 500 status', () => {
    const err = { response: { status: 500 } };
    expect(humanizeHttpError(err)).toBe('Server error while saving HTTP settings. Please try again.');
  });

  it('returns generic friendly message for "status code" errors', () => {
    const err = new Error('Request failed with status code 422');
    expect(humanizeHttpError(err)).toBe('Unable to save HTTP settings. Please try again or contact your administrator.');
  });

  it('returns raw message for unknown errors', () => {
    const err = new Error('Network timeout');
    expect(humanizeHttpError(err)).toBe('Network timeout');
  });

  it('handles non-Error objects', () => {
    expect(humanizeHttpError('string error')).toBe('string error');
  });

  it('handles unknown status codes by falling through', () => {
    const err = { response: { status: 418 }, message: 'I am a teapot' };
    expect(humanizeHttpError(err)).toBe('[object Object]');
  });
});
