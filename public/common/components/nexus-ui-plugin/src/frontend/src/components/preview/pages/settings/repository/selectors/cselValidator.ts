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

import { isKnownAttribute, isOperatorValidForAttribute, } from './cselConfig';

/**
 * Validation result types
 */
export interface ValidationMessage {
  type: 'error' | 'warning';
  message: string;
  position?: number;
  length?: number;
}

export interface ValidationResult {
  isValid: boolean;
  hasBlockingErrors: boolean;
  messages: ValidationMessage[];
}

/**
 * Token types for CSEL parsing
 */
export type TokenType =
  | 'attribute'
  | 'operator'
  | 'value'
  | 'logical'
  | 'openParen'
  | 'closeParen'
  | 'unknown';

export interface Token {
  type: TokenType;
  value: string;
  start: number;
  end: number;
}

/**
 * Tokenize a CSEL expression
 */
export function tokenize(expression: string): Token[] {
  const tokens: Token[] = [];
  let pos = 0;

  while (pos < expression.length) {
    // Skip whitespace
    if (/\s/.test(expression[pos])) {
      pos++;
      continue;
    }

    // Open parenthesis
    if (expression[pos] === '(') {
      tokens.push({ type: 'openParen', value: '(', start: pos, end: pos + 1 });
      pos++;
      continue;
    }

    // Close parenthesis
    if (expression[pos] === ')') {
      tokens.push({ type: 'closeParen', value: ')', start: pos, end: pos + 1 });
      pos++;
      continue;
    }

    // Quoted string value
    if (expression[pos] === '"') {
      const start = pos;
      pos++;
      while (pos < expression.length && expression[pos] !== '"') {
        if (expression[pos] === '\\' && pos + 1 < expression.length) {
          pos += 2; // Skip escaped character
        } else {
          pos++;
        }
      }
      if (pos < expression.length) {
        pos++; // Include closing quote
      }
      tokens.push({
        type: 'value',
        value: expression.slice(start, pos),
        start,
        end: pos,
      });
      continue;
    }

    // Operators (check multi-char first)
    const operatorMatch = expression.slice(pos).match(/^(==|!=|=~|=\^|\^=)/);
    if (operatorMatch) {
      tokens.push({
        type: 'operator',
        value: operatorMatch[1],
        start: pos,
        end: pos + operatorMatch[1].length,
      });
      pos += operatorMatch[1].length;
      continue;
    }

    // Logical operators and identifiers
    const wordMatch = expression.slice(pos).match(/^[a-zA-Z_][a-zA-Z0-9_.]*/);
    if (wordMatch) {
      const word = wordMatch[0];
      const lowerWord = word.toLowerCase();
      const isLogical = ['and', 'or', 'not'].includes(lowerWord);
      tokens.push({
        type: isLogical ? 'logical' : 'attribute',
        value: word,
        start: pos,
        end: pos + word.length,
      });
      pos += word.length;
      continue;
    }

    // Unknown character
    tokens.push({
      type: 'unknown',
      value: expression[pos],
      start: pos,
      end: pos + 1,
    });
    pos++;
  }

  return tokens;
}

/**
 * Validate a CSEL expression
 * Returns both blocking errors (prevent save) and warnings (allow save)
 */
