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
package org.sonatype.nexus.repository.content.event.component;

import java.util.List;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.store.ContentStoreEvent;
import org.sonatype.nexus.repository.content.store.internal.ComponentAuditor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Designed to inform a {@link ComponentAuditor} that some {@link Component}'s from a {@link Repository}, purged and
 * must be recorded in more informative way.
 */
public class ComponentsPurgedAuditEvent
    extends ContentStoreEvent
{
  private final List<Component> components;

  /**
   * Contains a {@link List} of purged components
   *
   * @param repositoryId - not NULL
   * @param components   - not NULL, not empty;
   * @throws NullPointerException - if components is null
   */
  public ComponentsPurgedAuditEvent(Integer repositoryId, final List<Component> components) {
    super(checkNotNull(repositoryId));
    this.components = checkNotNull(components);
  }

  public List<Component> getComponents() {
    return components;
  }
}
