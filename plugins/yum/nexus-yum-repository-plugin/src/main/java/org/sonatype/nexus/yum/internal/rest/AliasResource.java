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
package org.sonatype.nexus.yum.internal.rest;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.nexus.plugins.capabilities.CapabilityNotFoundException;
import org.sonatype.nexus.plugins.capabilities.CapabilityReference;
import org.sonatype.nexus.plugins.capabilities.CapabilityRegistry;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.yum.Yum;
import org.sonatype.nexus.yum.YumHosted;
import org.sonatype.nexus.yum.YumRegistry;
import org.sonatype.nexus.yum.internal.capabilities.GenerateMetadataCapability;
import org.sonatype.nexus.yum.internal.capabilities.GenerateMetadataCapabilityConfiguration;
import org.sonatype.nexus.yum.internal.capabilities.GenerateMetadataCapabilityDescriptor;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import org.restlet.Context;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.restlet.data.MediaType.TEXT_PLAIN;
import static org.restlet.data.Method.POST;
import static org.restlet.data.Status.CLIENT_ERROR_BAD_REQUEST;
import static org.restlet.data.Status.CLIENT_ERROR_NOT_FOUND;

/**
 * Resource providing an aliases view on the repositories provided by Nexus. That means, that you can configure
 * serveral
 * aliases for artifact versions. E.g. you can introduce "trunk", "testing" and "production" aliases for the versions
 * "91.0.0", "90.0.0" and "89.0.0" and can access the RPMs via
 * http://localhost:8080/nexus/service/local/yum-alias/<repo-id>/alias.rpm
 *
 * @since yum 3.0
 */
@Path(AliasResource.RESOURCE_URI)
@Produces({"application/xml", "application/json", "text/plain"})
@Named
@Singleton
public class AliasResource
    extends AbstractNexusPlexusResource
    implements PlexusResource
{

  public static final String URL_PREFIX = "yum/alias";

  private static final String PATH_PATTERN_TO_PROTECT = "/" + URL_PREFIX + "/**";

  public static final String REPOSITORY_ID_PARAM = "repositoryId";

  public static final String ALIAS_PARAM = "alias";

  public static final String RESOURCE_URI = "/" + URL_PREFIX + "/{" + REPOSITORY_ID_PARAM + "}/{" + ALIAS_PARAM + "}";

  private final YumRegistry yumRegistry;

  private final CapabilityRegistry capabilityRegistry;

  @Inject
  public AliasResource(final YumRegistry yumRegistry,
                       final CapabilityRegistry capabilityRegistry)
  {
    this.yumRegistry = checkNotNull(yumRegistry);
    this.capabilityRegistry = checkNotNull(capabilityRegistry);
    setModifiable(true);
    setRequireStrictChecking(false);
  }

  @Override
  public Object get(Context context, Request request, Response response, Variant variant)
      throws ResourceException
  {
    final String repositoryId = getAttributeAsString(request, REPOSITORY_ID_PARAM);
    if (repositoryId == null) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Repository Id must be specified");
    }

    String alias = getAttributeAsString(request, ALIAS_PARAM);
    if (alias == null) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Alias must be specified");
    }

    final Yum yum = yumRegistry.get(repositoryId);
    if (yum == null) {
      throw new ResourceException(
          CLIENT_ERROR_NOT_FOUND, "Could not find Yum repository with id '" + repositoryId + "'"
      );
    }

    if (!(yum instanceof YumHosted)) {
      throw new ResourceException(
          CLIENT_ERROR_BAD_REQUEST, "Repository " + repositoryId + " does not support versions"
      );
    }

    YumHosted yumHosted = (YumHosted) yum;

    final String version = yumHosted.getVersion(alias);

    if (version == null) {
      throw new ResourceException(
          Status.CLIENT_ERROR_NOT_FOUND,
          "Could not find alias '" + alias + "' for repository '" + repositoryId + "'"
      );
    }

    return new StringRepresentation(version);
  }

  @Override
  public Object post(Context context, Request request, Response response, Object payload)
      throws ResourceException
  {
    final String repositoryId = getAttributeAsString(request, REPOSITORY_ID_PARAM);
    if (repositoryId == null) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Repository Id must be specified");
    }

    String alias = getAttributeAsString(request, ALIAS_PARAM);
    if (alias == null) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Alias must be specified");
    }

    if ((payload == null) || !String.class.isAssignableFrom(payload.getClass())) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Please provide a valid artifact version.");
    }

    final Yum yum = yumRegistry.get(repositoryId);
    if (yum == null) {
      throw new ResourceException(
          CLIENT_ERROR_NOT_FOUND, "Could not find Yum repository with id '" + repositoryId + "'"
      );
    }

    final Collection<? extends CapabilityReference> capabilities =
        capabilityRegistry.get(new Predicate<CapabilityReference>()
        {
          @Override
          public boolean apply(final CapabilityReference reference) {
            final String capabilityRepositoryId = reference.context().properties().get(
                GenerateMetadataCapabilityConfiguration.REPOSITORY_ID
            );
            return GenerateMetadataCapabilityDescriptor.TYPE.equals(reference.context().type())
                && repositoryId.equals(capabilityRepositoryId);
          }
        });

    if (capabilities.isEmpty()) {
      throw new ResourceException(
          CLIENT_ERROR_NOT_FOUND, "Could not find Yum repository with id '" + repositoryId + "'"
      );
    }

    final CapabilityReference capabilityReference = capabilities.iterator().next();
    final GenerateMetadataCapabilityConfiguration configuration =
        capabilityReference.capabilityAs(GenerateMetadataCapability.class).getConfig();

    final String version = payload.toString();

    final Map<String, String> newAliases = Maps.newHashMap();
    newAliases.putAll(configuration.aliases());
    newAliases.put(alias, version);

    final GenerateMetadataCapabilityConfiguration newConfiguration =
        new GenerateMetadataCapabilityConfiguration(
            configuration.repository(),
            newAliases,
            configuration.shouldProcessDeletes(),
            configuration.deleteProcessingDelay(),
            configuration.getYumGroupsDefinitionFile()
        );

    try {
      capabilityRegistry.update(
          capabilityReference.context().id(),
          capabilityReference.context().isEnabled(),
          capabilityReference.context().notes(),
          newConfiguration.asMap()
      );
    }
    catch (CapabilityNotFoundException e) {
      throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
    }
    catch (InvalidConfigurationException e) {
      handleConfigurationException(e);
    }
    catch (IOException e) {
      throw new ResourceException(
          Status.SERVER_ERROR_INTERNAL,
          "Could not manage capabilities configuration persistence store"
      );
    }

    return new StringRepresentation(version, TEXT_PLAIN);
  }

  private String getAttributeAsString(final Request request, final String attrName) {
    final Object attrValue = request.getAttributes().get(attrName);
    return (attrValue != null) ? attrValue.toString() : null;
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor(PATH_PATTERN_TO_PROTECT, "authcBasic,perms[nexus:yumAlias]");
  }

  @Override
  public Object getPayloadInstance(Method method) {
    if (POST.equals(method)) {
      return "";
    }
    return null;
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

}
