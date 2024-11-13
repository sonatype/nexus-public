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
import GenericStorageConfiguration from './facets/GenericStorageConfiguration';
import RewritePackageUrlsConfiguration from './facets/RewritePackageUrlsConfiguration';
import ContentDespositionConfiguration from './facets/ContentDespositionConfiguration';
import GenericProxyConfiguration from './facets/GenericProxyConfiguration';
import GenericGroupConfiguration from './facets/GenericGroupConfiguration';
import GenericOptionsConfiguration from './facets/GenericOptionsConfiguration';
import GenericCleanupConfiguration from './facets/GenericCleanupConfiguration';
import GenericHttpAuthConfiguration from './facets/GenericHttpAuthConfiguration';
import GenericHttpReqConfiguration from './facets/GenericHttpReqConfiguration';
import GenericHostedConfiguration from './facets/GenericHostedConfiguration';
import ReplicationConfiguration from './facets/ReplicationConfiguration';
import RepodataDepthConfiguration from './facets/RepodataDepthConfiguration';
import RpmDeployPolicyConfiguration from './facets/RpmDeployPolicyConfiguration';
import VersionPolicyConfiguration from './facets/VersionPolicyConfiguration';
import NpmConfiguration from './facets/NpmConfiguration';
import LayoutPolicyConfiguration from './facets/LayoutPolicyConfiguration';
import RepositoryConnectorsConfiguration from './facets/RepositoryConnectorsConfiguration';
import RegistryApiSupportConfiguration from './facets/RegistryApiSupportConfiguration';
import NugetProxyConfiguration from './facets/NugetProxyConfiguration';
import NugetGroupConfiguration from './facets/NugetGroupConfiguration';
import WritableRepositoryConfiguration from './facets/WritableRepositoryConfiguration';
import PreEmptiveAuthConfiguration from './facets/PreEmptiveAuthConfiguration';
import AptDistributionConfiguration from './facets/AptDistributionConfiguration';
import AptSigningConfiguration from './facets/AptSigningConfiguration';
import AptFlatConfiguration from './facets/AptFlatConfiguration';

import {genericDefaultValues} from './RepositoryFormDefaultValues';
import {
  genericValidators,
  validateDockerConnectorPort,
  validateDockerIndexUrl,
  validateNugetQueryCacheItemMaxAge,
  validateWritableMember,
  validateDockerSubdomain
} from './RepositoryFormValidators';

import {mergeDeepRight} from 'ramda';

import {ValidationUtils} from '@sonatype/nexus-ui-plugin';

export const DOCKER_INDEX_TYPES = {
  registry: 'REGISTRY',
  hub: 'HUB',
  custom: 'CUSTOM'
};

// temp
const isCapabilityEnabled = false;
const replicationDefaultValue = isCapabilityEnabled ? {replication: {enabled: false}} : null;

const genericFacets = {
  proxy: [
    GenericStorageConfiguration,
    GenericProxyConfiguration,
    GenericOptionsConfiguration,
    GenericCleanupConfiguration,
    GenericHttpAuthConfiguration,
    GenericHttpReqConfiguration
  ],
  hosted: [GenericStorageConfiguration, GenericHostedConfiguration, GenericCleanupConfiguration],
  group: [GenericStorageConfiguration, GenericGroupConfiguration]
};

