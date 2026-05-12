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
  tokenize,
  validateCSEL,
  getCursorContext,
  interpretExpression,
} from '../cselValidator';

describe('cselValidator', () => {
  describe('tokenize', () => {
    it('tokenizes simple expression', () => {
      const tokens = tokenize('format == "maven2"');
      expect(tokens).toHaveLength(3);
      expect(tokens[0]).toMatchObject({ type: 'attribute', value: 'format' });
      expect(tokens[1]).toMatchObject({ type: 'operator', value: '==' });
      expect(tokens[2]).toMatchObject({ type: 'value', value: '"maven2"' });
    });

    it('tokenizes expression with logical operators', () => {
      const tokens = tokenize('format == "maven2" and path =^ "/org"');
      expect(tokens).toHaveLength(7);
      expect(tokens[3]).toMatchObject({ type: 'logical', value: 'and' });
    });

    it('tokenizes expression with parentheses', () => {
      const tokens = tokenize('(format == "maven2")');
      expect(tokens[0]).toMatchObject({ type: 'openParen', value: '(' });
      expect(tokens[4]).toMatchObject({ type: 'closeParen', value: ')' });
    });

    it('tokenizes not operator', () => {
      const tokens = tokenize('not (format == "raw")');
      expect(tokens[0]).toMatchObject({ type: 'logical', value: 'not' });
    });

    it('handles escaped quotes in values', () => {
      const tokens = tokenize('path =~ ".*\\\\.jar$"');
      expect(tokens[2]).toMatchObject({ type: 'value', value: '".*\\\\.jar$"' });
    });
  });

  describe('validateCSEL', () => {
    it('returns error for empty expression', () => {
      const result = validateCSEL('');
      expect(result.isValid).toBe(false);
      expect(result.hasBlockingErrors).toBe(true);
      expect(result.messages[0].message).toBe('Expression is required');
    });

    it('validates correct simple expression', () => {
      const result = validateCSEL('format == "maven2"');
      expect(result.hasBlockingErrors).toBe(false);
    });

    it('validates correct complex expression', () => {
      const result = validateCSEL('format == "maven2" and path =^ "/org"');
      expect(result.hasBlockingErrors).toBe(false);
    });

    it('detects unclosed quote', () => {
      const result = validateCSEL('format == "maven2');
      expect(result.hasBlockingErrors).toBe(true);
      expect(result.messages.some((m) => m.message.includes('Unclosed quote'))).toBe(true);
    });

    it('detects unclosed parenthesis', () => {
      const result = validateCSEL('(format == "maven2"');
      expect(result.hasBlockingErrors).toBe(true);
      expect(result.messages.some((m) => m.message.includes('Unclosed parenthesis'))).toBe(true);
    });

    it('detects unmatched closing parenthesis', () => {
      const result = validateCSEL('format == "maven2")');
      expect(result.hasBlockingErrors).toBe(true);
      expect(result.messages.some((m) => m.message.includes('Unmatched closing'))).toBe(true);
    });

    it('warns about unknown attributes', () => {
      const result = validateCSEL('unknownAttr == "value"');
      expect(result.hasBlockingErrors).toBe(false);
      expect(result.messages.some((m) => m.type === 'warning' && m.message.includes('Unknown attribute'))).toBe(true);
    });

    it('validates known attributes without warnings', () => {
      const result = validateCSEL('format == "maven2"');
      expect(result.messages.filter((m) => m.type === 'warning')).toHaveLength(0);
    });

    it('validates expression with not operator', () => {
      const result = validateCSEL('not (format == "raw")');
      expect(result.hasBlockingErrors).toBe(false);
    });
  });

  describe('getCursorContext', () => {
    it('returns start context for empty expression', () => {
      const context = getCursorContext('', 0);
      expect(context.context).toBe('start');
    });

    it('returns afterLogical context after and', () => {
      const context = getCursorContext('format == "maven2" and ', 23);
      expect(context.context).toBe('afterLogical');
    });

    it('returns afterAttribute context after attribute', () => {
      const context = getCursorContext('format ', 7);
      expect(context.context).toBe('afterAttribute');
      expect(context.currentAttribute).toBe('format');
    });

    it('returns afterOperator context after operator', () => {
      const context = getCursorContext('format == ', 10);
      expect(context.context).toBe('afterOperator');
      expect(context.currentAttribute).toBe('format');
      expect(context.currentOperator).toBe('==');
    });

    it('returns insideValue context inside quotes', () => {
      const context = getCursorContext('format == "mav', 14);
      expect(context.context).toBe('insideValue');
    });

    it('returns afterValue context after value', () => {
      const context = getCursorContext('format == "maven2" ', 19);
      expect(context.context).toBe('afterValue');
    });

    it('returns start with partial text when typing attribute', () => {
      const context = getCursorContext('for', 3);
      expect(context.context).toBe('start');
      expect(context.partialText).toBe('for');
    });
  });

  describe('interpretExpression', () => {
    it('interprets simple equality', () => {
      const result = interpretExpression('format == "maven2"');
      expect(result.success).toBe(true);
      expect(result.text).toContain('format');
      expect(result.text).toContain('equals');
      expect(result.text).toContain('maven2');
    });

    it('interprets starts with operator', () => {
      const result = interpretExpression('path =^ "/org"');
      expect(result.success).toBe(true);
      expect(result.text).toContain('starts with');
    });

    it('interprets regex match', () => {
      const result = interpretExpression('path =~ ".*\\.jar$"');
      expect(result.success).toBe(true);
      expect(result.text).toContain('matches pattern');
    });

    it('interprets logical AND', () => {
      const result = interpretExpression('format == "maven2" and path =^ "/org"');
      expect(result.success).toBe(true);
      expect(result.text).toContain('AND');
    });

    it('returns failure for empty expression', () => {
      const result = interpretExpression('');
      expect(result.success).toBe(false);
    });
  });
});


