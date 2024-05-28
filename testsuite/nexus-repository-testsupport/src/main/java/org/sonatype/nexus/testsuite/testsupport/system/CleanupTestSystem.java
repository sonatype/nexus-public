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
package org.sonatype.nexus.testsuite.testsupport.system;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyStorage;
import org.sonatype.nexus.common.event.EventManager;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class CleanupTestSystem
    extends TestSystemSupport
{
  private CleanupPolicyStorage cleanupPolicyStorage;

  private Set<String> names = new HashSet<>();

  @Inject
  public CleanupTestSystem(final CleanupPolicyStorage cleanupPolicyStorage, final EventManager eventManager) {
    super(eventManager);
    this.cleanupPolicyStorage = checkNotNull(cleanupPolicyStorage);
  }

  @Nullable
  public CleanupPolicy get(final String name) {
    return cleanupPolicyStorage.get(name);
  }

  public CleanupPolicy createCleanupPolicy(final String name, final String notes) {
    return createCleanupPolicy(name, "ALL_FORMATS", notes, Collections.emptyMap());
  }

  public CleanupPolicy createCleanupPolicy(
      final String name,
      final String format,
      final String notes,
      final Map<String, String> criteria)
  {
    CleanupPolicy policy = cleanupPolicyStorage.newCleanupPolicy();
    policy.setName(name);
    policy.setNotes(notes);
    policy.setFormat(format);
    policy.setMode("delete");
    policy.setCriteria(criteria);

    managePolicy(name);

    return cleanupPolicyStorage.add(policy);
  }

  public void managePolicy(final String name) {
    names.add(name);
  }

  @Override
  protected void doAfter() {
    names.stream()
        .map(cleanupPolicyStorage::get)
        .filter(Objects::nonNull)
        .forEach(cleanupPolicyStorage::remove);
  }
}
