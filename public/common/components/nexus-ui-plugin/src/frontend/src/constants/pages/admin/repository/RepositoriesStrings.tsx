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

/**
 * UI strings for all repository facet components.
 * Centralises all user-visible text so it can be reviewed, localised, and tested in isolation.
 */
export default {

  // ---------------------------------------------------------------------------
  // AlpineFacet
  // ---------------------------------------------------------------------------
  ALPINE: {
    SIGNING: {
      title: 'Alpine Signing',
      description: 'RSA signing configuration for Alpine repositories',
      KEYPAIR: {
        // Generation command mirrors Classic's help text, which the old string omitted.
        label: 'RSA Signing Key',
        helpText:
          'RSA private key in PKCS#8 format (PEM encoded). Used to sign APKINDEX files. ' +
          'Generate with: openssl genrsa 2048 | openssl pkcs8 -topk8 -nocrypt',
      },
      PASSPHRASE: {
        label: 'RSA Signing Key Passphrase',
        helpText: 'Passphrase for the RSA signing key (leave empty if key has no passphrase)',
      },
    },
  },

  // ---------------------------------------------------------------------------
  // AptFacet
  // ---------------------------------------------------------------------------
  APT: {
    SETTINGS: {
      title: 'APT Settings',
      description: 'Debian/APT repository configuration',
      ENFORCE_DISTRIBUTION: {
        label: 'Enforce Distribution',
        description: 'Restrict the distribution field to the value configured below',
      },
      DISTRIBUTION: {
        label: 'Distribution',
        helpText: 'Distribution to fetch (e.g., bionic, focal, jammy) or path for flat repositories',
        placeholder: 'e.g., bionic',
      },
      FLAT: {
        label: 'Flat Repository',
        description: 'Is this repository flat (i.e., no distribution folder hierarchy)?',
      },
    },
    SIGNING: {
      title: 'APT Signing',
      description: 'GPG signing configuration for APT repositories',
      KEYPAIR: {
        label: 'GPG Signing Key',
        helpText: 'PEM encoded GPG signing key pair',
      },
      PASSPHRASE: {
        label: 'GPG Signing Key Passphrase',
        helpText: 'Passphrase for the GPG signing key (leave empty if key has no passphrase)',
      },
    },
  },

  // ---------------------------------------------------------------------------
  // CargoFacet
  // ---------------------------------------------------------------------------
  CARGO: {
    SECTION: {
      title: 'Authentication Requirements',
      description:
        'Leaving this box unchecked only allows anonymous access to this repository if anonymous access is also enabled in your instance',
    },
    REQUIRE_AUTHENTICATION: {
      label: 'Restrict repository content to authenticated users',
    },
  },

  // ---------------------------------------------------------------------------
  // CertificateViewDialog
  // ---------------------------------------------------------------------------
  CERTIFICATE: {
    DIALOG: {
      title: 'Certificate Details',
      retrieving: 'Retrieving certificate…',
      httpsOnlyError: 'Certificate inspection is only available for HTTPS URLs.',
      untrustedWarning:
        'This certificate was retrieved over an untrusted connection. Always verify the details before adding it to the Nexus Repository Truststore.',
    },
    FIELDS: {
      commonName: 'Common Name',
      organization: 'Organization',
      unit: 'Unit',
      issuerCommonName: 'Issuer Common Name',
      issuerOrganization: 'Issuer Organization',
      issuerUnit: 'Issuer Unit',
      issuedOn: 'Certificate Issued On',
      validUntil: 'Valid Until',
      fingerprint: 'Fingerprint',
    },
    ACTIONS: {
      addToTrustStore: 'Add certificate to truststore',
      removeFromTrustStore: 'Remove certificate from truststore',
      close: 'Close',
    },
    ERRORS: {
      fetchFailed: 'Failed to retrieve certificate',
      addFailed: 'Failed to add certificate to trust store',
      removeFailed: 'Failed to remove certificate from trust store',
    },
  },

  // ---------------------------------------------------------------------------
  // CleanupFacet
  // ---------------------------------------------------------------------------
  CLEANUP: {
    SECTION: {
      title: 'Cleanup',
    },
    helpText:
      'Apply cleanup policies to automatically remove components from this repository.',
    MANAGE_LINK: {
      label: 'Manage Cleanup Policies',
    },
    TRANSFER_LIST: {
      label: 'Cleanup Policies',
      availableLabel: 'Available Policies',
      selectedLabel: 'Applied Policies',
      helpText: 'Select cleanup policies to apply to this repository',
    },
  },

  // ---------------------------------------------------------------------------
  // DockerFacet
  // ---------------------------------------------------------------------------
  DOCKER: {
    REGISTRY: {
      title: 'Docker Registry API Support',
      description: 'Configure Docker registry connector and authentication settings',
    },
    INDEX: {
      title: 'Docker Index',
      description: 'Configure how this proxy repository connects to the Docker registry index',
    },
    ROUTING_MODE: {
      label: 'Routing Mode',
      helpText:
        'Path-based routing uses the repository name in the URL path. Connectors use dedicated ports or subdomains.',
      PATH_BASED: 'Use path-based routing',
      CONNECTORS: 'Use connectors (ports and/or subdomain)',
    },
    HTTP_CONNECTOR: {
      label: 'HTTP Connector',
      helpText:
        'Create an HTTP connector at the specified port. Normally used if the server is behind a reverse proxy.',
      placeholder: 'e.g., 8082',
      suggestPort: 'Suggest available port',
    },
    HTTPS_CONNECTOR: {
      label: 'HTTPS Connector',
      helpText:
        'Create an HTTPS connector at the specified port. Normally used if the server is not behind a reverse proxy.',
      placeholder: 'e.g., 8083',
      suggestPort: 'Suggest available port',
    },
    SUBDOMAIN: {
      label: 'Subdomain',
      helpText:
        'Use the specified subdomain to access this Docker repository. Only used when behind a reverse proxy with subdomain-based routing.',
      placeholder: 'e.g., docker-hosted',
    },
    FORCE_BASIC_AUTH: {
      label: 'Force Basic Authentication',
      description: 'Require authentication even for anonymous access (docker login required for pull)',
    },
    V1_ENABLED: {
      label: 'Enable Docker V1 API',
      description: 'Allow clients to use the V1 API to interact with this repository',
    },
    INDEX_TYPE: {
      label: 'Docker Index',
      helpText: 'Type of Docker Index',
      USE_HUB: 'Use Docker Hub',
      USE_PROXY: 'Use proxy registry (specified in Remote URL)',
      CUSTOM: 'Custom index',
    },
    INDEX_URL: {
      label: 'Index URL',
      helpText: 'Location of Docker Index',
      placeholder: 'https://index.example.com',
    },
    CACHE_FOREIGN_LAYERS: {
      label: 'Allow Nexus Repository Manager to download and cache foreign layers',
      description: 'Cache foreign layers (layers stored on a different server) in this proxy repository',
    },
    FOREIGN_LAYER_WHITELIST: {
      label: 'Foreign Layer URL Whitelist',
      helpText: 'Regular expressions of foreign layer URLs to allow (one per line). Leave empty to allow all.',
      placeholder: 'https://example.com/.*',
    },
  },

  // ---------------------------------------------------------------------------
  // GroupFacet
  // ---------------------------------------------------------------------------
  GROUP: {
    SECTION: {
      title: 'Group',
    },
    WRITABLE_MEMBER: {
      label: 'Writable Repository',
      helpText: 'The member repository that POST and PUT requests will be routed to',
      noneOption: '(None)',
    },
    MEMBER_REPOSITORIES: {
      label: 'Member Repositories',
      helpText: 'Select repositories to include in this group. Order determines search priority.',
      addPlaceholder: 'Add a member repository...',
      emptyMessage: 'No member repositories selected',
    },
    BUTTONS: {
      moveUp: 'Move up',
      moveDown: 'Move down',
      remove: 'Remove',
    },
  },

  // ---------------------------------------------------------------------------
  // HostedFacet
  // ---------------------------------------------------------------------------
  HOSTED: {
    SECTION: {
      title: 'Hosted',
    },
    DEPLOYMENT_POLICY: {
      label: 'Deployment Policy',
      helpText: 'Controls if deployments of and updates to artifacts are allowed',
    },
    PROPRIETARY_COMPONENTS: {
      label: 'Proprietary Components',
      description: 'Components in this repository count as proprietary for firewall',
    },
  },

  // ---------------------------------------------------------------------------
  // HttpClientFacet
  // ---------------------------------------------------------------------------
  HTTP_CLIENT: {
    SECTION: {
      title: 'HTTP',
    },
    AUTH_TYPE: {
      label: 'Authentication Type',
      helpText: 'Type of authentication used to connect to the remote repository',
      NONE: 'No authentication',
      USERNAME: 'Username',
      NTLM: 'Windows NTLM',
      BEARER: 'Bearer Token',
    },
    USERNAME: {
      label: 'Username',
    },
    PASSWORD: {
      label: 'Password',
    },
    NTLM_HOST: {
      label: 'NTLM Host',
    },
    NTLM_DOMAIN: {
      label: 'NTLM Domain',
    },
    BEARER_TOKEN: {
      label: 'Bearer Token',
    },
    PREEMPTIVE_AUTH: {
      label: 'Use pre-emptive authentication',
      description:
        'Caution! Use this only when absolutely necessary. Enabling this option means configured authentication credentials will be sent to the remote URL regardless of whether the remote server has asked for them or not.',
      disabledDescription: 'Pre-emptive authentication requires HTTPS remote URL',
    },
    ADVANCED: {
      toggleLabel: 'HTTP Request Settings',
    },
    USER_AGENT_SUFFIX: {
      label: 'User-Agent Suffix',
      helpText: 'Custom fragment to append to the User-Agent header',
    },
    RETRIES: {
      label: 'Connection Retries',
      helpText: 'Total retries if the initial connection attempt suffers a timeout',
    },
    TIMEOUT: {
      label: 'Connection/Socket Timeout',
      helpText: 'Seconds to wait for activity before stopping and retrying the connection',
    },
    CIRCULAR_REDIRECTS: {
      label: 'Enable circular redirects',
      description: 'Enable redirects to the same location',
    },
    COOKIES: {
      label: 'Enable cookies',
      description: 'Allow cookies to be stored and used',
    },
  },

  // ---------------------------------------------------------------------------
  // MavenFacet
  // ---------------------------------------------------------------------------
  MAVEN: {
    SECTION: {
      title: 'Maven 2',
      description: 'Maven-specific repository configuration',
    },
    VERSION_POLICY: {
      label: 'Version Policy',
      helpText: 'Controls what type of artifacts can be deployed to this repository',
      RELEASE: 'Release',
      SNAPSHOT: 'Snapshot',
      MIXED: 'Mixed',
    },
    LAYOUT_POLICY: {
      label: 'Layout Policy',
      helpText: 'Validates that all paths are Maven artifact or metadata paths',
      STRICT: 'Strict',
      PERMISSIVE: 'Permissive',
    },
    CONTENT_DISPOSITION: {
      label: 'Content Disposition',
      helpText:
        "Sets Content-Disposition header to 'Attachment', causing browsers to download files rather than display them inline.",
      INLINE: 'Inline',
      ATTACHMENT: 'Attachment',
      inlineWarning:
        'Serving content inline allows uploaded HTML to render on a trusted Nexus URL, which can be exploited for phishing attacks against other users.',
    },
  },

  // ---------------------------------------------------------------------------
  // NegativeCacheFacet
  // ---------------------------------------------------------------------------
  NEGATIVE_CACHE: {
    SECTION: {
      title: 'Negative Cache',
    },
    ENABLED: {
      label: 'Not found cache enabled',
      description: 'Cache responses for content not present in the remote repository',
    },
    TTL: {
      label: 'Negative Cache TTL (Minutes)',
      helpText:
        'How long (in minutes) to cache that a file was not found in the remote repository',
    },
  },

  // ---------------------------------------------------------------------------
  // NpmFacet
  // ---------------------------------------------------------------------------
  NPM: {
    SECTION: {
      title: 'npm Settings',
      description: 'npm proxy repository configuration',
    },
    REMOVE_QUARANTINED: {
      label: 'Filter component versions that fail Sonatype Repository Firewall policy',
      description:
        'If enabled, automatically filter component versions from metadata that fail Sonatype Repository Firewall policy at the Proxy stage.',
    },
  },

  // ---------------------------------------------------------------------------
  // NugetFacet
  // ---------------------------------------------------------------------------
  NUGET: {
    SECTION: {
      title: 'NuGet',
      description: 'NuGet proxy repository configuration',
    },
    PROTOCOL_VERSION: {
      label: 'Protocol version',
      V2: 'NuGet V2',
      V3: 'NuGet V3',
    },
    QUERY_CACHE_AGE: {
      label: 'Metadata query cache age',
      helpText: 'How long to cache query results from the proxied repository (in seconds)',
    },
    SYMBOL_SERVER_URL: {
      label: 'Symbol Server URL',
      helpText: 'Optional upstream symbol server URL for proxying symbol downloads (e.g. https://symbols.nuget.org/download/symbols).',
      placeholder: 'https://',
    },
    ALLOW_ANONYMOUS_SYMBOL_ACCESS: {
      label: 'Allow Anonymous Symbol Access',
      description: 'Allow unauthenticated access to the symbol server (required for debugger integration)',
    },
    MIXED_VERSION_WARNING: (
      conflictingName: string,
      conflictingVersion: string,
      firstMemberName: string,
      firstMemberVersion: string
    ) =>
      `Group repositories cannot include a mix of NuGet v2 and v3 members. You cannot add ${conflictingName} (${conflictingVersion}) because the group contains ${firstMemberName} (${firstMemberVersion}).`,
  },

  // ---------------------------------------------------------------------------
  // HuggingFaceFacet
  // ---------------------------------------------------------------------------
  HUGGINGFACE: {
    SECTION: {
      title: 'Transfer Protocol',
      description: 'HuggingFace proxy transfer-protocol configuration',
    },
    ENABLE_XET: {
      label: 'Enable XET',
      description:
        'When enabled, the XET protocol is used for file transfers. When disabled, the legacy LFS path is used.',
    },
    XET_DISABLES_FIREWALL_WARNING:
      'Enabling XET disables Firewall for this repository, including existing non-XET content. If XET is disabled later, Firewall must be re-enabled manually.',
    XET_CONFIRM_TITLE: 'Enable XET and disable Firewall?',
    XET_CONFIRM_MESSAGE:
      'Firewall and IQ Server will not evaluate content in this repository while XET is enabled. This applies to all content, including existing non-XET content. If you disable XET later, you will need to re-enable Firewall manually.',
    XET_CONFIRM_LABEL: 'Disable Firewall and enable XET',
    FIREWALL_STEP_XET_DISABLED_WARNING:
      'Firewall is disabled while XET is enabled. This applies to all content in the repository, including existing non-XET content. To re-enable Firewall, disable XET first, then configure Firewall manually.',
  },

  // ---------------------------------------------------------------------------
  // ProxyFacet
  // ---------------------------------------------------------------------------
  PROXY: {
    SECTION: {
      title: 'Proxy',
    },
    REMOTE_STORAGE: {
      label: 'Remote Storage',
      helpText: (urlExample: string) =>
        `Location of the remote repository being proxied. ${urlExample}`,
      nugetChocolateyHelpText:
        'Enter the URL to proxy. Supports NuGet V2, NuGet V3, and Chocolatey. For example: https://api.nuget.org/v3/index.json',
      placeholder: 'https://',
    },
    TRUST_STORE: {
      label: 'Use the Nexus Repository truststore',
      description:
        'Use certificates stored in the Nexus Repository truststore to connect to external systems',
      viewCertificate: 'View Certificate',
    },
    ORIGIN_CHANGE_WARNING:
      'Remote URL has changed. Authentication credentials have been reset and must be re-entered.',
    PREEMPTIVE_PULL: {
      label: 'Pre-emptive Pull',
      description:
        'If enabled, the remote storage will be monitored for changes, and new components will be replicated automatically, and cached locally',
      enabledCheckbox: 'Enabled',
    },
    ASSET_NAME_MATCHER: {
      label: 'Asset Name Matcher',
      helpText:
        'Enter a regular expression to match asset names. When left blank, all assets are matched.',
    },
    PRESERVE_ENCODED_CHARACTERS: {
      label: 'Preserve Encoded Characters',
      description:
        'When checked, keeps encoded characters like %2B (plus), %23 (hash), and %20 (space) in their encoded form when proxying to the remote repository. Enable when proxying to AWS S3, Cloudflare CDN, or Azure Blob Storage.',
    },
    BLOCKING: {
      sectionLabel: 'Blocking',
      BLOCKED: {
        label: 'Blocked',
        description: 'Block outbound connections to the repository',
      },
      AUTO_BLOCK: {
        label: 'Auto blocking enabled',
        description:
          'Auto-block outbound connections to the repository if remote peer is detected as unreachable/unresponsive',
      },
    },
    AUTO_BLOCK_STATUS_CODES: {
      label: 'Auto-block HTTP Status Codes',
      helpText:
        'Comma-separated list of remote HTTP status codes (100-599). Only the codes listed here trigger auto-blocking; any code not listed will not auto-block. Leave empty to use the format default. For example: 500, 502, 429',
      placeholder: 'e.g. 500, 502, 429',
      validationError: 'Each status code must be a whole number between 100 and 599',
      duplicateError: 'Status codes must not contain duplicates',
    },
    CONTENT_MAX_AGE: {
      label: 'Maximum Component Age',
      helpText:
        'How long (in minutes) to cache artifacts before rechecking the remote repository. Set to -1 to disable caching.',
    },
    METADATA_MAX_AGE: {
      label: 'Maximum Metadata Age',
      helpText: 'How long (in minutes) to cache metadata before rechecking the remote repository.',
    },
  },

  // ---------------------------------------------------------------------------
  // PyPiFacet
  // ---------------------------------------------------------------------------
  PYPI: {
    SECTION: {
      title: 'PyPI Settings',
    },
    INDEX_PATH: {
      label: 'Remote Index Path',
      helpText:
        'Path appended to the remote URL for PyPI Simple API access. Use "/simple" (default) for standard PyPI repositories like PyPI.org, or leave empty for root-path repositories like pypi.nvidia.com or pypi.fury.io.',
      placeholder: '/simple',
    },
    REMOVE_QUARANTINED: {
      label: 'Filter component versions that fail Sonatype Repository Firewall policy',
      description:
        'If enabled, automatically filter component versions from metadata that fail Sonatype Repository Firewall policy at the Proxy stage.',
    },
  },

  // ---------------------------------------------------------------------------
  // RawFacet
  // ---------------------------------------------------------------------------
  RAW: {
    SECTION: {
      title: 'Raw Settings',
      description: 'Raw repository configuration',
    },
    CONTENT_DISPOSITION: {
      label: 'Content Disposition',
      helpText: 'Controls whether content is displayed inline in the browser or downloaded as an attachment',
      INLINE: 'Inline',
      ATTACHMENT: 'Attachment',
      inlineWarning:
        'Serving content inline allows uploaded HTML to render on a trusted Nexus URL, which can be exploited for phishing attacks against other users.',
    },
  },

  // ---------------------------------------------------------------------------
  // SwiftFacet
  // ---------------------------------------------------------------------------
  SWIFT: {
    REMOTE_URL_VALIDATION_ERROR: 'Remote storage URL for Swift repositories must be https://github.com/',
  },

  // ---------------------------------------------------------------------------
  // RoutingRuleFacet
  // ---------------------------------------------------------------------------
  ROUTING_RULE: {
    SECTION: {
      title: 'Routing Rule',
    },
    helpText: 'Choose a rule to restrict some requests from being served by this repository.',
    SELECT: {
      label: 'Routing Rule',
      helpText: 'Routing rule to apply to this repository',
      noneOption: 'None (allow all requests)',
    },
  },

  // ---------------------------------------------------------------------------
  // StorageFacet
  // ---------------------------------------------------------------------------
  STORAGE: {
    SECTION: {
      title: 'Storage',
    },
    BLOB_STORE: {
      label: 'Blob Store',
      selectPlaceholder: 'Select a blob store...',
      helpText: 'Select the blob store used to store repository contents',
      editHelpText: 'Blob store cannot be changed after creation',
    },
    STRICT_CONTENT_VALIDATION: {
      label: 'Strict Content Type Validation',
      description:
        'Validate that all content uploaded to this repository is of a MIME type appropriate for the repository format',
    },
    GLOBAL_DISTRIBUTION: {
      title: 'Global Distribution',
      label: 'Enable Global Distribution',
      description: 'Serve repository content through CloudFront edge locations',
      enabled: 'Global Distribution is enabled',
      servedFrom: (blobStoreName: string) => `Served from blob store: ${blobStoreName}`,
      blobStoreOff: (blobStoreName: string) =>
        `Global Distribution is unavailable because it is not enabled for the "${blobStoreName}" blob store.`,
      notSupportedForBlobStore: 'Global Distribution is only available for S3 blob stores.',
      configureBlobStore: 'Configure Blob Store',
    },
  },

  // ---------------------------------------------------------------------------
  // YumFacet
  // ---------------------------------------------------------------------------
  YUM: {
    SIGNING: {
      title: 'Yum Settings',
      description: 'GPG signing configuration for verifying Yum repodata files',
      KEYPAIR: {
        label: 'Signing Key',
        helpText: 'PGP signing key pair (armored private key e.g. gpg --export-secret-key --armor)',
      },
      PASSPHRASE: {
        label: 'Passphrase',
        helpText: 'Passphrase for the GPG signing key (leave empty if key has no passphrase)',
      },
    },
    HOSTED: {
      title: 'Yum Settings',
      description: 'Yum/RPM repository configuration',
      REPODATA_DEPTH: {
        label: 'Repodata Depth',
        helpText: 'Specifies the repository depth where repodata folder(s) are created (0-5)',
      },
      DEPLOY_POLICY: {
        label: 'Deploy Policy',
        helpText: 'Validate that RPM deployments comply with the deployed version',
        STRICT: 'Strict',
        PERMISSIVE: 'Permissive',
      },
    },
  },

  // ---------------------------------------------------------------------------
  // TerraformFacet
  // ---------------------------------------------------------------------------
  TERRAFORM: {
    SIGNING: {
      title: 'Terraform Signing',
      description: 'GPG signing configuration for Terraform hosted repositories',
      KEYPAIR: {
        label: 'GPG Signing Key',
        helpText:
          'PGP signing key pair (armored private key e.g. gpg --export-secret-key --armor)',
      },
      PASSPHRASE: {
        label: 'GPG Signing Key Passphrase',
        helpText: 'Passphrase for the PGP signing key (leave empty if key has no passphrase)',
      },
    },
  },

  // ---------------------------------------------------------------------------
  // Repository operational action confirm dialogs (shared by RepositoriesList
  // and RepositoriesPage). Copy is centralised here so the two callers cannot
  // drift apart.
  // ---------------------------------------------------------------------------
  REPOSITORY_ACTION_DIALOGS: {
    defaultConfirmLabel: 'Confirm',
    REBUILD_INDEX: {
      title: 'Rebuild Repository Index',
      message: (name: string) =>
        `Rebuild the search index for repository "${name}"? This may take some time depending on repository size.`,
      confirmLabel: 'Rebuild Index',
    },
    INVALIDATE_CACHE: {
      title: 'Invalidate Repository Cache',
      message: (name: string) =>
        `Invalidate cached metadata and content for repository "${name}"? Subsequent requests will refetch from the upstream source.`,
      confirmLabel: 'Invalidate Cache',
    },
    TAKE_OFFLINE: {
      title: 'Take Repository Offline',
      message: (name: string) =>
        `Take repository "${name}" offline? Clients will no longer be able to read from or write to this repository until it is brought back online.`,
      confirmLabel: 'Take Offline',
    },
    BRING_ONLINE: {
      title: 'Bring Repository Online',
      message: (name: string) => `Bring repository "${name}" online? Clients will be able to access it again.`,
      confirmLabel: 'Bring Online',
    },
  },

  // ---------------------------------------------------------------------------
  // RepositoriesList
  // ---------------------------------------------------------------------------
  REPOSITORIES_LIST: {
    heading: 'Repositories',
    searchPlaceholder: 'Search repositories by name...',
    resultsSummary: (shown: number, total: number) => `Showing ${shown} of ${total} repositories`,
    loading: 'Loading repositories...',
    online: 'Online',
    offline: 'Offline',
    SORT: {
      prefix: 'sort:',
      name: 'Name',
      format: 'Format',
      type: 'Type',
      status: 'Status',
    },
    COLUMNS: {
      name: 'Name',
      size: 'Size',
      type: 'Type',
      ecosystem: 'Ecosystem',
      blobStore: 'Blob Store',
      status: 'Status',
      url: 'URL',
      healthCheck: 'Health Check',
      firewallReport: 'Firewall Report',
    },
    EMPTY_BODY: {
      withFilters: 'No repositories match the current filters',
      noRepositories: 'No repositories available',
    },
    ARIA: {
      filterBar: 'Filter bar',
      clearFilter: 'Clear filter',
      copyUrl: 'Copy URL to Clipboard',
      rowActions: 'Row actions',
      viewRepository: (name: string) => `View ${name}`,
      actionsFor: (name: string) => `Actions for ${name}`,
    },
    ROW_ACTIONS: {
      edit: 'Edit',
      rebuildIndex: 'Rebuild Index',
      invalidateCache: 'Invalidate Cache',
      takeOffline: 'Take Offline',
      bringOnline: 'Bring Online',
      copyUrl: 'Copy URL',
      delete: 'Delete',
    },
    EMPTY_STATE: {
      NO_MATCH: {
        title: 'No Matching Repositories',
        description: 'No repositories match your current filters. Try adjusting your filter criteria.',
        action: 'Clear Filters',
      },
      NO_REPOSITORIES: {
        title: 'No Repositories',
        description: 'Create your first repository to start storing and managing your software components.',
        action: 'Create Repository',
        secondaryAction: 'Learn more about repositories',
        tip: 'Start with a proxy repository to cache artifacts from Maven Central or npm registry.',
      },
    },
    HELP: {
      title: 'About Repositories',
      content:
        'Repositories are storage locations where software components are stored, organized, and retrieved. Nexus supports hosted repositories for internal artifacts, proxy repositories for caching remote content, and group repositories for combining multiple repositories into a single URL.',
      docLink: 'View Documentation',
    },
    TOASTS: {
      healthCheckEnabled: (name: string) => `Health check enabled for ${name}`,
      healthCheckFailed: 'Failed to enable health check',
      analysisFailed: (name: string, message: string) => `Failed to start analysis for ${name}: ${message}`,
      loadFailed: 'Failed to load repositories',
      deleteSuccess: (name: string) => `Repository "${name}" deleted successfully`,
      deleteFailed: 'Failed to delete repository',
      rebuildStarted: (name: string) => `Repository index rebuild started for "${name}"`,
      cacheInvalidated: (name: string) => `Repository caches invalidated for "${name}"`,
      nowOnline: (name: string) => `Repository "${name}" is now online`,
      nowOffline: (name: string) => `Repository "${name}" is now offline`,
      actionFailed: 'Action failed',
      unknownError: 'Unknown error',
      urlCopied: 'URL copied to clipboard',
      urlCopyFailed: 'Failed to copy URL to clipboard',
    },
  },

  // ---------------------------------------------------------------------------
  // RepositoriesPage
  // ---------------------------------------------------------------------------
  REPOSITORIES_PAGE: {
    WIZARD_STEPS: {
      selectFormat: 'Select Format',
      selectType: 'Select Type',
      configure: 'Configure',
      enableFirewall: 'Enable Firewall',
      enableRhc: 'Enable RHC',
    },
    HEADER: {
      listTitle: 'Repositories',
      listDescription: 'Manage hosted, proxy, and group repositories',
      createRepository: 'Create Repository',
      selectTypeTitle: 'Create Repository',
      selectTypeDescription: 'Configure your new repository settings',
      createTitle: (format: string, type: string) => `Create ${format} (${type}) Repository`,
      createDescription: 'Configure settings',
      editTitle: (name: string) => `Edit ${name}`,
      editFallbackTitle: 'Details',
      editDescription: (format: string, type: string, online: boolean) =>
        `${format} • ${type} • ${online ? 'Online' : 'Offline'}`,
      loading: 'Loading...',
      manageRepositories: 'Manage repositories',
    },
    BREADCRUMBS: {
      settings: 'Settings',
      repositories: 'Repositories',
      create: 'Create',
      loading: 'Loading...',
    },
    evalBackLink: 'Hosted Repository Evaluation',
    completeLabel: 'Create Repository',
    TOASTS: {
      healthCheckEnableFailed: 'Failed to enable Health Check',
      healthCheckCouldNotEnable: (name: string, message: string) =>
        `Health Check could not be enabled for "${name}": ${message}`,
      createSuccess: (name: string) => `Repository "${name}" created successfully`,
      createFailed: 'Failed to create repository',
      movedToTrash: (name: string) => `Repository "${name}" moved to Trash`,
      deleteSuccess: (name: string) => `Repository "${name}" deleted successfully`,
      rebuildStarted: (name: string) => `Repository index rebuild started for "${name}"`,
      cacheInvalidated: (name: string) => `Repository caches invalidated for "${name}"`,
      nowOnline: (name: string) => `Repository "${name}" is now online`,
      nowOffline: (name: string) => `Repository "${name}" is now offline`,
      actionFailed: 'Action failed',
      unknownError: 'Unknown error',
    },
  },

  // ---------------------------------------------------------------------------
  // RepositoryForm
  // ---------------------------------------------------------------------------
  REPOSITORY_FORM: {
    loading: 'Loading form data...',
    SETTINGS: {
      sectionTitle: 'Repository Settings',
      nameLabel: 'Name',
      nameHelpCreate: 'A unique identifier for this repository',
      nameHelpEdit: 'Repository name cannot be changed',
      onlineLabel: 'Online',
      onlineDescription: 'Allow clients to access this repository',
    },
    TABS: {
      summary: 'Summary',
      settings: 'Settings',
      usage: 'Usage',
      firewall: 'Firewall',
      healthCheck: 'Health Check',
      audit: 'Audit',
      evaluation: 'Evaluation',
      tasksCapabilities: 'Tasks & Capabilities',
      accessSecurity: 'Access & Security',
    },
    TOOLTIPS: {
      toggleBlockedByDirty: 'Save or discard your unsaved changes before changing whether this repository is online',
      takeOffline: 'Take this repository offline',
      bringOnline: 'Bring this repository online',
      browseBlockedByDirty: 'Save or discard your unsaved changes before browsing the repository',
    },
    BUTTONS: {
      createRepository: 'Create Repository',
      saveChanges: 'Save Changes',
      cancel: 'Cancel',
      browseRepository: 'Browse Repository',
      rebuildIndex: 'Rebuild Index',
      invalidateCache: 'Invalidate Cache',
      takeOffline: 'Take Offline',
      bringOnline: 'Bring Online',
      delete: 'Delete',
    },
  },
};
