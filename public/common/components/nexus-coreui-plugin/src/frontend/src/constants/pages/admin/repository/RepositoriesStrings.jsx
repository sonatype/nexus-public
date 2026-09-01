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
import { faDatabase } from '@fortawesome/free-solid-svg-icons';

export default {
  REPOSITORIES: {
    MENU: {
      text: 'Repositories',
      description: 'Create and manage repositories',
      icon: faDatabase
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
        IQ: 'Firewall Report',
      },
      FILTER_PLACEHOLDER: 'Filter by name',
      EMPTY_LIST: 'There are no repositories available',
      COPY_URL_TITLE: 'Copy URL to Clipboard',
      URL_COPIED_MESSAGE: 'URL Copied to Clipboard',
      URL_COPY_ERROR_MESSAGE: 'Failed to copy URL to clipboard',
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
        DENY: 'Read-only'
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
      PRESERVE_ENCODED_CHARS_LABEL: 'Preserve Encoded Characters',
      PRESERVE_ENCODED_CHARS_SUBLABEL: 'When checked, keeps encoded characters like %2B (plus), %23 (hash), and %20 (space) in their encoded form when proxying to the remote repository. Enable when proxying to AWS S3, Cloudflare CDN, or Azure Blob Storage.',
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
        ENFORCE_DISTRIBUTION: {
          LABEL: 'Enforce Distribution',
          DESCR: 'Only allow requests for the specified distribution'
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
        TERRAFORM: {
            CAPTION: 'Terraform Settings',
            SIGNING: {
                CAPTION: 'GPG Signing',
                KEY: {
                    LABEL: 'Signing Key',
                    SUBLABEL: 'PGP signing key pair (armored private key e.g., gpg --export-secret-key --armor)',
                    PLACEHOLDER: '-----BEGIN PGP PRIVATE KEY BLOCK-----\n...\n-----END PGP PRIVATE KEY BLOCK-----'
                },
                PASSPHRASE: {
                    LABEL: 'Passphrase',
                    SUBLABEL: 'Passphrase to access PGP signing key (leave empty if key has no passphrase)'
                }
            }
        },
        ALPINE: {
            CAPTION: 'Alpine Settings',
            SIGNING: {
                CAPTION: 'RSA Signing',
                KEY: {
                    LABEL: 'Signing Key',
                    SUBLABEL: 'RSA private key for APKINDEX signing (PEM format)',
                    PLACEHOLDER: '-----BEGIN RSA PRIVATE KEY-----\n...\n-----END RSA PRIVATE KEY-----'
                },
                PASSPHRASE: {
                    LABEL: 'Passphrase',
                    SUBLABEL: 'Passphrase to access RSA signing key (leave empty if key has no passphrase)'
                }
            }
        },
      NPM: {
        REMOVE_QUARANTINED: {
          LABEL: 'Remove Quarantined Versions',
          SUBLABEL: 'IQ Audit and Quarantine capability must be enabled for this feature to take effect.',
          DESCR: 'Remove quarantined versions from the package metadata',
          WARNING: 'This feature requires IQ Server Release 134 or higher'
        },
      },
      RAW: {
        QUERY_PARAMS: {
          CAPTION: 'Query Parameter Forwarding',
          SUBLABEL: 'Control how query parameters are forwarded to the upstream repository.',
          CHECKBOX: 'Forward query parameters to upstream',
          DESCRIPTION: 'Query parameter forwarding is disabled. All query parameters will be stripped from upstream requests.',
          DESCRIPTION_ENABLED: 'Query parameters from client requests will be forwarded to the upstream repository. You can exclude specific parameters below.',
          CACHING_WARNING_TITLE: 'Caching Behavior',
          CACHING_WARNING_CONTENT: 'When query parameter forwarding is enabled, each unique combination of query parameters is cached as a separate asset. This may increase storage usage.',
          EXAMPLES_TITLE: 'Example query parameters:',
          EXAMPLES: [
            'Versioning: ?version=1.2.3',
            'Pagination: ?page=1&limit=10',
            'Cache busting: ?v=20260313',
            'API filters: ?format=json&include_metadata=true'
          ],
          USE_CASES_TITLE: 'Common use cases:',
          USE_CASES: [
            'Proxying REST APIs that use query parameters for filtering',
            'VS Code extension marketplace with version parameters',
            'CDN resources with cache-busting query strings',
            'GitHub/GitLab release downloads with filter parameters'
          ],
          EXCLUSION_LABEL: 'Excluded Parameters',
          EXCLUSION_SUBLABEL: 'Query parameters to exclude from forwarding (case-insensitive)',
          EXCLUSION_PLACEHOLDER: 'Enter parameter name (e.g., api_key)',
          ADD_EXCLUSION: 'Add excluded parameter',
          REMOVE_EXCLUSION: 'Remove excluded parameter'
        }
      },
      FIREWALL: {
        CAPTION: 'Sonatype Nexus Firewall',
        LABEL: 'Sonatype Nexus Firewall',
        SUBLABEL: 'Requires IQ Server connection configured in Capabilities',
        WARNING: 'Requires IQ Server connection configured in Capabilities',
        MODE_LABEL: 'Firewall Mode',
        MODE_DISABLED: 'Disabled',
        MODE_AUDIT: 'Audit Only - allow components that violate policy',
        MODE_QUARANTINE: 'Quarantine - block components that violate policy',
        MODE_PCCS: 'PCCS - Quarantine, plus metadata filtering to help clients select a policy compliant version',
      },
      LEARN_MORE: 'Learn more',
      REGISTRY_API_SUPPORT_CAPTION: 'Docker Registry API Support',
      REGISTRY_API_SUPPORT_LABEL: 'Enable Docker V1 API',
      REGISTRY_API_SUPPORT_DESCR: 'Allow clients to use the V1 API to interact with this repository',
      DOCKER: {
        ECR: {
          CAPTION: 'AWS ECR Authentication',
          SESSION_TOKEN: {
            LABEL: 'AWS Session Token',
            SUBLABEL: 'Optional. Provide only when using short-lived (STS) credentials whose Access Key ID starts with "ASIA". Leave blank for long-lived IAM access keys ("AKIA").',
            PLACEHOLDER: 'Enter an AWS session token'
          },
          EXPIRY_WARNING: 'Short-lived AWS session tokens expire (typically within 12 hours) and cannot be refreshed automatically. When the token expires, ECR pulls will fail until you re-enter a fresh session token here.'
        },
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
            VALIDATION_ERROR: <>Subdomain field must be a minimum of 1 and maximum of 63 characters (letters, numbers, and dashes) <br /> and must start with a letter and end with a letter or digit</>
          },
          ALLOW_ANON_DOCKER_PULL: {
            LABEL: 'Allow Anonymous Docker Pulls for This Repository',
            DESCR: 'Allow anonymous Docker pulls for this repository (Global Anonymous Access and Docker Bearer Token Realm required)',
            HELP: <>
              Anonymous access to Docker repositories requires configuration in two places: globally on the Security → Anonymous Access page and within each Docker repository's configuration form.{' '}
              <NxTextLink external href="https://links.sonatype.com/products/nxrm3/docs/docker-authentication">
                Learn more in our Docker help documentation
              </NxTextLink>.
            </>
          },
          SAME_PORTS_ERROR: 'HTTP and HTTPS ports must be different',
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
      OCI: {
        CONNECTORS: {
          CAPTION: 'OCI Repository Connectors',
          HELP: 'Configure how OCI clients reach this repository: path-based routing, HTTP/HTTPS connector ports, optional subdomain routing, and force-basic-auth toggle.',
          PATH_ENABLED: {
            LABEL: 'Path-Based Routing',
            DESCR: 'Expose this OCI repository under /repository/<name>. Required when no dedicated connector port is configured.',
          },
          HTTP: {
            LABEL: 'HTTP Connector Port',
            SUBLABEL: 'Create an HTTP connector at the specified port. Useful behind a TLS-terminating proxy.',
            PLACEHOLDER: 'Enter a port number',
          },
          HTTPS: {
            LABEL: 'HTTPS Connector Port',
            SUBLABEL: 'Create an HTTPS connector at the specified port. Recommended for direct OCI client connections.',
            PLACEHOLDER: 'Enter a port number',
          },
          SUBDOMAIN: {
            LABEL: 'Subdomain',
            SUBLABEL: 'Optional subdomain prefix for this OCI repository',
            PLACEHOLDER: 'Enter a subdomain',
          },
          FORCE_BASIC_AUTH: {
            LABEL: 'Force Basic Authentication',
            DESCR: 'Require clients to authenticate with username/password instead of an anonymous Bearer token.',
            // NEXUS-53064 B2: surfaced as a warning banner under the checkbox so
            // admins do not silently leave a registry open to anonymous pulls.
            WARNING: 'Disabling Force Basic Authentication allows clients to pull images without authentication. Anonymous OCI pulls require both Global Anonymous Access and the Docker Bearer Token Realm to be enabled. Confirm this is intended before saving.',
          },
        },
        COSIGN: {
          CAPTION: 'Cosign Keyless Policy',
          HELP: 'Reject manifests whose Sigstore Fulcio identity or issuer does not match the configured regexes. Default mode is Off (no enforcement).',
          // NEXUS-53064 / UX P0-1: the keyless verifier is currently a no-op at
          // both upload AND pull time, so the legacy stub message would mislead
          // admins into believing partial enforcement was in effect.
          // The KEYLESS option is hidden in the dropdown (see OciConnectorSettings.jsx)
          // until a real verifier ships; this string is retained so the field
          // can be re-enabled without copy churn.
          KEYLESS_STUB_WARNING: 'Cosign keyless verification is not yet enforced. The configured identity and issuer regexes are recorded for future use, but signatures are not validated at upload OR pull time. Do not rely on this setting for supply-chain enforcement.',
          ENFORCEMENT: {
            LABEL: 'Enforcement Mode',
            SUBLABEL: 'Off (no cosign enforcement) | Keyless (require cosign signature)',
            OPTIONS: {
              NONE: 'Off (no cosign enforcement)',
              KEYLESS: 'Keyless (require cosign signature)',
            },
          },
          IDENTITY_REGEX: {
            LABEL: 'Identity Regex',
            SUBLABEL: 'Regex matched against the cosign signing identity (e.g. mailto: subject on the Fulcio certificate)',
            PLACEHOLDER: '^mailto:.*@example\\.com$',
          },
          ISSUER_REGEX: {
            LABEL: 'Issuer Regex',
            SUBLABEL: 'Regex matched against the OIDC issuer extension on the Fulcio certificate',
            PLACEHOLDER: '^https://accounts\\.example\\.com$',
          },
        },
      },
      REMOTE_URL_EXAMPLES: {
        pub: ' (e.g., https://pub.dev)',
        docker: ' (e.g., https://registry-1.docker.io)',
        maven2: ' (e.g., https://repo1.maven.org/maven2/)',
        npm: ' (e.g., https://registry.npmjs.org)',
        nuget: ' (e.g., https://api.nuget.org/v3/index.json (NuGet v3), https://community.chocolatey.org/api/v2/ (Chocolatey), or https://www.nuget.org/api/v2/ (NuGet v2))',
        r: ' (e.g., https://cran.r-project.org/)',
        pypi: ' (e.g., https://pypi.org)',
        rubygems: ' (e.g., https://rubygems.org)',
        yum: ' (e.g., https://mirror.stream.centos.org/)',
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
        SYMBOL_SERVER_URL: {
          LABEL: 'Symbol Server URL',
          SUBLABEL: 'Optional upstream symbol server URL for proxying symbol downloads (e.g. https://symbols.nuget.org/download/symbols).'
        },
        SYMSRV_ENDPOINT: {
          LABEL: 'SymSrv Endpoint URL',
          SUBLABEL: 'Configure your debugger to use this URL for symbol resolution.'
        },
        ALLOW_ANONYMOUS_SYMBOL_ACCESS: {
          LABEL: 'Allow Anonymous Symbol Access',
          SUBLABEL: 'Allow unauthenticated access to the symbol server (required for debugger integration)'
        },
        MIXED_VERSION_WARNING: (conflictingName, conflictingVersion, firstMemberName, firstMemberVersion) =>
          `Group repositories cannot include a mix of NuGet v2 and v3 members. You cannot add ${conflictingName} (${conflictingVersion}) because the group contains ${firstMemberName} (${firstMemberVersion}).`
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
  }
};
