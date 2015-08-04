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
package org.sonatype.nexus.client.rest.jersey;

import java.net.MalformedURLException;

import org.sonatype.nexus.client.core.NexusClient;
import org.sonatype.nexus.client.internal.util.Version;
import org.sonatype.nexus.client.rest.BaseUrl;
import org.sonatype.nexus.client.rest.NexusClientFactory;

import com.sun.jersey.client.apache4.ApacheHttpClient4;
import org.apache.http.params.CoreProtocolPNames;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * UT for NexusClient UA, as it should carry the version from now on.
 *
 * @author cstamas
 * @since 2.5
 */
public class JerseyNexusTestClientUserAgentTest
    extends JerseyNexusClientTestSupport
{
  @Test
  public void checkUAVersionIsProperlyReadAndSet()
      throws MalformedURLException
  {
    final String version =
        Version.readVersion("META-INF/maven/org.sonatype.nexus/nexus-client-core/pom.properties", "foo");
    assertThat("Version read must not return null!", version, notNullValue());
    assertThat("Version read must not return the default (it should succeed in reading the stuff up)!", version,
        not(equalTo("foo")));

    final NexusClientFactory factory = new JerseyNexusClientFactory();
    final NexusClient client = factory.createFor(BaseUrl.baseUrlFrom("https://repository.sonatype.org/"));
    assertThat(client.getNexusStatus(), notNullValue());
    final String userAgent =
        (String) ((ApacheHttpClient4) ((JerseyNexusClient) client).getClient()).getClientHandler().getHttpClient()
            .getParams().getParameter(
                CoreProtocolPNames.USER_AGENT);
    assertThat("UA must not be null!", userAgent, notNullValue());
    assertThat("UA must not be empty!", userAgent, containsString("Nexus-Client"));
    assertThat("UA should be correct!", userAgent, equalTo("Nexus-Client/" + version));
  }
}
