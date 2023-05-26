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
package org.sonatype.nexus.repository.apt;

import java.util.Collections;
import java.util.Set;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;

/**
 * Common base for an Apt upload handlers
 *
 * @since 3.31
 */
public abstract class AptUploadHandlerSupport
    extends UploadHandlerSupport
{
  private UploadDefinition definition;

  private final VariableResolverAdapter variableResolverAdapter;

  private final ContentPermissionChecker contentPermissionChecker;

  public AptUploadHandlerSupport(final VariableResolverAdapter variableResolverAdapter,
                                 final ContentPermissionChecker contentPermissionChecker,
                                 final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
  {
    super(uploadDefinitionExtensions);
    this.variableResolverAdapter = variableResolverAdapter;
    this.contentPermissionChecker = contentPermissionChecker;
  }

  protected void doValidation(final Repository repository, final String assetPath)
  {
    ensurePermitted(repository.getName(), AptFormat.NAME, assetPath, Collections.emptyMap());
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      definition = getDefinition(AptFormat.NAME, false);
    }
    return definition;
  }

  public VariableResolverAdapter getVariableResolverAdapter() {
    return variableResolverAdapter;
  }

  public ContentPermissionChecker contentPermissionChecker() {
    return contentPermissionChecker;
  }
}
