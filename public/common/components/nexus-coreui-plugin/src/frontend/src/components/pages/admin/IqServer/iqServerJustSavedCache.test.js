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
import {
  clearJustSavedIqConfig,
  peekJustSavedIqConfig,
  setJustSavedIqConfig
} from './iqServerJustSavedCache';

describe('iqServerJustSavedCache', () => {
  afterEach(() => {
    clearJustSavedIqConfig();
  });

  it('returns null before anything has been set', () => {
    expect(peekJustSavedIqConfig()).toBeNull();
  });

  it('peeks without clearing, and clear empties the slot', () => {
    const config = {enabled: true, url: 'http://iq', licensedSolutions: [{id: 'lifecycle'}]};
    setJustSavedIqConfig(config);

    expect(peekJustSavedIqConfig()).toEqual(config);
    expect(peekJustSavedIqConfig()).toEqual(config);
    clearJustSavedIqConfig();
    expect(peekJustSavedIqConfig()).toBeNull();
  });

  it('keeps only enabled, url and licensedSolutions, so credentials never reach window', () => {
    setJustSavedIqConfig({
      enabled: true,
      url: 'http://iq',
      licensedSolutions: [{id: 'lifecycle'}],
      authenticationType: 'USER',
      username: 'iq-user',
      password: 'secret'
    });

    expect(peekJustSavedIqConfig()).toEqual({enabled: true, url: 'http://iq', licensedSolutions: [{id: 'lifecycle'}]});
    expect(JSON.stringify(window.__nxIqServerJustSaved)).not.toMatch(/iq-user|secret|USER/);
  });

  it('setJustSavedIqConfig(null) clears any prior value', () => {
    setJustSavedIqConfig({enabled: true, url: 'http://iq'});
    setJustSavedIqConfig(null);

    expect(peekJustSavedIqConfig()).toBeNull();
  });

  it('the latest setJustSavedIqConfig wins if called twice', () => {
    setJustSavedIqConfig({url: 'http://iq-old'});
    setJustSavedIqConfig({url: 'http://iq-new'});

    expect(peekJustSavedIqConfig().url).toBe('http://iq-new');
  });

  it('hands off between separately loaded copies of this module', () => {
    // The production build gives each lazy-loaded page chunk its own copy of this module.
    let formCopy;
    let connectedCopy;
    jest.isolateModules(() => {
      formCopy = require('./iqServerJustSavedCache');
    });
    jest.isolateModules(() => {
      connectedCopy = require('./iqServerJustSavedCache');
    });
    const config = {enabled: true, url: 'http://iq', licensedSolutions: [{id: 'lifecycle'}]};

    formCopy.setJustSavedIqConfig(config);

    expect(connectedCopy.peekJustSavedIqConfig()).toEqual(config);
    connectedCopy.clearJustSavedIqConfig();
    expect(formCopy.peekJustSavedIqConfig()).toBeNull();
  });

  it('rejects and clears a stale entry so an aborted navigation cannot feed a later visit', () => {
    const realNow = Date.now;
    let now = 1_000_000;
    Date.now = () => now;
    try {
      setJustSavedIqConfig({url: 'http://iq'});
      now += 10_000; // 10s later, well past the 5s window
      expect(peekJustSavedIqConfig()).toBeNull();
      expect('__nxIqServerJustSaved' in window).toBe(false);
    } finally {
      Date.now = realNow;
    }
  });
});
