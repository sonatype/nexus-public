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
package org.sonatype.nexus.internal.httpclient

import org.sonatype.goodies.common.Time
import org.sonatype.nexus.httpclient.config.ConnectionConfiguration
import org.sonatype.nexus.repository.httpclient.internal.HttpClientFacetImpl
import org.sonatype.nexus.security.PasswordHelper

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import spock.lang.Specification
import spock.lang.Subject

/**
 * @since 3.1.0
 */
class HttpClientConfigurationObjectMapperCustomizerTest
    extends Specification
{
  PasswordHelper passwordHelper = Mock()

  HttpClientConfigurationObjectMapperCustomizer customizer = new HttpClientConfigurationObjectMapperCustomizer(passwordHelper)
  
  @Subject
  ObjectMapper objectMapper = new ObjectMapper()
  
  def setup() {
    customizer.customize(objectMapper)    
  }
  
  def 'Can serialize and deserialize HttpClientConfiguration timeouts'() {
    given: 'A configuration with a timeout set'
      def config = new HttpClientFacetImpl.Config(connection: new ConnectionConfiguration(timeout: Time.seconds(1)))
    
    when: 'Serializing that with ObjectMapper'
      def json = objectMapper.writeValueAsString(config)
      def map = new JsonSlurper().parseText(json)
    
    then: 'We can confirm that the timeout is set to a number'
      map.connection.timeout == 1
    
    when: 'We convert back to an object with ObjectMapper'
      def marshalledConfig = objectMapper.readValue(json, HttpClientFacetImpl.Config)
      
    then: 'The time is correctly converted'
      marshalledConfig.connection.timeout == Time.seconds(1)
  }
}
