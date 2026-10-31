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

import {
  isValidUrl,
  validateIqConfig,
  parseApplicationReason,
  parseVersion,
  formatErrorMessage,
  humanizeStage,
} from '../iqServerUtils';
import { DEFAULT_IQ_CONFIGURATION, PASSWORD_PLACEHOLDER } from '../types';

describe('iqServerUtils', () => {
  // ==========================================================================
  // Validation + parsing helpers (NEXUS-53610)
  // ==========================================================================

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

  // ==========================================================================
  // Display + error-message helpers
  // ==========================================================================

  describe('humanizeStage', () => {
    it('title-cases single words', () => {
      expect(humanizeStage('BUILD')).toBe('Build');
      expect(humanizeStage('OPERATE')).toBe('Operate');
    });
    it('expands UPPER_SNAKE_CASE to "Title Case"', () => {
      expect(humanizeStage('STAGE_RELEASE')).toBe('Stage Release');
    });
    it('returns empty string for nullish input', () => {
      expect(humanizeStage(undefined)).toBe('');
      expect(humanizeStage(null)).toBe('');
      expect(humanizeStage('')).toBe('');
    });
  });

describe('formatErrorMessage', () => {
    it('returns string error directly', () => {
      expect(formatErrorMessage('boom', 'fallback')).toBe('boom');
    });
    it('reads .message from objects', () => {
      expect(formatErrorMessage({ message: 'oops' }, 'fallback')).toBe('oops');
    });
    it('reads .response.data.message for axios errors', () => {
      expect(formatErrorMessage({ response: { data: { message: 'bad' } } }, 'x')).toBe('bad');
    });
    it('joins JSR-303 validation arrays', () => {
      const err = { response: { data: [{ message: 'a' }, { message: 'b' }] } };
      expect(formatErrorMessage(err, 'x')).toBe('a b');
    });
    it('falls through to fallback when nothing usable', () => {
      expect(formatErrorMessage({}, 'fallback')).toBe('fallback');
      expect(formatErrorMessage(null, 'fallback')).toBe('fallback');
    });
    it('humanizes ValidationErrorXO connection-refused messages', () => {
      const raw =
        "ValidationErrorXO{id='*', message='Connect to localhost:8073 [localhost/127.0.0.1, localhost/0:0:0:0:0:0:0:1] failed: Connection refused'}";
      expect(formatErrorMessage(raw, 'x')).toBe(
        'Cannot reach localhost:8073 — connection refused. Check the URL and that the IQ Server is running.',
      );
    });
    it('humanizes connection-timeout messages', () => {
      const raw =
        "ValidationErrorXO{id='*', message='Connect to iq.example.com:8070 [iq.example.com/10.0.0.1] failed: Connection timed out'}";
      expect(formatErrorMessage(raw, 'x')).toBe(
        'Cannot reach iq.example.com:8070 — connection timed out. Check the URL, network, and firewall.',
      );
    });
    it('humanizes unknown-host errors', () => {
      expect(
        formatErrorMessage(
          { message: "java.net.UnknownHostException: iq.wrong: nodename nor servname provided" },
          'x',
        ),
      ).toMatch(/^Host not found: /);
    });
    it('humanizes SSL/PKIX errors', () => {
      expect(formatErrorMessage('SSLHandshakeException: PKIX path building failed', 'x')).toMatch(
        /^SSL certificate could not be verified/,
      );
    });
    it('humanizes 401/auth errors', () => {
      expect(formatErrorMessage({ message: '401 Unauthorized' }, 'x')).toBe(
        'Authentication failed. Check the username and password.',
      );
    });
    it('passes through unrelated messages unchanged', () => {
      expect(formatErrorMessage('some other backend error', 'x')).toBe('some other backend error');
    });
  });
});
