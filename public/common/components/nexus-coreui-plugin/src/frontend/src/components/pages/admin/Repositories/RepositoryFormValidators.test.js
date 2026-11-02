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
import {genericValidators} from './RepositoryFormValidators';
import UIStrings from '../../../../constants/UIStrings';

const SWIFT_ERROR = UIStrings.REPOSITORIES.EDITOR.SWIFT_REMOTE_URL_VALIDATION_ERROR;
const REQUIRED = UIStrings.ERROR.FIELD_REQUIRED;

const proxyData = ({format = 'swift', type = 'proxy', remoteUrl}) => ({
  format,
  type,
  proxy: {remoteUrl, contentMaxAge: 1440, metadataMaxAge: 1440},
  negativeCache: {timeToLive: 1440},
  httpClient: {connection: {}}
});

const remoteUrlError = (data) => genericValidators.proxy(data).proxy.remoteUrl;

describe('Swift proxy remote URL validation', () => {
  describe('accepted and rejected remote URLs', () => {
    // `undefined` means accepted.
    it.each([
      // Accepted values fall through the `||` chain, which yields undefined (not null).
      ['https://github.com/', undefined, 'canonical form'],
      ['https://github.com', undefined, 'trailing slash optional'],
      ['https://GitHub.com', SWIFT_ERROR, 'letter case must match exactly'],
      ['HTTPS://github.com/', SWIFT_ERROR, 'letter case must match exactly'],
      ['https://GITHUB.COM', SWIFT_ERROR, 'letter case must match exactly'],
      ['https://github.com:443/', SWIFT_ERROR, 'explicit port not accepted'],
      ['https://github.com/apple', SWIFT_ERROR, 'nothing may follow the root'],
      ['https://github.com/apple/swift-nio', SWIFT_ERROR, 'nothing may follow the root'],
      ['http://github.com/', SWIFT_ERROR, 'scheme must be https'],
      ['https://raw.githubusercontent.com/', SWIFT_ERROR, 'different host'],
      ['https://api.github.com/', SWIFT_ERROR, 'different host'],
      ['https://github.com.evil.example/', SWIFT_ERROR, 'must not be a prefix match'],
      ['https://user:pw@github.com/', SWIFT_ERROR, 'userinfo not accepted'],
      ['https://gitlab.com/foo/bar', SWIFT_ERROR, 'different host'],
      ['git@github.com:apple/swift-nio.git', SWIFT_ERROR, 'not an HTTPS URL'],
      ['ftp://github.com/', SWIFT_ERROR, 'scheme must be https'],
      ['//github.com/', SWIFT_ERROR, 'scheme is required'],
      ['github.com', SWIFT_ERROR, 'scheme is required']
    ])('%s -> %s (%s)', (remoteUrl, expected) => {
      expect(remoteUrlError(proxyData({remoteUrl}))).toBe(expected);
    });

    it.each([
      ['', 'empty'],
      [null, 'null'],
      [undefined, 'undefined'],
      ['   ', 'whitespace only']
    ])('rejects %s (%s) with the unchanged required message', (remoteUrl) => {
      // The existing not-blank rule is unchanged, so blank must NOT surface
      // the Swift-specific message.
      expect(remoteUrlError(proxyData({remoteUrl}))).toBe(REQUIRED);
    });

    it('rejects an untrimmed value rather than trimming it', () => {
      // Whitespace convention for this PR: reject, matching the platform's existing
      // TRIM_ERROR convention for URL fields.
      expect(remoteUrlError(proxyData({remoteUrl: ' https://github.com/ '}))).toBe(SWIFT_ERROR);
      expect(remoteUrlError(proxyData({remoteUrl: 'https://github.com/ '}))).toBe(SWIFT_ERROR);
    });
  });

  describe('prefix-match regression guard', () => {
    it.each([
      'https://github.com.evil.example/',
      'https://github.com.evil.example',
      'https://github.company.com/',
      'https://github.com@evil.example/',
      'https://github.com/../apple',
      'https://github.com//',
      'https://github.com:/'
    ])('rejects %s', (remoteUrl) => {
      expect(remoteUrlError(proxyData({remoteUrl}))).toBe(SWIFT_ERROR);
    });
  });

  describe('case-insensitive generic validator interaction', () => {
    it('surfaces the Swift message, not the generic URL message, for mixed case', () => {
      // The shared URL check is case-insensitive and would accept these, so the Swift
      // rule must run first and own the message.
      expect(remoteUrlError(proxyData({remoteUrl: 'https://GitHub.com/'}))).toBe(SWIFT_ERROR);
      expect(remoteUrlError(proxyData({remoteUrl: 'HTTPS://GITHUB.COM/'}))).toBe(SWIFT_ERROR);
    });
  });

  describe('no collateral change to other formats', () => {
    it.each(['maven2', 'npm', 'docker', 'raw', 'nuget'])(
      '%s proxy still accepts an arbitrary remote URL',
      (format) => {
        expect(remoteUrlError(proxyData({format, remoteUrl: 'https://gitlab.com/foo/bar'}))).toBeUndefined();
      }
    );

    it('a non-swift proxy still accepts a non-GitHub host', () => {
      expect(remoteUrlError(proxyData({format: 'maven2', remoteUrl: 'https://repo1.maven.org/maven2/'}))).toBeUndefined();
    });

    it('a non-swift proxy still rejects a malformed URL via the generic rule', () => {
      expect(remoteUrlError(proxyData({format: 'maven2', remoteUrl: 'not-a-url'}))).toBeTruthy();
    });
  });

  describe('swift hosted and group are unaffected', () => {
    it('does not apply the rule when type is not proxy', () => {
      // Guarded on type as well as format; hosted/group have no remote URL field, so the
      // rule must never fire for them even if the validator is reached.
      const data = proxyData({type: 'hosted', remoteUrl: 'https://gitlab.com/foo/bar'});
      expect(remoteUrlError(data)).toBeUndefined();
    });
  });

  describe('error message', () => {
    it('states the accepted value so it is copyable', () => {
      expect(SWIFT_ERROR).toBe('Remote storage URL for Swift repositories must be https://github.com/');
    });
  });
});
