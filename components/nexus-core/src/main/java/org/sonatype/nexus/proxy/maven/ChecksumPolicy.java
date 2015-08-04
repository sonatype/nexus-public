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
package org.sonatype.nexus.proxy.maven;

import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.ResourceStoreRequest;

/**
 * Checksum policies known in Maven1/2 repositories where checksums are available according to maven layout.
 *
 * @author cstamas
 */
public enum ChecksumPolicy
{
  /**
   * Will simply ignore remote checksums and Nexus will recalculate those.
   */
  IGNORE,

  /**
   * Will warn on bad checksums in logs but will serve what it has.
   */
  WARN,

  /**
   * In case of checksum inconsistency, Nexus will behave like STRICT, otherwise it will warn.
   */
  STRICT_IF_EXISTS,

  /**
   * In case of checksum inconsistency, Nexus will behave like the Artifact was not found -- will refuse to serve it.
   */
  STRICT;

  /**
   * Key to be used in {@link ResourceStoreRequest} context to mark per-request "override" of the
   * {@link MavenProxyRepository} checksum policy. The policy put with this key into request context will be applied
   * to given request execution (and all sub-requests, as {@link RequestContext} hierarchy is used). The use of this
   * key has effect only on Maven1/2 proxy repositories, as {@link ChecksumPolicy} itself is a Maven specific
   * property, and is globally (on repository level) controlled by getters and setters on {@link
   * MavenProxyRepository}
   * interface. This "request override" - usually to relax the policy - should be used sparingly, for cases when it's
   * known that global repository level checksum policy might pose a blocker to the tried content retrieval, but even
   * a corrupted content would not pose any downstream problems (as Nexus would shield downstream consumers by some
   * other means, like processing and checking content in-situ).
   *
   * @since 2.5
   */
  public static String REQUEST_CHECKSUM_POLICY_KEY = "request.maven.checksumPolicy";

  public boolean shouldCheckChecksum() {
    return !IGNORE.equals(this);
  }
}
