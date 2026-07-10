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
 * Quartz-style cron expression validator.
 * Returns { valid: true } or { valid: false, reason: string }.
 *
 * Format: [seconds] [minutes] [hours] [day-of-month] [month] [day-of-week] [year(optional)]
 */

export interface CronValidationResult {
  valid: boolean;
  reason?: string;
}

const NUMERIC_RANGE = (min: number, max: number) => (token: string): boolean => {
  if (token === '*' || token === '?') return true;
  for (const part of token.split(',')) {
    const stepSplit = part.split('/');
    const range = stepSplit[0];
    const step = stepSplit[1];
    if (step !== undefined && !/^\d+$/.test(step)) return false;
    if (range === '*') continue;
    const dashSplit = range.split('-');
    for (const value of dashSplit) {
      if (!/^\d+$/.test(value)) return false;
      const n = parseInt(value, 10);
      if (Number.isNaN(n) || n < min || n > max) return false;
    }
  }
  return true;
};

const MONTH_NAMES = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
const DOW_NAMES = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];

const isMonth = (token: string): boolean => {
  if (NUMERIC_RANGE(1, 12)(token)) return true;
  // Allow JAN-FEB, JAN,FEB, JAN, etc.
  return token.split(/[,-]/).every((name) => MONTH_NAMES.includes(name.toUpperCase()));
};

const isDayOfWeek = (token: string): boolean => {
  if (token === 'L' || /^[1-7]L$/.test(token)) return true;
  if (/^[1-7]#[1-5]$/.test(token)) return true;
  if (NUMERIC_RANGE(0, 7)(token)) return true;
  return token.split(/[,-]/).every((name) => DOW_NAMES.includes(name.toUpperCase()));
};

const isDayOfMonth = (token: string): boolean => {
  if (token === 'L' || token === 'LW') return true;
  if (/^\d+W$/.test(token)) return true;
  return NUMERIC_RANGE(1, 31)(token);
};

export function validateCronExpression(input: string): CronValidationResult {
  if (!input || !input.trim()) {
    return { valid: false, reason: 'Cron expression is required' };
  }

  const parts = input.trim().split(/\s+/);
  if (parts.length < 6 || parts.length > 7) {
    return {
      valid: false,
      reason: `Cron expression must have 6 or 7 parts (got ${parts.length})`,
    };
  }

  const [sec, min, hour, dom, month, dow] = parts;

  if (!NUMERIC_RANGE(0, 59)(sec)) {
    return { valid: false, reason: 'Seconds must be between 0 and 59' };
  }
  if (!NUMERIC_RANGE(0, 59)(min)) {
    return { valid: false, reason: 'Minutes must be between 0 and 59' };
  }
  if (!NUMERIC_RANGE(0, 23)(hour)) {
    return { valid: false, reason: 'Hours must be between 0 and 23' };
  }
  if (!isDayOfMonth(dom)) {
    return { valid: false, reason: 'Day of month must be between 1 and 31' };
  }
  if (!isMonth(month)) {
    return { valid: false, reason: 'Month must be between 1 and 12 (or names like JAN, FEB)' };
  }
  if (!isDayOfWeek(dow)) {
    return { valid: false, reason: 'Day of week must be between 0 and 7 (or names like SUN, MON)' };
  }

  const domIsWild = dom === '?';
  const dowIsWild = dow === '?';
  if (domIsWild === dowIsWild) {
    return {
      valid: false,
      reason: 'Quartz requires either day-of-month or day-of-week to be "?"',
    };
  }

  return { valid: true };
}

export const isValidCronExpression = (input: string): boolean =>
  validateCronExpression(input).valid;
