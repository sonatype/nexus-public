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
package org.sonatype.nexus.testsuite.testsupport.fixtures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;

import org.junit.rules.ExternalResource;

/**
 * @since 3.next
 */
public class CleanupPolicyRule
    extends ExternalResource
{
  private Provider<CleanupPolicyStorage> cleanupPolicyStorageProvider;

  List<CleanupPolicy> policies = new ArrayList<>();

  public CleanupPolicyRule(final Provider<CleanupPolicyStorage> cleanupPolicyStorageProvider) {
    this.cleanupPolicyStorageProvider = cleanupPolicyStorageProvider;
  }

  public CleanupPolicy create(String name, Map<String, String> criteria) {
    CleanupPolicy policy = new CleanupPolicy(name, "notes", "format", "mode", criteria);
    policies.add(policy);
    return cleanupPolicyStorageProvider.get().add(policy);
  }

  @Override
  protected void after() {
    policies.forEach(cleanupPolicyStorageProvider.get()::remove);
  }
}
