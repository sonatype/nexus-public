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

export const sampleRepoSettings = {
  data: {
    result: {
      success: true,
      data: [
        { name: 'simple-repo', format: 'nuget', type: 'hosted' },
        { name: 'multi-repo', format: 'multi', type: 'hosted', status: { online: true } },
        { name: 'regex-map-repo', format: 'foo-format', type: 'hosted' },
        { name: 'maven-repo', format: 'maven2', type: 'hosted' },
        { name: 'proxy-repo', format: 'nuget', type: 'proxy' },
        { name: 'group-repo', format: 'nuget', type: 'group' },
        { name: 'offline-repo', format: 'nuget', type: 'hosted', status: { online: false } },
        { name: 'ui-upload-disabled-repo', format: 'uiUploadDisabled', type: 'hosted' },
        { name: 'snapshot-repo', format: 'maven2', type: 'hosted', versionPolicy: 'SNAPSHOT' }
      ]
    }
  }
};

const simpleUploadDefinition = {
  uiUpload: true,
  format: 'nuget',
  multipleUpload: false,
  componentFields: [{
    displayName: 'Field 1',
    group: 'A',
    helpText: null,
    name: 'field1',
    optional: true,
    type: 'STRING'
  }, {
    displayName: 'Field 2',
    group: 'B',
    helpText: 'This is the second field',
    name: 'field2',
    optional: false,
    type: 'STRING'
  }, {
    displayName: 'Field 3',
    group: 'A',
    helpText: null,
    name: 'field3',
    optional: true,
    type: 'STRING'
  }, {
    displayName: 'Field 4',
    group: 'C',
    helpText: 'FOUR',
    name: 'field4',
    optional: false,
    type: 'STRING'
  }, {
    displayName: 'Field 5',
    group: 'C',
    helpText: null,
    name: 'field5',
    optional: true,
    type: 'BOOLEAN'
  }],
  assetFields: [{
    displayName: 'Field 6',
    helpText: null,
    name: 'field6',
    optional: false,
    type: 'STRING'
  }, {
    displayName: 'Field 7',
    helpText: null,
    name: 'field7',
    optional: true,
    type: 'STRING'
  }]
};

const multiUploadDefinition = {
  ...simpleUploadDefinition,
  format: 'multi',
  multipleUpload: true
};

const uiUploadDisabledUploadDefinition = {
  ...simpleUploadDefinition,
  format: 'uiUploadDiabled',
  uiUpload: false
};

const regexUploadDefinition = {
  ...simpleUploadDefinition,
  format: 'foo-format',
  multipleUpload: true,
  assetFields: [{
    displayName: 'Field 6',
    helpText: null,
    name: 'field6',
    optional: false,
    type: 'STRING'
  }, {
    displayName: 'Field 7',
    helpText: null,
    name: 'field7',
    optional: false,
    type: 'STRING'
  }],
  regexMap: {
    regex: String.raw`([^-]+)\.(.*)`,
    fieldList: ['field6', 'field7']
  }
};

const mavenUploadDefinition = {
  uiUpload: true,
  multipleUpload: true,
  format: 'maven2',
  componentFields: [
    {
      displayName: 'Group ID',
      helpText: null,
      name: 'groupId',
      optional: false,
      type: 'STRING',
      group: 'Component coordinates'
    },
    {
      displayName: 'Artifact ID',
      helpText: null,
      name: 'artifactId',
      optional: false,
      type: 'STRING',
      group: 'Component coordinates'
    },
    {
      displayName: 'Version',
      helpText: null,
      name: 'version',
      optional: false,
      type: 'STRING',
      group: 'Component coordinates'
    },
    {
      displayName: 'Generate a POM file with these coordinates',
      helpText: null,
      name: 'generate-pom',
      optional: true,
      type: 'BOOLEAN',
      group: 'Component coordinates'
    },
    {
      displayName: 'Packaging',
      helpText: null,
      name: 'packaging',
      optional: true,
      type: 'STRING',
      group: 'Component coordinates'
    },
    {
      displayName: 'Tag',
      helpText: null,
      name: 'tag',
      optional: true,
      type: 'STRING',
      group: 'Component attributes'
    }
  ],
  assetFields: [
    {
      displayName: 'Classifier',
      helpText: null,
      name: 'classifier',
      optional: true,
      type: 'STRING',
      group: null
    },
    {
      displayName: 'Extension',
      helpText: null,
      name: 'extension',
      optional: false,
      type: 'STRING',
      group: null
    }
  ],
  regexMap: {
    regex: '-(?:(?:\\.?\\d)+)(?:-(?:SNAPSHOT|\\d+))?(?:-(\\w+))?\\.((?:\\.?\\w)+)$',
    fieldList: [
      'classifier',
      'extension'
    ]
  }
};

export const sampleUploadDefinitions = {
  data: {
    result: {
      success: true,
      data: [
        simpleUploadDefinition,
        multiUploadDefinition,
        regexUploadDefinition,
        mavenUploadDefinition,
        uiUploadDisabledUploadDefinition
      ]
    }
  }
};
