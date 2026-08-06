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

import { isValidUrl, validateIqConfig, parseApplicationReason, parseVersion } from '../iqServerUtils';
import { DEFAULT_IQ_CONFIGURATION, PASSWORD_PLACEHOLDER } from '../types';

describe('iqServerUtils', () => {
  it('isValidUrl', () => {
    expect(isValidUrl('https://iq.example.com')).toBe(true);
    expect(isValidUrl('not-a-url')).toBe(false);
  });

  it('validateIqConfig requires url + auth when enabled', () => {
    const errors = validateIqConfig({ ...DEFAULT_IQ_CONFIGURATION, enabled: true }, DEFAULT_IQ_CONFIGURATION);
    expect(errors.url).toMatch(/required/i);
    expect(errors.authenticationType).toMatch(/required/i);
  });

  it('validateIqConfig requires url, auth, username and password even when disabled, matching Classic UI', () => {
    const errors = validateIqConfig(
      { ...DEFAULT_IQ_CONFIGURATION, enabled: false, authenticationType: 'USER' },
      DEFAULT_IQ_CONFIGURATION,
    );
    expect(errors.url).toMatch(/required/i);
    expect(errors.username).toMatch(/required/i);
    expect(errors.password).toMatch(/required/i);
  });

  it('validateIqConfig requires password when URL changes and password is placeholder', () => {
    const pristine = { ...DEFAULT_IQ_CONFIGURATION, enabled: true, url: 'https://a', authenticationType: 'USER' as const, username: 'u', password: PASSWORD_PLACEHOLDER };
    const changed = { ...pristine, url: 'https://b' };
    expect(validateIqConfig(changed, pristine).password).toMatch(/required when changing the URL/i);
  });

  it('parseApplicationReason splits app lists', () => {
    expect(parseApplicationReason('Applications: A, B, C')).toEqual({ isList: true, items: ['A', 'B', 'C'] });
    expect(parseApplicationReason('No applications configured yet.').isList).toBe(false);
  });

  it('parseVersion extracts a version suffix', () => {
    expect(parseVersion('Connected v1.2.3')).toBe(' (v1.2.3)');
    expect(parseVersion('no version')).toBe('');
  });
});
