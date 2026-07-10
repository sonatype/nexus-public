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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.sonatype.nexus.datastore.mybatis.AbstractSerializableTypeHandler;
import org.sonatype.nexus.security.NexusSimplePrincipalCollection;
import org.sonatype.nexus.security.RealmCaseMapping;
import org.sonatype.nexus.security.anonymous.AnonymousPrincipalCollection;

import org.apache.ibatis.type.TypeHandler;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;

/**
 * MyBatis {@link TypeHandler} that serializes and encrypts {@link PrincipalCollection}s to/from SQL.
 *
 * @since 3.21
 */
// not @Component
public class PrincipalCollectionTypeHandler
    extends AbstractSerializableTypeHandler<PrincipalCollection>
{
  /**
   * Allow-list for classes used by Shiro's {@code PrincipalCollection} serialization.
   * Includes Shiro internals, Nexus security classes, and common JDK collection types.
   */
  private static final Set<Class<?>> ALLOWED_CLASSES = Set.of(
      // Shiro PrincipalCollection classes
      SimplePrincipalCollection.class,
      // Nexus PrincipalCollection classes
      AnonymousPrincipalCollection.class,
      NexusSimplePrincipalCollection.class,
      RealmCaseMapping.class,
      // Common JDK types used by SimplePrincipalCollection
      LinkedHashMap.class,
      HashMap.class,
      Map.Entry[].class,
      Map.Entry.class,
      LinkedHashSet.class,
      HashSet.class,
      LinkedList.class,
      ArrayList.class,
      String.class,
      Integer.class,
      Long.class,
      Number.class);

  @Override
  protected Set<Class<?>> getAllowedClasses() {
    return ALLOWED_CLASSES;
  }
}
