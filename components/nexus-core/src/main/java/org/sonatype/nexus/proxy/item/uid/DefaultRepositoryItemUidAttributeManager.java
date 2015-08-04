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

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.sisu.goodies.common.ComponentSupport;

/**
 * This is just a quick implementation for currently only one existing attribute: is hidden. Later this should be
 * extended.
 *
 * @author cstamas
 */
@Named
@Singleton
public class DefaultRepositoryItemUidAttributeManager
    extends ComponentSupport
    implements RepositoryItemUidAttributeManager
{
  private final Map<String, RepositoryItemUidAttributeSource> attributeSources;

  private final Map<Class<?>, Attribute<?>> attributes;

  @Inject
  public DefaultRepositoryItemUidAttributeManager(final Map<String, RepositoryItemUidAttributeSource> attributeSources) {
    this.attributeSources = attributeSources;
    this.attributes = new ConcurrentHashMap<Class<?>, Attribute<?>>();
  }

  @SuppressWarnings("unchecked")
  public <T extends Attribute<?>> T getAttribute(final Class<T> attributeKey, final RepositoryItemUid subject) {
    return (T) attributes.get(attributeKey);
  }

  public synchronized void reset() {
    attributes.clear();

    final ArrayList<String> sources = new ArrayList<String>(attributeSources.size());

    for (Map.Entry<String, RepositoryItemUidAttributeSource> attributeSourceEntry : attributeSources.entrySet()) {
      sources.add(attributeSourceEntry.getKey());

      Map<Class<?>, Attribute<?>> attrs = attributeSourceEntry.getValue().getAttributes();

      if (attrs != null) {
        attributes.putAll(attrs);
      }
    }

    if (log.isDebugEnabled()) {
      log.debug("Registered {} UID Attributes coming from following sources: {}",
          new Object[]{attributes.size(), sources.toString()});
    }
  }
}
