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
package org.sonatype.nexus.rest.repositories;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.model.NexusRepositoryTypeListResource;
import org.sonatype.nexus.rest.model.NexusRepositoryTypeListResourceResponse;
import org.sonatype.nexus.templates.Template;
import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.AbstractRepositoryTemplate;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

@Named
@Singleton
@Path(RepositoryTypesComponentListPlexusResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class RepositoryTypesComponentListPlexusResource
    extends AbstractNexusPlexusResource
{
  public static final String RESOURCE_URI = "/components/repo_types";

  private static final Pattern BRACKETS_PATTERN = Pattern.compile("(.*)( \\(.*\\))");

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(getResourceUri(), "authcBasic,perms[nexus:componentsrepotypes]");
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  /**
   * Retrieve the list of repository types available in Nexus.
   *
   * @param repoType The type of repository to retrieve providers for. (valid values are 'hosted', 'proxy', 'shadow'
   *                 and 'group').
   */
  @Override
  @GET
  @ResourceMethodSignature(queryParams = {@QueryParam("repoType")},
      output = NexusRepositoryTypeListResourceResponse.class)
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    Form form = request.getResourceRef().getQueryAsForm();

    // such horrible terminology for this class, its actually repo providers that are being returned
    String repoType = form.getFirstValue("repoType");

    TemplateSet templateSet = getRepositoryTemplates();

    if ("hosted".equals(repoType)) {
      templateSet = templateSet.getTemplates(HostedRepository.class);
    }
    else if ("proxy".equals(repoType)) {
      templateSet = templateSet.getTemplates(ProxyRepository.class);
    }
    else if ("shadow".equals(repoType)) {
      templateSet = templateSet.getTemplates(ShadowRepository.class);
    }
    else if ("group".equals(repoType)) {
      templateSet = templateSet.getTemplates(GroupRepository.class);
    }

    NexusRepositoryTypeListResourceResponse result = new NexusRepositoryTypeListResourceResponse();

    if (templateSet.getTemplatesList().isEmpty()) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
    }

    for (Template template : templateSet.getTemplatesList()) {
      NexusRepositoryTypeListResource resource = new NexusRepositoryTypeListResource();

      String providerRole = ((RepositoryTemplate) template).getRepositoryProviderRole();
      String providerHint = ((RepositoryTemplate) template).getRepositoryProviderHint();

      resource.setProvider(providerHint);

      resource.setProviderRole(providerRole);

      resource.setFormat(((AbstractRepositoryTemplate) template).getContentClass().getId());

      // To not disturb the "New repo UI", it's shitty right now: we select templates here that predefines is
      // something a "release" or "snapshot", but
      // UI allows to select that too.
      resource.setDescription(removeBrackets(template.getDescription()));

      // add it to the collection
      result.addData(resource);
    }

    return result;
  }

  protected String removeBrackets(String val) {
    Matcher m = BRACKETS_PATTERN.matcher(val);

    if (m.matches() && m.groupCount() == 2) {
      return m.group(1);
    }
    else {
      return val;
    }
  }
}
