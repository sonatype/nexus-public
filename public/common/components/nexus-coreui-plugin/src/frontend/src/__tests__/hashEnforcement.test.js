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
import { getContextPath, enforceHashRouting } from '../hashEnforcement';

describe('getContextPath', () => {
  let scriptEl;

  beforeEach(() => {
    // Remove any scripts added by previous tests
    document.querySelectorAll('script[data-test-rapture]').forEach(el => el.remove());
  });

  it('returns empty string when no rapture script is present', () => {
    expect(getContextPath()).toBe('');
  });

  it('returns empty string for root context path', () => {
    scriptEl = document.createElement('script');
    scriptEl.setAttribute('data-test-rapture', '');
    scriptEl.src = 'http://localhost:8081/static/rapture/resources/nexus-coreui-bundle.js';
    document.head.appendChild(scriptEl);

    expect(getContextPath()).toBe('');
  });

  it('returns context path when bundle is served under a non-root path', () => {
    scriptEl = document.createElement('script');
    scriptEl.setAttribute('data-test-rapture', '');
    scriptEl.src = 'https://example.com/nxrm/static/rapture/resources/nexus-coreui-bundle.js';
    document.head.appendChild(scriptEl);

    expect(getContextPath()).toBe('/nxrm');
  });

  it('returns nested context path', () => {
    scriptEl = document.createElement('script');
    scriptEl.setAttribute('data-test-rapture', '');
    scriptEl.src = 'https://example.com/company/nexus/static/rapture/resources/nexus-coreui-bundle.js';
    document.head.appendChild(scriptEl);

    expect(getContextPath()).toBe('/company/nexus');
  });
});

describe('enforceHashRouting', () => {
  let replaceSpy;

  function setLocation({ pathname, hash = '', search = '', origin = 'https://example.com' }) {
    delete window.location;
    window.location = { pathname, hash, search, origin, replace: jest.fn() };
    replaceSpy = window.location.replace;
  }

  function addRaptureScript(contextPath) {
    const script = document.createElement('script');
    script.setAttribute('data-test-rapture', '');
    script.src = `https://example.com${contextPath}/static/rapture/resources/nexus-coreui-bundle.js`;
    document.head.appendChild(script);
    return script;
  }

  beforeEach(() => {
    delete window.__nxHashEnforced;
    document.querySelectorAll('script[data-test-rapture]').forEach(el => el.remove());
  });

  it('does nothing when hash is already present', () => {
    setLocation({ pathname: '/admin/system/tasks', hash: '#/admin/system/tasks' });
    enforceHashRouting();
    expect(replaceSpy).not.toHaveBeenCalled();
  });

  it('does nothing when pathname is root', () => {
    setLocation({ pathname: '/' });
    enforceHashRouting();
    expect(replaceSpy).not.toHaveBeenCalled();
  });

  it('does not run twice (guards with __nxHashEnforced)', () => {
    setLocation({ pathname: '/admin/system/tasks' });
    window.__nxHashEnforced = true;
    enforceHashRouting();
    expect(replaceSpy).not.toHaveBeenCalled();
  });

  it('redirects non-hash URL to hash URL for root context path', () => {
    setLocation({ pathname: '/admin/system/tasks', origin: 'http://localhost:8081' });
    addRaptureScript('');
    enforceHashRouting();
    expect(replaceSpy).toHaveBeenCalledWith('http://localhost:8081/#/admin/system/tasks');
  });

  it('preserves context path when redirecting for non-root context path', () => {
    setLocation({ pathname: '/nxrm/admin/system/tasks', origin: 'https://example.com' });
    addRaptureScript('/nxrm');
    enforceHashRouting();
    expect(replaceSpy).toHaveBeenCalledWith('https://example.com/nxrm/#/admin/system/tasks');
  });

  it('preserves context path for browse routes', () => {
    setLocation({ pathname: '/nxrm/browse/components', origin: 'https://example.com' });
    addRaptureScript('/nxrm');
    enforceHashRouting();
    expect(replaceSpy).toHaveBeenCalledWith('https://example.com/nxrm/#/browse/components');
  });

  it('does not redirect when already at context path root', () => {
    setLocation({ pathname: '/nxrm/', origin: 'https://example.com' });
    addRaptureScript('/nxrm');
    enforceHashRouting();
    expect(replaceSpy).not.toHaveBeenCalled();
  });

  it('does not redirect when pathname equals context path without trailing slash', () => {
    setLocation({ pathname: '/nxrm', origin: 'https://example.com' });
    addRaptureScript('/nxrm');
    enforceHashRouting();
    expect(replaceSpy).not.toHaveBeenCalled();
  });

  it('preserves search query in the redirect URL', () => {
    setLocation({ pathname: '/nxrm/admin/system/tasks', search: '?debug', origin: 'https://example.com' });
    addRaptureScript('/nxrm');
    enforceHashRouting();
    expect(replaceSpy).toHaveBeenCalledWith('https://example.com/nxrm/?debug#/admin/system/tasks');
  });

  it('falls back to stripping all leading slashes when no rapture script is found', () => {
    setLocation({ pathname: '/admin/system/tasks', origin: 'https://example.com' });
    // No rapture script added — contextPath defaults to ''
    enforceHashRouting();
    expect(replaceSpy).toHaveBeenCalledWith('https://example.com/#/admin/system/tasks');
  });
});
