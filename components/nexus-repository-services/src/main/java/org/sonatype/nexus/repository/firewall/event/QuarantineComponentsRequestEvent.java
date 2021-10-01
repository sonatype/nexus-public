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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.sonatype.nexus.repository.Repository;

/**
 * @since 3.35
 */
public class QuarantineComponentsRequestEvent
{
  private final CompletableFuture<List<String>> result = new CompletableFuture<>();

  private final Repository repository;

  private final List<FirewallComponent> components;

  public QuarantineComponentsRequestEvent(
      final Repository repository,
      final List<FirewallComponent> components)
  {
    this.repository = repository;
    this.components = components;
  }

  public CompletableFuture<List<String>> getResult() {
    return result;
  }

  public Repository getRepository() {
    return repository;
  }

  public List<FirewallComponent> getComponents() {
    return components;
  }

  public static class FirewallComponent
  {
    private final String hash;

    private final String path;

    private final String version;

    public FirewallComponent(final String hash, final String path, final String version) {
      this.hash = hash;
      this.path = path;
      this.version = version;
    }

    public String getHash() {
      return hash;
    }

    public String getPath() {
      return path;
    }

    public String getVersion() {
      return version;
    }
  }
}