const repositoryFormats = {
  apt_hosted: {
    facets: [AptDistributionConfiguration, AptSigningConfiguration, ...genericFacets.hosted],
    defaultValues: {
      ...genericDefaultValues.hosted,
      apt: {
        distribution: null
      },
      aptSigning: {
        keypair: null,
        passphrase: null
      }
    },
    validators: (data) => ({
      ...genericValidators.hosted(data),
      apt: {
        distribution: ValidationUtils.validateNotBlank(data.apt?.distribution)
      },
      aptSigning: {
        keypair: ValidationUtils.validateNotBlank(data.aptSigning?.keypair),
      }
    })
  },
  apt_proxy: {
    facets: [AptDistributionConfiguration, AptFlatConfiguration, ...genericFacets.proxy],
    defaultValues: {
      ...genericDefaultValues.proxy,
      apt: {
        distribution: null,
        flat: false
      }
    },
    validators: (data) => ({
      ...genericValidators.proxy(data),
      apt: {
        distribution: ValidationUtils.validateNotBlank(data.apt?.distribution)
      }
    })
  },
  yum_hosted: {
    facets: [RepodataDepthConfiguration, RpmDeployPolicyConfiguration, ...genericFacets.hosted],
    defaultValues: {
      ...genericDefaultValues.hosted,
      yum: {
        repodataDepth: 0,
        deployPolicy: 'STRICT'
      }
    },
    validators: (data) => ({
      ...genericValidators.hosted(data)
    })
  },
  raw_proxy: {
    facets: [ContentDespositionConfiguration, ...genericFacets.proxy],
    defaultValues: {
      ...genericDefaultValues.proxy,
      ...replicationDefaultValue,
      raw: {contentDisposition: 'ATTACHMENT'}
    },
    validators: (data) => ({
      ...genericValidators.proxy(data)
    })
  },
  raw_hosted: {
    facets: [ContentDespositionConfiguration, ...genericFacets.hosted],
    defaultValues: {
      ...mergeDeepRight(genericDefaultValues.hosted, {
        storage: {strictContentTypeValidation: false}
      }),
      ...replicationDefaultValue,
      raw: {contentDisposition: 'ATTACHMENT'}
    },
    validators: (data) => ({
      ...genericValidators.hosted(data)
    })
  },
  raw_group: {
    facets: [ContentDespositionConfiguration, ...genericFacets.group],
    defaultValues: {
      ...genericDefaultValues.group,
      ...replicationDefaultValue,
      raw: {contentDisposition: 'ATTACHMENT'}
    },
    validators: (data) => ({
      ...genericValidators.group(data)
    })
  },
  maven2_proxy: {
    facets: [
      VersionPolicyConfiguration,
      LayoutPolicyConfiguration,
      ContentDespositionConfiguration,
      GenericStorageConfiguration,
      GenericProxyConfiguration,
      GenericOptionsConfiguration,
      GenericCleanupConfiguration,
      GenericHttpAuthConfiguration,
      PreEmptiveAuthConfiguration,
      GenericHttpReqConfiguration
    ],
    defaultValues: {
      ...genericDefaultValues.proxy,
      ...replicationDefaultValue,
      proxy: {
        ...genericDefaultValues.proxy.proxy,
        contentMaxAge: -1
      },
      maven: {
        layoutPolicy: 'STRICT',
        contentDisposition: 'INLINE',
        versionPolicy: 'RELEASE'
      }
    },
    validators: (data) => ({
      ...genericValidators.proxy(data)
    })
  },
  maven2_hosted: {
    facets: [
      VersionPolicyConfiguration,
      LayoutPolicyConfiguration,
      ContentDespositionConfiguration,
      ...genericFacets.hosted
    ],
    defaultValues: {
      ...genericDefaultValues.hosted,
      ...replicationDefaultValue,
      maven: {
        layoutPolicy: 'STRICT',
        contentDisposition: 'INLINE',
        versionPolicy: 'RELEASE'
      }
    },
    validators: (data) => ({
      ...genericValidators.hosted(data)
    })
  },
  maven2_group: {
    facets: [
      VersionPolicyConfiguration,
      LayoutPolicyConfiguration,
      ContentDespositionConfiguration,
      ...genericFacets.group
    ],
    defaultValues: {
      ...genericDefaultValues.group,
      ...replicationDefaultValue,
      maven: {
        layoutPolicy: 'STRICT',
        contentDisposition: 'INLINE',
        versionPolicy: 'RELEASE'
      }
    },
    validators: (data) => ({
      ...genericValidators.group(data)
    })
  },
  npm_proxy: {
    facets: [NpmConfiguration, ...genericFacets.proxy],
    defaultValues: {
      ...genericDefaultValues.proxy,
      npm: {
        removeQuarantined: false
      }
    },
    validators: (data) => ({
      ...genericValidators.proxy(data)
    })
  },
  docker_proxy: {
    facets: [
      RepositoryConnectorsConfiguration,
      RegistryApiSupportConfiguration,
      ...genericFacets.proxy
    ],
    defaultValues: {
      ...genericDefaultValues.proxy,
      docker: {
        httpPort: null,
        httpsPort: null,
        forceBasicAuth: false,
        v1Enabled: false,
        subdomain: null
      },
      dockerProxy: {
        indexType: DOCKER_INDEX_TYPES.registry,
        indexUrl: null,
        cacheForeignLayers: false,
        foreignLayerUrlWhitelist: []
      }
    },
    validators: (data) => ({
      ...genericValidators.proxy(data),
      docker: {
        httpPort: validateDockerConnectorPort(data, 'httpPort'),
        httpsPort: validateDockerConnectorPort(data, 'httpsPort'),
        subdomain: validateDockerSubdomain(data)
      },
      dockerProxy: {
        indexUrl: validateDockerIndexUrl(data)
      }
    })
  },
  docker_hosted: {
    facets: [
      RepositoryConnectorsConfiguration,
      RegistryApiSupportConfiguration,
      ...genericFacets.hosted
    ],
    defaultValues: {
      ...mergeDeepRight(genericDefaultValues.hosted, {
        storage: {writePolicy: 'ALLOW'}
      }),
      docker: {
        httpPort: null,
        httpsPort: null,
        forceBasicAuth: false,
        v1Enabled: false,
        subdomain: null
      }
    },
    validators: (data) => ({
      docker: {
        httpPort: validateDockerConnectorPort(data, 'httpPort'),
        httpsPort: validateDockerConnectorPort(data, 'httpsPort'),
        subdomain: validateDockerSubdomain(data)
      }
    })
  },
  docker_group: {
    facets: [
      RepositoryConnectorsConfiguration,
      RegistryApiSupportConfiguration,
      ...genericFacets.group,
      WritableRepositoryConfiguration
    ],
    defaultValues: {
      ...mergeDeepRight(genericDefaultValues.group, {
        group: {writableMember: null}
      }),
      docker: {
        httpPort: null,
        httpsPort: null,
        forceBasicAuth: false,
        v1Enabled: false,
        subdomain: null
      }
    },
    validators: (data) => ({
      ...mergeDeepRight(genericValidators.group(data), {
        group: {writableMember: validateWritableMember(data)}
      }),
      docker: {
        httpPort: validateDockerConnectorPort(data, 'httpPort'),
        httpsPort: validateDockerConnectorPort(data, 'httpsPort'),
        subdomain: validateDockerSubdomain(data)
      }
    })
  },
  nuget_proxy: {
    facets: [NugetProxyConfiguration, ...genericFacets.proxy],
    defaultValues: {
      ...genericDefaultValues.proxy,
      nugetProxy: {
        queryCacheItemMaxAge: 3600,
        nugetVersion: 'V3'
      }
    },
    validators: (data) => ({
      ...genericValidators.proxy(data),
      nugetProxy: {
        queryCacheItemMaxAge: validateNugetQueryCacheItemMaxAge(data)
      }
    })
  },
  nuget_group: {
    facets: [GenericStorageConfiguration, NugetGroupConfiguration],
    defaultValues: {
      ...genericDefaultValues.group
    },
    validators: (data) => ({
      ...genericValidators.group(data)
    })
  }
};

export const getFacets = (format, type) =>
  repositoryFormats[`${format}_${type}`]?.facets || genericFacets[type];

export const getDefaultValues = (format, type) =>
  repositoryFormats[`${format}_${type}`]?.defaultValues || genericDefaultValues[type];

export const getValidators = (format, type) =>
  repositoryFormats[`${format}_${type}`]?.validators || genericValidators[type] || (() => ({}));
