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
package org.sonatype.nexus.configuration.validator;

import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.validation.ValidationMessage;
import org.sonatype.configuration.validation.ValidationRequest;
import org.sonatype.configuration.validation.ValidationResponse;
import org.sonatype.nexus.configuration.model.CHttpProxySettings;
import org.sonatype.nexus.configuration.model.CMirror;
import org.sonatype.nexus.configuration.model.CPathMappingItem;
import org.sonatype.nexus.configuration.model.CRemoteAuthentication;
import org.sonatype.nexus.configuration.model.CRemoteConnectionSettings;
import org.sonatype.nexus.configuration.model.CRemoteHttpProxySettings;
import org.sonatype.nexus.configuration.model.CRemoteNexusInstance;
import org.sonatype.nexus.configuration.model.CRemoteProxySettings;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.CRepositoryGrouping;
import org.sonatype.nexus.configuration.model.CRepositoryTarget;
import org.sonatype.nexus.configuration.model.CRestApiSettings;
import org.sonatype.nexus.configuration.model.CRouting;
import org.sonatype.nexus.configuration.model.CScheduleConfig;
import org.sonatype.nexus.configuration.model.CScheduledTask;
import org.sonatype.nexus.configuration.model.CSmtpConfiguration;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.proxy.repository.LocalStatus;

import org.codehaus.plexus.util.StringUtils;

/**
 * The default configuration validator provider. It checks the model for semantical validity.
 *
 * @author cstamas
 * @deprecated see Configurable
 */
