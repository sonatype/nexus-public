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
        label: 'RSA Signing Key',
        helpText: 'PEM encoded RSA signing key pair',
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
      retry: 'Retry',
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
};
