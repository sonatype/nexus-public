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
package org.sonatype.nexus.capability.internal;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.sonatype.nexus.capability.Capability;
import org.sonatype.nexus.capability.CapabilityIdentity;
import org.sonatype.nexus.capability.CapabilityReference;
import org.sonatype.nexus.capability.CapabilityRegistry;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.sonatype.nexus.capability.CapabilityIdentity.capabilityIdentity;
import static org.sonatype.nexus.capability.CapabilityType.capabilityType;

/**
 * A REST resource for capabilities.
 *
 * Note this is not fully tested as it is currently used for HA tests.
 */
@FeatureFlag(name = "nexus.internal.ha.tests")
@Named
@Singleton
@Path(CapabilityResource.RESOURCE_URI)
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
public class CapabilityResource implements Resource
{
  static final String PASSWORD_PLACEHOLDER = "#~NXRM~PLACEHOLDER~PASSWORD~#";

  static final String RESOURCE_URI = "internal/capabilities";

  private final CapabilityRegistry registry;

  @Inject
  public CapabilityResource(final CapabilityRegistry registry) {
    this.registry = checkNotNull(registry);
  }

  @GET
  @RequiresPermissions("nexus:capabilities:read")
  public Collection<CapabilityDTO> list() {
    return registry.getAll().stream()
        .map(CapabilityDTO::new)
        .collect(Collectors.toList());
  }

  /**
   * Important This path should avoid triggering a potential database load of the configurations.
   *
   * This is intended for use by HA tests to validate event propagation between nodes.
   */
  @GET
  @Path("active")
  @RequiresPermissions("nexus:capabilities:read")
  public Collection<CapabilityDTO> listActive() {
    return registry.getAll().stream()
        .map(CapabilityDTO::new)
        .collect(Collectors.toList());
  }

  @POST
  @RequiresPermissions("nexus:capabilities:create")
  public CapabilityDTO create(final CapabilityDTO capability) {
    return new CapabilityDTO(registry.add(
            capabilityType(capability.getType()),
            capability.isEnabled(),
            capability.getNotes(),
            capability.getProperties()
        ));
  }

  @PUT
  @Path("{capabilityId}")
  @RequiresPermissions("nexus:capabilities:update")
  public void update(@PathParam("capabilityId") final String capabilityId, final CapabilityDTO capabilityDto) {
    CapabilityReference reference = registry.get(CapabilityIdentity.capabilityIdentity(capabilityId));
    registry.update(
        capabilityIdentity(capabilityDto.getId()),
        capabilityDto.isEnabled(),
        capabilityDto.getNotes(),
        unfilterProperties(capabilityDto.getProperties(), reference.context().properties())
    );
  }

  @DELETE
  @Path("{capabilityId}")
  @RequiresPermissions("nexus:capabilities:delete")
  public void delete(@PathParam("capabilityId") final String capabilityId) {
    registry.remove(capabilityIdentity(capabilityId));
  }

  private static Map<String, String> unfilterProperties(
      final Map<String, String> properties,
      final Map<String, String> referenceProperties)
  {
    return properties.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> {
          if (PASSWORD_PLACEHOLDER.equals(entry.getValue())) {
            return referenceProperties.get(entry.getKey());
          }
          return entry.getValue();
        }));
  }

  static Map<String, String> filterProperties(final Map<String, String> properties, final Capability capability) {
    return properties.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey, entry -> {
          if (capability.isPasswordProperty(entry.getKey())) {
            if ("PKI".equals(properties.get("authenticationType"))) {
              return "";
            }
            else {
              return PASSWORD_PLACEHOLDER;
            }
          }
          return entry.getValue();
        }));
  }
}
