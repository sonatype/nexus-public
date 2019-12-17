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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 3.20
 */
public class CleanupPolicyRule
    extends ExternalResource
{
  private static final Logger log = LoggerFactory.getLogger(CleanupPolicyRule.class);

  private final Provider<CleanupPolicyStorage> cleanupPolicyStorageProvider;

  private final List<CleanupPolicy> cleanupPolicies = new ArrayList<>();

  public CleanupPolicyRule(final Provider<CleanupPolicyStorage> cleanupPolicyStorageProvider) {
    this.cleanupPolicyStorageProvider = cleanupPolicyStorageProvider;
  }

  public CleanupPolicy create(final String name, final Map<String, String> criteria) {
    return createCleanupPolicy(name, "format", "mode", criteria);
  }


  public CleanupPolicy createCleanupPolicy(
      final String name,
      final String format,
      final String mode,
      final Map<String, String> criteria)
  {
    CleanupPolicyStorage storage = cleanupPolicyStorageProvider.get();
    CleanupPolicy policy = storage.newCleanupPolicy();
    policy.setName(name);
    policy.setNotes("notes");
    policy.setFormat(format);
    policy.setMode(mode);
    policy.setCriteria(criteria);

    storage.add(policy);
    cleanupPolicies.add(policy);

    return policy;
  }

  @Override
  protected void after() {
    cleanupPolicies.forEach(cleanupPolicy -> {
      try {
        cleanupPolicyStorageProvider.get().remove(cleanupPolicy);
      }
      catch (Exception e) {
        log.error("Failed to remove CleanupPolicy {}", cleanupPolicy, e);
      }
    });
  }
}
