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
package org.sonatype.nexus.repository.content.fluent;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.content.AttributeOperation;

import static org.sonatype.nexus.repository.content.AttributeOperation.REMOVE;
import static org.sonatype.nexus.repository.content.AttributeOperation.SET;

/**
 * Fluent API for repository content attributes.
 *
 * @since 3.21
 */
public interface FluentAttributes<A extends FluentAttributes<A>>
{
  /**
   * Sets the given attribute, overwriting any existing value.
   */
  default A withAttribute(String key, Object value) {
    return attributes(SET, key, value);
  }

  /**
   * Removes the given attribute from the current attributes.
   */
  default A withoutAttribute(String key) {
    return attributes(REMOVE, key, null);
  }

  /**
   * Applies the given change to the current attributes.
   */
  A attributes(AttributeOperation change, String key, @Nullable Object value);

}
