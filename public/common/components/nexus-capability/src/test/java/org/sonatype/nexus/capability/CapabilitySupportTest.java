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

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.common.template.TemplateThrowableAdapter;
import org.sonatype.nexus.crypto.secrets.SecretsService;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.PasswordFormField;
import org.sonatype.nexus.formfields.StringTextFormField;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CapabilitySupportTest
{
  @Mock
  private CapabilityContext context;

  @Mock
  private CapabilityDescriptor descriptor;

  @Mock
  private TemplateHelper templateHelper;

  @Mock
  private SecretsService secretsService;

  private TestCapability underTest;

  @Before
  public void setup() {
    underTest = new TestCapability();
    underTest.init(context, secretsService);
    underTest.setTemplateHelper(templateHelper);
  }

  @Test
  public void renderFailureReturnsNullOnFailure() {
    TemplateParameters params = new TemplateParameters()
        .set("cause", new TemplateThrowableAdapter(new Exception()));
    assertNull(underTest.render("missing-template.vm", params));
    verifyNoInteractions(templateHelper);
  }

  @Test
  public void renderFailureSuccess() {
    when(templateHelper.render(any(URL.class), any(TemplateParameters.class))).thenReturn("rendered");
    TemplateParameters params = new TemplateParameters()
        .set("cause", new TemplateThrowableAdapter(new Exception()));
    assertEquals("rendered", underTest.render("failure.vm", params));
    verify(templateHelper).render(underTest.getClass().getResource("failure.vm"), params);
  }

  @Test
  public void isPasswordPropertyReturnsTrueForEncryptedField() {
    PasswordFormField secretField = new PasswordFormField("secret", "Secret", "help", false);
    when(context.descriptor()).thenReturn(descriptor);
    when(descriptor.formFields()).thenReturn(List.<FormField>of(secretField));

    assertTrue(underTest.isPasswordProperty("secret"));
  }

  @Test
  public void isPasswordPropertyReturnsFalseForNonEncryptedField() {
    StringTextFormField nameField = new StringTextFormField("name", "Name", "help", false);
    when(context.descriptor()).thenReturn(descriptor);
    when(descriptor.formFields()).thenReturn(List.<FormField>of(nameField));

    assertFalse(underTest.isPasswordProperty("name"));
  }

  @Test
  public void isPasswordPropertyReturnsFalseForUnknownProperty() {
    when(context.descriptor()).thenReturn(descriptor);
    when(descriptor.formFields()).thenReturn(List.<FormField>of());

    assertFalse(underTest.isPasswordProperty("unknown"));
  }

  @Test
  public void isPasswordPropertyReturnsFalseWhenFormFieldsIsNull() {
    when(context.descriptor()).thenReturn(descriptor);
    when(descriptor.formFields()).thenReturn(null);

    assertFalse(underTest.isPasswordProperty("secret"));
  }

  private class TestCapability
      extends CapabilitySupport<TestCapabilityConfig>
  {
    @Override
    protected TestCapabilityConfig createConfig(final Map<String, String> properties) throws Exception {
      return new TestCapabilityConfig();
    }
  }

  private class TestCapabilityConfig
      extends CapabilityConfigurationSupport
  {
  }
}
