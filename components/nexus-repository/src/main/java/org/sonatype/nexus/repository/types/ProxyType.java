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
package org.sonatype.nexus.repository.types;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Type;

import com.google.common.annotations.VisibleForTesting;

/**
 * Proxy repository type.
 *
 * @since 3.0
 */
@Named(ProxyType.NAME)
@Singleton
public class ProxyType
  extends Type
{
  public static final String NAME = "proxy";

  @VisibleForTesting
  public ProxyType() {
    super(NAME);
  }

  @Override
  public Class<?> getValidationGroup() {
    return ValidationGroup.class;
  }

  public static interface ValidationGroup {}
}
