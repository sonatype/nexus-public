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

import { ENDPOINTS } from '../rest-client';

const FIREWALL_STATUS_PATH = '/service/rest/internal/ui/firewall/status';
const FIREWALL_STATUS_SUMMARY_PATH = `${FIREWALL_STATUS_PATH}/summary`;
const FIREWALL_STATUS_REPO_PATH = `${FIREWALL_STATUS_PATH}/repo`;
const REPOSITORY_NAME_SIMPLE = 'maven-central';
const REPOSITORY_NAME_WITH_SPACES = 'maven repo with spaces';
const REPOSITORY_NAME_WITH_SPECIAL = 'repo/with/slashes';

describe('ENDPOINTS firewall constants', () => {
  it('exposes FIREWALL_STATUS as the internal-UI firewall status path', () => {
    expect(ENDPOINTS.FIREWALL_STATUS).toBe(FIREWALL_STATUS_PATH);
  });

  it('exposes FIREWALL_STATUS_SUMMARY as the internal-UI firewall summary path', () => {
    expect(ENDPOINTS.FIREWALL_STATUS_SUMMARY).toBe(FIREWALL_STATUS_SUMMARY_PATH);
  });

  it('builds FIREWALL_STATUS_REPO for a simple repository name', () => {
    expect(ENDPOINTS.FIREWALL_STATUS_REPO(REPOSITORY_NAME_SIMPLE))
      .toBe(`${FIREWALL_STATUS_REPO_PATH}/${REPOSITORY_NAME_SIMPLE}`);
  });

  it('URL-encodes whitespace in FIREWALL_STATUS_REPO repository names', () => {
    expect(ENDPOINTS.FIREWALL_STATUS_REPO(REPOSITORY_NAME_WITH_SPACES))
      .toBe(`${FIREWALL_STATUS_REPO_PATH}/maven%20repo%20with%20spaces`);
  });

  it('URL-encodes path separators in FIREWALL_STATUS_REPO repository names', () => {
    expect(ENDPOINTS.FIREWALL_STATUS_REPO(REPOSITORY_NAME_WITH_SPECIAL))
      .toBe(`${FIREWALL_STATUS_REPO_PATH}/repo%2Fwith%2Fslashes`);
  });

  it('produces a defined value for every firewall ENDPOINTS key (regression guard)', () => {
    expect(ENDPOINTS.FIREWALL_STATUS).toBeDefined();
    expect(ENDPOINTS.FIREWALL_STATUS_SUMMARY).toBeDefined();
    expect(typeof ENDPOINTS.FIREWALL_STATUS_REPO).toBe('function');
    expect(ENDPOINTS.FIREWALL_STATUS_REPO(REPOSITORY_NAME_SIMPLE)).toBeDefined();
  });
});
