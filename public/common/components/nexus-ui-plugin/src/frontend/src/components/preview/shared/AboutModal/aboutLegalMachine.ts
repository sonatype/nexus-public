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

import { assign, createMachine } from 'xstate';
import { ExtJS } from '../../../../interface/ExtJS';

/*
 * Model for the Preview UI About modal's legal panel. The legal terms and
 * copyright text is edition-branched server output (COPYRIGHT.vm renders
 * /COPYRIGHT.html per edition). To keep that content byte-for-byte identical to
 * the classic UI, we fetch the same server document rather than reproduce it in
 * React, then inject its <body> content into a themed container. States:
 * idle -> loading -> loaded (cached) / error (retryable via FETCH).
 */

export const COPYRIGHT_PATH = '/COPYRIGHT.html';

export interface AboutLegalContext {
  html: string;
}

export type AboutLegalEvent = { type: 'FETCH' };

/*
 * Parse the fetched COPYRIGHT.html and return its <body> inner HTML. The server
 * document wraps the legal paragraphs in <html><head><style>...</head><body>;
 * we keep only the body content so the paragraphs and links inherit Preview UI
 * theming instead of the document's own light-mode <style>. The legal text and
 * links themselves are preserved exactly.
 */
export function extractBodyInnerHtml(documentHtml: string): string {
  const parsed = new DOMParser().parseFromString(documentHtml, 'text/html');
  return parsed.body ? parsed.body.innerHTML.trim() : '';
}

async function fetchCopyrightBody(): Promise<string> {
  const url = ExtJS.urlOf(COPYRIGHT_PATH);
  const response = await fetch(url, { credentials: 'same-origin' });
  if (!response.ok) {
    throw new Error(`Failed to load ${COPYRIGHT_PATH}: ${response.status}`);
  }
  const text = await response.text();
  return extractBodyInnerHtml(text);
}

export function createAboutLegalMachine() {
  return createMachine<AboutLegalContext, AboutLegalEvent>(
    {
      id: 'aboutLegal',
      initial: 'idle',
      context: {
        html: '',
      },
      states: {
        idle: {
          on: { FETCH: 'loading' },
        },
        loading: {
          invoke: {
            id: 'fetchCopyright',
            src: 'fetchCopyright',
            onDone: {
              target: 'loaded',
              actions: 'assignHtml',
            },
            onError: {
              target: 'error',
            },
          },
        },
        /* Terminal-by-design: once loaded, the content is cached for the session. */
        loaded: {},
        error: {
          on: { FETCH: 'loading' },
        },
      },
    },
    {
      services: {
        fetchCopyright: () => fetchCopyrightBody(),
      },
      actions: {
        assignHtml: assign<AboutLegalContext, AboutLegalEvent>({
          html: (_context, event) => (event as unknown as { data: string }).data,
        }),
      },
    },
  );
}
