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
package org.sonatype.nexus.rest.routing;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.routing.DiscoveryConfig;
import org.sonatype.nexus.rest.model.RoutingConfigMessage;
import org.sonatype.nexus.rest.model.RoutingConfigMessageWrapper;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import com.google.common.primitives.Ints;
import org.codehaus.enunciate.contract.jaxrs.ResourceMethodSignature;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;

/**
 * Autorouting Configuration REST resource, usable only on Maven Proxy repositories.
 *
 * @author cstamas
 * @since 2.4
 */
@Named
@Singleton
@Path(RoutingConfigResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class RoutingConfigResource
    extends RoutingResourceSupport
{
  /**
   * REST resource URI.
   */
  public static final String RESOURCE_URI = "/repositories/{" + REPOSITORY_ID_KEY + "}/routing/config";

  @Override
  public Object getPayloadInstance() {
    return new RoutingConfigMessageWrapper();
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/repositories/*/routing/config", "authcBasic,perms[nexus:repositories]");
  }

  /**
   * Returns the current autorouting configuration for given repository.
   */
  @Override
  @GET
  @ResourceMethodSignature(pathParams = {@PathParam(REPOSITORY_ID_KEY)}, output = RoutingConfigMessageWrapper.class)
  public RoutingConfigMessageWrapper get(final Context context, final Request request, final Response response,
                                         final Variant variant)
      throws ResourceException
  {
    final MavenProxyRepository mavenProxyRepository = getMavenRepository(request, MavenProxyRepository.class);
    final DiscoveryConfig config = getManager().getRemoteDiscoveryConfig(mavenProxyRepository);
    final RoutingConfigMessage payload = new RoutingConfigMessage();
    payload.setDiscoveryEnabled(config.isEnabled());
    payload.setDiscoveryIntervalHours(Ints.saturatedCast(TimeUnit.MILLISECONDS.toHours(config.getDiscoveryInterval())));
    final RoutingConfigMessageWrapper responseNessage = new RoutingConfigMessageWrapper();
    responseNessage.setData(payload);
    return responseNessage;
  }

  /**
   * Sets the autorouting configuration for given repository.
   */
  @Override
  @PUT
  @ResourceMethodSignature(pathParams = {@PathParam(REPOSITORY_ID_KEY)}, input = RoutingConfigMessageWrapper.class,
      output = RoutingConfigMessageWrapper.class)
  public RoutingConfigMessageWrapper put(final Context context, final Request request, final Response response,
                                         final Object payload)
      throws ResourceException
  {
    try {
      final MavenProxyRepository mavenProxyRepository = getMavenRepository(request, MavenProxyRepository.class);
      final DiscoveryConfig oldConfig = getManager().getRemoteDiscoveryConfig(mavenProxyRepository);
      final RoutingConfigMessageWrapper wrapper = RoutingConfigMessageWrapper.class.cast(payload);
      // NEXUS-5567 related and some other cases (like scripting and sending partial message to enable/disable only).
      // The error range of the interval value is (>0) since it's defined in hours, and 0 or negative value would be
      // undefined. But still, due to unmarshalling, the field in the bean would be 0 (or is -1 like in NEXUS-5567).
      // Hence, if non valid value sent, we use the "old" value to keep it.
      final DiscoveryConfig config =
          new DiscoveryConfig(
              wrapper.getData().isDiscoveryEnabled(),
              wrapper.getData().getDiscoveryIntervalHours() > 0 ? TimeUnit.HOURS
                  .toMillis(wrapper.getData().getDiscoveryIntervalHours())
                  : oldConfig.getDiscoveryInterval());
      getManager().setRemoteDiscoveryConfig(mavenProxyRepository, config);
      return wrapper;
    }
    catch (ClassCastException e) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Message not recognized!", e);
    }
    catch (IllegalArgumentException e) {
      throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Invalid or illegal configuration!", e);
    }
    catch (IOException e) {
      throw new ResourceException(Status.SERVER_ERROR_INTERNAL, e);
    }
  }
}
