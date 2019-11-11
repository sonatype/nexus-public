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
package org.sonatype.nexus.plugins.capabilities.internal.rest;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.sonatype.nexus.capabilities.model.CapabilityTypeXO;
import org.sonatype.nexus.capabilities.model.FormFieldXO;
import org.sonatype.nexus.capability.CapabilitiesPlugin;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.Selectable;
import org.sonatype.nexus.plugins.capabilities.CapabilityDescriptor;
import org.sonatype.nexus.plugins.capabilities.CapabilityDescriptorRegistry;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.siesta.common.Resource;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

/**
 * Capabilities Types REST resource.
 *
 * @since 2.7
 */
@Named
@Singleton
@Path(CapabilityTypesResource.RESOURCE_URI)
@Produces({"application/xml", "application/json"})
public class CapabilityTypesResource
    extends ComponentSupport
    implements Resource
{

  public static final String RESOURCE_URI = CapabilitiesPlugin.REST_PREFIX + "/types";

  public static final String $INCLUDE_NOT_EXPOSED = "$includeNotExposed";

  private final CapabilityDescriptorRegistry capabilityDescriptorRegistry;

  @Inject
  public CapabilityTypesResource(final CapabilityDescriptorRegistry capabilityDescriptorRegistry) {
    this.capabilityDescriptorRegistry = capabilityDescriptorRegistry;
  }

  /**
   * Retrieve a list of capability types available.
   */
  @GET
  @Produces({APPLICATION_XML, APPLICATION_JSON})
  @RequiresPermissions(CapabilitiesPlugin.PERMISSION_PREFIX_TYPES + "read")
  public List<CapabilityTypeXO> get(@QueryParam($INCLUDE_NOT_EXPOSED) Boolean includeNotExposed) {

    final List<CapabilityTypeXO> types = Lists.newArrayList();
    final CapabilityDescriptor[] descriptors = capabilityDescriptorRegistry.getAll();

    if (descriptors != null) {
      for (final CapabilityDescriptor descriptor : descriptors) {
        if (((includeNotExposed != null && includeNotExposed) || descriptor.isExposed())) {

          CapabilityTypeXO type = new CapabilityTypeXO()
              .withId(descriptor.type().toString())
              .withName(descriptor.name())
              .withAbout(descriptor.about());

          types.add(type);

          if (descriptor.formFields() != null) {
            type.withFormFields(Lists.transform(descriptor.formFields(), new Function<FormField, FormFieldXO>()
            {
              @Nullable
              @Override
              public FormFieldXO apply(@Nullable final FormField input) {
                if (input == null) {
                  return null;
                }

                FormFieldXO formField = new FormFieldXO()
                    .withId(input.getId())
                    .withType(input.getType())
                    .withLabel(input.getLabel())
                    .withHelpText(input.getHelpText())
                    .withRequired(input.isRequired())
                    .withRegexValidation(input.getRegexValidation())
                    .withEnabled(input.isEnabled());

                if (input.getInitialValue() != null) {
                  formField.setInitialValue(input.getInitialValue().toString());
                }

                if (input instanceof Selectable) {
                  formField
                      .withStorePath(((Selectable) input).getStorePath())
                      .withStoreRoot(((Selectable) input).getStoreRoot())
                      .withIdMapping(((Selectable) input).getIdMapping())
                      .withNameMapping(((Selectable) input).getNameMapping());
                }

                return formField;
              }
            }));
          }
        }
      }
    }

    return types;
  }

}
