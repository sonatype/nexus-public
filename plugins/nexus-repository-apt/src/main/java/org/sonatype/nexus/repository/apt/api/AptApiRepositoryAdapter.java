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
package org.sonatype.nexus.repository.apt.api;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.internal.AptFormat;
import org.sonatype.nexus.repository.rest.api.SimpleApiRepositoryAdapter;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

/**
 * Adapter to expose Apt specific properties for the repositories REST API.
 *
 * @since 3.20
 */
@Named(AptFormat.NAME)
public class AptApiRepositoryAdapter
    extends SimpleApiRepositoryAdapter
{
  @Inject
  public AptApiRepositoryAdapter(final RoutingRuleStore routingRuleStore) {
    super(routingRuleStore);
  }

  @Override
  public AbstractApiRepository adapt(final Repository repository) {
    boolean online = repository.getConfiguration().isOnline();
    String name = repository.getName();
    String url = repository.getUrl();

    switch (repository.getType().toString()) {
      case HostedType.NAME:
        return new AptHostedApiRepository(name, url, online, getHostedStorageAttributes(repository),
            getCleanupPolicyAttributes(repository), createAptHostedRepositoriesAttributes(repository),
            createAptSigningRepositoriesAttributes(repository));
      case ProxyType.NAME:
        return new AptProxyApiRepository(name, url, online, getHostedStorageAttributes(repository),
            getCleanupPolicyAttributes(repository), createAptProxyRepositoriesAttributes(repository),
            getProxyAttributes(repository), getNegativeCacheAttributes(repository),
            getHttpClientAttributes(repository), getRoutingRuleName(repository));
    }
    return null;
  }

  private AptHostedRepositoriesAttributes createAptHostedRepositoriesAttributes(final Repository repository) {
    String distribution = repository.getConfiguration().attributes(AptFormat.NAME).get("distribution", String.class);
    return new AptHostedRepositoriesAttributes(distribution);
  }

  private AptSigningRepositoriesAttributes createAptSigningRepositoriesAttributes(final Repository repository) {
    NestedAttributesMap aptAttributes = repository.getConfiguration().attributes(AptFormat.NAME);
    String keypair = aptAttributes.get("keypair", String.class);
    String passphrase = aptAttributes.get("passphrase", String.class);
    if (!Strings2.isBlank(passphrase)) {
      return new AptSigningRepositoriesAttributes(keypair, null);
    }
    return null;
  }

  private AptProxyRepositoriesAttributes createAptProxyRepositoriesAttributes(final Repository repository) {
    NestedAttributesMap aptAttributes = repository.getConfiguration().attributes(AptFormat.NAME);
    String distribution = aptAttributes.get("distribution", String.class);
    Boolean flat = aptAttributes.get("flat", Boolean.class);
    return new AptProxyRepositoriesAttributes(distribution, flat);
  }
}
