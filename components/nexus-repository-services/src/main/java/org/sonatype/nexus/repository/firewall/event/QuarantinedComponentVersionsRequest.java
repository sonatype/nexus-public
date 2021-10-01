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
package org.sonatype.nexus.repository.firewall.event;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.sonatype.nexus.repository.vulnerability.AuditRepositoryComponent;

/**
 * @since 3.35
 */
public class QuarantinedComponentVersionsRequest
{
  private final String repositoryName;

  private final Set<AuditRepositoryComponent> repositoryComponents;

  private final CompletableFuture<QuarantinedComponentVersions> quarantinedResult = new CompletableFuture<>();

  public QuarantinedComponentVersionsRequest(
      final String repositoryName,
      final Set<AuditRepositoryComponent> repositoryComponents)
  {
    this.repositoryName = repositoryName;
    this.repositoryComponents = repositoryComponents;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public Set<AuditRepositoryComponent> getRepositoryComponents() {
    return repositoryComponents;
  }

  public CompletableFuture<QuarantinedComponentVersions> getQuarantinedResult() {
    return quarantinedResult;
  }
}
