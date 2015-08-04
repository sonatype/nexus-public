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
package org.sonatype.nexus.testsuite.plexusplugin;

import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;

import com.sun.jersey.api.client.ClientResponse;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;


/**
 * Support for PlexusPlugin plugin integration tests.
 *
 * @since 2.7.0
 */
public class PlexusPluginLivesIT
    extends PlexusPluginITSupport
{

  public PlexusPluginLivesIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void fetchAndValidatePlexusPluginResource() {
    final ClientResponse response =((JerseyNexusClient)client()).serviceResource("plexusplugin").get(ClientResponse.class);
    // this check is completely enough, as if plexus DI would not work, server side components would NPE
    // and some sort of error (most probably Server Error 500) would be returned.
    assertThat(response.getStatus(), equalTo(200));
  }

}
