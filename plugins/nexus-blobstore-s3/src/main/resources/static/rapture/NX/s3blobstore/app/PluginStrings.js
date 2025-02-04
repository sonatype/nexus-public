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
/*global Ext, NX*/

/**
 * @since 3.17
 */
Ext.define('NX.s3blobstore.app.PluginStrings', {
  '@aggregate_priority': 90,

  singleton: true,
  requires: [
    'NX.I18n'
  ],

  keys: {
    S3Blobstore_Help: '<em>S3 blob stores require specific permissions to support full provisioning and functionality through Nexus Repository Manager. ' +
        'Consult our <a href="https://links.sonatype.com/products/nexus/blobstores/s3/docs" target="_blank">documentation</a>' +
        ' for the specific set of permissions required.</em>',
    S3Blobstore_Region_FieldLabel: 'Region',
    S3Blobstore_Region_HelpText: 'The AWS Region to use',
    S3Blobstore_Bucket_FieldLabel: 'Bucket',
    S3Blobstore_Bucket_HelpText: 'S3 Bucket Name (must be between 3 and 63 characters long containing only lower-case characters, numbers, periods, and dashes)',
    S3Blobstore_Prefix_FieldLabel: 'Prefix',
    S3Blobstore_Prefix_HelpText: 'S3 Path prefix',
    S3Blobstore_Expiration_FieldLabel: 'Expiration Days',
    S3Blobstore_Expiration_HelpText: 'How many days until deleted blobs are finally removed from the S3 bucket (-1 to disable)',
    S3Blobstore_Authentication_Title: 'Authentication',
    S3Blobstore_Authentication_AccessKeyId: 'Access Key ID',
    S3Blobstore_Authentication_SecretAccessKey: 'Secret Access Key',
    S3Blobstore_Authentication_AssumeRoleArn: 'Assume Role ARN (Optional)',
    S3Blobstore_Authentication_SessionToken: 'Session Token (Optional)',
    S3Blobstore_EncryptionSettings_Title: 'Encryption',
    S3Blobstore_EncryptionSettings_Type_FieldLabel: 'Encryption Type',
    S3Blobstore_EncryptionSettings_Type_HelpText: 'The type of encryption for objects in the S3 Blob Store',
    S3Blobstore_EncryptionSettings_KeyID_FieldLabel: 'KMS Key ID (Optional)',
    S3Blobstore_EncryptionSettings_KeyID_HelpText: 'If using KMS encryption, you can supply a Key ID. If left blank, then the default will be used',
    S3Blobstore_AdvancedConnectionSettings_Title: 'Advanced Connection Settings',
    S3Blobstore_AdvancedConnectionSettings_EndPointUrl_FieldLabel: 'Endpoint URL',
    S3Blobstore_AdvancedConnectionSettings_EndPointUrl_HelpText: 'A custom endpoint URL for third party object stores using the S3 API',
    S3Blobstore_AdvancedConnectionSettings_MaxConnectionPoolSize_FieldLabel: 'Max Connection Pool Size',
    S3Blobstore_AdvancedConnectionSettings_MaxConnectionPoolSize_HelpText: 'When set this value overrides the default connection pool size defined by Nexus, or the AWS Client',
    S3Blobstore_AdvancedConnectionSettings_SignatureVersion_FieldLabel: 'Signature Version',
    S3Blobstore_AdvancedConnectionSettings_SignatureVersion_HelpText: 'An API signature version which may be required for third party object stores using the S3 API',
    S3Blobstore_AdvancedConnectionSettings_PathStyleAccess_FieldLabel: 'Use path-style access',
    S3Blobstore_AdvancedConnectionSettings_PathStyleAccess_HelpText: 'Setting this flag will result in path-style access being used for all requests'
  }

}, function(obj) {
  NX.I18n.register(obj);
});
