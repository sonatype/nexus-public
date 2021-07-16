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
        TEXT: `\
The binary assets you download via proxy repositories, or publish to hosted repositories, are stored in \
the blob store attached to those repositories. In traditional, single node NXRM deployments, blob stores \
are typically associated with a local filesystem directory, usually within the sonatype-work directory. \
For more information, check \
<a href="http://links.sonatype.com/products/nxrm3/docs/blob-store" target="_blank" rel="noopener noreferrer">\
the documentation</a>.`
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
      PROMOTE_BUTTON: 'Promote to Group',
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
        NO_AUTH: 'Use Environment Variables',
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
    HELP_TEXT: `\
Content selectors provide a means for you to select specific content from your repositories. \
Repository content is evaluated against expressions written in CSEL (Content Selector Expression Language). \
For more information, <a href="http://links.sonatype.com/products/nxrm3/docs/content-selector" target="_blank" rel="noopener noreferrer">check the documentation</a>.\
`,

    CREATE_BUTTON: 'Create Selector',
    FILTER_PLACEHOLDER: 'Filter',

    NAME_LABEL: 'Name',
    TYPE_LABEL: 'Type',
    DESCRIPTION_LABEL: 'Description',
    EXPRESSION_LABEL: 'Search Expression',
    EXPRESSION_DESCRIPTION: 'Use the following query to identify repository content',

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

  LOG_VIEWER: {
    MENU: {
      text: 'Log Viewer',
      description: 'View the current log contents'
    },
    REFRESH: {
      TEXT: 'Refresh Interval',
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
    },
    DOWNLOAD: 'Download',
    INSERT_MARK: 'Insert Mark',
    MARK_PLACEHOLDER: 'Marker to insert into log'
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
        HEALTH_CHECK: 'Health check',
        IQ: 'IQ Policy Violations'
      },
      FILTER_PLACEHOLDER: 'Filter by name',
      EMPTY_LIST: 'There are no repositories available',
      HELP: {
        TITLE: 'What is a repository?',
        TEXT: `\
A repository is a storage location where components, such as packages, libraries, binaries, and containers, \
are retrieved so they can be installed or used. Creating and managing repositories is an essential part of \
your Nexus Repository Manager configuration since it allows you to expose content to your end users as well \
as provide a location for them to store more content. For more information, check \
<a href="http://links.sonatype.com/products/nxrm3/docs/repository" target="_blank" rel="noopener noreferrer">\
the documentation</a>.`
      },
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
      HELP_TEXT: `\
Routes are like filters you can apply to groups in terms of security access and general component retrieval. \
They can be used to reduce the number of repositories within a group accessed in order to retrieve a component. \
For more information, <a href="http://links.sonatype.com/products/nxrm3/docs/routing-rule" target="_blank" \
rel="noopener noreferrer">check the documentation</a>.
`
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
      PASSWORD_NO_MATCH_ERROR: 'Passwords do not match',
      PASSWORD_MUST_DIFFER_ERROR: 'New password must be different',
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
      API_KEY_TEXT: 'Your NuGet API Key is:',
      REGISTER_TEXT: 'You can register this key for a given repository with the following command:',
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
    AUTHENTICATE_TEXT: 'Downloading support ZIP requires validation of your credentials.'
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
    HELP_TEXT: `\
Cleanup policies can be used to remove content from your repositories. These policies will execute at the configured frequency. \
Once created, a cleanup policy must be assigned to a repository from \
<a href="#admin/repository/repositories">the repository configuration screen</a>. For more information, check \
<a href="http://links.sonatype.com/products/nxrm3/docs/cleanup-policy" target="_blank" rel="noopener noreferrer">the documentation</a>.\
`,
    EMPTY_MESSAGE: 'No cleanup policies were found',
    CREATE_BUTTON: 'Create Cleanup Policy',
    FILTER_PLACEHOLDER: 'Filter',

    NAME_LABEL: 'Name',
    FORMAT_LABEL: 'Format',
    NOTES_LABEL: 'Notes',
    CRITERIA_LABEL: 'Cleanup Criteria',
    LAST_UPDATED_LABEL: 'Component Age',
    LAST_DOWNLOADED_LABEL: 'Component Usage',
    RELEASE_TYPE_LABEL: 'Release Type',
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
      EMPTY: 'No assets in repository matched the criteria'
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
  }
};
