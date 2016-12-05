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
package org.sonatype.nexus.coreui.internal.capability

import org.sonatype.nexus.capability.Capability
import org.sonatype.nexus.capability.CapabilityContext
import org.sonatype.nexus.capability.CapabilityDescriptor
import org.sonatype.nexus.capability.CapabilityDescriptorRegistry
import org.sonatype.nexus.capability.CapabilityIdentity
import org.sonatype.nexus.capability.CapabilityReference
import org.sonatype.nexus.capability.CapabilityRegistry
import org.sonatype.nexus.rapture.PasswordPlaceholder

import com.google.common.base.Predicate;
import spock.lang.Specification

/**
 * Tests {@link CapabilityComponent}.
 */
public class CapabilityComponentTest
    extends Specification
{

  CapabilityDescriptorRegistry capabilityDescriptorRegistry = Mock()

  CapabilityRegistry capabilityRegistry = Mock()

  Capability capability = Mock()

  CapabilityContext capabilityContext = Mock()

  CapabilityReference capabilityReference = Mock()

  CapabilityComponent underTest = new CapabilityComponent()

  def setup() {
    underTest.capabilityDescriptorRegistry = capabilityDescriptorRegistry
    underTest.capabilityRegistry = capabilityRegistry
    capabilityContext.properties() >> [username: 'username', password: 'its a secret to everybody']
    capabilityContext.descriptor() >> Mock(CapabilityDescriptor)
    capabilityReference.capability() >> capability
    capabilityReference.context() >> capabilityContext
    capabilityRegistry.get(_ as CapabilityIdentity) >> capabilityReference
    capabilityRegistry.get(_ as Predicate) >> [capabilityReference]
  }

  def 'password is not returned in as cleartext'() {
    when:
      def capabilities = underTest.read()

    then:
      capabilities[0].properties.username == 'username'
      capabilities[0].properties.password == PasswordPlaceholder.get()
      2 * capability.isPasswordProperty(_) >> { String propertyName -> propertyName == 'password' }
  }

  def 'when password provide to update is placeholder use actual current password'() {
    when:
      def xoProperties = [username: 'username', password: PasswordPlaceholder.get()]
      underTest.update(new CapabilityXO(id: 'mycap', properties: xoProperties, enabled: true))

    then:
      1 * capabilityRegistry.update(*_) >> { CapabilityIdentity id, boolean enabled,
                                             String notes, Map<String, String> properties ->
        properties.username == 'username'
        properties.password == 'its a secret to everybody'
        return capabilityReference
      }
  }
}
