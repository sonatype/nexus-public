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

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.security.RepositorySelector;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.BucketEntityAdapter;
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

/**
 * Custom {@link OSQLFunction} for applying a jexl expression and repository(ies) to an individual asset as part of a
 * database query.
 *
 * Required parameters for the functiona are
 * - asset (typically provided as @this)
 * - expression {a jexl expression string, i.e. "format == 'nuget'"}
 * - repository (a repository name, or a RepositorySelector based string, i.e. * or *maven2 etc.)
 * - memberRepositories (if repository is a group repository, this should be a comma seperated list of repository
 *   names that are included in the group repository, this should be all member repositories including those from
 *   child group repositories, see GroupFacet.leaftMembers, otherwise an empty string will suffice)
 *
 * @since 3.1
 */
@Named
@Singleton
public class ContentExpressionFunction
    extends OSQLFunctionAbstract
{
  public static final String NAME = "contentExpression";

  private final VariableResolverAdapterManager variableResolverAdapterManager;

  private final SelectorManager selectorManager;

  private static final Logger log = LoggerFactory.getLogger(ContentExpressionFunction.class);

  @Inject
  public ContentExpressionFunction(final VariableResolverAdapterManager variableResolverAdapterManager,
                                   final SelectorManager selectorManager)
  {
    super(NAME, 4, 4);
    this.variableResolverAdapterManager = checkNotNull(variableResolverAdapterManager);
    this.selectorManager = checkNotNull(selectorManager);
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
    return checkRepository(asset, (String) iParams[2], (String) iParams[3]) &&
        checkJexlExpression(asset, (String) iParams[1], asset.field(AssetEntityAdapter.P_FORMAT, String.class));
  }

  /**
   * Will validate any one of the following conditions are true
   * - request is for * (i.e. match anything
   * - request is for *{format} and the asset format matches
   * - request is for a single repository and the asset repository matches
   * - request is for a group repository and the asset repository is contained in the memberRepositories list
   */
  private boolean checkRepository(ODocument asset, String repository, String memberRepositories) {
    RepositorySelector repositorySelector = RepositorySelector.fromSelector(repository);

    OIdentifiable bucketId = asset.field(AssetEntityAdapter.P_BUCKET, OIdentifiable.class);
    ODocument bucket = bucketId.getRecord();
    String assetRepository = bucket.field(BucketEntityAdapter.P_REPOSITORY_NAME, String.class);
    String assetFormat = asset.field(AssetEntityAdapter.P_FORMAT, String.class);

    if (!repositorySelector.isAllRepositories()) {
      if (!Strings2.isBlank(memberRepositories)) {
        String[] members = memberRepositories.split(",");
        boolean found = false;
        for (String member : members) {
          if (member.equals(assetRepository)) {
            found = true;
            break;
          }
        }
        if (!found) {
          return false;
        }
      }
      else if (!assetRepository.equals(repositorySelector.getName())){
        return false;
      }
    }

    if (!repositorySelector.isAllFormats() && !assetFormat.equals(repositorySelector.getFormat())) {
      return false;
    }

    return true;
  }

  private boolean checkJexlExpression(final ODocument asset,
                                      final String jexlExpression,
                                      final String format)
  {
    VariableResolverAdapter variableResolverAdapter = variableResolverAdapterManager.get(format);
    VariableSource variableSource = variableResolverAdapter.fromDocument(asset);

    SelectorConfiguration selectorConfiguration = new SelectorConfiguration();

    selectorConfiguration.setAttributes(ImmutableMap.of("expression", jexlExpression));
    selectorConfiguration.setType("jexl");
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
