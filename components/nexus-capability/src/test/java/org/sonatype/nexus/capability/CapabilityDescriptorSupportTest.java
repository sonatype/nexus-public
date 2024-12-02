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
package org.sonatype.nexus.capability;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.validation.ValidationException;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.capability.CapabilityDescriptor.ValidationMode;
import org.sonatype.nexus.capability.CapabilityReferenceFilterBuilder.CapabilityReferenceFilter;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

public class CapabilityDescriptorSupportTest
    extends TestSupport
{
  @Mock
  private CapabilityRegistry capabilityRegistry;

  @Mock
  private CapabilityReference capabilityReference;

  @Mock
  private CapabilityContext capabilityContext;

  private CapabilityIdentity capabilityIdentity;

  @Captor
  private ArgumentCaptor<CapabilityReferenceFilter> filterRecorder;

  @Before
  public void prepare() {
    capabilityIdentity = CapabilityIdentity.capabilityIdentity("test");
    when(capabilityContext.id()).thenReturn(capabilityIdentity);
    when(capabilityReference.context()).thenReturn(capabilityContext);
  }

  @Test
  public void capabilityWithSameTypeDoesNotExist() {
    when(capabilityRegistry.get(filterRecorder.capture())).thenReturn(Collections.emptyList());
    TestCapabilityDescriptor underTest = new TestCapabilityDescriptor(Collections.emptyList(), Collections.emptySet());
    underTest.installComponents(() -> capabilityRegistry);

    underTest.validate(null, Collections.emptyMap(), ValidationMode.CREATE);
    assertThat(filterRecorder.getValue().getTypeId(), is("test"));
    assertThat(filterRecorder.getValue().getIgnoreCapabilityId(), is(nullValue()));
  }

  @Test
  public void capabilityWithSameTypeAlreadyPresent() {
    when(capabilityRegistry.get(filterRecorder.capture())).thenAnswer(
        invocation -> Collections.singletonList(capabilityReference));
    TestCapabilityDescriptor underTest = new TestCapabilityDescriptor(Collections.emptyList(), Collections.emptySet());
    underTest.installComponents(() -> capabilityRegistry);

    ValidationException expected = assertThrows(ValidationException.class,
        () -> underTest.validate(null, Collections.emptyMap(), ValidationMode.CREATE));

    assertThat(expected.getMessage(), containsString("Test"));
    assertThat(filterRecorder.getValue().getTypeId(), is("test"));
    assertThat(filterRecorder.getValue().getIgnoreCapabilityId(), is(nullValue()));
  }

  @Test
  public void sameCapabilityWithSameTypeAlreadyPresent() {
    when(capabilityRegistry.get(filterRecorder.capture())).thenReturn(Collections.emptyList());
    TestCapabilityDescriptor underTest = new TestCapabilityDescriptor(Collections.emptyList(), Collections.emptySet());
    underTest.installComponents(() -> capabilityRegistry);

    underTest.validate(capabilityIdentity, Collections.emptyMap(), ValidationMode.UPDATE);
    assertThat(filterRecorder.getValue().getTypeId(), is("test"));
    assertThat(filterRecorder.getValue().getIgnoreCapabilityId(), is(capabilityIdentity));
  }

  @Test
  public void capabilityWithSameTypeAndSameRepositoryDoesNotExist() {
    when(capabilityRegistry.get(filterRecorder.capture())).thenReturn(Collections.emptyList());
    TestCapabilityDescriptor underTest =
        new TestCapabilityDescriptor(Collections.singletonList(new RepositoryCombobox("repository")),
            Collections.singleton("repository"));

    underTest.installComponents(() -> capabilityRegistry);

    underTest.validate(null, Collections.singletonMap("repository", "foo"), ValidationMode.CREATE);
    assertThat(filterRecorder.getValue().getTypeId(), is("test"));
    assertThat(filterRecorder.getValue().getIgnoreCapabilityId(), is(nullValue()));
    assertThat(filterRecorder.getValue().getProperties().get("repository"), is("foo"));
  }

  @Test
  public void capabilityWithSameTypeAndSameRepositoryAlreadyPresent() {
    when(capabilityRegistry.get(filterRecorder.capture())).thenAnswer(
        invocation -> Collections.singletonList(capabilityReference));
    TestCapabilityDescriptor underTest =
        new TestCapabilityDescriptor(Collections.singletonList(new RepositoryCombobox("repository")),
            Collections.singleton("repository"));

    underTest.installComponents(() -> capabilityRegistry);

    ValidationException expected = assertThrows(ValidationException.class, () ->
        underTest.validate(null, Collections.singletonMap("repository", "foo"), ValidationMode.CREATE));

    assertThat(expected.getMessage(),
        allOf(containsString("Test"), containsString("repository"), containsString("foo")));

    assertThat(filterRecorder.getValue().getTypeId(), is("test"));
    assertThat(filterRecorder.getValue().getIgnoreCapabilityId(), is(nullValue()));
    assertThat(filterRecorder.getValue().getProperties().get("repository"), is("foo"));
  }

  @Test
  public void sameCapabilityWithSameTypeAndSameRepositoryAlreadyPresent() {
    when(capabilityRegistry.get(filterRecorder.capture())).thenReturn(Collections.emptyList());
    TestCapabilityDescriptor underTest =
        new TestCapabilityDescriptor(Collections.singletonList(new RepositoryCombobox("repository")),
            Collections.singleton("repository"));

    underTest.installComponents(() -> capabilityRegistry);

    underTest.validate(capabilityIdentity, Collections.singletonMap("repository", "foo"), ValidationMode.UPDATE);
    assertThat(filterRecorder.getValue().getTypeId(), is("test"));
    assertThat(filterRecorder.getValue().getIgnoreCapabilityId(), is(capabilityIdentity));
    assertThat(filterRecorder.getValue().getProperties().get("repository"), is("foo"));
  }

  private static class TestCapabilityDescriptor
      extends CapabilityDescriptorSupport
  {
    private final List<FormField> formFields;

    private final Set<String> uniqueProperties;

    public TestCapabilityDescriptor(final List<FormField> formFields, final Set<String> uniqueProperties) {
      this.formFields = formFields;
      this.uniqueProperties = uniqueProperties;
    }

    @Override
    public CapabilityType type() {
      return CapabilityType.capabilityType("test");
    }

    @Override
    public String name() {
      return "Test";
    }

    @Override
    public List<FormField> formFields() {
      return formFields;
    }

    @Nullable
    @Override
    protected Set<String> uniqueProperties() {
      return uniqueProperties;
    }
  }
}
