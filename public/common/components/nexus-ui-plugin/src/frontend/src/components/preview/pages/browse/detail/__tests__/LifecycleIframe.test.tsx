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

import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import { Theme } from '@radix-ui/themes';

import { LifecycleIframe, buildHealthCheckFileDetailUrl, bridgeInsight } from '../LifecycleIframe';
import ExtJS from '../../../../../../interface/ExtJS';
import Permissions from '../../../../../../constants/Permissions';

const renderWithTheme = (ui: React.ReactElement) => render(<Theme>{ui}</Theme>);

// Standard base64 of the UTF-8 bytes, matching NX.util.Base64.encode.
const b64 = (s: string) => Buffer.from(s, 'utf-8').toString('base64');
// One URL path segment as the builder emits it: base64, then percent-encoded so `+/=` stay data.
const seg = (s: string) => encodeURIComponent(b64(s));

/**
 * Drives the four inputs of Classic's `isHealthCheckEnabled()` — user, permission, edition,
 * license — independently, so each arm of the entitlement gate can be asserted on its own.
 * Defaults are the fully-entitled Pro instance; each test overrides just the arm it exercises.
 */
const setEntitlement = ({
  user = { id: 'admin', authenticated: true },
  permission = true,
  edition = 'PRO',
  license = { installed: true, valid: true },
}: {
  user?: unknown;
  permission?: boolean;
  edition?: string;
  license?: { installed?: boolean; valid?: boolean } | null;
} = {}) => {
  jest.spyOn(ExtJS, 'useUser').mockReturnValue(user as never);
  jest.spyOn(ExtJS, 'useLicense').mockReturnValue(license as never);
  // Run the real predicate so the test covers the permission constant, not just a boolean.
  jest.spyOn(ExtJS, 'usePermission').mockImplementation((getValue: () => boolean) => getValue());
  jest
    .spyOn(ExtJS, 'checkPermission')
    .mockImplementation((p: string) => permission && p === Permissions.HEALTHCHECK_ALERTS.READ);
  jest.spyOn(ExtJS, 'state').mockReturnValue({
    getValue: () => undefined,
    getEdition: () => edition,
  } as unknown as ReturnType<typeof ExtJS.state>);
};

const ENTITLED_PROPS = {
  repositoryName: 'maven-releases',
  path: 'a/b/thing.jar',
  format: 'maven2',
  componentId: 'component-1',
};

beforeEach(() => {
  jest.restoreAllMocks();
  // urlOf: prefix a relative path with the app base, mirroring NX.util.Url.urlOf.
  jest.spyOn(ExtJS, 'urlOf').mockImplementation((p: string) => `/nexus${p.startsWith('/') ? p : `/${p}`}`);
});

describe('buildHealthCheckFileDetailUrl (Area 6 / NEXUS-54501)', () => {
  it('encodes the path and repository as base64 and appends the format query', () => {
    const path = 'org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar';
    const repositoryName = 'maven-releases';

    const url = buildHealthCheckFileDetailUrl({ path, repositoryName, format: 'maven2' });

    expect(url).toBe(
      `/nexus/service/rest/healthcheck/healthCheckFileDetail/${seg(path)}/${seg(repositoryName)}/index.html?format=maven2`,
    );
  });

  // The server template is /{base64Uri}/{base64Repository}/{path:.*}, so a raw `/` from the standard
  // base64 alphabet would shift the segments and hand decodeBase64 a fragment. Percent-encoding keeps
  // the segment intact; @PathParam decodes it back to the original base64.
  it.each([
    // A literal `?` at an index 2 mod 3 puts 0x3F in the low six bits: 'ab?cd' -> 'YWI/Y2Q='.
    ['an ASCII path whose base64 contains a slash', 'ab?cd/thing.jar'],
    ['a Cyrillic path', 'пакет/x.jar'],
    ['an emoji path', 'dir/🚀.jar'],
  ])('percent-encodes the base64 segments for %s', (_case, path) => {
    const raw = b64(path);
    expect(raw).toContain('/');

    const url = buildHealthCheckFileDetailUrl({ path, repositoryName: 'maven-releases', format: 'maven2' });

    // Exactly the three template segments plus index.html — the base64 slash must not add a fourth.
    expect(url).toBe(
      `/nexus/service/rest/healthcheck/healthCheckFileDetail/${encodeURIComponent(raw)}/${seg('maven-releases')}/index.html?format=maven2`,
    );
    expect(url).toContain('%2F');
    // What the resource receives after @PathParam decoding is the untouched base64.
    const segments = url.slice(url.indexOf('healthCheckFileDetail/')).split('/');
    expect(decodeURIComponent(segments[1])).toBe(raw);
  });

  it('percent-encodes a repository name whose base64 contains a slash', () => {
    const repositoryName = 'ab?cd';
    const raw = b64(repositoryName);
    expect(raw).toContain('/');

    const url = buildHealthCheckFileDetailUrl({ path: 'a/b.jar', repositoryName, format: 'maven2' });

    expect(url).toContain(`/${encodeURIComponent(raw)}/index.html`);
  });
});

