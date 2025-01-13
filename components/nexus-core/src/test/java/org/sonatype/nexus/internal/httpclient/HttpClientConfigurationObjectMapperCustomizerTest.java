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
package org.sonatype.nexus.internal.httpclient;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.json.JsonSlurper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration;
import org.sonatype.nexus.repository.httpclient.internal.HttpClientFacetImpl;

import static org.junit.Assert.assertEquals;

/**
 * @since 3.1.0
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpClientConfigurationObjectMapperCustomizerTest
{

  @Mock
  private SecretsService secretsService;

  private HttpClientConfigurationObjectMapperCustomizer customizer;

  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    customizer = new HttpClientConfigurationObjectMapperCustomizer(secretsService);
    objectMapper = new ObjectMapper();
    customizer.customize(objectMapper);
  }

  @Test
  public void canSerializeAndDeserializeHttpClientConfigurationTimeouts() throws Exception {
    // Given: A configuration with a timeout set
    HttpClientFacetImpl.Config config = new HttpClientFacetImpl.Config();
    config.connection = new ConnectionConfiguration();
    config.connection.setTimeout(Time.seconds(1));

    // When: Serializing that with ObjectMapper
    String json = objectMapper.writeValueAsString(config);
    JsonSlurper jsonSlurper = new JsonSlurper();
    Map<String, Object> map = (Map<String, Object>) jsonSlurper.parseText(json);
    Map<String, Object> innermap = (Map<String, Object>) map.get("connection");
    // Then: We can confirm that the timeout is set to a number
    assertEquals(1, innermap.get("timeout"));

    // When: We convert back to an object with ObjectMapper
    HttpClientFacetImpl.Config marshalledConfig = objectMapper.readValue(json, HttpClientFacetImpl.Config.class);

    // Then: The time is correctly converted
    assertEquals(Time.seconds(1), marshalledConfig.connection.getTimeout());
  }
}
