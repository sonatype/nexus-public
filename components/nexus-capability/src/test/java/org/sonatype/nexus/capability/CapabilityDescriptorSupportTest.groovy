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
package org.sonatype.nexus.capability

import javax.validation.ValidationException

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.capability.CapabilityDescriptor.ValidationMode
import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.CapabilityReferenceFilter
import org.sonatype.nexus.formfields.FormField
import org.sonatype.nexus.formfields.RepositoryCombobox

import com.google.inject.util.Providers
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor

import static org.junit.Assert.fail
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

/**
 * {@link CapabilityDescriptorSupport} tests.
 *
 * @since 3.0
 */
class CapabilityDescriptorSupportTest
    extends TestSupport
{
  CapabilityRegistry capabilityRegistry

  CapabilityReference capabilityReference

  CapabilityIdentity capabilityIdentity

  ArgumentCaptor<CapabilityReferenceFilter> filterRecorder

  @Before
  void prepare() {
    capabilityRegistry = mock(CapabilityRegistry)
    capabilityReference = mock(CapabilityReference)
    capabilityIdentity = CapabilityIdentity.capabilityIdentity('test')
    def capabilityContext = mock(CapabilityContext)
    when(capabilityContext.id()).thenReturn(capabilityIdentity)
    when(capabilityReference.context()).thenReturn(capabilityContext)
    filterRecorder = ArgumentCaptor.forClass(CapabilityReferenceFilter)
  }

  @Test
  void 'Capability with same type does not exist'() {
    when(capabilityRegistry.get(filterRecorder.capture())).thenReturn([])
    def underTest = new TestCapabilityDescriptor()
    underTest.installComponents(Providers.of(capabilityRegistry))

    underTest.validate(null, [:], ValidationMode.CREATE)
    assert filterRecorder.value.typeId == 'test'
  }

  @Test
  void 'Capability with same type already present'() {
    when(capabilityRegistry.get(filterRecorder.capture())).thenReturn([capabilityReference])
    def underTest = new TestCapabilityDescriptor()
    underTest.installComponents(Providers.of(capabilityRegistry))

    try {
      underTest.validate(null, [:], ValidationMode.CREATE)
      fail('Expected to throw exception')
    }
    catch (ValidationException e) {
      assert e.message.contains('Test')
    }
    assert filterRecorder.value.typeId == 'test'
  }

  @Test
  void 'Same Capability with same type already present'() {
    when(capabilityRegistry.get(filterRecorder.capture())).thenReturn([capabilityReference])
    def underTest = new TestCapabilityDescriptor()
    underTest.installComponents(Providers.of(capabilityRegistry))

    underTest.validate(capabilityIdentity, [:], ValidationMode.UPDATE)
    assert filterRecorder.value.typeId == 'test'
  }

  @Test
  void 'Capability with same type and same repository does not exist'() {
    when(capabilityRegistry.get(filterRecorder.capture())).thenReturn([])
    def underTest = new TestCapabilityDescriptor() {
      @Override
      List<FormField> formFields() {
        return [new RepositoryCombobox('repository')]
      }

      @Override
      protected Set<String> uniqueProperties() {
        return ['repository']
      }
    }
    underTest.installComponents(Providers.of(capabilityRegistry))

    underTest.validate(null, ['repository': 'foo'], ValidationMode.CREATE)
    assert filterRecorder.value.typeId == 'test'
    assert filterRecorder.value.properties['repository'] == 'foo'
  }

  @Test
  void 'Capability with same type and same repository already present'() {
    when(capabilityRegistry.get(filterRecorder.capture())).thenReturn([capabilityReference])
    def underTest = new TestCapabilityDescriptor() {
      @Override
      List<FormField> formFields() {
        return [new RepositoryCombobox('repository')]
      }

      @Override
      protected Set<String> uniqueProperties() {
        return ['repository']
      }
    }
    underTest.installComponents(Providers.of(capabilityRegistry))

    try {
      underTest.validate(null, ['repository': 'foo'], ValidationMode.CREATE)
      fail('Expected to throw exception')
    }
    catch (ValidationException e) {
      assert e.message.contains('Test')
      assert e.message.contains('repository')
      assert e.message.contains('foo')
    }
    assert filterRecorder.value.typeId == 'test'
    assert filterRecorder.value.properties['repository'] == 'foo'
  }

  @Test
  void 'Same Capability with same type and same repository already present'() {
    when(capabilityRegistry.get(filterRecorder.capture())).thenReturn([capabilityReference])
    def underTest = new TestCapabilityDescriptor() {
      @Override
      List<FormField> formFields() {
        return [new RepositoryCombobox('repository')]
      }

      @Override
      protected Set<String> uniqueProperties() {
        return ['repository']
      }
    }
    underTest.installComponents(Providers.of(capabilityRegistry))

    underTest.validate(capabilityIdentity, ['repository': 'foo'], ValidationMode.UPDATE)
    assert filterRecorder.value.typeId == 'test'
    assert filterRecorder.value.properties['repository'] == 'foo'
  }

  private class TestCapabilityDescriptor
      extends CapabilityDescriptorSupport
  {

    @Override
    CapabilityType type() {
      return CapabilityType.capabilityType('test')
    }

    @Override
    String name() {
      return 'Test'
    }

    @Override
    List<FormField> formFields() {
      return null
    }
  }
}
