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

import { parseApiError } from '../error-handler';
import { AxiosError } from 'axios';

function makeAxiosError(status: number, data: unknown): AxiosError {
  const error = new Error('Request failed') as AxiosError;
  error.isAxiosError = true;
  error.response = { status, data, headers: {}, config: {} as any, statusText: '' };
  return error;
}

describe('resolveConstraintMessage (via parseApiError)', () => {
  it('translates a known constraint key to a human-readable message', () => {
    const data = [{ id: 'PARAMETER name', message: '{org.sonatype.nexus.validation.constraint.name}' }];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toContain('Only letters, digits, hyphens');
    expect(result.message).not.toContain('{org.sonatype.nexus.validation.constraint.name}');
  });

  it('passes through an unknown constraint key unchanged', () => {
    const data = [{ id: '*', message: '{org.sonatype.nexus.validation.constraint.unknown}' }];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe('{org.sonatype.nexus.validation.constraint.unknown}');
  });

  it('passes through a plain human-readable message unchanged', () => {
    const data = [{ id: '*', message: 'Tag name already exists' }];
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toBe('Tag name already exists');
  });

  it('resolves constraint key in Hibernate Validator nested errors format', () => {
    const data = {
      errors: [
        { field: 'name', message: '{org.sonatype.nexus.validation.constraint.name}' },
      ],
    };
    const result = parseApiError(makeAxiosError(400, data));
    expect(result.message).toContain('Only letters, digits, hyphens');
    expect(result.message).not.toContain('{org.sonatype.nexus.validation.constraint.name}');
  });

  it('returns status 422 correctly for 422 responses', () => {
    const data = [{ id: 'PARAMETER name', message: 'Invalid value' }];
    const result = parseApiError(makeAxiosError(422, data));
    expect(result.status).toBe(422);
  });
});
