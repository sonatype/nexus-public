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
package org.sonatype.nexus.datastore.mybatis.handlers;

import java.util.Map;

import org.sonatype.nexus.datastore.mybatis.AbstractJsonTypeHandler;

import org.apache.ibatis.type.TypeHandler;

/**
 * MyBatis {@link TypeHandler} that maps a (possibly nested) attribute map to/from JSON.
 *
 * Sensitive fields will be automatically encrypted at rest when persisting to the config store.
 *
 * @see org.sonatype.nexus.datastore.mybatis.SensitiveAttributes
 *
 * @since 3.19
 */
// not @Named because we register this manually
public class AttributesTypeHandler
    extends AbstractJsonTypeHandler<Map<String, ?>>
{
  // nothing to do
}
