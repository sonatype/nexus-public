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

import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * A content generator is a special component, that is able to generate content on-the-fly, that will be substituted
 * with the content coming from the Local Storage.
 *
 * @author cstamas
 */
public interface ContentGenerator
{
  public static final String CONTENT_GENERATOR_ID = "contentGenerator";

  String getGeneratorId();

  ContentLocator generateContent(Repository repository, String path, StorageFileItem item)
      throws IllegalOperationException, ItemNotFoundException, StorageException;
}
