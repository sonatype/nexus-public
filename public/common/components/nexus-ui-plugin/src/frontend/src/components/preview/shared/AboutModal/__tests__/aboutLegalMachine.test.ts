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

import { interpret } from 'xstate';

jest.mock('../../../../../interface/ExtJS', () => ({
  ExtJS: {
    urlOf: (path: string) => `https://testurl${path}`,
  },
}));

import {
  COPYRIGHT_PATH,
  createAboutLegalMachine,
  extractBodyInnerHtml,
} from '../aboutLegalMachine';

const COPYRIGHT_DOCUMENT = `<!DOCTYPE html>
<html>
<head>
  <title>Copyright</title>
  <style type="text/css">
    body { font-family: arial; font-size: 14px; }
  </style>
</head>
<body>
  <p>
    All use of and access to Sonatype Nexus Repository Professional is governed by these
    <a href="http://links.sonatype.com/products/nxrm/tos/pro" target="_blank" rel="noopener">terms and conditions</a>.
  </p>
<p>
  Copyright &copy; 2008-Present, Sonatype, Inc. All rights reserved.
</p>
</body>
</html>`;

function waitForState(service: ReturnType<typeof interpret>, matches: string): Promise<void> {
  return new Promise((resolve) => {
    service.onTransition((state) => {
      if (state.matches(matches)) {
        resolve();
      }
    });
  });
}

describe('extractBodyInnerHtml', () => {
  it('returns the body inner HTML and drops the head/style', () => {
    const result = extractBodyInnerHtml(COPYRIGHT_DOCUMENT);

    expect(result).toContain('terms and conditions');
    expect(result).toContain('href="http://links.sonatype.com/products/nxrm/tos/pro"');
    expect(result).toContain('Sonatype, Inc. All rights reserved.');
    expect(result).not.toContain('<style');
    expect(result).not.toContain('font-family');
    expect(result).not.toContain('<title>');
  });

  it('returns an empty string when there is no parseable body content', () => {
    expect(extractBodyInnerHtml('')).toBe('');
  });
});

describe('createAboutLegalMachine', () => {
  const originalFetch = global.fetch;

  afterEach(() => {
    global.fetch = originalFetch;
    jest.restoreAllMocks();
  });

  it('starts in the idle state', () => {
    const service = interpret(createAboutLegalMachine()).start();
    expect(service.state.matches('idle')).toBe(true);
    service.stop();
  });

  it('fetches COPYRIGHT.html and transitions to loaded with the body html', async () => {
    const fetchMock = jest.fn().mockResolvedValue({
      ok: true,
      text: () => Promise.resolve(COPYRIGHT_DOCUMENT),
    });
    global.fetch = fetchMock as unknown as typeof fetch;

    const service = interpret(createAboutLegalMachine()).start();
    const loaded = waitForState(service, 'loaded');
    service.send({ type: 'FETCH' });
    await loaded;

    expect(fetchMock).toHaveBeenCalledWith(`https://testurl${COPYRIGHT_PATH}`, {
      credentials: 'same-origin',
    });
    expect(service.state.context.html).toContain('terms and conditions');
    service.stop();
  });

  it('transitions to error when the response is not ok', async () => {
    global.fetch = jest.fn().mockResolvedValue({ ok: false, status: 500 }) as unknown as typeof fetch;

    const service = interpret(createAboutLegalMachine()).start();
    const errored = waitForState(service, 'error');
    service.send({ type: 'FETCH' });
    await errored;

    expect(service.state.matches('error')).toBe(true);
    expect(service.state.context.html).toBe('');
    service.stop();
  });

  it('retries from the error state on a subsequent FETCH', async () => {
    const fetchMock = jest
      .fn()
      .mockResolvedValueOnce({ ok: false, status: 503 })
      .mockResolvedValueOnce({ ok: true, text: () => Promise.resolve(COPYRIGHT_DOCUMENT) });
    global.fetch = fetchMock as unknown as typeof fetch;

    const service = interpret(createAboutLegalMachine()).start();

    const errored = waitForState(service, 'error');
    service.send({ type: 'FETCH' });
    await errored;

    const loaded = waitForState(service, 'loaded');
    service.send({ type: 'FETCH' });
    await loaded;

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(service.state.context.html).toContain('terms and conditions');
    service.stop();
  });
});
