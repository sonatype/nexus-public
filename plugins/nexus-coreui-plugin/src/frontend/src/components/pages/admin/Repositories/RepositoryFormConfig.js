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

import GenericStorageConfiguration from './facets/GenericStorageConfiguration';
import YumHostedConfiguration from './facets/YumHostedConfiguration';
import BowerProxyConfiguration from './facets/BowerProxyConfiguration';
import RawConfiguration from './facets/RawConfiguration';
import GenericProxyConfiguration from './facets/GenericProxyConfiguration';
import GenericGroupConfiguration from './facets/GenericGroupConfiguration';
import GenericOptionsConfiguration from './facets/GenericOptionsConfiguration';
import GenericCleanupConfiguration from './facets/GenericCleanupConfiguration';
import GenericHttpAuthConfiguration from './facets/GenericHttpAuthConfiguration';
import GenericHttpReqConfiguration from './facets/GenericHttpReqConfiguration';
import GenericHostedConfiguration from './facets/GenericHostedConfiguration';
import ReplicationConfiguration from './facets/ReplicationConfiguration';

const genericProxyFacets = [
  GenericStorageConfiguration,
  GenericProxyConfiguration,
  GenericOptionsConfiguration,
  GenericCleanupConfiguration,
  GenericHttpAuthConfiguration,
  GenericHttpReqConfiguration
];

const genericHostedFacets = [
  GenericStorageConfiguration,
  GenericHostedConfiguration,
  GenericCleanupConfiguration
];

const genericGroupFacets = [GenericStorageConfiguration, GenericGroupConfiguration];

const repositoryFacets = {
  bower_proxy: [BowerProxyConfiguration, ...genericProxyFacets],
  yum_hosted: [YumHostedConfiguration, ...genericHostedFacets],
  raw_proxy: [ReplicationConfiguration, RawConfiguration, , ...genericProxyFacets],
  raw_hosted: [ReplicationConfiguration, RawConfiguration, ...genericHostedFacets],
  raw_group: [ReplicationConfiguration, RawConfiguration, ...genericGroupFacets]
};

export const getRepositoryFacets = (format, type) => {
  const recipe = `${format}_${type}`;
  if (repositoryFacets.hasOwnProperty(recipe)) {
    return repositoryFacets[recipe];
  } else if (type === 'proxy') {
    return genericProxyFacets;
  } else if (type === 'hosted') {
    return genericHostedFacets;
  } else if (type === 'group') {
    return genericGroupFacets;
  }
};
