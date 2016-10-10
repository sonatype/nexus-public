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
package org.sonatype.nexus.coreui.internal.wonderland;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.rest.Resource;

import com.google.common.collect.Lists;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

/**
 * Expose Nexus settings.
 *
 * @since 2.7
 */
@Named
@Singleton
@Path(SettingsResource.RESOURCE_URI)
public class SettingsResource
    extends ComponentSupport
    implements Resource
{
  public static final String RESOURCE_URI = "/wonderland/settings";

  @GET
  @Produces({APPLICATION_JSON, APPLICATION_XML})
  public List<PropertyXO> get() {
    List<PropertyXO> properties = Lists.newArrayList();

    properties.add(
        new PropertyXO().withKey("keepAlive")
            .withValue(Boolean.toString(SystemPropertiesHelper.getBoolean("nexus.ui.keepAlive", true)))
    );

    return properties;
  }
}