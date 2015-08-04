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
package org.sonatype.nexus.proxy.repository;

import java.io.IOException;
import java.util.Map;

import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.walker.AbstractWalkerProcessor;
import org.sonatype.nexus.proxy.walker.WalkerContext;

public class RecreateAttributesWalker
    extends AbstractWalkerProcessor
{
  public static final String FORCE_ATTRIBUTE_RECREATION = RecreateAttributesWalker.class.getName()
      + ".forceAttributeRecreation";

  private final Repository repository;

  private final Map<String, String> initialData;

  private boolean forceAttributeRecreation;

  public RecreateAttributesWalker(final Repository repository, final Map<String, String> initialData) {
    this.repository = repository;
    this.initialData = initialData;
  }

  public Repository getRepository() {
    return repository;
  }

  public Map<String, String> getInitialData() {
    return initialData;
  }

  // == WalkerProcessor

  @Override
  public void beforeWalk(WalkerContext context)
      throws Exception
  {
    forceAttributeRecreation = isForceAttributeRecreation(context);
  }

  @Override
  public final void processItem(WalkerContext context, StorageItem item)
      throws Exception
  {
    if (item instanceof StorageCollectionItem) {
      return; // collections have no attributes persisted
    }

    doProcessFileItem(context, item);
  }

  // == Internal

  protected void doProcessFileItem(final WalkerContext ctx, final StorageItem item)
      throws IOException
  {
    if (getInitialData() != null) {
      item.getRepositoryItemAttributes().putAll(initialData);
    }

    if (forceAttributeRecreation && item instanceof StorageFileItem) {
      getRepository().getAttributesHandler().storeAttributes(item,
          ((StorageFileItem) item).getContentLocator());
    }
    else {
      getRepository().getAttributesHandler().storeAttributes(item, null);
    }
  }

  protected boolean isForceAttributeRecreation(final WalkerContext ctx) {
    final RequestContext reqestContext = ctx.getResourceStoreRequest().getRequestContext();
    if (reqestContext.containsKey(FORCE_ATTRIBUTE_RECREATION, false)) {
      // obey the "hint"
      return Boolean.parseBoolean(String.valueOf(reqestContext.get(FORCE_ATTRIBUTE_RECREATION, false)));
    }
    else {
      // fallback to default behavior: do force it
      return true;
    }
  }
}
