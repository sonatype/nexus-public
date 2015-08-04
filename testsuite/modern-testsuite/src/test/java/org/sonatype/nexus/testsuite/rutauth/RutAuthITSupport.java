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
package org.sonatype.nexus.testsuite.rutauth;

import org.sonatype.nexus.bundle.launcher.NexusBundleConfiguration;
import org.sonatype.nexus.capabilities.client.Capabilities;
import org.sonatype.nexus.capabilities.client.Capability;
import org.sonatype.nexus.capabilities.client.Filter;
import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.core.exception.NexusClientNotFoundException;
import org.sonatype.nexus.client.core.subsystem.ServerConfiguration;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.testsuite.support.NexusRunningParametrizedITSupport;
import org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import static org.sonatype.nexus.testsuite.support.NexusStartAndStopStrategy.Strategy.EACH_TEST;

/**
 * Support for Rut Auth integration tests.
 *
 * @since 2.7
 */
@NexusStartAndStopStrategy(EACH_TEST)
public class RutAuthITSupport
    extends NexusRunningParametrizedITSupport
{

  static final String RUTAUTH_REALM = "rutauth-realm";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public RutAuthITSupport(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Override
  protected NexusBundleConfiguration configureNexus(final NexusBundleConfiguration configuration) {
    return configuration
        .setLogLevel("org.apache.shiro", "DEBUG")
        .setLogLevel("org.sonatype.nexus.security.filter.authc", "DEBUG")
        .setLogLevel("org.sonatype.nexus.rutauth", "TRACE")
        .addPlugins(
            artifactResolver().resolvePluginFromDependencyManagement(
                "org.sonatype.nexus.plugins", "nexus-rutauth-plugin"
            )
        );
  }

  void configureSecurityRealms() {
    serverConfiguration().security().settings().addRealm(RUTAUTH_REALM);
    serverConfiguration().security().save();
  }

  void configureRemoteHeader(final String headerName) {
    Capability rutauthCapability;
    try {
      rutauthCapability = capabilities().getUnique(Filter.capabilitiesThat().haveType("rutauth"));
    }
    catch (NexusClientNotFoundException e) {
      rutauthCapability = capabilities().create("rutauth");
    }
    rutauthCapability.withProperty("httpHeader", headerName).save();
  }

  protected NexusClient createNexusClientForRemoteHeader(final String headerName,
                                                         final String headerValue)
  {
    final NexusClient nexusClient = createNexusClientForAnonymous(nexus());
    ((JerseyNexusClient) nexusClient).getClient().addFilter(new ClientFilter()
    {
      @Override
      public ClientResponse handle(final ClientRequest clientRequest) throws ClientHandlerException {
        clientRequest.getHeaders().putSingle(headerName, headerValue);
        return getNext().handle(clientRequest);
      }
    });
    return nexusClient;
  }

  Capabilities capabilities() {
    return client().getSubsystem(Capabilities.class);
  }

  ServerConfiguration serverConfiguration() {
    return client().getSubsystem(ServerConfiguration.class);
  }

}
