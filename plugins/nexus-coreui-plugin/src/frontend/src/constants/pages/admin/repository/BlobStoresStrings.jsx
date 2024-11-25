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
import {NxTextLink} from '@sonatype/react-shared-components';

export default {
  BLOB_STORES: {
    MENU: {
      text: 'Blob Stores',
      description: 'Configure local and cloud blob storage'
    },

    MESSAGES: {
      CONFIRM_SAVE: {
        TITLE: 'Update Blob Store',
        MESSAGE: 'Warning: The blob store will be temporarily unavailable for a short period.  This function does not migrate data to a new location. Previously created data will not be available',
        YES: 'Update',
        NO: 'Cancel'
      },
      CONFIRM_DELETE: {
        TITLE: 'Delete Blob Store',
        YES: 'Delete',
        NO: 'Cancel'
      },
      CANNOT_DELETE: (repositoryUsage, blobStoreUsage) =>
          `This blob store is in use by ${repositoryUsage} repositories and ${blobStoreUsage} blob stores`,
      CONFIRM_PROMOTE: {
        TITLE: 'Promote Blob Store',
        MESSAGE: 'Warning: The blob store will be promoted to a new group blob store containing the original blob store as a member. This operation cannot be undone.',
        YES: 'Promote',
        NO: 'Cancel'
      }
    },

    LIST: {
      CREATE_BUTTON: 'Create Blob Store',
      COLUMNS: {
        NAME: 'Name',
        PATH: 'Path',
        TYPE: 'Type',
        STATE: 'State',
        COUNT: 'Blob Count',
        SIZE: 'Total Size',
        SPACE: 'Available Space'
      },
      FILTER_PLACEHOLDER: 'Filter by name',
      EMPTY_LIST: 'There are no blob stores available',
      HELP: {
        TITLE: 'What is a blob store?',
        TEXT: <>
          The binary assets you download via proxy repositories, or publish to hosted repositories, are stored in
          the blob store attached to those repositories. In traditional, single node NXRM deployments, blob stores
          are typically associated with a local filesystem directory, usually within the sonatype-work directory.
          For more information, check{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/blob-store">
            the documentation
          </NxTextLink>.
        </>,
      },
      AVAILABLE: 'Started',
      UNAVAILABLE: 'Failed',
      UNKNOWN: 'Unavailable',
      UNLIMITED: 'Unlimited'
    },

    FORM: {
      CREATE_TITLE: 'Create Blob Store',
      EDIT_WARNING: '\
        Updating the blob store configuration will cause it to be temporarily unavailable for a short period of time. \
        Edits to configuration may also leave the blob store in a non-functional state.\
      ',
      EDIT_TILE: (name) => `Edit ${name}`,
      EDIT_DESCRIPTION: (type) => `${type} Blob Store`,
      CONVERT_TO_GROUP_BUTTON: 'Convert to Group',
      CONVERT_TO_GROUP_MODAL: {
        HEADER: 'Convert to Group Blob Store',
        LABEL: 'Rename Original Blob Store',
        SUBLABEL: 'Assign a new name to the original blob store',
        ALERT: 'You are converting to a group blob store. This action cannot be undone.',
        CONVERT_BUTTON: 'Convert'
      },
      TYPE: {
        label: 'Type',
        sublabel: 'Select the type of the blob store'
      },
      NAME: {
        label: 'Name'
      },
      SOFT_QUOTA: {
        ENABLED: {
          label: 'Soft Quota',
          sublabel: 'Raises an alert when the blob store exceeds a constraint',
          text: 'Enabled'
        },
        TYPE: {
          label: 'Constraint Type'
        },
        LIMIT: {
          label: 'Constraint Limit (in MB)'
        }
      }
    },

    AZURE: {
      ACCOUNT_NAME: {
        label: 'Account Name',
        sublabel: 'The name of the Azure storage account'
      },
      CONTAINER_NAME: {
        label: 'Container Name',
        sublabel: 'The name of a container to be used for storage; the container will be created if it does not already exist'
      },
      AUTHENTICATION: {
        label: 'Authentication',
        ENVIRONMENTVARIABLE: 'Use Environment Variables',
        MANAGED: 'Managed Identity (System)',
        ACCOUNT_KEY: {
          label: 'Account Key',
          sublabel: 'Account key found under Access keys for the storage account'
        }
      },
      TEST_CONNECTION: 'Test Connection',
      TEST_CONNECTION_ERROR: 'Connection failed, check the logs for more information',
      TEST_CONNECTION_SUCCESS: 'Connection succeeded',
      TESTING: 'Testing connection'
    },

    GOOGLE: {
      PROJECT_ID: {
        label: 'Project ID',
        sublabel: 'Your GCP Project ID is a unique identifier for your Google Cloud project. '
          + 'It typically consists of lowercase letters, digits, and/or hyphens.'
      },
      BUCKET: {
        label: 'Bucket',
        sublabel: 'Google Cloud Platform bucket name (must be between 3 and 63 characters long containing only '
          + 'lower-case characters, numbers, periods, dashes, and underscores. Spaces are not permitted).'
      },
      REGION: {
        label: 'Region',
        sublabel: 'Region must correspond to bucket\'s location.'
      },
      PREFIX: {
        label: 'Prefix',
        sublabel: 'Google Cloud Storage path prefix.'
      },
      AUTHENTICATION: {
        LABEL: 'Authentication',
        APPLICATION_DEFAULT_CREDENTIALS: 'Use Google Application Default Credentials',
        CREDENTIAL_JSON_FILE: 'Use a separate credential JSON file (select to upload)',
        JSON_PATH: {
          label: 'JSON Credential File Path',
          sublabel: 'Upload a .json file (maximum size: 4KB).'
        }
      },
      ENCRYPTION: {
        LABEL: 'Encryption',
        SUBLABEL: 'Encryption type',
        DEFAULT: 'Default Cloud-managed encryption',
        KMS_MANAGED: 'Enable KMS managed encryption',
        KEY_NAME: {
          label: 'KMS Key ID',
          sublabel: 'Enter the KMS Key ID to use for encryption'
        }
      },
      ERROR: {
        bucketRegionMismatchException: 'GoogleCloudBucketRegionMismatchException',
        bucketRegionMismatchMessage: 'Region and bucket location do not match.',
        bucketRegionMismatchTitle: 'Selected region does not match the bucket\'s location.',
        bucketEncryptionMismatchException: 'GoogleCloudEncryptionKeyMismatchException',
        bucketEncryptionMismatchMessage: 'Bucket and Encryption do not match.',
        bucketEncryptionMismatchTitle: 'Selected encryption does not match the bucket\'s encryption.'
      }
    }
  },

  S3_BLOBSTORE_CONFIGURATION: {
    S3BlobStore_Help: '<em>S3 blob stores require specific permissions to support full provisioning and functionality through Nexus Repository Manager. ' +
        'Consult our <a href="https://links.sonatype.com/products/nexus/blobstores/s3/docs" target="_blank">documentation</a>' +
        ' for the specific set of permissions required.</em>',
    S3BlobStore_RegionStatus_FieldLabel: 'Region Status',
    S3BlobStore_RegionStatus_InUseText: 'In use:',
    S3BlobStore_RegionStatus_PrimaryRegionText: 'Primary Region',
    S3BlobStore_RegionStatus_FailoverRegionText: 'Failover Region',
    S3BlobStore_Region_FieldLabel: 'Region',
    S3BlobStore_Region_HelpText: 'Select an AWS Region',
    S3BlobStore_Region_PrimaryRegionInactiveText: 'The primary region is currently not in use.',
    S3BlobStore_Region_FailoverRegionActiveText: 'The region for this AWS S3 replication bucket is currently in use.',
    S3BlobStore_Bucket_FieldLabel: 'Bucket',
    S3BlobStore_Bucket_HelpText: 'S3 Bucket Name (must be between 3 and 63 characters long containing only lower-case characters, numbers, periods, and dashes)',
    S3BlobStore_Prefix_FieldLabel: 'Prefix',
    S3BlobStore_Prefix_HelpText: 'S3 Path prefix',
    S3BlobStore_Expiration_FieldLabel: 'Expiration Days',
    S3BlobStore_Expiration_HelpText: 'How many days until deleted blobs are finally removed from the S3 bucket (-1 to disable)',
    S3BlobStore_Expiration_DaysText: 'days',
    S3BlobStore_Authentication_Title: 'Authentication (Optional)',
    S3BlobStore_Authentication_AccessKeyId: 'Access Key ID',
    S3BlobStore_Authentication_SecretAccessKey: 'Secret Access Key',
    S3BlobStore_Authentication_AssumeRoleArn: 'Assume Role ARN (Optional)',
    S3BlobStore_Authentication_SessionToken: 'Session Token ARN (Optional)',
    S3BlobStore_EncryptionSettings_Title: 'Encryption (Optional)',
    S3BlobStore_EncryptionSettings_Type_FieldLabel: 'Encryption Type',
    S3BlobStore_EncryptionSettings_Type_HelpText: 'The type of encryption for objects in the S3 Blob Store',
    S3BlobStore_EncryptionSettings_KeyID_FieldLabel: 'KMS Key ID (Optional)',
    S3BlobStore_EncryptionSettings_KeyID_HelpText: 'If using KMS encryption, you can supply a Key ID. If left blank, then the default will be used',
    S3BlobStore_AdvancedConnectionSettings_Title: 'Advanced Connection Settings (Optional)',
    S3BlobStore_AdvancedConnectionSettings_EndPointUrl_FieldLabel: 'Endpoint URL',
    S3BlobStore_AdvancedConnectionSettings_EndPointUrl_HelpText: 'A custom endpoint URL for third party object stores using the S3 API',
    S3Blobstore_AdvancedConnectionSettings_MaxConnectionPoolSize_FieldLabel: 'Max Connection Pool Size',
    S3Blobstore_AdvancedConnectionSettings_MaxConnectionPoolSize_HelpText: 'When set this value overrides the default connection pool size defined by Nexus, or the AWS Client',
    S3BlobStore_AdvancedConnectionSettings_SignatureVersion_FieldLabel: 'Signature Version',
    S3BlobStore_AdvancedConnectionSettings_SignatureVersion_HelpText: 'An API signature version which may be required for third party object stores using the S3 API',
    S3BlobStore_AdvancedConnectionSettings_PathStyleAccess_FieldLabel: 'Use path-style access',
    S3BlobStore_AdvancedConnectionSettings_PathStyleAccess_HelpText: 'Setting this flag will result in path-style access being used for all requests',
    S3BlobStore_ReplicationBucketsSettings_Title: 'AWS S3 Replication Buckets (Optional)',
    S3BlobStore_ReplicationBucketsSettings_Region_FieldLabel: 'Region',
    S3BlobStore_ReplicationBucketsSettings_Region_HelpText: 'Select an AWS Region',
    S3BlobStore_ReplicationBucketsSettings_BucketName_FieldLabel: 'Bucket Name',
    S3BlobStore_ReplicationBucketsSettings_BucketName_HelpText: 'S3 Bucket Name (must be between 3 and 63 characters long containing only lower-case characters, numbers, periods, and dashes)',
    S3BlobStore_ReplicationBucketsSettings_AddFailoverBucket: 'Add Replication Bucket',
    S3BlobStore_ReplicationBucketsSettings_DeleteFailoverBucket: 'Remove Bucket',
    S3BlobStore_ReplicationBucketsSettings_ConfigureBucketReplicationMessage: 'Bi-directional replication should be configured between the primary bucket and the replication bucket',
    S3BlobStore_ReplicationBucketsSettings_MaxFailoverBucketsWarning: 'You have reached the maximum number of failover buckets allowed (5). Remove a bucket to configure another one.',
  }
};
