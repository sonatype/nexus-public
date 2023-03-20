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
export const REPOS = [
  {
    name: 'maven-central',
    type: 'proxy',
    format: 'maven2',
    status: {repositoryName: 'maven-central', online: true, description: 'Ready to Connect'},
    url: 'http://localhost:8081/repository/maven-central/'
  },
  {
    name: 'maven-public',
    type: 'group',
    format: 'maven2',
    status: {repositoryName: 'maven-public', online: true},
    url: 'http://localhost:8081/repository/maven-public/'
  },
  {
    name: 'maven-releases',
    type: 'hosted',
    format: 'maven2',
    status: {repositoryName: 'maven-releases', online: true},
    url: 'http://localhost:8081/repository/maven-releases/'
  },
  {
    name: 'maven-snapshots',
    type: 'hosted',
    format: 'maven2',
    status: {repositoryName: 'maven-snapshots', online: false},
    url: 'http://localhost:8081/repository/maven-snapshots/'
  },
  {
    name: 'nuget-group',
    type: 'group',
    format: 'nuget',
    status: {repositoryName: 'nuget-group', online: true},
    url: 'http://localhost:8081/repository/nuget-group/'
  },
  {
    name: 'nuget-hosted',
    type: 'hosted',
    format: 'nuget',
    status: {repositoryName: 'nuget-hosted',online: true},
    url: 'http://localhost:8081/repository/nuget-hosted/'
  },
  {
    name: 'nuget.org-proxy',
    type: 'proxy',
    format: 'nuget',
    status: {repositoryName: 'nuget.org-proxy', online: true, description: 'Ready to Connect'},
    url: 'http://localhost:8081/repository/nuget.org-proxy/'
  }
];

export const FIELDS = {
  NAME: 'name',
  TYPE: 'type',
  FORMAT: 'format',
  STATUS: 'status',
};

export const ROW_INDICES = {
  MAVEN_CENTRAL: 1, // health-check/fiewall status available
  MAVEN_PUBLIC: 2,
  NUGET_HOSTED: 6,
  NUGET_ORG_PROXY: 7 // health-check/fiewall status available
};

export const READ_HEALTH_CHECK_DATA = [
  {
    repositoryName: 'maven-central',
    enabled: false,
    analyzing: false
  },
  {
    repositoryName: 'nuget.org-proxy',
    enabled: false,
    analyzing: false
  }
];

export const READ_FIREWALL_STATUS_DATA = [
  {
    repositoryName: 'maven-central',
    affectedComponentCount: 0,
    criticalComponentCount: 10,
    moderateComponentCount: 30,
    quarantinedComponentCount: 15,
    severeComponentCount: 20,
    reportUrl:
      'http://localhost:8070/ui/links/repository/b3b7a0c423014acb9ad1e414b221a111/result',
    message: null,
    errorMessage: null
  },
  {
    repositoryName: 'nuget.org-proxy',
    affectedComponentCount: 0,
    criticalComponentCount: 11,
    moderateComponentCount: 33,
    quarantinedComponentCount: 17,
    severeComponentCount: 22,
    reportUrl:
      'http://localhost:8070/ui/links/repository/b3b7a0c423014acb9ad1e414b221a222/result',
    message: null,
    errorMessage: null
  }
];

export const READ_FIREWALL_STATUS_DATA_WITH_MESSAGE = [
  {
    repositoryName: 'maven-central',
    affectedComponentCount: 0,
    criticalComponentCount: 10,
    moderateComponentCount: 30,
    quarantinedComponentCount: 15,
    severeComponentCount: 20,
    reportUrl:
      'http://localhost:8070/ui/links/repository/b3b7a0c423014acb9ad1e414b221a111/result',
    message: 'foo',
    errorMessage: null
  },
  {
    repositoryName: 'nuget.org-proxy',
    affectedComponentCount: 0,
    criticalComponentCount: 11,
    moderateComponentCount: 33,
    quarantinedComponentCount: 17,
    severeComponentCount: 22,
    reportUrl:
      'http://localhost:8070/ui/links/repository/b3b7a0c423014acb9ad1e414b221a222/result',
    message: null,
    errorMessage: 'error'
  }
];
