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
package org.sonatype.nexus.client.core.subsystem.artifact;

/**
 * The core service at URI of {@code /service/local/artifact/maven}.
 *
 * @since 2.1
 */
public interface ArtifactMaven
{

  /**
   * Performs a "resolve" operation on Nexus side, using whatever metadata targeted repository uses and returns the
   * resolved artifact or {@code null} if resolution failed.
   */
  ResolveResponse resolve(ResolveRequest req);

}
