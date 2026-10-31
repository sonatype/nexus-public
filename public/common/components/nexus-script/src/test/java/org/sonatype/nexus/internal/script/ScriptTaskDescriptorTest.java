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
package org.sonatype.nexus.internal.script;

import java.util.List;

import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.formfields.TextAreaFormField;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScriptTaskDescriptorTest
{
  private ScriptTaskDescriptor createDescriptor(final boolean allowCreation, final boolean clustered) {
    NodeAccess nodeAccess = mock(NodeAccess.class);
    when(nodeAccess.isClustered()).thenReturn(clustered);
    return new ScriptTaskDescriptor(nodeAccess, allowCreation);
  }

  @Test
  public void testConstants() {
    assertThat(ScriptTaskDescriptor.TYPE_ID, is("script"));
    assertThat(ScriptTaskDescriptor.LANGUAGE, is("language"));
    assertThat(ScriptTaskDescriptor.SOURCE, is("source"));
  }

  @Test
  public void testDescriptorProperties() {
    ScriptTaskDescriptor underTest = createDescriptor(true, false);

    assertThat(underTest.getId(), is(ScriptTaskDescriptor.TYPE_ID));
    assertThat(underTest.getName(), is("Admin - Execute script"));
    assertThat(underTest.getType(), is(ScriptTask.class));
    assertThat(underTest.isVisible(), is(true));
    assertThat(underTest.isRecoverable(), is(false));
    assertThat(underTest.allowConcurrentRun(), is(true));
  }

  @Test
  public void testExposedWhenAllowCreationTrue() {
    ScriptTaskDescriptor underTest = createDescriptor(true, false);

    assertThat(underTest.isExposed(), is(true));
  }

  @Test
  public void testNotExposedWhenAllowCreationFalse() {
    ScriptTaskDescriptor underTest = createDescriptor(false, false);

    assertThat(underTest.isExposed(), is(false));
  }

  @Test
  public void testFormFieldsWhenNotClustered() {
    ScriptTaskDescriptor underTest = createDescriptor(true, false);

    List<FormField> formFields = underTest.getFormFields();
    assertThat(formFields.size(), is(2));

    FormField languageField = formFields.get(0);
    assertThat(languageField, instanceOf(StringTextFormField.class));
    assertThat(languageField.getId(), is(ScriptTaskDescriptor.LANGUAGE));
    assertThat(languageField.getType(), is("string"));
    assertThat(languageField.getLabel(), is("Language"));
    assertThat(languageField.getHelpText(), is("Script language"));
    assertThat(languageField.isRequired(), is(true));
    assertThat(languageField.getInitialValue(), is(ScriptEngineManagerProvider.DEFAULT_LANGUAGE));
    assertThat(languageField.isReadOnly(), is(false));

    FormField sourceField = formFields.get(1);
    assertThat(sourceField, instanceOf(TextAreaFormField.class));
    assertThat(sourceField.getId(), is(ScriptTaskDescriptor.SOURCE));
    assertThat(sourceField.getType(), is("text-area"));
    assertThat(sourceField.getLabel(), is("Source"));
    assertThat(sourceField.getHelpText(), is("Script source"));
    assertThat(sourceField.isRequired(), is(true));
  }

  @Test
  public void testSourceFieldNotReadOnlyWhenAllowCreationTrue() {
    ScriptTaskDescriptor underTest = createDescriptor(true, false);

    FormField sourceField = underTest.getFormFields().get(1);
    assertThat(sourceField.isReadOnly(), is(false));
  }

  @Test
  public void testSourceFieldReadOnlyWhenAllowCreationFalse() {
    ScriptTaskDescriptor underTest = createDescriptor(false, false);

    FormField sourceField = underTest.getFormFields().get(1);
    assertThat(sourceField.isReadOnly(), is(true));
  }

  @Test
  public void testMultinodeFormFieldWhenClustered() {
    ScriptTaskDescriptor underTest = createDescriptor(true, true);

    List<FormField> formFields = underTest.getFormFields();
    assertThat(formFields.size(), is(3));

    FormField multinodeField = formFields.get(2);
    assertThat(multinodeField, is(notNullValue()));
    assertThat(multinodeField, instanceOf(CheckboxFormField.class));
    assertThat(multinodeField.getId(), is(TaskDescriptorSupport.MULTINODE_KEY));
    assertThat(multinodeField.getType(), is("checkbox"));
    assertThat(multinodeField.getLabel(), is(TaskDescriptorSupport.MULTINODE_LABEL));
    assertThat(multinodeField.getHelpText(), is(TaskDescriptorSupport.MULTINODE_HELP));
    assertThat(multinodeField.isRequired(), is(false));
    assertThat(multinodeField.isReadOnly(), is(false));
  }

  @Test
  public void testNoMultinodeFormFieldWhenNotClustered() {
    ScriptTaskDescriptor underTest = createDescriptor(false, false);

    assertThat(underTest.getFormFields().size(), is(2));
  }

  @Test
  public void testDescriptorPropertiesWhenAllowCreationFalse() {
    ScriptTaskDescriptor underTest = createDescriptor(false, false);

    // only isExposed() is wired to allowCreation; these flags are fixed constants
    assertThat(underTest.getId(), is(ScriptTaskDescriptor.TYPE_ID));
    assertThat(underTest.getName(), is("Admin - Execute script"));
    assertThat(underTest.getType(), is(ScriptTask.class));
    assertThat(underTest.isVisible(), is(true));
    assertThat(underTest.isRecoverable(), is(false));
    assertThat(underTest.allowConcurrentRun(), is(true));
  }

  @Test
  public void testLanguageFieldNeverReadOnly() {
    // only the source field toggles read-only with allowCreation; language stays editable
    assertThat(createDescriptor(true, false).getFormFields().get(0).isReadOnly(), is(false));
    assertThat(createDescriptor(false, false).getFormFields().get(0).isReadOnly(), is(false));
  }

  @Test
  public void testMultinodeFormFieldWhenClusteredAndAllowCreationFalse() {
    // the multinode field depends only on clustering, independent of allowCreation
    ScriptTaskDescriptor underTest = createDescriptor(false, true);

    List<FormField> formFields = underTest.getFormFields();
    assertThat(formFields.size(), is(3));

    FormField multinodeField = formFields.get(2);
    assertThat(multinodeField, instanceOf(CheckboxFormField.class));
    assertThat(multinodeField.getId(), is(TaskDescriptorSupport.MULTINODE_KEY));

    // source remains read-only and the descriptor stays not-exposed because allowCreation is false
    assertThat(formFields.get(1).isReadOnly(), is(true));
    assertThat(underTest.isExposed(), is(false));
  }
}
