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
package org.sonatype.nexus.common.node;

import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;

import org.apache.commons.io.IOUtils;

/**
 * Support class for {@link NodeAccess} to share common code.
 */
public abstract class NodeAccessSupport
    extends StateGuardLifecycleSupport
    implements NodeAccess
{
  private final CompletableFuture<String> future = CompletableFuture.supplyAsync(this::compute);

  @Override
  public CompletionStage<String> getHostName() {
    return future;
  }

  private String compute() {
    Optional<String> hostname = Optional.empty();
    try {
      Process process = Runtime.getRuntime().exec("hostname");
      process.waitFor(5, TimeUnit.SECONDS);
      if (process.exitValue() == 0) {
        try (InputStream in = process.getInputStream()) {
          hostname = Optional.ofNullable(IOUtils.toString(in, StandardCharsets.UTF_8));
        }
      }
    }
    catch (Exception e) { // NOSONAR
      log.debug("Failed retrieve hostname from external process", e);
    }

    if (hostname.isPresent()) {
      return hostname.get();
    }

    try {
      hostname = Optional.ofNullable(InetAddress.getLocalHost().getHostName());
    }
    catch (Exception e) { // NOSONAR
      log.debug("Failed to retrieve hostname from InetAddress", e);
    }

    log.error("Failed to determine hostname, using nodeId instead.");

    return hostname
        .map(String::trim)
        .orElse(getId());
  }
}
