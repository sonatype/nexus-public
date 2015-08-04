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
package org.sonatype.nexus.configuration.model;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.validator.ApplicationValidationContext;
import org.sonatype.nexus.configuration.validator.ApplicationValidationResponse;

import org.codehaus.plexus.util.StringUtils;

public class CRepositoryGroupingCoreConfiguration
    extends AbstractCoreConfiguration<CRepositoryGrouping>
{
  public CRepositoryGroupingCoreConfiguration(ApplicationConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected CRepositoryGrouping extractConfiguration(Configuration configuration) {
    return configuration.getRepositoryGrouping();
  }

  @Override
  public ValidationResponse doValidateChanges(CRepositoryGrouping changedConfiguration) {
    CRepositoryGrouping settings = (CRepositoryGrouping) changedConfiguration;

    ValidationResponse response = new ApplicationValidationResponse();

    ApplicationValidationContext context = (ApplicationValidationContext) response.getContext();

    context.addExistingPathMappingIds();

    if (settings.getPathMappings() != null) {
      for (CPathMappingItem item : (List<CPathMappingItem>) settings.getPathMappings()) {
        response.append(validateGroupsSettingPathMappingItem(context, item));
      }
    }

    return response;
  }

  // ==

  private Random rand = new Random(System.currentTimeMillis());

  public String generateId() {
    return Long.toHexString(System.nanoTime() + rand.nextInt(2008));
  }

  protected boolean isValidRegexp(String regexp) {
    if (regexp == null) {
      return false;
    }

    try {
      Pattern.compile(regexp);

      return true;
    }
    catch (PatternSyntaxException e) {
      return false;
    }
  }

  public ValidationResponse validateGroupsSettingPathMappingItem(ApplicationValidationContext ctx,
                                                                 CPathMappingItem item)
  {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    ApplicationValidationContext context = (ApplicationValidationContext) response.getContext();

    if (StringUtils.isEmpty(item.getId())
        || "0".equals(item.getId())
        || (context.getExistingPathMappingIds() != null && context.getExistingPathMappingIds().contains(
        item.getId()))) {
      String newId = generateId();

      item.setId(newId);

      response.addValidationWarning("Fixed wrong route ID from '" + item.getId() + "' to '" + newId + "'");

      response.setModified(true);
    }

    if (StringUtils.isEmpty(item.getGroupId())) {
      item.setGroupId(CPathMappingItem.ALL_GROUPS);

      response.addValidationWarning("Fixed route without groupId set, set to ALL_GROUPS to keep backward comp, ID='"
          + item.getId() + "'.");

      response.setModified(true);
    }

    if (item.getRoutePatterns() == null || item.getRoutePatterns().isEmpty()) {
      response.addValidationError("The Route with ID='" + item.getId()
          + "' must contain at least one Route Pattern.");
    }

    for (String regexp : (List<String>) item.getRoutePatterns()) {
      if (!isValidRegexp(regexp)) {
        response.addValidationError("The regexp in Route with ID='" + item.getId() + "' is not valid: "
            + regexp);
      }
    }

    if (context.getExistingPathMappingIds() != null) {
      context.getExistingPathMappingIds().add(item.getId());
    }

    if (!CPathMappingItem.INCLUSION_RULE_TYPE.equals(item.getRouteType())
        && !CPathMappingItem.EXCLUSION_RULE_TYPE.equals(item.getRouteType())
        && !CPathMappingItem.BLOCKING_RULE_TYPE.equals(item.getRouteType())) {
      response.addValidationError("The groupMapping pattern with ID=" + item.getId()
          + " have invalid routeType='" + item.getRouteType() + "'. Valid route types are '"
          + CPathMappingItem.INCLUSION_RULE_TYPE + "', '" + CPathMappingItem.EXCLUSION_RULE_TYPE + "' and '"
          + CPathMappingItem.BLOCKING_RULE_TYPE + "'.");
    }

    if (!CPathMappingItem.BLOCKING_RULE_TYPE.equals(item.getRouteType())) {
      // NOT TRUE ANYMORE:
      // if you delete a repo(ses) that were belonging to a route, we insist on
      // leaving the route "empty" (to save a users hardly concieved regexp) but with empty
      // repo list

      // here we must have a repo list
      // if ( item.getRepositories() == null || item.getRepositories().size() == 0 )
      // {
      // response.addValidationError( "The repository list in Route with ID='" + item.getId()
      // + "' is not valid: it cannot be empty!" );
      // }
    }

    if (context.getExistingRepositoryIds() != null && context.getExistingRepositoryShadowIds() != null) {
      List<String> existingReposes = context.getExistingRepositoryIds();

      List<String> existingShadows = context.getExistingRepositoryShadowIds();

      for (String repoId : (List<String>) item.getRepositories()) {
        if (!existingReposes.contains(repoId) && !existingShadows.contains(repoId)) {
          response.addValidationError("The groupMapping pattern with ID=" + item.getId()
              + " refers to a nonexistent repository with repoID = " + repoId);
        }
      }
    }

    return response;
  }
}