describe('LifecycleIframe entitlement gate (Area 6 / NEXUS-54501)', () => {
  it('renders the embedded Health Check report iframe for an entitled user', () => {
    setEntitlement();

    renderWithTheme(<LifecycleIframe {...ENTITLED_PROPS} />);

    const iframe = screen.getByTitle('Sonatype Lifecycle');
    expect(iframe).toBeInTheDocument();
    expect(iframe).toHaveAttribute(
      'src',
      buildHealthCheckFileDetailUrl({
        path: ENTITLED_PROPS.path,
        repositoryName: ENTITLED_PROPS.repositoryName,
        format: ENTITLED_PROPS.format,
      }),
    );
  });

  // Each case is one arm of Classic's isHealthCheckEnabled() plus its componentId check. All must
  // withhold the iframe: the backend answers an unentitled request with a 403 or licence error page,
  // which inside an iframe reads as a broken tab rather than a graceful degradation.
  it.each([
    ['the user is anonymous', { user: null }],
    ['the user lacks nexus:healthcheckalerts:read', { permission: false }],
    ['the edition is EVAL', { edition: 'EVAL' }],
    ['the license is not installed', { license: { installed: false, valid: true } }],
    ['the license is installed but invalid', { license: { installed: true, valid: false } }],
    ['the license state has not been published yet', { license: null }],
    // The two non-Pro editions, with the license state they actually publish:
    // `ApplicationLicenseImpl.isInstalled()`/`isValid()` are hardcoded false in both
    // nexus-community-edition and nexus-repository-core-edition, so the license arm already
    // excludes every edition the backend resource is not built for. Pro and Cloud both report the
    // `PRO` short name (ProfessionalNexusEdition/CloudNexusEdition), and the resource is
    // `@ConditionalOnEdition(cloud = true, pro = true)` — so the edition set is closed and no
    // separate isProEdition() term is needed here.
    ['the edition is COMMUNITY', { edition: 'COMMUNITY', license: { installed: false, valid: false } }],
    ['the edition is CORE', { edition: 'CORE', license: { installed: false, valid: false } }],
  ])('withholds the report when %s', (_case, overrides) => {
    setEntitlement(overrides as Parameters<typeof setEntitlement>[0]);

    renderWithTheme(<LifecycleIframe {...ENTITLED_PROPS} />);

    expect(screen.queryByTitle('Sonatype Lifecycle')).not.toBeInTheDocument();
    expect(screen.getByText(/not available for this asset/i)).toBeInTheDocument();
  });

  it.each([
    ['componentId is absent', { componentId: undefined }],
    ['componentId is blank', { componentId: '   ' }],
    ['the asset path is missing', { path: '' }],
    ['the repository name is missing', { repositoryName: '' }],
    ['the format is missing', { format: '' }],
  ])('withholds the report when %s', (_case, overrides) => {
    setEntitlement();

    renderWithTheme(<LifecycleIframe {...ENTITLED_PROPS} {...overrides} />);

    expect(screen.queryByTitle('Sonatype Lifecycle')).not.toBeInTheDocument();
    expect(screen.getByText(/not available for this asset/i)).toBeInTheDocument();
  });

  it('does not gate on the clm state value: an entitled instance with IQ disconnected still renders', () => {
    // Repository Health Check serves the report without an IQ Server connection, so `clm.enabled`
    // must not be part of the gate (the bug this replaced).
    setEntitlement();
    jest.spyOn(ExtJS, 'state').mockReturnValue({
      getValue: (key: string) => (key === 'clm' ? { enabled: false } : undefined),
      getEdition: () => 'PRO',
    } as unknown as ReturnType<typeof ExtJS.state>);

    renderWithTheme(<LifecycleIframe {...ENTITLED_PROPS} />);

    expect(screen.getByTitle('Sonatype Lifecycle')).toBeInTheDocument();
  });
});

