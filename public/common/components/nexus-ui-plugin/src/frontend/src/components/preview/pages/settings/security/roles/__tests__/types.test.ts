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
  formatRoleSourceDisplay,
  isExternalRole,
  isReadOnlyRole,
  Role,
} from '../types';

const DEFAULT_SOURCE_LOWER = 'default';
const NEXUS_SOURCE = 'Default';
const LDAP_SOURCE = 'LDAP';
const SAML_SOURCE = 'SAML';
const CROWD_SOURCE = 'Crowd';
const OAUTH2_SOURCE = 'OAuth2';

function makeRole(overrides: Partial<Role> = {}): Role {
  return {
    id: 'r1',
    version: '1',
    source: NEXUS_SOURCE,
    name: 'Role One',
    description: '',
    readOnly: false,
    privileges: [],
    roles: [],
    ...overrides,
  };
}

describe('isReadOnlyRole', () => {
  it('returns true when role.readOnly is true', () => {
    expect(isReadOnlyRole(makeRole({ readOnly: true }))).toBe(true);
  });

  it('returns false when role.readOnly is false', () => {
    expect(isReadOnlyRole(makeRole({ readOnly: false }))).toBe(false);
  });

  // NEXUS-54589 AC0 regression guard: an LDAP/SAML/Crowd/OAuth2 role must not be
  // treated as read-only just because its source is external. Editability is
  // decided by the backend readOnly flag alone (mirrors classic UI's rule).
  it('returns false for LDAP role when readOnly is false', () => {
    expect(isReadOnlyRole(makeRole({ source: LDAP_SOURCE, readOnly: false }))).toBe(false);
  });

  it('returns false for SAML role when readOnly is false', () => {
    expect(isReadOnlyRole(makeRole({ source: SAML_SOURCE, readOnly: false }))).toBe(false);
  });

  it('returns false for Crowd role when readOnly is false', () => {
    expect(isReadOnlyRole(makeRole({ source: CROWD_SOURCE, readOnly: false }))).toBe(false);
  });

  it('returns false for OAuth2 role when readOnly is false', () => {
    expect(isReadOnlyRole(makeRole({ source: OAUTH2_SOURCE, readOnly: false }))).toBe(false);
  });

  it('returns true for an external role when the backend flags it read-only', () => {
    expect(isReadOnlyRole(makeRole({ source: LDAP_SOURCE, readOnly: true }))).toBe(true);
  });
});

describe('isExternalRole', () => {
  it('returns false for the Default (Nexus) source', () => {
    expect(isExternalRole(NEXUS_SOURCE)).toBe(false);
  });

  it('returns false for the lowercase "default" source', () => {
    expect(isExternalRole(DEFAULT_SOURCE_LOWER)).toBe(false);
  });

  it('returns false for the literal "nexus" source', () => {
    expect(isExternalRole('nexus')).toBe(false);
  });

  it('returns false for an empty source string', () => {
    expect(isExternalRole('')).toBe(false);
  });

  it('returns true for LDAP source', () => {
    expect(isExternalRole(LDAP_SOURCE)).toBe(true);
  });

  it('returns true for SAML source', () => {
    expect(isExternalRole(SAML_SOURCE)).toBe(true);
  });

  it('returns true for Crowd source', () => {
    expect(isExternalRole(CROWD_SOURCE)).toBe(true);
  });
});

describe('formatRoleSourceDisplay', () => {
  it('returns "Default" when source is the lowercase "default"', () => {
    expect(formatRoleSourceDisplay(DEFAULT_SOURCE_LOWER)).toBe('Default');
  });

  it('returns "Default" when source is the legacy "Nexus"', () => {
    expect(formatRoleSourceDisplay('Nexus')).toBe('Default');
  });

  it('returns "Default" when source is missing', () => {
    expect(formatRoleSourceDisplay('')).toBe('Default');
  });

  it('returns the original source string for external providers', () => {
    expect(formatRoleSourceDisplay(LDAP_SOURCE)).toBe(LDAP_SOURCE);
    expect(formatRoleSourceDisplay(SAML_SOURCE)).toBe(SAML_SOURCE);
  });
});
