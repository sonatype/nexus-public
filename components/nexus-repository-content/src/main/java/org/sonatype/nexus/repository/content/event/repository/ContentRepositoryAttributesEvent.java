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
package org.sonatype.nexus.repository.content.event.repository;

import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.content.AttributeOperation;
import org.sonatype.nexus.repository.content.ContentRepository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;

/**
 * Event sent whenever a {@link ContentRepository}'s attributes change.
 *
 * @since 3.26
 */
public class ContentRepositoryAttributesEvent
    extends ContentRepositoryUpdatedEvent
{
  private final AttributeOperation change;

  private final String key;

  @Nullable
  private final Object value;

  public ContentRepositoryAttributesEvent(
      final ContentRepository contentRepository,
      final AttributeOperation change,
      final String key,
      @Nullable final Object value)
  {
    super(contentRepository);
    this.change = checkNotNull(change);
    this.key = checkNotNull(key);
    this.value = value;
  }

  public AttributeOperation getChange() {
    return change;
  }

  public String getKey() {
    return key;
  }

  @SuppressWarnings("unchecked")
  public <T> Optional<T> getValue() {
    return ofNullable((T) value);
  }
}
