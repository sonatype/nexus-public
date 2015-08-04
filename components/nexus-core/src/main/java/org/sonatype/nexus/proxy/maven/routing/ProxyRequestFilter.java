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
package org.sonatype.nexus.proxy.maven.routing;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;

/**
 * Component making the decision about the request in a Maven2 proxy repository, should it be allowed to result in
 * remote storage request or not (if not present in local cache). In other words, is it "expected" that artifact
 * corresponding to incoming request exists remotely, or not.
 *
 * @author cstamas
 * @since 2.4
 */
public interface ProxyRequestFilter
{
  /**
   * Evaluates the passed in combination of {@link MavenProxyRepository} and {@link ResourceStoreRequest} and decides
   * does the prefix list (if any) of given repository allows the request to be passed to remote storage of proxy
   * repository. If allows, will return {@code true}, if not, it returns {@code false}. Still, possibility is left to
   * this method to throw some exceptions too to signal some extraordinary information, or, to provide extra
   * information why some request should result in "not found" response.
   *
   * @return {@code true} if request is allowed against remote storage of given maven repository, {@code false}
   *         otherwise.
   */
  boolean allowed(MavenProxyRepository mavenProxyRepository, ResourceStoreRequest resourceStoreRequest);
}
