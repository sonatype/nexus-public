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
package org.sonatype.nexus.orient;

import java.util.Locale;

import com.orientechnologies.orient.core.metadata.schema.OClass;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Helper to build {@link OClass} names.
 *
 * @since 3.0
 */
public class OClassNameBuilder
{
  public static final String PREFIX_SEPARATOR = "_";

  private String prefix;

  private String type;

  public OClassNameBuilder prefix(final String prefix) {
    this.prefix = prefix;
    return this;
  }

  public OClassNameBuilder type(final String type) {
    this.type = type;
    return this;
  }

  public OClassNameBuilder type(final Class type) {
    checkNotNull(type);
    return type(type.getSimpleName());
  }

  public String build() {
    // prefix is optional
    checkState(type != null, "Type required");

    StringBuilder buff = new StringBuilder();
    if (prefix != null) {
      buff.append(prefix);
      buff.append(PREFIX_SEPARATOR);
    }
    buff.append(type);

    // OClass names are always lower-case
    return buff.toString().toLowerCase(Locale.US);
  }
}
