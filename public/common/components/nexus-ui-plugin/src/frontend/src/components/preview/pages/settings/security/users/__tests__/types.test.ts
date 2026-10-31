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
  getUserProtectionReason,
  isProtectedUser,
  ProtectionContext,
  User,
} from '../types';

const EXTERNAL_REASON = 'External users cannot be deleted from Nexus.';
const ANONYMOUS_REASON = 'The anonymous user is a system account and cannot be deleted.';
const SELF_REASON = 'You cannot delete your own account.';

const ANONYMOUS_USERNAME = 'anonymous';
const CURRENT_USER_ID = 'jsmith';

function makeUser(overrides: Partial<User> = {}): User {
  return {
    userId: 'jsmith',
    realm: 'default',
    source: 'default',
    firstName: 'John',
    lastName: 'Smith',
    emailAddress: 'jsmith@example.com',
    status: 'active',
    roles: [],
    ...overrides,
  };
}

const emptyContext: ProtectionContext = {};

const populatedContext: ProtectionContext = {
  anonymousUsername: ANONYMOUS_USERNAME,
  currentUserId: CURRENT_USER_ID,
};

describe('getUserProtectionReason', () => {
  it('returns the external reason for a user whose source is not "default"', () => {
    const externalUser = makeUser({ userId: 'someone', source: 'LDAP' });
    expect(getUserProtectionReason(externalUser, populatedContext)).toBe(EXTERNAL_REASON);
  });

  it('returns the anonymous reason when userId matches ctx.anonymousUsername', () => {
    const anon = makeUser({ userId: ANONYMOUS_USERNAME, source: 'default' });
    expect(getUserProtectionReason(anon, populatedContext)).toBe(ANONYMOUS_REASON);
  });

  it('returns the self reason when userId matches ctx.currentUserId', () => {
    const self = makeUser({ userId: CURRENT_USER_ID, source: 'default' });
    expect(getUserProtectionReason(self, populatedContext)).toBe(SELF_REASON);
  });

  it('returns null for a non-protected local user (including userId "admin")', () => {
    const regular = makeUser({ userId: 'regular', source: 'default' });
    const admin = makeUser({ userId: 'admin', source: 'default' });
    expect(getUserProtectionReason(regular, populatedContext)).toBeNull();
    expect(getUserProtectionReason(admin, populatedContext)).toBeNull();
  });

  it('gives external precedence over anonymous (anonymous userId on external realm returns external reason)', () => {
    const anonOnLdap = makeUser({ userId: ANONYMOUS_USERNAME, source: 'LDAP' });
    expect(getUserProtectionReason(anonOnLdap, populatedContext)).toBe(EXTERNAL_REASON);
  });

  it('gives anonymous precedence over self (anonymous match wins over current-user match)', () => {
    const anonAsSelf = makeUser({ userId: ANONYMOUS_USERNAME, source: 'default' });
    const ctx: ProtectionContext = {
      anonymousUsername: ANONYMOUS_USERNAME,
      currentUserId: ANONYMOUS_USERNAME,
    };
    expect(getUserProtectionReason(anonAsSelf, ctx)).toBe(ANONYMOUS_REASON);
  });

  it('ignores anonymousUsername when it is null', () => {
    const anon = makeUser({ userId: ANONYMOUS_USERNAME, source: 'default' });
    expect(getUserProtectionReason(anon, { anonymousUsername: null })).toBeNull();
  });

  it('ignores anonymousUsername when it is an empty string', () => {
    const empty = makeUser({ userId: '', source: 'default' });
    expect(getUserProtectionReason(empty, { anonymousUsername: '' })).toBeNull();
  });

  it('ignores currentUserId when it is null', () => {
    const self = makeUser({ userId: CURRENT_USER_ID, source: 'default' });
    expect(getUserProtectionReason(self, { currentUserId: null })).toBeNull();
  });

  it('returns null when both context fields are omitted and the user is a local non-external account', () => {
    const regular = makeUser({ userId: 'regular', source: 'default' });
    expect(getUserProtectionReason(regular, emptyContext)).toBeNull();
  });
});

describe('isProtectedUser', () => {
  it('returns false when getUserProtectionReason returns null', () => {
    const regular = makeUser({ userId: 'regular', source: 'default' });
    expect(isProtectedUser(regular, populatedContext)).toBe(false);
  });

  it('returns true for an external user', () => {
    const externalUser = makeUser({ userId: 'someone', source: 'SAML' });
    expect(isProtectedUser(externalUser, populatedContext)).toBe(true);
  });

  it('returns true for the anonymous user', () => {
    const anon = makeUser({ userId: ANONYMOUS_USERNAME, source: 'default' });
    expect(isProtectedUser(anon, populatedContext)).toBe(true);
  });

  it('returns true for the currently-logged-in user', () => {
    const self = makeUser({ userId: CURRENT_USER_ID, source: 'default' });
    expect(isProtectedUser(self, populatedContext)).toBe(true);
  });

  it('returns false for userId "admin" when it is not the current user or anonymous', () => {
    const admin = makeUser({ userId: 'admin', source: 'default' });
    expect(isProtectedUser(admin, populatedContext)).toBe(false);
  });
});
