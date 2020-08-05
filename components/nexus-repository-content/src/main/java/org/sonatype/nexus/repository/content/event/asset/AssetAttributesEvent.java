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
package org.sonatype.nexus.repository.content.event.asset;

import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AttributeChange;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;

/**
 * Event sent whenever an {@link Asset}'s attributes change.
 *
 * @since 3.26
 */
public class AssetAttributesEvent
    extends AssetUpdateEvent
{
  private final AttributeChange change;

  private final String key;

  @Nullable
  private final Object value;

  public AssetAttributesEvent(
      final Asset asset,
      final AttributeChange change,
      final String key,
      @Nullable final Object value)
  {
    super(asset);
    this.change = checkNotNull(change);
    this.key = checkNotNull(key);
    this.value = value;
  }

  public AttributeChange getChange() {
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
