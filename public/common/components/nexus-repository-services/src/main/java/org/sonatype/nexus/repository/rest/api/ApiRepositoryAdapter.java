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
package org.sonatype.nexus.repository.rest.api;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.rest.api.model.FirewallAttributes;
import org.sonatype.nexus.repository.rest.api.model.SimpleApiProxyRepository;

/**
 * Formats can implement this interface to supply custom objects to use in the RepositoryApiResource.
 *
 * @since 3.20
 */
public interface ApiRepositoryAdapter
{
  AbstractApiRepository adapt(Repository repository);

  /**
   * Wraps {@link #adapt} so cross-format properties can be applied uniformly without every
   * per-format adapter having to thread them through its own constructors. Currently used to
   * populate {@link FirewallAttributes} on proxy repositories; future cross-format properties
   * should be added here too.
   * <p>
   * REST resources that present API repository representations to clients should call this
   * method instead of {@link #adapt} directly.
   */
  default AbstractApiRepository adaptDecorated(final Repository repository) {
    AbstractApiRepository result = adapt(repository);
    if (result instanceof SimpleApiProxyRepository proxy) {
      proxy.setFirewall(FirewallAttributes.fromConfiguration(repository.getConfiguration()));
    }
    return result;
  }
}
