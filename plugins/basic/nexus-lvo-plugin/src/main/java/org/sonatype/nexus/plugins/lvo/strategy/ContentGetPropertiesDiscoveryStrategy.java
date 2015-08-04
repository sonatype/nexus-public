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
package org.sonatype.nexus.plugins.lvo.strategy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.lvo.DiscoveryRequest;
import org.sonatype.nexus.plugins.lvo.DiscoveryResponse;
import org.sonatype.nexus.plugins.lvo.DiscoveryStrategy;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;

/**
 * This is a "local" strategy, uses Nexus content to get a Java properties file and get filtered keys from there.
 *
 * @author cstamas
 */
@Singleton
@Named("content-get-properties")
@Typed(DiscoveryStrategy.class)
public class ContentGetPropertiesDiscoveryStrategy
    extends ContentGetDiscoveryStrategy
{
  @Inject
  public ContentGetPropertiesDiscoveryStrategy(final RepositoryRegistry repositoryRegistry) {
    super(repositoryRegistry);
  }

  public DiscoveryResponse discoverLatestVersion(DiscoveryRequest request)
      throws NoSuchRepositoryException, IOException
  {
    final DiscoveryResponse dr = new DiscoveryResponse(request);
    // handle
    final StorageFileItem response = handleRequest(request);

    if (response != null) {
      final Properties properties = new Properties();
      try (InputStream content = response.getInputStream()) {
        properties.load(content);
      }

      final String keyPrefix = request.getKey() + ".";
      // repack it into response
      for (Object key : properties.keySet()) {
        final String keyString = key.toString();
        if (keyString.startsWith(keyPrefix)) {
          dr.getResponse().put(key.toString().substring(keyPrefix.length()), properties.get(key));
          dr.setSuccessful(true);
        }
      }
    }

    return dr;
  }
}
