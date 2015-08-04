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
package org.sonatype.nexus.security.filter.authz;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.router.RepositoryRouter;
import org.sonatype.sisu.goodies.common.Loggers;

import com.google.common.base.Strings;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;

/**
 * A filter that maps the targetId from the Request.
 *
 * @author cstamas
 */
public class NexusTargetMappingAuthorizationFilter
    extends AbstractNexusAuthorizationFilter
{
  /**
   * Request attribute key used to cache action, as in case of PUT verb it is doing heavy lifting.
   */
  private static final String ACTION_KEY = NexusTargetMappingAuthorizationFilter.class.getName() + ".action";

  private static final Logger log = Loggers.getLogger(NexusTargetMappingAuthorizationFilter.class);

  @Inject
  private RepositoryRouter rootRouter;

  private String pathReplacement;

  public String getPathReplacement() {
    if (pathReplacement == null) {
      pathReplacement = "";
    }

    return pathReplacement;
  }

  public void setPathReplacement(final String pathReplacement) {
    this.pathReplacement = pathReplacement;
  }

  @Nullable
  private String getResourceStorePath(final ServletRequest request) {
    String path = WebUtils.getPathWithinApplication((HttpServletRequest) request);
    if (getPathPrefix() != null) {
      final Pattern p = getPathPrefixPattern();
      final Matcher m = p.matcher(path);
      if (m.matches()) {
        path = getPathReplacement();
        // TODO: hardcoded currently
        if (path.contains("@1")) {
          path = path.replaceAll("@1", Matcher.quoteReplacement(m.group(1)));
        }
        if (path.contains("@2")) {
          path = path.replaceAll("@2", Matcher.quoteReplacement(m.group(2)));
        }
        // and so on... this will be reworked to be dynamic
      }
      else {
        // what happens here: router requests are formed as: /KIND/ID/REPO_PATH
        // where KIND = {"repositories", "groups", ...}, ID is a repo ID, and REPO_PATH is a repository path
        // being here, means we could not even match anything of these, usually having newline in string
        // as that's the only thing the "dotSTAR" regex would not match (it would match any other character)
        log.warn(formatMessage(request, "Cannot translate request to Nexus repository path, expected pattern {}"), p);
        return null;
      }
    }

    return path;
  }

  @Nullable
  private ResourceStoreRequest getResourceStoreRequest(final ServletRequest request, final boolean localOnly) {
    final String resourceStorePath = getResourceStorePath(request);
    if (resourceStorePath == null) {
      return null;
    }
    final ResourceStoreRequest rsr = new ResourceStoreRequest(resourceStorePath, localOnly, false);
    rsr.getRequestContext().put(RequestContext.CTX_AUTH_CHECK_ONLY, true);
    return rsr;
  }

  @Override
  protected String getHttpMethodAction(final ServletRequest request) {
    // caching the action as it might be costly to calculate it for PUT
    if (request.getAttribute(ACTION_KEY) == null) {
      String method = ((HttpServletRequest) request).getMethod().toLowerCase();
      // Shiro's HttpMethodPermissionFilter has fixed set of verbs and their action mapping
      // But, we do know to differentiate create/update by looking at the existence of the content item
      // This happens in the HTTP Verb is PUT, mapping to nexus otherwise corresponds to that done by Shiro
      if ("put".equals(method)) {
        // heavy handed thing
        // doing a LOCAL ONLY request to check is this exists?
        try {
          final ResourceStoreRequest storeRequest = getResourceStoreRequest(request, true);
          if (storeRequest != null) {
            rootRouter.retrieveItem(storeRequest);
          }
          // if storeRequest is null, isAccessAllowed will return false anyway
        }
        catch (ItemNotFoundException e) {
          // the path does not exists, it is a CREATE
          method = "post";
        }
        catch (AccessDeniedException e) {
          // no access for read, so chances are post or put doesn't matter
          method = "post";
        }
        catch (Exception e) {
          // Problems like IO errors and others will boil down here
          throw new IllegalStateException(formatMessage(request, "Cannot translate request to Nexus action"), e);
        }
      }
      request.setAttribute(ACTION_KEY, super.getHttpMethodAction(method));
    }
    return (String) request.getAttribute(ACTION_KEY);
  }

  @Override
  public boolean isAccessAllowed(final ServletRequest request, final ServletResponse response, final Object mappedValue)
      throws IOException
  {
    if (mappedValue != null) {
      // if we are not allowed at start, forbid it
      if (!super.isAccessAllowed(request, response, mappedValue)) {
        return false;
      }
    }
    final String action = getHttpMethodAction(request);
    try {
      final Action nxAction = Action.valueOf(action);
      final ResourceStoreRequest storeRequest = getResourceStoreRequest(request, false);
      return storeRequest != null && rootRouter.authorizePath(storeRequest, nxAction);
    }
    catch (IllegalArgumentException e) { // Enum.valueOf
      log.warn(formatMessage(request, "Cannot translate Shiro action '{}' to Nexus action"), action);
      return false;
    }
  }

  // ==

  private String formatMessage(final ServletRequest request, final String message) {
    final StringBuilder sb = new StringBuilder(message);
    if (request instanceof HttpServletRequest) {
      final HttpServletRequest hsr = (HttpServletRequest) request;
      sb.append(", request: ").append(hsr.getMethod()).append(" ").append(hsr.getRequestURL());
      final String query = hsr.getQueryString();
      if (!Strings.isNullOrEmpty(query)) {
        sb.append("?").append(query);
      }
    }
    return sb.toString();
  }
}
