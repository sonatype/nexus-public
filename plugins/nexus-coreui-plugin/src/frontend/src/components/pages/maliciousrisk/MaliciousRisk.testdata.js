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
export const maliciousRiskProxyUnprotectedResponse = {
  'countByEcosystem': [
    {
      'ecosystem': 'npm',
      'count': 10000,
    },
    {
      'ecosystem': 'pypi',
      'count': 5000,
    },
    {
      'ecosystem': 'maven',
      'count': 1000,
    }
  ],
  'totalMaliciousRiskCount': 16000,
  'totalProxyRepositoryCount': 10,
  'quarantineEnabledRepositoryCount': 0,
  'hdsError': false
}

export const maliciousRiskProxyPartiallyProtectedResponse = {
  'countByEcosystem': [
    {
      'ecosystem': 'npm',
      'count': 10000,
    },
    {
      'ecosystem': 'pypi',
      'count': 5000,
    },
    {
      'ecosystem': 'maven',
      'count': 1000,
    }
  ],
  'totalMaliciousRiskCount': 16000,
  'totalProxyRepositoryCount': 10,
  'quarantineEnabledRepositoryCount': 3,
  'hdsError': false
}

export const maliciousRiskProxyFullyProtectedResponse = {
  'countByEcosystem': [
    {
      'ecosystem': 'npm',
      'count': 10000,
    },
    {
      'ecosystem': 'pypi',
      'count': 5000,
    },
    {
      'ecosystem': 'maven',
      'count': 1000,
    }
  ],
  'totalMaliciousRiskCount': 16000,
  'totalProxyRepositoryCount': 10,
  'quarantineEnabledRepositoryCount': 10,
  'hdsError': false
}

export const maliciousRiskResponseWithHdsError = {
  'countByEcosystem': [],
  'totalMaliciousRiskCount': 0,
  'totalProxyRepositoryCount': 2,
  'quarantineEnabledRepositoryCount': 0,
  'hdsError': true
}
