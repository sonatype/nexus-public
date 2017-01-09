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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.storage.ComponentEntityAdapter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
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

  private final ContentAuthHelper contentAuthHelper;

  @Inject
  public ContentAuth(final ContentAuthHelper contentAuthHelper)
  {
    super(NAME, 2, 2);
    this.contentAuthHelper = checkNotNull(contentAuthHelper);
  }

  @Override
  public Object execute(final Object iThis,
                        final OIdentifiable iCurrentRecord,
                        final Object iCurrentResult,
                        final Object[] iParams,
                        final OCommandContext iContext)
  {
    OIdentifiable identifiable = (OIdentifiable) iParams[0];
    ODocument document = identifiable.getRecord();
    String browsedRepositoryName = (String) iParams[1];
    switch (document.getClassName()) {
      case "asset":
        return contentAuthHelper.checkAssetPermissions(document, browsedRepositoryName);
      case "component":
        return checkComponentAssetPermissions(document, browsedRepositoryName);
      default:
        return false;
    }
  }

  private boolean checkComponentAssetPermissions(final ODocument component, final String sourceRepositoryName) {
    checkNotNull(component);
    for (ODocument asset : browseComponentAssets(component)) {
      if (contentAuthHelper.checkAssetPermissions(asset, sourceRepositoryName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String getSyntax() {
    return NAME + "(<asset|component>)";
  }

  private List<ODocument> browseComponentAssets(ODocument component) {
    checkNotNull(component);
    OIdentifiable bucket = component.field(ComponentEntityAdapter.P_BUCKET, OIdentifiable.class);
    ODatabaseDocumentInternal db = component.getDatabase();
    Iterable<ODocument> results = db
        .command(new OCommandSQL("select from asset where bucket = :bucket and component = :component")).execute(
            new ImmutableMap.Builder<String, Object>()
                .put("bucket", bucket.getIdentity())
                .put("component", component.getIdentity())
                .build()
        );
    return Lists.newArrayList(results);
  }
}
