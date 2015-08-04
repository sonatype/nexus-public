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
package org.sonatype.nexus.proxy.item.uid;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * The source for attributes implemented in Nexus Core.
 *
 * @author cstamas
 */
@Named("core")
@Singleton
public class CoreRepositoryItemUidAttributeSource
    implements RepositoryItemUidAttributeSource
{
  private final Map<Class<?>, Attribute<?>> coreAttributes;

  public CoreRepositoryItemUidAttributeSource() {
    Map<Class<?>, Attribute<?>> attrs = new HashMap<Class<?>, Attribute<?>>(2);

    attrs.put(IsMetacontentAttribute.class, new IsMetacontentAttribute());
    attrs.put(IsTrashMetacontentAttribute.class, new IsTrashMetacontentAttribute());
    attrs.put(IsItemAttributeMetacontentAttribute.class, new IsItemAttributeMetacontentAttribute());
    attrs.put(IsHiddenAttribute.class, new IsHiddenAttribute());
    attrs.put(IsMetadataMaintainedAttribute.class, new IsMetadataMaintainedAttribute());
    attrs.put(IsRemotelyAccessibleAttribute.class, new IsRemotelyAccessibleAttribute());
    attrs.put(IsGroupLocalOnlyAttribute.class, new IsGroupLocalOnlyAttribute());

    this.coreAttributes = Collections.unmodifiableMap(attrs);
  }

  @Override
  public Map<Class<?>, Attribute<?>> getAttributes() {
    return coreAttributes;
  }
}
