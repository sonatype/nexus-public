/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.bolyuba.nexus.plugin.npm.group.NpmGroupRepository;
import com.bolyuba.nexus.plugin.npm.hosted.NpmHostedRepository;
import com.bolyuba.nexus.plugin.npm.proxy.NpmProxyRepository;
import com.bolyuba.nexus.plugin.npm.service.GroupMetadataService;
import com.bolyuba.nexus.plugin.npm.service.HostedMetadataService;
import com.bolyuba.nexus.plugin.npm.service.MetadataServiceFactory;
import com.bolyuba.nexus.plugin.npm.service.ProxyMetadataService;
import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link MetadataServiceFactory} implementation.
 */
@Singleton
@Named
public class MetadataServiceFactoryImpl
    extends ComponentSupport
    implements MetadataServiceFactory
{
  private final MetadataStore metadataStore;

  private final MetadataParser metadataParser;

  private final ProxyMetadataTransport proxyMetadataTransport;

  @Inject
  public MetadataServiceFactoryImpl(final MetadataStore metadataStore,
                                    final MetadataParser metadataParser,
                                    final ProxyMetadataTransport proxyMetadataTransport)
  {
    this.metadataStore = checkNotNull(metadataStore);
    this.metadataParser = checkNotNull(metadataParser);
    this.proxyMetadataTransport = checkNotNull(proxyMetadataTransport);
  }

  @VisibleForTesting
  public ProxyMetadataTransport getProxyMetadataTransport() {
    return proxyMetadataTransport;
  }

  @Override
  public HostedMetadataService createHostedMetadataService(final NpmHostedRepository npmHostedRepository) {
    return new HostedMetadataServiceImpl(npmHostedRepository, metadataStore, metadataParser);
  }

  @Override
  public ProxyMetadataService createProxyMetadataService(final NpmProxyRepository npmProxyRepository) {
    return new ProxyMetadataServiceImpl(npmProxyRepository, metadataStore,
        getProxyMetadataTransport(), metadataParser);
  }

  @Override
  public GroupMetadataService createGroupMetadataService(final NpmGroupRepository npmGroupRepository) {
    return new GroupMetadataServiceImpl(npmGroupRepository, metadataParser);
  }
}
