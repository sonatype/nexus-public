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

import {UIStrings} from '@sonatype/nexus-ui-plugin';

export default {
  ...UIStrings,

  FORMAT_PLACEHOLDER: 'Format',

  ANONYMOUS_SETTINGS: {
    MENU: {
      text: 'Anonymous Access',
      description: 'Configure anonymous access to server contents'
    },
    ENABLED_CHECKBOX_LABEL: 'Access',
    ENABLED_CHECKBOX_DESCRIPTION: 'Allow anonymous users to access the server',
    USERNAME_TEXTFIELD_LABEL: 'Username',
    REALM_SELECT_LABEL: 'Realm',
    MESSAGES: {
      LOAD_ERROR: 'An error occurred while loading Anonymous settings, see console for more details',
      SAVE_SUCCESS: 'Anonymous security settings updated',
      SAVE_ERROR: 'An error occurred while updating Anonymous settings, see console for more details'
    }
  },

  REALMS: {
    MENU: {
      text: 'Realms',
      description: 'Manage the active security realms and their order'
    },
    CONFIGURATION: {
      LABEL: 'Realms Configuration',
      SUB_LABEL: 'Active Realms',
      AVAILABLE_TITLE: 'Available',
      SELECTED_TITLE: 'Active',
      EMPTY_LIST: 'There are no configured realms for which you have view permissions.',
    },
    MESSAGES: {
      NO_REALMS_CONFIGURED: 'At least one realm must be selected and should include a user that is able to update the list of realms'
    }
  },

  PRIVILEGES: {
    MENU: {
      text: 'Privileges',
      description: 'Manage Privileges'
    },
    LIST: {
      CREATE_BUTTON: 'Create Privilege',
      EMPTY_LIST: 'There are no privileges available',
      COLUMNS: {
        NAME: 'Name',
        DESCRIPTION: 'Description',
        TYPE: 'Type',
        PERMISSION: 'Permission',
      },
      HELP: {
        TITLE: 'What is a Privilege?',
        TEXT: <>
          Privileges are assigned to roles and control the actions that each role can perform.
          See our{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/privileges">
            documentation
          </NxTextLink>
          {' '}for more information.
        </>,
      },
    },
    FORM: {
      CREATE_TITLE: 'Create Privilege',
      EDIT_TILE: (name) => `Edit ${name}`,
      EDIT_DESCRIPTION: 'Nexus Privilege',
      DEFAULT_PRIVILEGE_WARNING: 'This is a default privilege and cannot be modified.',
      SECTIONS: {
        SETUP: 'Privilege Setup',
      },
      TYPE: {
        LABEL: 'Type',
      },
      NAME: {
        LABEL: 'Name',
      },
      DESCRIPTION: {
        LABEL: 'Description',
      },
      FORMAT: {
        LABEL: 'Format',
        SUB_LABEL: 'The format(s) for the repository',
      },
      PRIVILEGE_STRING: {
        LABEL: 'Privilege String',
        SUB_LABEL: <>
          The internal segment matching algorithm uses Apache Shiro wildcard <br />permissions; see{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/privileges">
            our documentation
          </NxTextLink>
          {' '}for more details
        </>,
      },
    },
    MESSAGES: {
      CONFIRM_DELETE: {
        TITLE: 'Delete Privilege',
        MESSAGE: (name) => `Are you sure you want to delete the privilege named "${name}?"`,
        YES: 'Delete',
        NO: 'Cancel'
      },
      DELETE_SUCCESS: (name) => `Privilege deleted: ${name}`,
    },
  },

  ROLES: {
    MENU: {
      text: 'Roles',
      description: 'Manage roles'
    },
    LIST: {
      CREATE_BUTTON: 'Create Role',
      EMPTY_LIST: 'There are no roles available',
      COLUMNS: {
        ID: 'Id',
        NAME: 'Name',
        DESCRIPTION: 'Description',
      },
      HELP: {
        TITLE: 'What is a role?',
        TEXT: <>
          Roles bring together multiple privileges so that, when you assign a user to the role,
          that user will automatically have all of those privileges. Roles can comprise both
          other roles and individual privileges. See our{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/roles">
            help documentation
          </NxTextLink>
          {' '}for more information.
        </>,
      },
    },
    FORM: {
      CREATE_TITLE: 'Create Role',
      EDIT_TILE: (name) => `Edit ${name}`,
      EDIT_DESCRIPTION: 'Nexus Role',
      DEFAULT_ROLE_WARNING: 'This is a default role and cannot be modified.',
      SECTIONS: {
        TYPE: 'Role Type',
        SETUP: 'Role Setup',
        PRIVILEGES: 'Privileges',
        ROLES: 'Roles',
      },
      TYPE: {
        LABEL: 'Type',
        OPTIONS: {
          NEXUS: 'Nexus role',
          EXTERNAL: 'External Role Mapping',
        }
      },
      EXTERNAL_TYPE: {
        LABEL: 'External Role Type',
      },
      ID: {
        LABEL: 'Role ID',
      },
      MAPPED_ROLE: {
        LABEL: 'Mapped Role',
      },
      NAME: {
        LABEL: 'Role Name',
      },
      DESCRIPTION: {
        LABEL: 'Role Description',
      },
      PRIVILEGES: {
        LABEL: 'Privileges',
        AVAILABLE: 'Available',
        SELECTED: 'Given',
        EMPTY_LIST: 'This role does not have any privileges.',
      },
      ROLES: {
        LABEL: 'Roles',
        AVAILABLE: 'Available',
        SELECTED: 'Contained',
        EMPTY_LIST: 'This role does not contain any other roles.',
      },
    },
    MESSAGES: {
      CONFIRM_DELETE: {
        TITLE: 'Delete Role',
        MESSAGE: (name) => `Are you sure you want to delete the role named "${name}?"`,
        YES: 'Delete',
        NO: 'Cancel'
      },
      DELETE_SUCCESS: (name) => `Role deleted: ${name}`,
    }
  },

  USERS: {
    MENU: {
      text: 'Users',
      description: 'Manage users',
    },
    MODAL: {
      TEXT: 'You must confirm your current password before you are able to change or update the password',
      CHANGE_PASSWORD: 'Change Password',
      NEXT: 'Next',
      CONFIRM_PASSWORD: 'Confirm password',
      NEW_PASSWORD: 'New password',
      ADMIN_PASSWORD: 'Admin password',
      CONFIRMING_ADMIN_PASSWORD: 'Confirming Admin Password',
      ERROR: {
        MISSING_PASSWORD: 'Password was not supplied in the body of the request',
        PERMISSION: 'The user does not have permission to perform the operation.',
        NOT_FOUND: 'User not found in the system.'
      }
    },
    LIST: {
      CREATE_BUTTON: 'Create Local User',
      FILTER_PLACEHOLDER: 'Filter by user ID',
      EMPTY_LIST: 'There are no users available',
      COLUMNS: {
        USER_ID: 'User ID',
        REALM: 'Realm',
        FIRST_NAME: 'First Name',
        LAST_NAME: 'Last Name',
        EMAIL: 'Email',
        STATUS: 'Status',
      },
      HELP: {
        TITLE: 'What is a User?',
        TEXT: <>
          A user is an individual account attached to a single ID and email address.
          See our{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/users">
            documentation
          </NxTextLink>
          {' '}for more information.
        </>,
      },
    },
    FORM: {
      CREATE_TITLE: 'Create User',
      EDIT_TILE: (name) => `Edit ${name}`,
      EDIT_DESCRIPTION: 'Nexus User',
      DEFAULT_USER_WARNING: 'This is a default user and cannot be modified.',
      SECTIONS: {
        SETUP: 'User Setup',
        ROLES: 'Roles',
      },
      ID: {
        LABEL: 'ID',
        SUB_LABEL: 'This will be used as the username',
      },
      FIRST_NAME: {
        LABEL: 'First Name',
      },
      LAST_NAME: {
        LABEL: 'Last Name',
      },
      EMAIL: {
        LABEL: 'Email',
        SUB_LABEL: 'Used for notifications',
      },
      PASSWORD: {
        LABEL: 'Password',
      },
      CONFIRM_PASSWORD: {
        LABEL: 'Confirm password',
      },
      STATUS: {
        LABEL: 'Status',
        OPTIONS: {
          ACTIVE: 'Active',
          DISABLED: 'Disabled',
        }
      },
      ROLES: {
        AVAILABLE: 'Available',
        GRANTED: 'Granted',
        EMPTY_LIST: 'There are no assigned roles.',
      },
      EXTERNAL_ROLES: {
        LABEL: 'External Roles',
        EMPTY_LIST: 'No roles from the external system were mapped for this user.',
      },
    },
    MESSAGES: {
      CONFIRM_DELETE: {
        TITLE: 'Delete User',
        MESSAGE: (name) => `Are you sure you want to delete the user named "${name}?"`,
        YES: 'Delete',
        NO: 'Cancel'
      },
      DELETE_SUCCESS: (name) => `User deleted: ${name}`,
    },
    TOKEN: {
      LABEL: 'User Token Options',
      USER_TOKEN: 'User Token',
      ACTIVE_FEATURE: 'Resetting a user’s token will invalidate their current token and force a new token to be created the next time it is accessed by the user',
      REQUIRE_ENABLE_FEATURE: <>
        User tokens allow users to authenticate securely without typical user credentials such as those used by LDAP or Crowd. To use this feature, visit the <NxTextLink href="#admin/system/capabilities">
        capabilities page </NxTextLink> and create a “User Token” capability.
      </>,
      RESET_USER_TOKEN: 'Reset user token',
      TEXT: 'Resetting user tokens requires validation of your credentials.',
      AUTHENTICATE: 'Authenticate',
      SAVE_SUCCESS: (user) => `User token of ${user} has been reset`
    }
  },

  SSL_CERTIFICATES: {
    MENU: {
      text: 'SSL Certificates',
      description: 'Manage Trusted SSL Certificates for use with the Nexus truststore'
    },
    LIST: {
      CREATE_BUTTON: 'Add Certificate',
      EMPTY_LIST: 'There are no SSL Certificates available',
      COLUMNS: {
        NAME: 'Name',
        ISSUED_TO: 'Issued to',
        ISSUED_BY: 'Issued by',
        FINGERPRINT: 'Fingerprint',
      },
      HELP: {
        TITLE: 'What is SSL?',
        TEXT: <>
          Using Secure Socket Layer (SSL) communication with the repository manager is an important security feature
          and a recommended best practice. Secure communication can be inbound or outbound. Outbound client
          communication may include integration with: proxy repository, email servers, LDAPS servers. Inbound client
          communication includes: web browser HTTPS access, tool access to repository content, usage of REST APIs.
          For more information check{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/ssl-certificate">
            the documentation
          </NxTextLink>.
        </>,
      },
    },
    ADD_FORM: {
      CAPTION: 'Add SSL Certificate',
      LOAD_BUTTON: 'Load Certificate',
      PEM: {
        RADIO_DESCRIPTION: 'Paste PEM',
        LABEL: 'Paste Certificate as PEM',
        PLACEHOLDER: 'Entry'
      },
      SERVER: {
        LABEL: 'Please enter a hostname, hostname:port or a URL to fetch a SSL certificate from',
        RADIO_DESCRIPTION: 'Load from server'
      }
    },
    FORM: {
      DETAILS_TITLE: (name) => `Certificate ${name}`,
      DETAILS_DESCRIPTION: 'Summary',
      WARNING: 'This certificate was retrieved over an untrusted connection. Always verify the details before adding it.',
      SECTIONS: {
        SETUP: 'Load SSL Certificates',
        SUBJECT: 'Subject',
        ISSUER: 'Issuer',
        CERTIFICATE: 'Certificate',
      },
      COMMON_NAME: {
        LABEL: 'Common name',
      },
      ORGANIZATION: {
        LABEL: 'Organization',
      },
      UNIT: {
        LABEL: 'Unit',
      },
      ISSUED_ON: {
        LABEL: 'Issued on',
      },
      VALID_UNTIL: {
        LABEL: 'Valid until',
      },
      FINGERPRINT: {
        LABEL: 'Fingerprint',
      },
      BUTTONS: {
        ADD: 'Add Certificate to Truststore',
        DELETE: 'Delete Certificate',
        LOAD: 'Load Certificate',
      },
    },
    MESSAGES: {
      CONFIRM_DELETE: {
        TITLE: 'Delete SSL Certificate',
        MESSAGE: (name) => `Delete the SSL Certificate named ${name}?`,
        YES: 'Delete',
        NO: 'Cancel'
      },
      DELETE_SUCCESS: (name) => `SSL Certificate deleted: ${name}`,
    },
  },

  LDAP_SERVERS: {
    MENU: {
      text: 'LDAP',
      description: 'Lightweight directory access protocol setup',
    },
    LIST: {
      LABEL: 'LDAP Connections',
      EMPTY_LIST: 'There are no LDAP servers available',
      COLUMNS: {
        ORDER: 'Order',
        NAME: 'Name',
        URL: 'Url',
      },
      BUTTONS: {
        CREATE: 'Create connection',
        CHANGE_ORDER: 'Change Order',
        CLEAR_CACHE: 'Clear cache',
      },
      HELP: {
        TITLE: 'What is LDAP?',
        TEXT: <>
          You can configure your NXRM instance to use LDAP for authentication and user role mapping. The repository
          manager can cache authentication information and supports multiple LDAP servers and user/group mappings
          to take advantage of the central authentication set up across your organization in all your repository managers.
          For more information check{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/ldap">
            the documentation
          </NxTextLink>.
        </>,
      },
      MODAL: {
        LABEL: 'Change LDAP servers ordering',
        SUB_LABEL: 'Reorder servers',
        FOOTER: (value) => `${value} Items`,
      },
      MESSAGES: {
        LIST_CHANGED: 'LDAP server order changed',
        CACHE_CLEARED: 'LDAP cache has been cleared'
      }
    },
  },

  EMAIL_SERVER: {
    MENU: {
      text: 'Email Server',
      description: 'Manage email server configuration'
    },
    FORM: {
      SECTIONS: {
        SETUP: 'Email Server Configuration',
      },
      ENABLED: {
        LABEL: 'Enable Email Server',
        SUB_LABEL: 'Enabled',
      },
      HOST: {
        LABEL: 'Host',
      },
      PORT: {
        LABEL: 'Port',
      },
      USERNAME: {
        LABEL: 'Username',
      },
      PASSWORD: {
        LABEL: 'Password',
      },
      FROM_ADDRESS: {
        LABEL: 'From Address',
      },
      SUBJECT_PREFIX: {
        LABEL: 'Subject Prefix',
      },
      SSL_TLS_OPTIONS: {
        LABEL: 'SSL/TLS Options',
        OPTIONS: {
          ENABLE_STARTTLS: 'Enable STARTTLS support for insecure connections',
          REQUIRE_STARTTLS: 'Require STARTTLS support',
          ENABLE_SSL_TLS: 'Enable SSL/TLS encryption upon connection',
          IDENTITY_CHECK: 'Enable server identity check',
        },
      },
    },
    VERIFY: {
      LABEL: 'Verify Email Server',
      SUB_LABEL: 'Where do you want to send the test email?',
      TEST: 'Test',
      SUCCESS: 'Email server verification email sent successfully',
      ERROR: 'Email server verification email failed'
    },
    READ_ONLY: {
      ENABLE: {
        ENABLE_STARTTLS: 'STARTTLS support enabled for insecure connections',
        REQUIRE_STARTTLS: 'STARTTLS support required',
        ENABLE_SSL_TLS: 'SSL/TLS encryption enabled upon connection',
        IDENTITY_CHECK: 'Server identity check enabled',
      },
      NOT_ENABLE: {
        ENABLE_STARTTLS: 'STARTTLS support not enabled for insecure connections',
        REQUIRE_STARTTLS: 'STARTTLS support not required',
        ENABLE_SSL_TLS: 'SSL/TLS encryption not enabled upon connection',
        IDENTITY_CHECK: 'Server identity check not enabled',
      }
    }
  },

  LICENSING: {
    MENU: {
      text: 'Licensing',
      description: 'A valid license is required for PRO features; manage it here'
    },
    SECTIONS: {
      DETAILS: 'Licensing',
      INSTALL: 'Install License',
    },
    DETAILS: {
      COMPANY: {
        LABEL: 'Company',
      },
      NAME: {
        LABEL: 'Name',
      },
      EMAIL: {
        LABEL: 'Email',
      },
      EFFECTIVE_DATE: {
        LABEL: 'Effective Date',
      },
      LICENSE_TYPES: {
        LABEL: 'License Type(s)',
      },
      EXPIRATION_DATE: {
        LABEL: 'Expiration Date',
      },
      NUMBER_OF_USERS: {
        LABEL: 'Number of Licensed Users',
      },
      FINGERPRINT: {
        LABEL: 'Fingerprint',
      },
    },
    INSTALL: {
      LABEL: 'License',
      DESCRIPTION: 'Installing a new license requires restarting the server to take effect',
      MESSAGES: {
        ERROR: (error) => `Unable to update license for the reason identified below. Verify that you selected the correct file. If the problem persists, contact our support team. Reason: ${error}`,
        SUCCESS: 'License installed. Restart is only required if you are enabling new PRO features.',
      },
      BUTTONS: {
        UPLOAD: 'Upload License',
      }
    },
    AGREEMENT: {
      CAPTION: 'Nexus Repository Manager License Agreement',
      BUTTONS: {
        DECLINE: 'I Decline',
        ACCEPT: 'I Accept',
        DOWNLOAD: 'Download a copy of the agreement'
      }
    },
  },

  BLOB_STORES: {
    MENU: {
      text: 'Blob Stores',
      description: 'Configure local and cloud blob storage'
    },

    MESSAGES: {
      CONFIRM_SAVE: {
        TITLE: 'Update Blob Store',
        MESSAGE: 'Warning: The blob store will be temporarily unavailable for a short period.  This function does not migrate data to a new location.',
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
        Updating the blob store configuration will cause it to be temporarily unavailble for a short period of time. \
        Edits to configuration may also leave the blob store in a non-functional state. \
        Use caution when changing values.\
      ',
      EDIT_TILE: (name) => `Edit ${name}`,
      EDIT_DESCRIPTION: (type) => `${type} Blob Store`,
      CONVERT_TO_GROUP_BUTTON: 'Convert to Group',
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
    }
  },

  S3_BLOBSTORE_CONFIGURATION: {
    S3BlobStore_Help: '<em>S3 blob stores require specific permissions to support full provisioning and functionality through Nexus Repository Manager. ' +
        'Consult our <a href="https://links.sonatype.com/products/nexus/blobstores/s3/docs" target="_blank">documentation</a>' +
        ' for the specific set of permissions required.</em>',
    S3BlobStore_Region_FieldLabel: 'Region',
    S3BlobStore_Region_HelpText: 'Select an AWS Region',
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
    S3BlobStore_AdvancedConnectionSettings_PathStyleAccess_HelpText: 'Setting this flag will result in path-style access being used for all requests'
  },

  CONTENT_SELECTORS: {
    MENU: {
      text: 'Content Selectors'
    },

    EMPTY_MESSAGE: 'No content selectors were found',

    HELP_TITLE: 'What is a content selector?',
    HELP_TEXT: <>
      Content selectors provide a means for you to select specific content from your repositories.
      Repository content is evaluated against expressions written in CSEL (Content Selector Expression Language).
      For more information,{' '}
      <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/content-selector">
        check the documentation
      </NxTextLink>.
    </>,
    CREATE_BUTTON: 'Create Selector',
    FILTER_PLACEHOLDER: 'Filter',

    NAME_LABEL: 'Name',
    TYPE_LABEL: 'Type',
    DESCRIPTION_LABEL: 'Description',
    EXPRESSION_LABEL: 'Search Expression',
    EXPRESSION_DESCRIPTION: 'Use the following query to identify repository content',
    EXPRESSION_EXAMPLES: 'Example Content Selector Expressions',
    RAW_EXPRESSION_EXAMPLE_LABEL: 'Select "raw" format content',
    MULTI_EXPRESSIONS_EXAMPLE_LABEL: 'Select "maven2" content along a path that starts with "/org"',

    PREVIEW: {
      TITLE: 'Preview Content Selector Results',
      REPOSITORY_LABEL: 'Preview Repository',
      REPOSITORY_DESCRIPTION: 'Select a repository to evaluate the content selector and see the content that would be available',
      BUTTON: 'Preview',
      RESULTS: 'Preview Results',
      NAME_COLUMN: 'Name',
      EMPTY: 'No content in repositories matched the expression'
    },

    MESSAGES: {
      DUPLICATE_ERROR: (name) => `Another content selector named ${name} already exists`,
      NAME_TOO_LONG: 'The name of this content selector is too long, please use a shorter name',
      SAVE_ERROR: 'An error occurred while saving the content selector',
      DELETE_ERROR: (name) => `Content selector ${name} is in use and cannot be deleted`,

      CONFIRM_DELETE: {
        TITLE: 'Delete Content Selector',
        MESSAGE: (name) => `Delete the content selector named ${name}?`,
        YES: 'Delete',
        NO: 'Cancel'
      }
    }
  },

  LOGGING: {
    MENU: {
      text: 'Logging',
      description: 'Control logging levels'
    },

    CREATE_BUTTON: 'Create Logger',
    RESET_ALL_BUTTON: 'Reset to Default Levels',
    RESET_BUTTON: 'Reset to Default Level',

    FILTER_PLACEHOLDER: 'Filter by Logger Name',

    NAME_LABEL: 'Logger Name',
    LEVEL_LABEL: 'Logger Level',

    MESSAGES: {
      SAVE_ERROR: 'An error occurred while saving the logger',
      RESETTING: 'Resetting to Default Level...',
      RESET_ERROR: 'An error occurred while resetting all loggers, see console for more details'
    },

    CONFIRM_UPDATE: {
      TITLE: 'Confirm Update?',
      MESSAGE: ({name, level}) => `Logger ${name} is already configured. Would you like to update its level to "${level}"?`,
      CONFIRM_BUTTON: 'Update Logger Level'
    },

    CONFIRM_RESET_ALL: {
      TITLE: 'Confirm Reset?',
      MESSAGE: 'Reset all loggers to their default levels?',
      CONFIRM_BUTTON: 'Reset All Loggers'
    },

    CONFIRM_RESET: {
      TITLE: 'Confirm Reset?',
      MESSAGE: 'Reset this logger to its default level? If this is a custom logger it will be removed.',
      CONFIRM_BUTTON: 'Reset Logger'
    }
  },

  LOGS: {
    MENU: {
      text: 'Logs',
      description: 'View the current log contents'
    },

    LIST: {
      FILTER_PLACEHOLDER: 'Filter by File Name',
      LOGGER_NAME_LABEL: 'Logger Name',
      FILE_NAME_LABEL: 'File Name',
      SIZE_LABEL: 'Size',
      LAST_MODIFIED_LABEL: 'Last Modified'
    },

    VIEW: {
      TITLE: (log) => `Viewing ${log}`,
      DOWNLOAD: 'Download',
      MARK: {
        LABEL: 'Marker to insert into log',
        INSERT: 'Insert'
      },
      REFRESH: {
        RATE_LABEL: 'Refresh Rate',
        SIZE_LABEL: 'Size'
      }
    },

    REFRESH: {
      MANUAL_ITEM: 'Manual',
      TWENTY_SECONDS_ITEM: 'Every 20 seconds',
      MINUTE_ITEM: 'Every minute',
      TWO_MINUTES_ITEM: 'Every 2 minutes',
      FIVE_MINUTES_ITEM: 'Every 5 minutes'
    },
    SIZE: {
      LAST25KB_ITEM: 'Last 25KB',
      LAST50KB_ITEM: 'Last 50KB',
      LAST100KB_ITEM: 'Last 100KB'
    }
  },

  REPOSITORIES: {
    MENU: {
      text: 'Repositories',
      description: 'Configure repositories'
    },

    LIST: {
      CREATE_BUTTON: 'Create repository',
      COLUMNS: {
        NAME: 'Name',
        TYPE: 'Type',
        FORMAT: 'Format',
        STATUS: 'Status',
        URL: 'URL',
        HEALTH_CHECK: 'Health Check',
        IQ: 'IQ Policy Violations',
      },
      FILTER_PLACEHOLDER: 'Filter by name',
      EMPTY_LIST: 'There are no repositories available',
      COPY_URL_TITLE: 'Copy URL to Clipboard',
      HELP: {
        TITLE: 'What is a repository?',
        TEXT: <>
          A repository is a storage location where components, such as packages, libraries, binaries, and containers,
          are retrieved so they can be installed or used. Creating and managing repositories is an essential part of
          your Nexus Repository Manager configuration since it allows you to expose content to your end users as well
          as provide a location for them to store more content. For more information, check{' '}
          <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/repository">
            the documentation
          </NxTextLink>.
        </>,
      },
      HEALTH_CHECK: {
        LOADING: 'Loading...',
        ANALYZING: 'Analyzing...',
        ANALYZE_BUTTON: 'Analyze',
        LOADING_ERROR: 'Loading Error',
        ANALYZE_THIS: (name) => `Analyze '${name}' repository`,
        ANALYZE_ALL: 'Analyze all repositories',
        MODAL_CONTENT: (name) => `Do you want to analyze the repository ${name} and others for secuirty vulnerabilities and licence issues?`,
        MODAL_HEADER: 'Analyze Repository',
        NOT_AVAILABLE_TOOLTIP_HC: 'Repository Health Check Unavailable',
        NOT_AVAILABLE_TOOLTIP_FS: 'Repository Firewall Status Unavailable',
        QUARANTINED_TOOLTIP: 'Quarantined',
        SUMMARY: {
          CAPTION: 'Repository Health Check',
          HELP_BUTTON: 'What should I do with this?',
          DETAILS_BUTTON: 'View Detailed Report',
          NO_PERMISSION: 'This user account does not have permission to view the summary report'
        }
      }
    },

    EDITOR: {
      ENABLED_CHECKBOX_DESCR: 'Enabled',
      NONE_OPTION: 'None',
      CREATE_TITLE: {
        text: 'Create Repository',
        description: 'Select a repository format; then, choose a type (group, hosted, or proxy)'
      },
      EDIT_TITLE: {
        text: 'Edit Repository',
        description: 'Edit your existing repository setup'
      },
      FORMAT_AND_TYPE_CAPTION: 'Repository Format & Type',
      FORMAT_LABEL: 'Format',
      TYPE_LABEL: 'Type',
      URL_LABEL: 'URL',
      CONFIGURATION_CAPTION: 'Configuration',
      NAME_LABEL: 'Name',
      STATUS_LABEL: 'Status',
      STATUS_DESCR: 'Online - Ready to connect',
      STORAGE_CAPTION: 'Storage',
      BLOB_STORE_LABEL: 'Blob Store',
      CONTENT_VALIDATION_LABEL: 'Strict Content Type Validation',
      GROUP_CAPTION: 'Group',
      MEMBERS_LABEL: 'Member Repositories',
      SELECT_FORMAT_OPTION: 'Select a format...',
      SELECT_TYPE_OPTION: 'Select a type...',
      SELECT_STORE_OPTION: 'Select a blob store...',
      CREATE_BUTTON: 'Create Repository',
      SAVE_BUTTON: 'Save',
      CLEANUP_CAPTION: 'Cleanup',
      CLEANUP_POLICIES_LABEL: 'Cleanup Policies',
      CLEANUP_POLICIES_SUBLABEL: 'Nexus Repository will delete components that match any of the applied policies',
      HOSTED_CAPTION: 'Hosted',
      DEPLOYMENT_POLICY_LABEL: 'Deployment Policy',
      DEPLOYMENT_POLICY_SUBLABEL: 'Controls whether or not to allow updates and deployments to artifacts',
      PROPRIETARY_COMPONENTS_LABEL: 'Proprietary Components',
      PROPRIETARY_COMPONENTS_DESCR: 'Components in this repository count as proprietary for namespace conflict attacks (requires Sonatype Nexus Firewall)',
      DEPLOYMENT_POLICY_OPTIONS: {
        ALLOW: 'Allow redeploy',
        ALLOW_ONCE: 'Disable redeploy',
        DENY: 'Read-only',
        REPLICATION_ONLY: 'Deploy by Replication Only'
      },
      REDEPLOY_LATEST: {
        LABEL: 'Allow redeploy only on "latest" tag',
        DESCRIPTION: 'Allow redeploy on "latest" tag; otherwise, defer to deployment policy',
        TOOLTIP: 'Only applicable when Deployment Policy is set to "Disable redeploy"'
      },
      PROXY_CAPTION: 'Proxy Settings',
      REMOTE_STORAGE_LABEL: 'Remote Storage',
      REMOTE_STORAGE_SUBLABEL: 'Location of the remote repository to proxy',
      PREEMPTIVE_PULL_LABEL: 'Pre-emptive Pull',
      PREEMPTIVE_PULL_SUBLABEL: 'If enabled, the remote storage will be monitored for changes, and new components will be replicated automatically, and cached locally',
      ASSET_NAME_LABEL: 'Asset Name Matcher',
      ASSET_NAME_DESCRIPTION: <>
        This field allows you to use a RegEx to match search for specific components to help define scope.
        For more information check out our{' '}
        <NxTextLink external href="https://links.sonatype.com/products/nxrm3/docs/pull-replication/asset-name-matcher">
          documentation for format specific options
        </NxTextLink>.
      </>,
      URL_PLACEHOLDER: 'Enter a URL',
      BLOCKING_LABEL: 'Blocking',
      BLOCK_DESCR: 'Block outbound connections to the repository',
      AUTO_BLOCK_DESCR: 'Auto-block outbound connections to the repository if remote peer is detected as unreachable/unresponsive',
      MAX_COMP_AGE_LABEL: 'Maximum Component Age',
      MAX_COMP_AGE_SUBLABEL: 'How long (in minutes) to cache artifacts before re-checking the remote repository. Release repositories should use -1',
      MAX_META_AGE_LABEL: 'Maximum Metadata Age',
      MAX_META_AGE_SUBLABEL: 'How long (in minutes) to cache metadata before rechecking the remote repository',
      OPTIONS_CAPTION: 'Options',
      ROUTING_RULE_LABEL: 'Routing Rule',
      NEGATIVE_CACHE_LABEL: 'Negative Cache',
      NEGATIVE_CACHE_DESCR: 'Enabled',
      NEGATIVE_CACHE_TTL_LABEL: 'Negative Cache TTL (Minutes)',
      NEGATIVE_CACHE_TTL_SUBLABEL: 'How long to cache that a file was not able to be found in the repository',
      HTTP_AUTH_CAPTION: 'HTTP Authentication',
      AUTH_TYPE_LABEL: 'Authentication type',
      USERNAME_LABEL: 'Username',
      PASSWORD_LABEL: 'Password',
      USERNAME_OPTION: 'Username',
      NTLM_OPTION: 'Windows NTLM',
      NTLM_HOST_LABEL: 'Windows NTLM hostname',
      NTLM_DOMAIN_LABEL: 'Windows NTLM domain',
      REQUEST_SETTINGS_CAPTION: 'HTTP Request Settings',
      USER_AGENT_LABEL: 'User-Agent Customization',
      USER_AGEN_SUBLABEL: 'Define a custom fragment to append to "User-Agent" header in HTTP requests',
      RETRIES_LABEL: 'Connection Retries',
      RETRIES_SUBLABEL: 'Number of times to retry if the first connection attempt times out',
      TIMEOUT_LABEL: 'Connection Timeout (Seconds)',
      TIMEOUT_SUBLABEL: 'Time (in seconds) to wait before stopping and retrying the connection. Leave blank to use the globally defined HTTP timeout',
      REDIRECTS_LABEL: 'Circular Redirects',
      COOKIES_LABEL: 'Cookies',
      REPLICATION_LABEL: 'Replication',
      REPLICATION_SUBLABEL: 'If checked, this repository is the target of a replication',
      REWRITE_URLS_LABEL: 'Enable Rewrite of Package URLs',
      REWRITE_URLS_DESCR: 'Force Bower to retrieve packages through the proxy repository',
      REPODATA_DEPTH_LABEL: 'Repodata Depth',
      REPODATA_DEPTH_SUBLABEL: 'Specifies the repository depth where the repodata folder are created',
      LAYOUT_POLICY_LABEL: 'Layout Policy',
      DEPLOY_POLICY_SUBLABEL: 'Validate that all paths are RPMs or yum metadata',
      LAYOUT_POLICY_SUBLABEL: 'Validate that all paths are maven artifact or metadata paths',
      CONTENT_DISPOSITION_LABEL: 'Content Disposition',
      CONTENT_DISPOSITION_SUBLABEL: 'Add Content-Disposition header as "Attachment" to disable some content from being inline in a browser',
      VERSION_POLICY_LABEL: 'Version Policy',
      VERSION_POLICY_SUBLABEL: 'What type of artifacts does this repository store?',
      APT: {
        CAPTION: 'APT Settings',
        DISTRIBUTION: {
          LABEL: 'Distribution',
          SUBLABEL: 'Distribution to fetch (e.g., bionic)'
        },
        FLAT: {
          LABEL: 'Flat',
          DESCR: 'Is this repository flat?'
        },
        SIGNING: {
          KEY: {
            LABEL: 'Signing key',
            SUBLABEL: 'PGP signing key pair (armored private key e.g., gpg --export-secret-key --armor)',
            PLACEHOLDER: 'Entry'
          },
          PASSPHRASE: {
            LABEL: 'Passphrase'
          }
        }
      },
      NPM: {
        REMOVE_NON_CATALOGED: {
          LABEL: 'Remove Non-catalogued Versions',
          SUBLABEL: 'IQ Audit and Quarantine capability must be enabled for this feature to take effect.',
          DESCR: 'Remove non-catalogued versions from the package metadata'
        },
        REMOVE_QUARANTINED: {
          LABEL: 'Remove Quarantined Versions',
          SUBLABEL: 'IQ Audit and Quarantine capability must be enabled for this feature to take effect.',
          DESCR: 'Remove quarantined versions from the package metadata',
          WARNING: 'This feature requires IQ Server Release 134 or higher'
        },
      },
      LEARN_MORE: 'Learn more',
      REGISTRY_API_SUPPORT_CAPTION: 'Docker Registry API Support',
      REGISTRY_API_SUPPORT_LABEL: 'Enable Docker V1 API',
      REGISTRY_API_SUPPORT_DESCR: 'Allow clients to use the V1 API to interact with this repository',
      DOCKER: {
        INDEX: {
          LABEL: 'Docker Index',
          OPTIONS: {
            REGISTRY: 'Use Proxy registry (specified above)',
            HUB: 'Use Docker Hub',
            CUSTOM: 'Custom index'
          },
          URL: {
            LABEL: 'Location of the Docker Index',
            PLACEHOLDER: 'Enter a URL'
          }
        },
        CONNECTORS: {
          CAPTION: 'Repository Connectors',
          HTTP: {
            LABEL: 'HTTP',
            SUBLABEL: 'Create an HTTP connector at specified port. Normally used if the server is behind a secure proxy',
            PLACEHOLDER: 'Enter a port number'
          },
          HTTPS: {
            LABEL: 'HTTPS',
            SUBLABEL: 'Create an HTTP connector at specified port. Normally used if the server is configured for https',
            PLACEHOLDER: 'Enter a port number'
          },
          SUBDOMAIN: {
            LABEL: 'Allow Subdomain Routing',
            SUBLABEL: 'Use the following subdomain to make push and pull requests for this repository',
            PLACEHOLDER: 'Enter a subdomain',
            VALIDATION_ERROR: 'Subdomain field must be a minimum of 1 and maximum of 63 characters (letters, numbers, and dashes) and must start with a letter and end with a letter or digit'
          },
          ALLOW_ANON_DOCKER_PULL: {
            LABEL: 'Allow anonymous docker pull',
            DESCR: 'Allow anonymous docker pull (Docker Bearer Token Realm required)'
          },
          HELP: <>
            Connectors allow Docker clients to connect directly to hosted registries, but are not always
            required.
            <br />
            Consult our{' '}
            <NxTextLink
              href="https://links.sonatype.com/products/nexus/docker-ssl-connector/docs"
              external
            >
              documentation
            </NxTextLink>
            {' '}for which connector is appropriate for your use case.
            <br />
            For information on scaling see our{' '}
            <NxTextLink
              href="https://links.sonatype.com/products/nexus/docker-scaling-repositories/docs"
              external
            >
              scaling documentation
            </NxTextLink>
            .
          </>,
        }
      },
      FOREIGN_LAYER: {
        CACHING: 'Foreign Layer Caching',
        URL: 'Foreign Layer Allowed URLs',
        URL_SUBLABEL: 'Regular expressions used to identify URLs that are allowed for foreign layer requests',
        CHECKBOX: 'Allow Nexus Repository Manager to download and cache foreign layers',
        ADD: 'Add URL pattern',
        REMOVE: 'Remove',
      },
      REMOTE_URL_EXAMPLES: {
        bower: ' (e.g., https://registry.bower.io)',
        docker: ' (e.g., https://registry-1.docker.io)',
        maven2: ' (e.g., https://repo1.maven.org/maven2/)',
        npm: ' (e.g., https://registry.npmjs.org)',
        nuget: ' (e.g., https://api.nuget.org/v3/index.json)',
        pypi: ' (e.g., https://pypi.org)',
        rubygems: ' (e.g., https://rubygems.org)',
        yum: ' (e.g., http://mirror.centos.org/centos/)',
        default: ' (e.g., https://example.com)'
      },
      NUGET: {
        PROTOCOL_VERSION: {
          LABEL: 'Protocol Version',
          V2_RADIO_DESCR: 'NuGet V2',
          V3_RADIO_DESCR: 'NuGet V3'
        },
        METADATA_QUERY_CACHE_AGE: {
          LABEL: 'Metadata Query Cache Age',
          SUBLABEL: 'How long to cache query results from the proxied repository (in seconds)'
        },
        GROUP_VERSION: {
          LABEL: 'NuGet Type',
          SUBLABEL: 'Restrict proxy repositories to one NuGet version',
        }
      },
      WRITABLE: {
        LABEL: 'Writable Repository',
        SUBLABEL: <>The member repository to which POST and PUT requests will be routed. When pushing to a group repository, Nexus Repository checks existing layers of all members to avoid pushing those layers. See our {' '}
          <NxTextLink
            href="https://help.sonatype.com/repomanager3/nexus-repository-administration/formats/docker-registry/pushing-images-to-a-group-repository"
            external
          >
            documentation
          </NxTextLink>
          {' '} for details.'</>,
        PLACEHOLDER: 'Select repository...',
        VALIDATION_ERROR: (name) => `Writable repository ${name} is not a group member`
      },
      MESSAGES: {
        SAVE_ERROR: 'An error occurred while saving the repository',
        DELETE_ERROR: (name) => `Repository ${name} cannot be deleted\n`,
        DELETE_SUCCESS: (name) => `Repository deleted: ${name}`,
        CONFIRM_DELETE: {
          TITLE: 'Delete repository',
          MESSAGE: (name) => name,
          YES: 'Delete',
          NO: 'Cancel'
        }
      },
      PRE_EMPTIVE_AUTH: {
        LABEL: 'Use pre-emptive authentication',
        DESCR: <>
          <strong>Caution! </strong>
          Use this only when absolutely necessary.
          Enabling this option means configured authentication
          credentials will be sent to the remote URL regardless
          of whether the remote server has asked for them or not.
        </>,
        TOOLTIP: 'Proxy\'s URL must be HTTPS to enable this feature'
      }
    }
  },

  ROUTING_RULES: {
    MENU: {
      text: 'Routing Rules',
      description: 'Restrict which requests are handled by repositories'
    },

    LIST: {
      PREVIEW_BUTTON: 'Global Routing Preview',
      CREATE_BUTTON: 'Create Routing Rule',
      FILTER_PLACEHOLDER: 'Filter by Name or Description',
      NAME_LABEL: 'Name',
      DESCRIPTION_LABEL: 'Description',
      USED_BY_LABEL: 'Used By',
      NEEDS_ASSIGNMENT: '0 repositories, assign it to a repository',
      USED_BY: (count) => count === 1 ? '1 repository' : `${count} repositories`,
      EMPTY_LIST: 'There are no routing rules created yet',
      HELP_TITLE: 'What is a routing rule?',
      HELP_TEXT: <>
        Routes are like filters you can apply to groups in terms of security access and general component retrieval.
        They can be used to reduce the number of repositories within a group accessed in order to retrieve a component.
        For more information,{' '}
        <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/routing-rule">
          check the documentation
        </NxTextLink>.
      </>,
    },

    PREVIEW: {
      TITLE: 'Global Routing Preview',
      REPOSITORIES_LABEL: 'Repositories',
      REPOSITORIES_DESCRIPTION: 'Choose a set of repositories to test against',
      REPOSITORIES: {
        ALL: 'All Repositories',
        GROUPS: 'All Group Repositories',
        PROXIES: 'All Proxy Repositories'
      },
      PATH_LABEL: 'Path',
      PATH_DESCRIPTION: 'Enter a request path to check if it would be blocked or allowed. Requests always start with a leading slash.',
      COLUMNS: {
        REPOSITORY: 'Repository',
        TYPE: 'Type',
        FORMAT: 'Format',
        RULE: 'Routing Rule',
        STATUS: 'Status'
      },
      NO_RULE: 'None',
      EMPTY_PREVIEW: 'No results found or preview was not yet submitted',
      DETAILS_TITLE: (ruleName) => `Routing Rule Details for ${ruleName}`,
      TYPE_FILTER: {
        any: 'any',
        group: 'group',
        proxy: 'proxy'
      }
    },

    FORM: {
      CREATE_TITLE: 'Create Routing Rule',
      EDIT_TITLE: 'Edit Routing Rule',
      UNUSED: `To use this rule, <a href="#admin/repository/repositories">assign it to a repository</a>`,
      USED_BY: (repositoryNames) => {
        const repositoryLinks = repositoryNames.map(name =>
            `<a href="#admin/repository/repositories:${window.encodeURIComponent(name)}">${name}</a>`);
        const repository = repositoryNames.length === 1 ? 'repository' : 'repositories';
        return `This rule is in use by ${repositoryNames.length} ${repository} (${repositoryLinks.join(', ')})`;
      },
      SAVE_ERROR: 'An error occured while saving the routing rule',
      NAME_LABEL: 'Name',
      DESCRIPTION_LABEL: 'Description',
      MODE_LABEL: 'Mode',
      MODE: {
        ALLOW: 'Allow',
        BLOCK: 'Block'
      },
      PREVIEW: {
        ALLOWED: 'This request would be allowed',
        BLOCKED: 'This request would be blocked'
      },
      MODE_DESCRIPTION: 'Allow or block requests when their path matches any of the following matchers',
      MATCHERS_LABEL: 'Matchers',
      MATCHER_LABEL: (index) => `Matcher ${index}`,
      MATCHERS_DESCRIPTION: 'Enter regular expressions that will be used to identify request paths to allow or block (depending on above mode)',
      NAME_IS_NONE_ERROR: 'Rule must not be named None',
      DELETE_MATCHER_BUTTON: 'Delete this matcher',
      ADD_MATCHER_BUTTON: 'Add Another Matcher',
      CREATE_BUTTON: 'Create Routing Rule',
      CANNOT_DELETE: (repositoryNames) => `\
This rule is in use by ${repositoryNames.length} ${repositoryNames.length === 1 ? 'repository' : 'repositories'} \
(${repositoryNames.join(', ')})`
    },

    MESSAGES: {
      CONFIRM_DELETE: {
        TITLE: 'Delete Routing Rule',
        MESSAGE: (name) => `Delete the routing rule named ${name}`,
        YES: 'Delete',
        NO: 'Cancel'
      },
      DELETE_ERROR: (name) => `Unable to delete routing rule named ${name}`
    },

    ALLOWED: 'Allowed',
    BLOCKED: 'Blocked',

    PATH_LABEL: 'Path',
    PATH_DESCRIPTION: 'Enter a request path to check if it would be blocked or allowed. Requests always start with a leading slash.',
    TEST_BUTTON: 'Test'
  },

  LOG4J_VISUALIZER: {
    MENU: {
      text: 'Log4j Visualizer',
      description: 'Overview of vulnerability risk from open-source supply chain'
    }
  },

  SYSTEM_INFORMATION: {
    MENU: {
      text: 'System Information'
    },
    ACTIONS: {
      download: 'Download as JSON'
    },
    LOAD_ERROR: 'An error occurred while fetching the system information'
  },

  SUPPORT_REQUEST: {
    MENU: {
      text: 'Support Request',
      description: 'Submit a support request to Sonatype'
    },
    ACTIONS: {
      submitRequest: 'Submit Request'
    },
    DESCRIPTION: `Please include a complete description of your problem and steps to allow us to reproduce the problem (if available).`,
    ATTACH_SUPPORT_ZIP: `Attaching a support ZIP to your request will help our engineers give you a faster response.`
  },

  USER_ACCOUNT: {
    MENU: {
      text: 'Account',
      description: 'Manage your account',
    },
    ACTIONS: {
      changePassword: 'Change password',
      discardChangePassword: 'Discard password change',
    },
    MESSAGES: {
      LOAD_ERROR: 'An error occurred while loading User Account, see console for more details',
      UPDATE_SUCCESS: 'User account settings updated',
      UPDATE_ERROR: 'An error occurred while updating user account settings',
      PASSWORD_MUST_DIFFER_ERROR: 'New password must be different',
      PASSWORD_CHANGE_SUCCESS: 'Password changed',
      PASSWORD_CHANGE_ERROR: 'Change password failed',
    },
    ID_FIELD_LABEL: 'Username',
    FIRST_FIELD_LABEL: 'First Name',
    LAST_FIELD_LABEL: 'Last Name',
    EMAIL_FIELD_LABEL: 'Email',
    PASSWORD_CURRENT_FIELD_LABEL: 'Current Password',
    PASSWORD_NEW_FIELD_LABEL: 'New Password',
    PASSWORD_NEW_CONFIRM_FIELD_LABEL: 'New Password (Confirm)',
  },

  NUGET_API_KEY: {
    MENU: {
      text: 'NuGet API Key',
      description: 'Configure credentials for NuGet repositories'
    },
    INSTRUCTIONS: 'A new API Key will be created the first time it is accessed. Resetting your API Key will invalidate the current key.',
    AUTH_INSTRUCTIONS: 'Accessing NuGet API Key requires validation of your credentials.',
    AUTH_ERROR: 'Authentication Failure',
    CLOSE: 'Close',
    ACCESS: {
      BUTTON: 'Access API Key',
      HELP_TEXT: 'Accessing NuGet API Key requires validation of your credentials.',
      ERROR: 'Failed to access API Key'
    },
    RESET: {
      BUTTON: 'Reset API Key',
      HELP_TEXT: 'Resetting NuGet API Key requires validation of your credentials.',
      ERROR: 'Failed to Reset API Key'
    },
    DETAILS: {
      MAIN: 'Your NuGet API Key enables pushing packages using nuget.exe.',
      WARNING: 'Keep this key secret!',
      API_KEY_TEXT: 'Your NuGet API Key is',
      REGISTER_TEXT: 'You can register this key for a given repository with the following command',
      REGISTER_COMMAND: 'nuget setapikey {0} -source {1}',
      AUTO_CLOSE: 'This window will automatically close after one minute.'
    }
  },

  METRIC_HEALTH: {
    MENU: {
      text: 'Status',
      description: 'System status checks'
    },
    NAME_HEADER: 'Name',
    MESSAGE_HEADER: 'Message',
    ERROR_HEADER: 'Error',
  },

  SUPPORT_ZIP: {
    MENU: {
      text: 'Support ZIP',
      description: 'Creates a ZIP file containing useful support information about your server'
    },

    DESCRIPTION: '<p>No information will be sent to Sonatype when creating the support ZIP file.</p>' +
        '<p>Support ZIP creation may take a few minutes to complete.</p>',
    CONTENTS: 'Contents',
    OPTIONS: 'Options',

    REPORT_LABEL: 'System information report',
    DUMP_LABEL: 'JVM thread-dump',
    CONFIGURATION_LABEL: 'Configuration files',
    SECURITY_LABEL: 'Security configuration files',
    LOGFILES_LABEL: 'Log files',
    TASKLOGFILES_LABEL: 'Task log files',
    AUDITLOGFILES_LABEL: 'Audit log files',
    METRICS_LABEL: 'System and component metrics',
    JMX_LABEL: 'JMX information',
    REPLICATION_LABEL: 'Native replication logs (rsync or awscli)',
    LIMITFILESIZES_LABEL: 'Limit files in the ZIP archive to 30 MB apiece',
    LIMITZIPSIZE_LABEL: 'Limit the ZIP archive to 20 MB',
    CREATED_TITLE: 'Support ZIP Created',
    CREATED_DESCRIPTION: 'Support ZIP has been created.<br/>You can reference this file on the filesystem or download the file from your browser.',
    CREATED_NODEID_LABEL: 'Node:',
    CREATED_NAME_LABEL: 'Name:',
    CREATED_SIZE_LABEL: 'Size:',
    CREATED_PATH_LABEL: 'Path:',
    CREATED_DOWNLOAD_BUTTON: 'Download',
    AUTHENTICATE_TEXT: 'Downloading support ZIP requires validation of your credentials.',

    MENU_HA: {
      text: 'Support ZIP',
      description: 'Manage your support zips'
    },

    AVAILABLE_NODES: 'Available Nodes',
    NODE_IS_ACTIVE: 'Node Active',
    ZIP_UPDATED: 'Zip updated',
    NO_ZIP_CREATED: 'No Zip created',
    DOWNLOAD_ZIP: 'Download Zip',
    CREATING_ZIP: 'Creating Zip...',
    CREATE_SUPPORT_ZIP: 'Create Support zip',

    CREATE_SUPPORT_ZIP_MODAL_HEADER: 'Options for Export'
  },

  HEALTHCHECK_EULA: {
    HEADER: 'Nexus IQ Server Terms of Use',
    BUTTONS: {
      ACCEPT: 'I accept',
      DECLINE: 'I do not accept'
    }
  },

  ANALYZE_APPLICATION: {
    HEADER: 'Analyze Application',
    MAIN: 'Application analysis performs a deep inspection of this application, identifying potential risks.<br/> More information is available here <a href=\"http://links.sonatype.com/products/insight/ac/home\">here</a>',
    EMAIL: {
      LABEL: 'Email address',
      DESCRIPTION: 'The address where the summary report will be sent'
    },
    PASSWORD: {
      LABEL: 'Report password',
      DESCRIPTION: 'A password to gain access to the detailed report'
    },
    PACKAGES: {
      LABEL: 'Proprietary packages',
      DESCRIPTION: 'A comma separated list of proprietary packages'
    },
    REPORT: {
      LABEL: 'Report label',
      DESCRIPTION: 'The name the report will be given'
    },
    SELECT_ASSET: {
      LABEL: 'Select Asset',
      DESCRIPTION: 'Select an asset to base the analysis on'
    },
    BUTTONS: {
      ANALYZE: 'Analyze',
      CANCEL: 'Cancel'
    }
  },

  CLEANUP_POLICIES: {
    MENU: {
      text: 'Cleanup Policies',
      description: 'Manage component removal configuration'
    },

    CREATE_TITLE: 'Create Cleanup Policy',
    EDIT_TITLE: 'Edit Cleanup Policy',

    HELP_TITLE: 'What is a cleanup policy?',
    HELP_TEXT: <>
      Cleanup policies can be used to remove content from your repositories. These policies will execute
      at the configured frequency. Once created, a cleanup policy must be assigned to a repository from{' '}
      <NxTextLink href="#admin/repository/repositories">the repository configuration screen</NxTextLink>.
      For more information, check{' '}
      <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/cleanup-policy">
        the documentation
      </NxTextLink>.
    </>,
    EMPTY_MESSAGE: 'No cleanup policies were found',
    CREATE_BUTTON: 'Create Cleanup Policy',
    FILTER_PLACEHOLDER: 'Filter',

    NAME_LABEL: 'Name',
    FORMAT_LABEL: 'Format',
    NOTES_LABEL: 'Notes',
    CRITERIA_LABEL: 'Cleanup Criteria',
    LAST_UPDATED_CHECKBOX_TITLE: (enabled) => `${enabled ? 'Disable' : 'Enable'} Component Age Criteria`,
    LAST_UPDATED_LABEL: 'Component Age',
    LAST_DOWNLOADED_CHECKBOX_TITLE: (enabled) => `${enabled ? 'Disable' : 'Enable'} Component Usage Criteria`,
    LAST_DOWNLOADED_LABEL: 'Component Usage',
    RELEASE_TYPE_CHECKBOX_TITLE: (enabled) => `${enabled ? 'Disable' : 'Enable'} Release Type Criteria`,
    RELEASE_TYPE_LABEL: 'Release Type',
    ASSET_NAME_CHECKBOX_TITLE: (enabled) => `${enabled ? 'Disable' : 'Enable'} Asset Name Matcher Criteria`,
    ASSET_NAME_LABEL: 'Asset Name Matcher',
    FORMAT_SELECT: 'Select a format...',
    RELEASE_TYPE_SELECT: 'Select a release type...',
    REPOSITORY_SELECT: 'Select a repository...',

    NAME_DESCRIPTION: 'Use a unique name for the cleanup policy',
    FORMAT_DESCRIPTION: 'The format that this cleanup policy can be applied to',
    LAST_UPDATED_DESCRIPTION: 'Remove components that were published over:',
    LAST_DOWNLOADED_DESCRIPTION: "Remove components that haven't been downloaded in:",
    RELEASE_TYPE_DESCRIPTION: 'Remove components that are of the following release type:',
    ASSET_NAME_DESCRIPTION: 'Remove components that have at least one asset name matching the following regular expression pattern:',

    LAST_UPDATED_SUFFIX: 'days ago',
    LAST_DOWNLOADED_SUFFIX: 'days',

    RELEASE_TYPE_RELEASE: 'Release Versions',
    RELEASE_TYPE_PRERELEASE: 'Pre-Release / Snapshot Versions',

    PREVIEW: {
      TITLE: 'Cleanup policy preview',
      REPOSITORY_LABEL: 'Preview Repository',
      REPOSITORY_DESCRIPTION: 'Select a repository to preview what might get cleaned up if this policy was applied',
      INVALID_QUERY: 'The filter is invalid so results could not be retrieved.',
      BUTTON: 'Preview',
      RESULTS: 'Preview Results',
      NAME_COLUMN: 'Name',
      GROUP_COLUMN: 'Group',
      VERSION_COLUMN: 'Version',
      EMPTY: 'No assets in repository matched the criteria',
      COMPONENT_COUNT: (actual, total) => {
        if (total === 0) {
          return 'Criteria matched no components.';
        }
        else if (total > 0) {
          return `Component count (matching criteria) viewing ${actual} out of ${total}.`;
        }
        return `Component count (matching criteria) viewing first ${actual}`;
      },
      SAMPLE_WARNING: 'Results may only be a sample of what will be deleted using the current criteria.',
      BUTTON_TOOLTIP: 'Please select a repository and at least one cleanup criteria',
    },

    MESSAGES: {
      SAVE_ERROR: 'An error occurred while saving the cleanup policy',
      DELETE_ERROR: (name) => `Cleanup policy ${name} is in use and cannot be deleted`,

      CONFIRM_DELETE: {
        TITLE: 'Delete cleanup policy',
        MESSAGE: (inUseCount) => inUseCount ?
            `This Cleanup Policy is used by ${inUseCount} repositories` :
            'This Cleanup Policy is not used by any repository',
        YES: 'Delete',
        NO: 'Cancel'
      }
    }
  },

  IQ_SERVER: {
    MENU: {
      text: 'IQ Server',
      description: 'Manage IQ Server Connection Configuration'
    },

    OPEN_DASHBOARD: 'Open IQ Server Dashboard',

    ENABLED: {
      label: 'Enable IQ Server',
      sublabel: 'Enable the use of IQ Server'
    },

    IQ_SERVER_URL: {
      label: 'IQ Server URL',
      sublabel: 'This is the address of your IQ server'
    },

    TRUST_STORE: {
      label: 'Use the Nexus Repository Truststore',
      sublabel: 'Use certificate connected to the Nexus Repository Truststore'
    },

    CERTIFICATE: 'View Certificate',

    AUTHENTICATION_TYPE: {
      label: 'Authentication Method',
      USER: 'User Authentication',
      PKI: 'PKI Authentication'
    },

    USERNAME: {
      label: 'Username',
      sublabel: 'User with Access to the IQ Server'
    },

    PASSWORD: {
      label: 'Password',
      sublabel: 'Credentials for the IQ Server User'
    },

    CONNECTION_TIMEOUT: {
      label: 'Connection Timeout',
      sublabel: <>
        Seconds to wait for activity before stopping and retrying the Connection.
        <br/>
        Leave blank to use the globally defined HTTP timeout.
      </>,
    },
    CONNECTION_TIMEOUT_DEFAULT_VALUE_LABEL: 'Globally Defined',
    PROPERTIES: {
      label: 'Properties',
      sublabel: 'Additional properties to configure for IQ Server'
    },
    SHOW_LINK: {
      label: 'Show IQ Server Link',
      sublabel: 'Show IQ Server link in the Browse menu when the server is enabled'
    },

    VERIFY_CONNECTION_BUTTON_LABEL: 'Verify Connection',
    VERIFY_CONNECTION_SUCCESSFUL: (msg) => `Connection Successful! Applications: ${msg}`,
    VERIFY_CONNECTION_ERROR: (msg) => `Connection Failed: ${msg}`,
    FORM_NOTES: 'can evaluate application and organizing policies',
    HELP_TEXT: 'To enable this feature configure the IQ Server URL, username and password',
    PASSWORD_ERROR: 'Reenter your password to validate and save your changes.'
  },

  BUNDLES: {
    MENU: {
      text: 'Bundles',
      description: 'View OSGI bundles'
    },
    LIST: {
      ID_LABEL: 'ID',
      STATE_LABEL: 'State',
      LEVEL_LABEL: 'Level',
      NAME_LABEL: 'Name',
      VERSION_LABEL: 'Version'
    },
    BUNDLES_DETAIL: {
      MENU: {
        text: 'Bundle Summary'
      },
      LIST: {
        ID_LABEL: 'ID',
        NAME_LABEL: 'Name',
        SYMBOLIC_NAME_LABEL: 'Symbolic Name',
        VERSION_LABEL: 'Version',
        STATE_LABEL: 'State',
        LOCATION_LABEL: 'Location',
        START_LEVEL_LABEL: 'Start Level',
        LAST_MODIFIED_LABEL: 'Last Modified',
        FRAGMENT_LABEL: 'Fragment'
      },
    }
  },

  API: {
    MENU: {
      text: 'API',
      description: 'Learn how to interact with Nexus Repository Manager programmatically'
    },
  },

  PROPRIETARY_REPOSITORIES: {
    MENU: {
      text: 'Proprietary Repositories',
      description: 'Manage the set of hosted repositories that contain proprietary components'
    },
    CONFIGURATION: {
      LABEL: 'Proprietary Repositories Configuration',
      AVAILABLE_TITLE: 'Generic Hosted Repositories',
      SELECTED_TITLE: 'Proprietary Hosted Repositories',
      EMPTY_LIST: 'There are no configured proprietary hosted repositories for which you have view permissions.',
    },
    HELP_TEXT: <>
      To help prevent dependency confusion attacks, identify your hosted repositories that contain
      proprietary components. Refer to the{' '}
      <NxTextLink external href="http://links.sonatype.com/products/nxrm3/docs/proprietary-repositories">
        documentation
      </NxTextLink>
      {' '}for details on setting up appropriate IQ policies to quarantine public components with the same names
      as your proprietary components.
    </>,
  },

  HTTP: {
    MENU: {
      text: 'HTTP',
      description: 'Manage outbound HTTP/HTTPS configuration',
    },
    CONFIGURATION: {
      USER_AGENT: {
        LABEL: 'User-Agent Customization',
        SUB_LABEL: 'Custom fragment to append to “User-Agent” header in HTTP requests'
      },
      TIMEOUT: {
        LABEL: 'Connection/Socket Timeout',
        SUB_LABEL: 'Time (seconds) to wait for activity before stopping and retrying the connection'
      },
      ATTEMPTS: {
        LABEL: 'Connection/Socket Retry Attempts',
        SUB_LABEL: 'Maximum number of retry attempts if the initial connection attempt suffers a timeout'
      },
      PROXY: {
        LABEL: 'Proxy Settings',
        SUB_LABEL: 'Provide an IP address or DNS name (e.g., proxy-host or 192.168.1.101), not a URL',
        HTTP_HOST: 'HTTP Proxy Host',
        HTTP_PORT: 'HTTP Proxy Port',
        HTTP_CHECKBOX: 'Enable HTTP proxy',
        HTTPS_HOST: 'HTTPS Proxy Host',
        HTTPS_PORT: 'HTTPS Proxy Port',
        HTTPS_CHECKBOX: 'Enable HTTPS proxy',
        HTTP_AUTHENTICATION: 'HTTP Authentication',
        HTTPS_AUTHENTICATION: 'HTTPS Authentication',
        USERNAME: 'Username',
        PASSWORD: 'Password',
        HOST_NAME: 'Windows NTLM Hostname',
        DOMAIN: 'Windows NTLM Domain'
      },
      EXCLUDE: {
        LABEL: 'Hosts to exclude from HTTP/HTTPS Proxy',
        SUB_LABEL: 'Accepts Java “http.nonProxyHosts” wildcard patterns (one per line, no “l” hostname delimiters)',
        ADD: 'Add',
        REMOVE: 'Remove',
      },
      READ_ONLY: {
        LABEL: 'HTTP Configuration',
        WARNING: 'You are viewing a read-only version of this page. Some fields will not appear if they have not changed from their default values or if HTTP/HTTPS authentication is not enabled. Contact your Administrator if you require edit permissions.'
      }
    }
  },
};