export function validateCSEL(expression: string): ValidationResult {
  const messages: ValidationMessage[] = [];

  if (!expression.trim()) {
    return {
      isValid: false,
      hasBlockingErrors: true,
      messages: [{ type: 'error', message: 'Expression is required' }],
    };
  }

  const tokens = tokenize(expression);

  // Check for unclosed quotes
  const quoteCount = (expression.match(/"/g) || []).length;
  const escapedQuoteCount = (expression.match(/\\"/g) || []).length;
  if ((quoteCount - escapedQuoteCount) % 2 !== 0) {
    messages.push({
      type: 'error',
      message: 'Unclosed quote in expression',
    });
  }

  // Check for balanced parentheses
  let parenDepth = 0;
  for (const token of tokens) {
    if (token.type === 'openParen') parenDepth++;
    if (token.type === 'closeParen') parenDepth--;
    if (parenDepth < 0) {
      messages.push({
        type: 'error',
        message: 'Unmatched closing parenthesis',
        position: token.start,
      });
      break;
    }
  }
  if (parenDepth > 0) {
    messages.push({
      type: 'error',
      message: 'Unclosed parenthesis',
    });
  }

  // Check for unknown tokens
  for (const token of tokens) {
    if (token.type === 'unknown') {
      messages.push({
        type: 'error',
        message: `Invalid character: "${token.value}"`,
        position: token.start,
      });
    }
  }

  // Validate attribute-operator-value sequences
  let i = 0;
  while (i < tokens.length) {
    const token = tokens[i];

    // Skip parentheses and logical operators
    if (token.type === 'openParen' || token.type === 'closeParen' || token.type === 'logical') {
      i++;
      continue;
    }

    // Expect: attribute operator value
    if (token.type === 'attribute') {
      const attrName = token.value;

      // Check if attribute is known
      if (!isKnownAttribute(attrName)) {
        messages.push({
          type: 'warning',
          message: `Unknown attribute: "${attrName}"`,
          position: token.start,
          length: token.value.length,
        });
      }

      // Check for operator
      const nextToken = tokens[i + 1];
      if (nextToken && nextToken.type === 'operator') {
        // Check if operator is valid for this attribute
        if (isKnownAttribute(attrName) && !isOperatorValidForAttribute(attrName, nextToken.value)) {
          messages.push({
            type: 'warning',
            message: `Operator "${nextToken.value}" may not be supported for attribute "${attrName}"`,
            position: nextToken.start,
            length: nextToken.value.length,
          });
        }

        // Check for value
        const valueToken = tokens[i + 2];
        if (!valueToken || valueToken.type !== 'value') {
          messages.push({
            type: 'error',
            message: `Expected quoted value after operator "${nextToken.value}"`,
            position: nextToken.end,
          });
        }
        i += 3;
        continue;
      } else if (nextToken && nextToken.type !== 'logical' && nextToken.type !== 'closeParen') {
        messages.push({
          type: 'error',
          message: `Expected operator after attribute "${attrName}"`,
          position: token.end,
        });
      }
    }

    i++;
  }

  // Check for empty expression after logical operator
  for (let j = 0; j < tokens.length; j++) {
    const token = tokens[j];
    if (token.type === 'logical' && token.value.toLowerCase() !== 'not') {
      const prevToken = tokens[j - 1];
      const nextToken = tokens[j + 1];
      if (!prevToken || (prevToken.type !== 'value' && prevToken.type !== 'closeParen')) {
        messages.push({
          type: 'error',
          message: `Missing expression before "${token.value}"`,
          position: token.start,
        });
      }
      if (!nextToken || (nextToken.type !== 'attribute' && nextToken.type !== 'openParen' && nextToken.type !== 'logical')) {
        messages.push({
          type: 'error',
          message: `Missing expression after "${token.value}"`,
          position: token.end,
        });
      }
    }
  }

  const hasBlockingErrors = messages.some((m) => m.type === 'error');

  return {
    isValid: messages.length === 0,
    hasBlockingErrors,
    messages,
  };
}

/**
 * Determine the context at cursor position for autocomplete
 */
export type CursorContext =
  | 'start'
  | 'afterLogical'
  | 'afterAttribute'
  | 'afterOperator'
  | 'insideValue'
  | 'afterValue'
  | 'unknown';

export interface CursorContextResult {
  context: CursorContext;
  currentAttribute?: string;
  currentOperator?: string;
  partialText?: string;
}

export function getCursorContext(expression: string, cursorPos: number): CursorContextResult {
  const beforeCursor = expression.slice(0, cursorPos);
  const tokens = tokenize(beforeCursor);

  if (tokens.length === 0) {
    return { context: 'start' };
  }

  const lastToken = tokens[tokens.length - 1];

  // Check if cursor is inside an unclosed quote
  const quoteCount = (beforeCursor.match(/"/g) || []).length;
  const escapedQuoteCount = (beforeCursor.match(/\\"/g) || []).length;
  if ((quoteCount - escapedQuoteCount) % 2 !== 0) {
    return { context: 'insideValue' };
  }

  // Check cursor position relative to last token
  const _isAtEndOfToken = cursorPos === lastToken.end;
  const isAfterToken = cursorPos > lastToken.end;
  const hasSpaceAfterToken = isAfterToken && /\s$/.test(beforeCursor);

  if (lastToken.type === 'logical' || lastToken.type === 'openParen') {
    return { context: 'afterLogical' };
  }

  if (lastToken.type === 'attribute') {
    if (hasSpaceAfterToken) {
      return { context: 'afterAttribute', currentAttribute: lastToken.value };
    }
    // Still typing attribute name
    return { context: 'start', partialText: lastToken.value };
  }

  if (lastToken.type === 'operator') {
    // Find the attribute before this operator
    const attrToken = tokens.slice().reverse().find((t) => t.type === 'attribute');
    return {
      context: 'afterOperator',
      currentAttribute: attrToken?.value,
      currentOperator: lastToken.value,
    };
  }

  if (lastToken.type === 'value' || lastToken.type === 'closeParen') {
    return { context: 'afterValue' };
  }

  return { context: 'unknown' };
}

/**
 * Parse expression into human-readable interpretation
 */
export interface InterpretedExpression {
  success: boolean;
  text: string;
}

export function interpretExpression(expression: string): InterpretedExpression {
  if (!expression.trim()) {
    return { success: false, text: '' };
  }

  try {
    const tokens = tokenize(expression);
    const parts: string[] = [];
    let i = 0;

    while (i < tokens.length) {
      const token = tokens[i];

      if (token.type === 'logical') {
        const word = token.value.toLowerCase();
        if (word === 'and') {
          parts.push('AND');
        } else if (word === 'or') {
          parts.push('OR');
        } else if (word === 'not') {
          parts.push('NOT');
        }
        i++;
        continue;
      }

      if (token.type === 'openParen') {
        parts.push('(');
        i++;
        continue;
      }

      if (token.type === 'closeParen') {
        parts.push(')');
        i++;
        continue;
      }

      if (token.type === 'attribute') {
        const attr = token.value;
        const opToken = tokens[i + 1];
        const valueToken = tokens[i + 2];

        if (opToken?.type === 'operator' && valueToken?.type === 'value') {
          const op = opToken.value;
          const value = valueToken.value.slice(1, -1); // Remove quotes

          let description = '';
          const attrLabel = attr.replace('coordinate.', '').replace(/([A-Z])/g, ' $1').toLowerCase();

          switch (op) {
            case '==':
              description = `${attrLabel} equals "${value}"`;
              break;
            case '!=':
              description = `${attrLabel} does not equal "${value}"`;
              break;
            case '=~':
              description = `${attrLabel} matches pattern "${value}"`;
              break;
            case '=^':
            case '^=':
              description = `${attrLabel} starts with "${value}"`;
              break;
            default:
              description = `${attrLabel} ${op} "${value}"`;
          }

          parts.push(description);
          i += 3;
          continue;
        }
      }

      i++;
    }

    if (parts.length === 0) {
      return { success: false, text: '' };
    }

    const text = 'Matches content where: ' + parts.join(' ');
    return { success: true, text };
  } catch {
    return { success: false, text: '' };
  }
}


