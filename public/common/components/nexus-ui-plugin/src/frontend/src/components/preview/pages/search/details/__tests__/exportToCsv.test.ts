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

import { exportToCsv } from '../../../../shared/utils/exportToCsv';

function readBlob(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result as string);
    reader.onerror = reject;
    reader.readAsText(blob);
  });
}

describe('exportToCsv', () => {
  let createObjectURLMock: jest.Mock;
  let revokeObjectURLMock: jest.Mock;
  let clickMock: jest.Mock;
  let createElementMock: jest.Mock;
  let originalURL: typeof URL;

  beforeEach(() => {
    originalURL = window.URL;

    createObjectURLMock = jest.fn(() => 'blob:mock-url');
    revokeObjectURLMock = jest.fn();
    clickMock = jest.fn();

    const mockAnchor = {
      href: '',
      download: '',
      click: clickMock,
      style: { display: '' },
    };

    createElementMock = jest.fn(() => mockAnchor);

    Object.defineProperty(window, 'URL', {
      value: { createObjectURL: createObjectURLMock, revokeObjectURL: revokeObjectURLMock },
      writable: true,
    });

    jest.spyOn(document, 'createElement').mockImplementation(createElementMock);
    jest.spyOn(document.body, 'appendChild').mockImplementation(jest.fn());
    jest.spyOn(document.body, 'removeChild').mockImplementation(jest.fn());
  });

  afterEach(() => {
    Object.defineProperty(window, 'URL', { value: originalURL, writable: true });
    jest.restoreAllMocks();
  });

  it('triggers a browser download with the given filename', () => {
    exportToCsv([{ version: '1.0.0', status: 'none' }], 'versions.csv', ['version', 'status']);

    expect(createObjectURLMock).toHaveBeenCalledTimes(1);
    const anchor = createElementMock.mock.results[0].value;
    expect(anchor.download).toBe('versions.csv');
    expect(clickMock).toHaveBeenCalledTimes(1);
  });

  it('writes a header row from the columns array', async () => {
    exportToCsv([{ version: '1.0.0', status: 'none' }], 'versions.csv', ['version', 'status']);

    const blob: Blob = createObjectURLMock.mock.calls[0][0];
    const text = await readBlob(blob);
    const lines = text.split('\n');
    expect(lines[0]).toBe('version,status');
  });

  it('writes one data row per item', async () => {
    const data = [
      { version: '1.0.0', status: 'recommended' },
      { version: '2.0.0', status: 'none' },
    ];
    exportToCsv(data, 'versions.csv', ['version', 'status']);

    const blob: Blob = createObjectURLMock.mock.calls[0][0];
    const text = await readBlob(blob);
    const lines = text.trim().split('\n');
    expect(lines).toHaveLength(3); // header + 2 rows
    expect(lines[1]).toBe('1.0.0,recommended');
    expect(lines[2]).toBe('2.0.0,none');
  });

  it('wraps fields containing commas in double quotes', async () => {
    exportToCsv([{ name: 'foo,bar', type: 'hosted' }], 'repos.csv', ['name', 'type']);

    const blob: Blob = createObjectURLMock.mock.calls[0][0];
    const text = await readBlob(blob);
    const lines = text.trim().split('\n');
    expect(lines[1]).toBe('"foo,bar",hosted');
  });

  it('escapes double quotes within field values', async () => {
    exportToCsv([{ name: 'say "hello"', type: 'proxy' }], 'repos.csv', ['name', 'type']);

    const blob: Blob = createObjectURLMock.mock.calls[0][0];
    const text = await readBlob(blob);
    const lines = text.trim().split('\n');
    expect(lines[1]).toBe('"say ""hello""",proxy');
  });

  it('wraps fields containing tab characters in double quotes', async () => {
    exportToCsv([{ name: 'col1\tcol2', type: 'hosted' }], 'repos.csv', ['name', 'type']);

    const blob: Blob = createObjectURLMock.mock.calls[0][0];
    const text = await readBlob(blob);
    const lines = text.trim().split('\n');
    expect(lines[1]).toBe('"col1\tcol2",hosted');
  });

  it('handles undefined and null field values as empty strings', async () => {
    exportToCsv([{ version: '1.0.0', statusReason: undefined }], 'versions.csv', ['version', 'statusReason']);

    const blob: Blob = createObjectURLMock.mock.calls[0][0];
    const text = await readBlob(blob);
    const lines = text.trim().split('\n');
    expect(lines[1]).toBe('1.0.0,');
  });

  it('revokes the object URL after triggering the download', () => {
    exportToCsv([{ version: '1.0.0' }], 'versions.csv', ['version']);
    expect(revokeObjectURLMock).toHaveBeenCalledWith('blob:mock-url');
  });

  it('includes a UTF-8 BOM as the first three bytes for Excel compatibility', async () => {
    exportToCsv([{ version: '1.0.0' }], 'versions.csv', ['version']);
    const blob: Blob = createObjectURLMock.mock.calls[0][0];
    const buffer = await new Promise<ArrayBuffer>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as ArrayBuffer);
      reader.onerror = reject;
      reader.readAsArrayBuffer(blob);
    });
    // UTF-8 BOM is EF BB BF
    const bytes = new Uint8Array(buffer);
    expect(bytes[0]).toBe(0xef);
    expect(bytes[1]).toBe(0xbb);
    expect(bytes[2]).toBe(0xbf);
  });
});
