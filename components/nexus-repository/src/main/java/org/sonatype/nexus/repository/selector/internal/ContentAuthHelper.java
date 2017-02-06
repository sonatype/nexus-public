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
package org.sonatype.nexus.repository.selector.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.selector.VariableSource;

import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.storage.DatabaseThreadUtils.withOtherDatabase;
import static org.sonatype.nexus.security.BreadActions.BROWSE;

/**
 * Simple helper class that encapsulates the auth checks for reuse by the orientdb user defined functions
 * @since 3.2.1
 */
@Named
@Singleton
public class ContentAuthHelper
{
  private final VariableResolverAdapterManager variableResolverAdapterManager;

  private final ContentPermissionChecker contentPermissionChecker;

  @Inject
  public ContentAuthHelper(final VariableResolverAdapterManager variableResolverAdapterManager,
                           final ContentPermissionChecker contentPermissionChecker)
  {
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
    this.contentPermissionChecker = checkNotNull(contentPermissionChecker);
  }

  public boolean checkAssetPermissions(final ODocument asset, final String... repositoryNames) {
    String format = asset.field(AssetEntityAdapter.P_FORMAT, String.class);
    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(format);
    VariableSource variableSource = variableResolverAdapter.fromDocument(asset);
    return withOtherDatabase(() -> {
      for (String repositoryName : repositoryNames) {
        if (contentPermissionChecker.isPermitted(repositoryName, format, BROWSE, variableSource)) {
          return true;
        }
      }
      return false;
    });
  }
}
