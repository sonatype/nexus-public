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

import { isLocalDevHostname } from '../isLocalDevHostname';

const originalLocation = window.location;

afterEach(() => {
  // @ts-ignore
  window.location = originalLocation;
});

function setHostname(hostname: string) {
  // @ts-ignore — jsdom allows full replacement of window.location
  delete window.location;
  // @ts-ignore
  window.location = { hostname };
}

describe('isLocalDevHostname', () => {
  it('returns true for localhost', () => {
    setHostname('localhost');
    expect(isLocalDevHostname()).toBe(true);
  });

  it('returns true for 127.0.0.1', () => {
    setHostname('127.0.0.1');
    expect(isLocalDevHostname()).toBe(true);
  });

  it('returns true for IPv6 loopback [::1]', () => {
    setHostname('[::1]');
    expect(isLocalDevHostname()).toBe(true);
  });

  it('returns false for a production hostname', () => {
    setHostname('nexus.example.com');
    expect(isLocalDevHostname()).toBe(false);
  });

  it('returns false when window is undefined (SSR guard)', () => {
    const savedWindow = global.window;
    try {
      // @ts-ignore
      delete global.window;
      expect(isLocalDevHostname()).toBe(false);
    } finally {
      // @ts-ignore
      global.window = savedWindow;
    }
  });
});
