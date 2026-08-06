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

const MALICIOUS_RISK_PATH = '/service/rest/v1/malicious-risk';

describe('ENDPOINTS malicious risk constants', () => {
  it('exposes MALICIOUS_RISK_ACTIVE_FINDINGS as the active-findings path', () => {
    expect(ENDPOINTS.MALICIOUS_RISK_ACTIVE_FINDINGS).toBe(`${MALICIOUS_RISK_PATH}/active-findings`);
  });

  it('exposes MALICIOUS_RISK_HISTORY as the history path', () => {
    expect(ENDPOINTS.MALICIOUS_RISK_HISTORY).toBe(`${MALICIOUS_RISK_PATH}/history`);
  });

  it('exposes MALICIOUS_RISK_ACKNOWLEDGE as the acknowledge path', () => {
    expect(ENDPOINTS.MALICIOUS_RISK_ACKNOWLEDGE).toBe(`${MALICIOUS_RISK_PATH}/acknowledge`);
  });

  it('exposes MALICIOUS_RISK_DELETE_FINDING as the delete-finding path', () => {
    expect(ENDPOINTS.MALICIOUS_RISK_DELETE_FINDING).toBe(`${MALICIOUS_RISK_PATH}/delete-finding`);
  });

  it('exposes MALICIOUS_RISK_REMEDIATE as the remediate path', () => {
    expect(ENDPOINTS.MALICIOUS_RISK_REMEDIATE).toBe(`${MALICIOUS_RISK_PATH}/remediate`);
  });

  it('produces a defined value for every malicious risk ENDPOINTS key (regression guard)', () => {
    expect(ENDPOINTS.MALICIOUS_RISK_ACTIVE_FINDINGS).toBeDefined();
    expect(ENDPOINTS.MALICIOUS_RISK_HISTORY).toBeDefined();
    expect(ENDPOINTS.MALICIOUS_RISK_ACKNOWLEDGE).toBeDefined();
    expect(ENDPOINTS.MALICIOUS_RISK_DELETE_FINDING).toBeDefined();
    expect(ENDPOINTS.MALICIOUS_RISK_REMEDIATE).toBeDefined();
  });
});

const HEALTH_CHECK_REPO_PATH = '/service/rest/v1/repositories/maven-central/health-check';

describe('ENDPOINTS repository health check constants', () => {
  it('exposes REPOSITORY_HEALTH_CHECK as a function building the per-repo health-check path', () => {
    expect(typeof ENDPOINTS.REPOSITORY_HEALTH_CHECK).toBe('function');
    expect(ENDPOINTS.REPOSITORY_HEALTH_CHECK('maven-central')).toBe(HEALTH_CHECK_REPO_PATH);
  });

  it('matches HEALTH_CHECK_ANALYZE for the same repository (POST enable / DELETE disable share one URL)', () => {
    expect(ENDPOINTS.REPOSITORY_HEALTH_CHECK('maven-central')).toBe(ENDPOINTS.HEALTH_CHECK_ANALYZE('maven-central'));
  });

  it('URL-encodes special characters in REPOSITORY_HEALTH_CHECK repository names', () => {
    expect(ENDPOINTS.REPOSITORY_HEALTH_CHECK('repo/with/slashes'))
      .toBe('/service/rest/v1/repositories/repo%2Fwith%2Fslashes/health-check');
  });
});
