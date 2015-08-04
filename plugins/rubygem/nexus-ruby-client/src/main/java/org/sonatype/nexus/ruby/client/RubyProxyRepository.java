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
package org.sonatype.nexus.ruby.client;

import org.sonatype.nexus.client.core.subsystem.repository.ProxyRepository;

public interface RubyProxyRepository
    extends ProxyRepository<RubyProxyRepository>
{
  /**
   * Configures number of minutes artifacts will be cached.
   *
   * @param minutes to be cached
   * @return itself, for fluent api usage
   */
  RubyProxyRepository withArtifactMaxAge(int minutes);

  /**
   * Configures number of minutes artifact metadata will be cached.
   *
   * @param minutes to be cached
   * @return itself, for fluent api usage
   */
  RubyProxyRepository withMetadataMaxAge(int minutes);

  /**
   * @return the repository's max artifact age.
   */
  int artifactMaxAge();

  /**
   * @return the repository's max metadata age.
   */
  int metadataMaxAge();
}
