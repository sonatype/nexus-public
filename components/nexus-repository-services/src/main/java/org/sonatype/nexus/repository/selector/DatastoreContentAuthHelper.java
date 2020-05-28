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
package org.sonatype.nexus.repository.selector;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.selector.VariableSource;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.stream;
import static org.sonatype.nexus.security.BreadActions.BROWSE;

/**
 * Simple helper class that encapsulates the auth checks for reuse across datastore related classes
 * @since 3.24
 */
@Named
@Singleton
public class DatastoreContentAuthHelper
{
  private final VariableResolverAdapterManager variableResolverAdapterManager;

  private final ContentPermissionChecker contentPermissionChecker;

  @Inject
  public DatastoreContentAuthHelper(final VariableResolverAdapterManager variableResolverAdapterManager,
                                 final ContentPermissionChecker contentPermissionChecker)
  {
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
    this.contentPermissionChecker = checkNotNull(contentPermissionChecker);
  }

  public boolean checkPathPermissions(final String path, final String format, final String... repositoryNames) {
    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(format);
    VariableSource variableSource = variableResolverAdapter.fromPath(path, format);
    return stream(repositoryNames).anyMatch(repositoryName -> contentPermissionChecker
        .isPermitted(repositoryName, format, BROWSE, variableSource));
  }

  public boolean checkPathPermissionsJexlOnly(final String path, final String format, final String... repositoryNames) {
    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(format);
    VariableSource variableSource = variableResolverAdapter.fromPath(path, format);
    return stream(repositoryNames).anyMatch(repositoryName -> contentPermissionChecker
        .isPermittedJexlOnly(repositoryName, format, BROWSE, variableSource));
  }

}
