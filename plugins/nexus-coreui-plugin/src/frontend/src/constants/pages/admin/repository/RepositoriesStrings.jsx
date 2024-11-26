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
        IQ: 'Firewall Report',
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
      GOOGLE_OPTION: 'Google',
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
            VALIDATION_ERROR: <>Subdomain field must be a minimum of 1 and maximum of 63 characters (letters, numbers, and dashes) <br /> and must start with a letter and end with a letter or digit</>
          },
          ALLOW_ANON_DOCKER_PULL: {
            LABEL: 'Allow anonymous docker pull',
            DESCR: 'Allow anonymous docker pull (Docker Bearer Token Realm required)'
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
      REMOTE_URL_EXAMPLES: {
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
  }
};