describe('bridgeInsight (Area 6 / NEXUS-54501)', () => {
  const identity = {
    componentIdentifier: {
      format: 'maven',
      coordinates: { groupId: 'org.apache.commons', artifactId: 'commons-lang3', version: '3.14.0' },
    },
    hash: 'abc123',
  };

  it('identifies the component and sets coordinates on a modern Insight bridge', async () => {
    const setCoordinates = jest.fn();
    const registerCoordsViewDetailsListener = jest.fn();
    const insight = { setCoordinates, registerCoordsViewDetailsListener };
    const identify = jest.fn().mockResolvedValue(identity);

    await bridgeInsight(insight as never, {
      repositoryName: 'maven-releases',
      path: 'a/b/thing.jar',
      identify,
      openWindow: jest.fn(),
    });

    expect(identify).toHaveBeenCalledWith('maven-releases', 'a/b/thing.jar');
    expect(setCoordinates).toHaveBeenCalledWith('maven', identity.componentIdentifier.coordinates, { hash: 'abc123' });
    expect(registerCoordsViewDetailsListener).toHaveBeenCalledTimes(1);
  });

  it('falls back to setGav for a legacy Insight bridge without setCoordinates', async () => {
    const setGav = jest.fn();
    const registerViewDetailsListener = jest.fn();
    const insight = { setGav, registerViewDetailsListener };
    const identify = jest.fn().mockResolvedValue(identity);

    await bridgeInsight(insight as never, {
      repositoryName: 'maven-releases',
      path: 'a/b/thing.jar',
      identify,
      openWindow: jest.fn(),
    });

    expect(setGav).toHaveBeenCalledWith(
      expect.objectContaining({ groupId: 'org.apache.commons', artifactId: 'commons-lang3', version: '3.14.0', hash: 'abc123' }),
    );
  });

  it('sends a legacy bridge exactly the GAV key set Classic sends, dropping format-specific extras', async () => {
    const setGav = jest.fn();
    const insight = { setGav, registerViewDetailsListener: jest.fn() };
    const identify = jest.fn().mockResolvedValue({
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          groupId: 'org.apache.commons',
          artifactId: 'commons-lang3',
          version: '3.14.0',
          extension: 'jar',
          classifier: 'sources',
          // Not part of setGav's contract; a legacy bridge has no idea what to do with it.
          packaging: 'bundle',
        },
      },
      hash: 'abc123',
    });

    await bridgeInsight(insight as never, {
      repositoryName: 'maven-releases',
      path: 'a/b/thing.jar',
      identify,
      openWindow: jest.fn(),
    });

    expect(setGav).toHaveBeenCalledTimes(1);
    expect(Object.keys(setGav.mock.calls[0][0]).sort()).toEqual([
      'artifactId',
      'classifier',
      'extension',
      'groupId',
      'hash',
      'version',
    ]);
  });

  it('opens the IQ details window when the modern listener supplies coordinates', async () => {
    const openWindow = jest.fn();
    let listener: ((appId: string, format: string, coords: string[] | undefined, hash: string) => void) | undefined;
    const insight = {
      setCoordinates: jest.fn(),
      registerCoordsViewDetailsListener: jest.fn((cb) => {
        listener = cb;
      }),
    };

    await bridgeInsight(insight as never, {
      repositoryName: 'maven-releases',
      path: 'a/b/thing.jar',
      identify: jest.fn().mockResolvedValue(identity),
      openWindow,
    });

    listener?.('app-1', 'maven', ['groupId', 'org.apache.commons', 'artifactId', 'commons-lang3'], 'abc123');

    expect(openWindow).toHaveBeenCalledTimes(1);
    const url = openWindow.mock.calls[0][0] as string;
    expect(url).toContain('viewdetails.html?');
    expect(decodeURIComponent(url)).toContain('"artifactId":"commons-lang3"');
  });

  it('does not open the IQ details window when the modern listener supplies no coordinates', async () => {
    // Classic guards the NX.Windows.open call on `if (coordinates)`; without them viewdetails.html
    // has no component to resolve, so opening it would show the user an empty IQ page.
    const openWindow = jest.fn();
    let listener: ((appId: string, format: string, coords: string[] | undefined, hash: string) => void) | undefined;
    const insight = {
      setCoordinates: jest.fn(),
      registerCoordsViewDetailsListener: jest.fn((cb) => {
        listener = cb;
      }),
    };

    await bridgeInsight(insight as never, {
      repositoryName: 'maven-releases',
      path: 'a/b/thing.jar',
      identify: jest.fn().mockResolvedValue(identity),
      openWindow,
    });

    listener?.('app-1', 'maven', undefined, 'abc123');

    expect(openWindow).not.toHaveBeenCalled();
  });

  it('errors on a legacy bridge for a non-maven format instead of sending unusable coordinates', async () => {
    const setError = jest.fn();
    const setGav = jest.fn();
    const insight = { setGav, registerViewDetailsListener: jest.fn(), setError };

    await bridgeInsight(insight as never, {
      repositoryName: 'npm-proxy',
      path: 'lodash/-/lodash-4.17.21.tgz',
      identify: jest.fn().mockResolvedValue({
        componentIdentifier: { format: 'npm', coordinates: { packageId: 'lodash', version: '4.17.21' } },
        hash: 'def456',
      }),
      openWindow: jest.fn(),
    });

    expect(setGav).not.toHaveBeenCalled();
    expect(setError).toHaveBeenCalledWith(
      expect.objectContaining({ errorMessage: expect.stringContaining('1.13'), errorCode: 400 }),
    );
  });

  it('reports an error on the bridge when identification fails', async () => {
    const setError = jest.fn();
    const insight = { setCoordinates: jest.fn(), registerCoordsViewDetailsListener: jest.fn(), setError };
    const identify = jest.fn().mockRejectedValue(new Error('boom'));

    await bridgeInsight(insight as never, {
      repositoryName: 'maven-releases',
      path: 'a/b/thing.jar',
      identify,
      openWindow: jest.fn(),
    });

    expect(setError).toHaveBeenCalledWith(expect.objectContaining({ errorCode: expect.any(Number) }));
  });
});
