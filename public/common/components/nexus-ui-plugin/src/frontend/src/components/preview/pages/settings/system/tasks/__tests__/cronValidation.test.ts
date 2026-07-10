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

import { validateCronExpression } from '../cronValidation';

describe('validateCronExpression', () => {
  describe('valid expressions', () => {
    it.each([
      ['0 0 12 ? * MON-FRI', 'weekdays at noon, six parts'],
      ['0 0 12 1 * ?', 'first of month at noon'],
      ['0 0/15 * * * ?', 'every 15 minutes'],
      ['0 0 12 ? * MON-FRI 2026', 'with optional year (seven parts)'],
      ['0 0 12 ? * SUN,MON', 'comma-separated days'],
      ['0 0 12 L * ?', 'last day of month token'],
    ])('accepts %s (%s)', (expr) => {
      expect(validateCronExpression(expr)).toEqual({ valid: true });
    });
  });

  describe('invalid expressions', () => {
    it('rejects empty input with a helpful message', () => {
      expect(validateCronExpression('')).toEqual({
        valid: false,
        reason: 'Cron expression is required',
      });
    });

    it('rejects whitespace-only input', () => {
      expect(validateCronExpression('   ')).toEqual({
        valid: false,
        reason: 'Cron expression is required',
      });
    });

    it('rejects expressions with fewer than 6 parts', () => {
      expect(validateCronExpression('0 0 12 ? *')).toEqual({
        valid: false,
        reason: 'Cron expression must have 6 or 7 parts (got 5)',
      });
    });

    it('rejects expressions with more than 7 parts', () => {
      expect(validateCronExpression('0 0 12 ? * MON-FRI 2026 EXTRA')).toEqual({
        valid: false,
        reason: 'Cron expression must have 6 or 7 parts (got 8)',
      });
    });

    it('rejects out-of-range minutes', () => {
      expect(validateCronExpression('0 70 12 ? * MON')).toEqual({
        valid: false,
        reason: 'Minutes must be between 0 and 59',
      });
    });

    it('rejects out-of-range seconds', () => {
      expect(validateCronExpression('60 0 12 ? * MON')).toEqual({
        valid: false,
        reason: 'Seconds must be between 0 and 59',
      });
    });

    it('rejects out-of-range hours', () => {
      expect(validateCronExpression('0 0 25 ? * MON')).toEqual({
        valid: false,
        reason: 'Hours must be between 0 and 23',
      });
    });

    it('rejects out-of-range day-of-month', () => {
      expect(validateCronExpression('0 0 12 32 * ?')).toEqual({
        valid: false,
        reason: 'Day of month must be between 1 and 31',
      });
    });

    it('rejects out-of-range month', () => {
      expect(validateCronExpression('0 0 12 1 13 ?')).toEqual({
        valid: false,
        reason: 'Month must be between 1 and 12 (or names like JAN, FEB)',
      });
    });

    it('rejects out-of-range day-of-week', () => {
      expect(validateCronExpression('0 0 12 ? * 8')).toEqual({
        valid: false,
        reason: 'Day of week must be between 0 and 7 (or names like SUN, MON)',
      });
    });

    it('requires exactly one of day-of-month or day-of-week to be ?', () => {
      expect(validateCronExpression('0 0 12 1 * MON')).toEqual({
        valid: false,
        reason: 'Quartz requires either day-of-month or day-of-week to be "?"',
      });
    });

    it('rejects illegal characters', () => {
      expect(validateCronExpression('0 0 12 ? * abc')).toEqual({
        valid: false,
        reason: expect.stringMatching(/day of week/i),
      });
    });
  });
});
