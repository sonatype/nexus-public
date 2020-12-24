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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.sonatype.goodies.packageurl.PackageUrl;

/**
 * @since 3.29
 */
public class ComponentDetailsRequest
{
  private final CompletableFuture<Map<String, Date>> result = new CompletableFuture<>();

  private final String name;

  private final List<PackageUrl> packageUrls;

  public ComponentDetailsRequest(final String name, final List<PackageUrl> packageUrls) {
    this.name = name;
    this.packageUrls = packageUrls;
  }

  public CompletableFuture<Map<String, Date>> getResult() {
    return result;
  }

  public List<PackageUrl> getPackageUrls() {
    return packageUrls;
  }

  public String getName() {
    return name;
  }
}
