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
package org.sonatype.nexus.proxy.item;

import java.io.InputStreamReader;
import java.io.StringWriter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.repository.Repository;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import static com.google.common.base.Preconditions.checkNotNull;

@Named(VelocityContentGenerator.ID)
@Singleton
public class VelocityContentGenerator
    implements ContentGenerator
{
  public static final String ID = "velocity";

  private final Provider<VelocityEngine> velocityEngineProvider;

  @Inject
  public VelocityContentGenerator(final Provider<VelocityEngine> velocityEngineProvider) {
    this.velocityEngineProvider = checkNotNull(velocityEngineProvider);
  }

  @Override
  public String getGeneratorId() {
    return ID;
  }

  @Override
  public ContentLocator generateContent(Repository repository, String path, StorageFileItem item)
      throws IllegalOperationException, ItemNotFoundException, LocalStorageException
  {
    final StringWriter sw = new StringWriter();
    final VelocityContext vctx = new VelocityContext(item.getItemContext().flatten());

    try(final InputStreamReader isr = new InputStreamReader(item.getInputStream(), "UTF-8")) {
      velocityEngineProvider.get().evaluate(vctx, sw, item.getRepositoryItemUid().toString(), isr);
      return new StringContentLocator(sw.toString());
    }
    catch (Exception e) {
      throw new LocalStorageException("Could not expand the template: " + item.getRepositoryItemUid().toString(), e);
    }
  }
}
