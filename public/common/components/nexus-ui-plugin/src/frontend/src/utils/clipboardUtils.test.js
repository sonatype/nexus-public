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
import { copyToClipboard } from './clipboardUtils';

describe('clipboardUtils', () => {
  let consoleWarnSpy;
  let consoleErrorSpy;

  beforeEach(() => {
    // Spy on console methods
    consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
    delete navigator.clipboard;
  });

  describe('copyToClipboard', () => {
    describe('Modern Clipboard API (navigator.clipboard)', () => {
      it('should successfully copy text using Clipboard API', async () => {
        const testText = 'http://localhost:8081/repository/maven-releases';
        const writeTextMock = jest.fn().mockResolvedValue(undefined);

        Object.defineProperty(navigator, 'clipboard', {
          value: {
            writeText: writeTextMock
          },
          configurable: true
        });

        const result = await copyToClipboard(testText);

        expect(result).toBe(true);
        expect(writeTextMock).toHaveBeenCalledWith(testText);
        expect(consoleWarnSpy).not.toHaveBeenCalled();
      });

      it('should fallback when Clipboard API rejects', async () => {
        const testText = 'test text';
        const writeTextMock = jest.fn().mockRejectedValue(new Error('Permission denied'));

        Object.defineProperty(navigator, 'clipboard', {
          value: {
            writeText: writeTextMock
          },
          configurable: true
        });

        // Mock execCommand fallback
        document.execCommand = jest.fn().mockReturnValue(true);

        const result = await copyToClipboard(testText);

        expect(result).toBe(true);
        expect(writeTextMock).toHaveBeenCalledWith(testText);
        expect(consoleWarnSpy).toHaveBeenCalledWith(
          'Clipboard API failed, trying fallback:',
          expect.any(Error)
        );
        expect(document.execCommand).toHaveBeenCalledWith('copy');
      });

      it('should fallback when Clipboard API is undefined', async () => {
        const testText = 'test text';

        // Ensure navigator.clipboard is undefined
        Object.defineProperty(navigator, 'clipboard', {
          value: undefined,
          configurable: true
        });

        // Mock execCommand fallback
        document.execCommand = jest.fn().mockReturnValue(true);

        const result = await copyToClipboard(testText);

        expect(result).toBe(true);
        expect(document.execCommand).toHaveBeenCalledWith('copy');
      });

      it('should fallback when writeText method is missing', async () => {
        const testText = 'test text';

        Object.defineProperty(navigator, 'clipboard', {
          value: {},  // clipboard exists but no writeText
          configurable: true
        });

        // Mock execCommand fallback
        document.execCommand = jest.fn().mockReturnValue(true);

        const result = await copyToClipboard(testText);

        expect(result).toBe(true);
        expect(document.execCommand).toHaveBeenCalledWith('copy');
      });
    });

    describe('execCommand fallback', () => {
      beforeEach(() => {
        // Ensure modern API is not available
        Object.defineProperty(navigator, 'clipboard', {
          value: undefined,
          configurable: true
        });
      });

      it('should successfully copy text using execCommand', async () => {
        const testText = 'http://localhost:8081/repository/maven-releases';

        // Mock DOM methods
        const textarea = document.createElement('textarea');
        const createElementSpy = jest.spyOn(document, 'createElement').mockReturnValue(textarea);
        const appendChildSpy = jest.spyOn(document.body, 'appendChild').mockImplementation(() => {});
        const removeChildSpy = jest.spyOn(document.body, 'removeChild').mockImplementation(() => {});
        const selectSpy = jest.spyOn(textarea, 'select').mockImplementation(() => {});
        document.execCommand = jest.fn().mockReturnValue(true);

        const result = await copyToClipboard(testText);

        expect(result).toBe(true);
        expect(createElementSpy).toHaveBeenCalledWith('textarea');
        expect(textarea.value).toBe(testText);
        expect(textarea.style.position).toBe('fixed');
        expect(textarea.style.left).toBe('-99999px');
        expect(textarea.getAttribute('readonly')).toBe('');
        expect(appendChildSpy).toHaveBeenCalledWith(textarea);
        expect(selectSpy).toHaveBeenCalled();
        expect(document.execCommand).toHaveBeenCalledWith('copy');
        expect(removeChildSpy).toHaveBeenCalledWith(textarea);

        createElementSpy.mockRestore();
        appendChildSpy.mockRestore();
        removeChildSpy.mockRestore();
        selectSpy.mockRestore();
      });

      it('should return false when execCommand returns false', async () => {
        const testText = 'test text';

        document.execCommand = jest.fn().mockReturnValue(false);

        const result = await copyToClipboard(testText);

        expect(result).toBe(false);
        expect(document.execCommand).toHaveBeenCalledWith('copy');
      });

      it('should return false when execCommand throws an error', async () => {
        const testText = 'test text';

        document.execCommand = jest.fn().mockImplementation(() => {
          throw new Error('execCommand failed');
        });

        const result = await copyToClipboard(testText);

        expect(result).toBe(false);
        expect(consoleErrorSpy).toHaveBeenCalledWith(
          'All clipboard methods failed:',
          expect.any(Error)
        );
      });

      it('should clean up textarea even if execCommand throws', async () => {
        const testText = 'test text';

        const textarea = document.createElement('textarea');
        const createElementSpy = jest.spyOn(document, 'createElement').mockReturnValue(textarea);
        const appendChildSpy = jest.spyOn(document.body, 'appendChild').mockImplementation(() => {});
        const removeChildSpy = jest.spyOn(document.body, 'removeChild').mockImplementation(() => {});

        document.execCommand = jest.fn().mockImplementation(() => {
          throw new Error('execCommand failed');
        });

        await copyToClipboard(testText);

        expect(removeChildSpy).toHaveBeenCalledWith(textarea);

        createElementSpy.mockRestore();
        appendChildSpy.mockRestore();
        removeChildSpy.mockRestore();
      });
    });

    describe('input validation', () => {
      it('should return false for null input', async () => {
        const result = await copyToClipboard(null);

        expect(result).toBe(false);
        expect(consoleWarnSpy).toHaveBeenCalledWith(
          'copyToClipboard called with empty/null text:',
          null
        );
      });

      it('should return false for undefined input', async () => {
        const result = await copyToClipboard(undefined);

        expect(result).toBe(false);
        expect(consoleWarnSpy).toHaveBeenCalledWith(
          'copyToClipboard called with empty/null text:',
          undefined
        );
      });

      it('should return false for empty string', async () => {
        const result = await copyToClipboard('');

        expect(result).toBe(false);
        expect(consoleWarnSpy).toHaveBeenCalledWith(
          'copyToClipboard called with empty/null text:',
          ''
        );
      });
    });

    describe('edge cases', () => {

      it('should handle very long text', async () => {
        const longText = 'a'.repeat(10000);

        Object.defineProperty(navigator, 'clipboard', {
          value: {
            writeText: jest.fn().mockResolvedValue(undefined)
          },
          configurable: true
        });

        const result = await copyToClipboard(longText);

        expect(result).toBe(true);
        expect(navigator.clipboard.writeText).toHaveBeenCalledWith(longText);
      });

      it('should handle special characters', async () => {
        const specialText = 'Text with\nnewlines\tand\ttabs & special <chars>';

        Object.defineProperty(navigator, 'clipboard', {
          value: {
            writeText: jest.fn().mockResolvedValue(undefined)
          },
          configurable: true
        });

        const result = await copyToClipboard(specialText);

        expect(result).toBe(true);
        expect(navigator.clipboard.writeText).toHaveBeenCalledWith(specialText);
      });
    });
  });
});

