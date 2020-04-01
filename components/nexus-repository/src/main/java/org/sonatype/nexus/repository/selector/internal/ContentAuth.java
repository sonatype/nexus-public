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

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.functions.OSQLFunction;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionAbstract;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Custom {@link OSQLFunction} for applying content selectors to an individual asset as part of a database query. When
 * applied to the asset, the asset itself is tested. When applied to a component, the assets under the component are
 * tested to see if at least one is visible.
 *
 * @since 3.1
 */
@Named
@Singleton
public class ContentAuth
    extends OSQLFunctionAbstract
{
  public static final String NAME = "contentAuth";

  private final OrientContentAuthHelper contentAuthHelper;

  @Inject
  public ContentAuth(final OrientContentAuthHelper contentAuthHelper)
  {
    super(NAME, 3, 4);
    this.contentAuthHelper = checkNotNull(contentAuthHelper);
  }

  @Override
  public Object execute(final Object iThis,
                        final OIdentifiable iCurrentRecord,
                        final Object iCurrentResult,
                        final Object[] iParams,
                        final OCommandContext iContext)
  {
    String path = (String) iParams[0];
    String format = (String) iParams[1];
    String browsedRepositoryName = (String) iParams[2];
    boolean jexlOnly = iParams.length > 3 && (boolean) iParams[3];

    if (jexlOnly) {
      return contentAuthHelper.checkPathPermissionsJexlOnly(path, format, browsedRepositoryName);
    }

    return contentAuthHelper.checkPathPermissions(path, format, browsedRepositoryName);
  }

  @Override
  public String getSyntax() {
    return NAME + "(path, format, repositoryName, [jexlSelectorsOnly])";
  }
}
