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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.AssetVariableResolver;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.collect.ImmutableMap;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.functions.OSQLFunction;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_FORMAT;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * Custom {@link OSQLFunction} for applying a jexl expression and repository(ies) to an individual asset as part of a
 * database query.
 *
 * Required parameters for the functiona are
 * - asset (typically provided as @this)
 * - expression {a jexl expression string, i.e. "format == 'nuget'"}
 * - repositorySelector (a RepositorySelector based string, i.e. * or *maven2 or repoName directly, etc.)
 * - repoToContainedGroupMap a serialized map containing all repoIds in the system, with a list of any groups that may
 *   contain the repositories
 *
 * @since 3.1
 */
@Named
@Singleton
public class ContentExpressionFunction
    extends OSQLFunctionAbstract
{
  public static final String NAME = "contentExpression";

  private static final Logger log = LoggerFactory.getLogger(ContentExpressionFunction.class);

  private final VariableResolverAdapterManager variableResolverAdapterManager;

  private final SelectorManager selectorManager;

  private final OrientContentAuthHelper contentAuthHelper;

  @Inject
  public ContentExpressionFunction(final VariableResolverAdapterManager variableResolverAdapterManager,
                                   final SelectorManager selectorManager,
                                   final OrientContentAuthHelper contentAuthHelper)
  {
    super(NAME, 4, 4);
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
    this.selectorManager = checkNotNull(selectorManager);
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
    ODocument asset = identifiable.getRecord();
    RepositorySelector repositorySelector = RepositorySelector.fromSelector((String) iParams[2]);
    String jexlExpression = (String) iParams[1];
    List<String> membersForAuth;

    //if a single repo was selected, we want to auth against that member
    if (!repositorySelector.isAllRepositories()) {
      membersForAuth = Arrays.asList(repositorySelector.getName());
    }
    //if all repos (or all of format) was selected, use the repository the asset was in, as well as any groups
    //that may contain that repository
    else {
      @SuppressWarnings("unchecked")
      Map<String, List<String>> repoToContainedGroupMap = (Map<String, List<String>>) iParams[3];

      //find the repository that matches the asset
      String assetRepository = getAssetRepository(asset);

      //if can't find it, just back out, nothing more to see here
      if (assetRepository == null) {
        log.error("Asset {} references no repository", getAssetName(asset));
        return false;
      }

      membersForAuth = repoToContainedGroupMap.get(assetRepository);

      if (membersForAuth == null) {
        log.error("Asset {} references an invalid repository: {}", getAssetName(asset), assetRepository);
        return false;
      }
    }

    return contentAuthHelper.checkPathPermissions(asset.field(P_NAME), asset.field(P_FORMAT),
        membersForAuth.toArray(new String[membersForAuth.size()])) && checkJexlExpression(asset, jexlExpression,
        asset.field(AssetEntityAdapter.P_FORMAT, String.class));
  }

  @Nullable
  private String getAssetRepository(ODocument asset) {
    OIdentifiable bucketId = asset.field(AssetEntityAdapter.P_BUCKET, OIdentifiable.class);
    ODocument bucket = bucketId.getRecord();
    return bucket.field(BucketEntityAdapter.P_REPOSITORY_NAME, String.class);
  }

  private String getAssetName(ODocument asset) {
    return asset.field(P_NAME, String.class);
  }

  private boolean checkJexlExpression(final ODocument asset,
                                      final String jexlExpression,
                                      final String format)
  {
    AssetVariableResolver assetVariableResolver = variableResolverAdapterManager.get(format);
    VariableSource variableSource = assetVariableResolver.fromDocument(asset);

    SelectorConfiguration selectorConfiguration = selectorManager.newSelectorConfiguration();

    selectorConfiguration.setAttributes(ImmutableMap.of(SelectorConfiguration.EXPRESSION, jexlExpression));
    selectorConfiguration.setType(JexlSelector.TYPE);
    selectorConfiguration.setName("preview");

    try {
      return selectorManager.evaluate(selectorConfiguration, variableSource);
    }
    catch (SelectorEvaluationException e) {
      log.debug("Unable to evaluate expression {}.", jexlExpression, e);
      return false;
    }
  }

  @Override
  public String getSyntax() {
    return NAME + "(<asset>, <string>, <string>, <string>)";
  }
}