@Singleton
@Named
public class DefaultApplicationConfigurationValidator
    implements ApplicationConfigurationValidator
{
  private final Random rand = new Random(System.currentTimeMillis());

  public static final String REPOSITORY_ID_PATTERN = "^[a-zA-Z0-9_\\-\\.]+$";

  public String generateId() {
    return Long.toHexString(System.nanoTime() + rand.nextInt(2008));
  }

  public ValidationResponse validateModel(ValidationRequest request) {
    ValidationResponse response = new ApplicationValidationResponse();

    response.setContext(new ApplicationValidationContext());

    Configuration model = (Configuration) request.getConfiguration();

    ApplicationValidationContext context = (ApplicationValidationContext) response.getContext();

    response.setContext(context);

    // global conn settings
    if (model.getGlobalConnectionSettings() != null) {
      response.append(validateRemoteConnectionSettings(context, model.getGlobalConnectionSettings()));
    }
    else {
      model.setGlobalConnectionSettings(new CRemoteConnectionSettings());

      response
          .addValidationWarning(
              "Global connection settings block, which is mandatory, was missing. Reset with defaults.");

      response.setModified(true);
    }

    // remote proxy settings (optional)
    final CRemoteProxySettings rps = model.getRemoteProxySettings();
    if (rps != null) {
      if (rps.getHttpProxySettings() != null) {
        response.append(validateRemoteHttpProxySettings(context, rps.getHttpProxySettings()));
      }
      if (rps.getHttpsProxySettings() != null) {
        response.append(validateRemoteHttpProxySettings(context, rps.getHttpsProxySettings()));
      }
    }

    // rest api
    if (model.getRestApi() != null) {
      response.append(validateRestApiSettings(context, model.getRestApi()));
    }

    // nexus built-in http proxy
    if (model.getHttpProxy() != null) {
      response.append(validateHttpProxySettings(context, model.getHttpProxy()));
    }
    else {
      model.setHttpProxy(new CHttpProxySettings());

      response.addValidationWarning("The HTTP Proxy section was missing from configuration, defaulted it.");

      response.setModified(true);
    }

    // routing
    if (model.getRouting() != null) {
      response.append(validateRouting(context, model.getRouting()));
    }
    else {
      model.setRouting(new CRouting());

      response.addValidationWarning("The routing section was missing from configuration, defaulted it.");

      response.setModified(true);
    }

    // check existing reposes and check their realms
    context.addExistingRepositoryIds();

    List<CRepository> reposes = model.getRepositories();

    for (CRepository repo : reposes) {
      response.append(validateRepository(context, repo));
    }

    // check groups (optional section)
    if (model.getRepositoryGrouping() != null) {
      response.append(validateRepositoryGrouping(context, model.getRepositoryGrouping()));
    }

    // check remote nexus instances (optional section)
    if (model.getRemoteNexusInstances() != null) {
      List<CRemoteNexusInstance> instances = model.getRemoteNexusInstances();

      for (CRemoteNexusInstance instance : instances) {
        response.append(validateRemoteNexusInstance(context, instance));
      }
    }

    // check repo targets (optional section)
    if (model.getRepositoryTargets() != null) {
      List<CRepositoryTarget> targets = model.getRepositoryTargets();

      for (CRepositoryTarget target : targets) {
        response.append(validateRepositoryTarget(context, target));
      }
    }

    // check tasks (optional section)
    if (model.getTasks() != null) {
      List<CScheduledTask> tasks = model.getTasks();

      for (CScheduledTask task : tasks) {
        response.append(validateScheduledTask(context, task));
      }
    }

    response.append(validateSmtpConfiguration(context, model.getSmtpConfiguration()));

    return response;
  }

  // ---------------
  // Public

  @Override
  public ValidationResponse validateRepository(ApplicationValidationContext ctx, CRepository repo) {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    ApplicationValidationContext context = (ApplicationValidationContext) response.getContext();

    if (StringUtils.isEmpty(repo.getId())) {
      response.addValidationError(new ValidationMessage("id", "Repository ID's may not be empty!"));
    }
    else if (!repo.getId().matches(REPOSITORY_ID_PATTERN)) {
      response.addValidationError(
          new ValidationMessage("id",
              "Only letters, digits, underscores(_), hyphens(-), and dots(.) are allowed in Repository ID"));
    }
    // if repo id isn't valid, nothing below here will validate properly
    else {
      if (StringUtils.isEmpty(repo.getName())) {
        repo.setName(repo.getId());

        response.addValidationWarning(new ValidationMessage("id", "Repository with ID='" + repo.getId()
            + "' has no name, defaulted to it's ID."));

        response.setModified(true);
      }

      if (!validateLocalStatus(repo.getLocalStatus())) {
        response.addValidationError(
            new ValidationMessage("id", "LocalStatus of repository with ID='" + repo.getId()) + "' is wrong "
                + repo.getLocalStatus() + "! (Allowed values are: '" + LocalStatus.IN_SERVICE + "' and '"
                + LocalStatus.OUT_OF_SERVICE + "')");
      }
      if (context.getExistingRepositoryIds() != null) {
        if (context.getExistingRepositoryIds().contains(repo.getId())) {
          response.addValidationError(
              new ValidationMessage("id", "Repository with ID=" + repo.getId() + " already exists!"));
        }

        context.getExistingRepositoryIds().add(repo.getId());
      }

      if (context.getExistingRepositoryShadowIds() != null) {
        if (context.getExistingRepositoryShadowIds().contains(repo.getId())) {
          response.addValidationError(new ValidationMessage("id", "Repository " + repo.getId()
              + " conflicts with existing Shadow with same ID='" + repo.getId() + "'!"));
        }
      }

      if (context.getExistingRepositoryGroupIds() != null) {
        if (context.getExistingRepositoryGroupIds().contains(repo.getId())) {
          response.addValidationError(new ValidationMessage("id", "Repository " + repo.getId()
              + " conflicts with existing Group with same ID='" + repo.getId() + "'!"));
        }
      }
    }

    return response;
  }

  @Override
  public ValidationResponse validateRepositoryGrouping(ApplicationValidationContext ctx, CRepositoryGrouping settings) {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    ApplicationValidationContext context = (ApplicationValidationContext) response.getContext();

    context.addExistingPathMappingIds();

    if (settings.getPathMappings() != null) {
      for (CPathMappingItem item : settings.getPathMappings()) {
        response.append(validateGroupsSettingPathMappingItem(context, item));
      }
    }

    return response;
  }

  @Override
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

      response
          .addValidationWarning("Fixed route without groupId set, set to ALL_GROUPS to keep backward comp, ID='"
              + item.getId() + "'.");

      response.setModified(true);
    }

    if (item.getRoutePatterns() == null || item.getRoutePatterns().isEmpty()) {
      response.addValidationError("The Route with ID='" + item.getId() + "' must contain at least one Route Pattern.");
    }

    for (String regexp : item.getRoutePatterns()) {
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

    // REMOVED: check that a blocking route is not empty
    // if you delete a repo(ses) that were belonging to a route, we insist on
    // leaving the route "empty" (to save a users hardly concieved regexp) but with empty
    // repo list

    if (context.getExistingRepositoryIds() != null && context.getExistingRepositoryShadowIds() != null) {
      List<String> existingReposes = context.getExistingRepositoryIds();

      List<String> existingShadows = context.getExistingRepositoryShadowIds();

      for (String repoId : item.getRepositories()) {
        if (!existingReposes.contains(repoId) && !existingShadows.contains(repoId)) {
          response.addValidationError("The groupMapping pattern with ID=" + item.getId()
              + " refers to a nonexistent repository with repoID = " + repoId);
        }
      }
    }

    return response;
  }

  @Override
  public ValidationResponse validateHttpProxySettings(ApplicationValidationContext ctx, CHttpProxySettings settings) {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    if (settings.getPort() < 80) {
      settings.setPort(8082);

      response.addValidationWarning("The HTTP Proxy port is below 80? Settings defaulted.");

      response.setModified(true);
    }

    if (!CHttpProxySettings.PROXY_POLICY_PASS_THRU.equals(settings.getProxyPolicy())
        && !CHttpProxySettings.PROXY_POLICY_STRICT.equals(settings.getProxyPolicy())) {
      response.addValidationError("The HTTP Proxy policy settings is invalid: '" + settings.getProxyPolicy()
          + "'. Valid policies are '" + CHttpProxySettings.PROXY_POLICY_STRICT + "' and '"
          + CHttpProxySettings.PROXY_POLICY_PASS_THRU + "'.");
    }

    return response;
  }

  @Override
  public ValidationResponse validateRemoteAuthentication(ApplicationValidationContext ctx,
                                                         CRemoteAuthentication settings)
  {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    return response;
  }

  @Override
  public ValidationResponse validateRemoteConnectionSettings(ApplicationValidationContext ctx,
                                                             CRemoteConnectionSettings settings)
  {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    return response;
  }

  @Override
  public ValidationResponse validateRemoteHttpProxySettings(ApplicationValidationContext ctx,
                                                            CRemoteHttpProxySettings settings)
  {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    if (settings.getProxyPort() < 1 || settings.getProxyPort() > 65535) {
      response.addValidationError("The proxy port must be an integer between 1 and 65535!");
    }

    response.append(validateRemoteAuthentication(ctx, settings.getAuthentication()));

    return response;
  }

  @Override
  public ValidationResponse validateRepositoryMirrors(ApplicationValidationContext ctx, List<CMirror> mirrors) {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    for (CMirror mirror : mirrors) {
      if (StringUtils.isEmpty(mirror.getId())) {
        String newId = generateId();

        mirror.setId(newId);

        response
            .addValidationWarning("Fixed wrong mirror ID from '" + mirror.getId() + "' to '" + newId + "'");

        response.setModified(true);
      }

      if (StringUtils.isEmpty(mirror.getId())) {
        response.addValidationError("The Mirror may have no empty/null ID!");
      }

      if (StringUtils.isEmpty(mirror.getUrl())) {
        response.addValidationError("The Mirror may have no empty/null URL!");
      }
    }

    return response;
  }

  @Override
  public ValidationResponse validateRepositoryTarget(ApplicationValidationContext ctx, CRepositoryTarget settings) {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    ApplicationValidationContext context = (ApplicationValidationContext) response.getContext();

    if (StringUtils.isEmpty(settings.getId())) {
      response.addValidationError("The RepositoryTarget may have no empty/null ID!");
    }

    if (StringUtils.isEmpty(settings.getName())) {
      response.addValidationError("The RepositoryTarget may have no empty/null Name!");
    }

    if (StringUtils.isEmpty(settings.getContentClass())) {
      response.addValidationError("Repository target with ID='" + settings.getId()
          + "' has empty content class!");
    }

    if (context.getExistingRepositoryTargetIds() != null) {
      // check for uniqueness
      for (String id : context.getExistingRepositoryTargetIds()) {
        if (id.equals(settings.getId())) {
          response.addValidationError("This target ID is already existing!");
        }
      }
    }

    List<String> patterns = settings.getPatterns();

    if (patterns != null && patterns.size() > 0) {
      for (String pattern : patterns) {
        if (!isValidRegexp(pattern)) {
          response.addValidationError("Repository target with ID='" + settings.getId()
              + "' has invalid regexp pattern: " + pattern);
        }
      }
    }
    else {
      response.addValidationError("Repository target with ID='" + settings.getId()
          + "' has no regexp pattern defined!");
    }

    return response;
  }

  @Override
  public ValidationResponse validateRestApiSettings(ApplicationValidationContext ctx, CRestApiSettings settings) {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    return response;
  }

  @Override
  public ValidationResponse validateRouting(ApplicationValidationContext ctx, CRouting settings) {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    return response;
  }

  @Override
  public ValidationResponse validateRemoteNexusInstance(ApplicationValidationContext ctx,
                                                        CRemoteNexusInstance settings)
  {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    return response;
  }

  @Override
  public ValidationResponse validateScheduledTask(ApplicationValidationContext ctx, CScheduledTask settings) {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    ApplicationValidationContext context = (ApplicationValidationContext) response.getContext();

    response.append(validateSchedule(context, settings.getSchedule()));

    return response;
  }

  @Override
  public ValidationResponse validateSchedule(ApplicationValidationContext ctx, CScheduleConfig settings) {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    return response;
  }

  @Override
  public ValidationResponse validateSmtpConfiguration(ApplicationValidationContext ctx, CSmtpConfiguration settings) {
    ValidationResponse response = new ApplicationValidationResponse();

    if (ctx != null) {
      response.setContext(ctx);
    }

    if (StringUtils.isEmpty(settings.getHostname())) {
      ValidationMessage msg = new ValidationMessage("host", "SMTP Host is empty.");
      response.addValidationError(msg);
    }

    if (settings.getPort() < 0) {
      ValidationMessage msg = new ValidationMessage(
          "port",
          "SMTP Port is inavlid.  Enter a port greater than 0.");
      response.addValidationError(msg);
    }

    if (StringUtils.isEmpty(settings.getSystemEmailAddress())) {
      ValidationMessage msg = new ValidationMessage("systemEmailAddress", "System Email Address is empty.");
      response.addValidationError(msg);
    }

    return response;
  }

  // --------------
  // Inner stuff

  protected boolean validateLocalStatus(String ls) {
    return LocalStatus.IN_SERVICE.name().equals(ls) || LocalStatus.OUT_OF_SERVICE.name().equals(ls);
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
}
