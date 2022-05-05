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
package org.sonatype.nexus.internal.orient;

import java.io.File;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Provider;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.orient.DatabaseExternalizer;
import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PasswordSanitizedJsonSource}.
 */
public class PasswordSanitizedJsonSourceTest
    extends TestSupport
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Captor
  private ArgumentCaptor<OutputStream> outputStreamArgumentCaptor;

  @Captor
  private ArgumentCaptor<Set<String>> setArgumentCaptor;

  @Mock
  private DatabaseInstance databaseInstance;

  @Mock
  private DatabaseExternalizer databaseExternalizer;

  private Provider<DatabaseInstance> databaseInstanceProvider = () -> databaseInstance;

  private PasswordSanitizedJsonSource underTest;

  private File exportFile;

  private ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Before
  public void setup() throws Exception {
    when(databaseInstance.externalizer()).thenReturn(databaseExternalizer);

    exportFile = temporaryFolder.newFile();

    underTest = new PasswordSanitizedJsonSource(Type.CONFIG, "path", databaseInstanceProvider);
  }

  @Test
  public void testGenerate_excludesSensitiveClassesFromDBExport() throws Exception {
    doAnswer(invocation -> {
      OutputStream os = (OutputStream) invocation.getArguments()[0];
      os.write("{\"records\": []}".getBytes(StandardCharsets.UTF_8));
      return null;
    }).when(databaseExternalizer).export(any(), any());

    underTest.generate(exportFile);

    verify(databaseExternalizer).export(outputStreamArgumentCaptor.capture(), setArgumentCaptor.capture());

    assertThat(setArgumentCaptor.getValue(), is(new HashSet<>(Arrays.asList("api_key", "usertoken_record"))));
  }

  @Test
  public void testGenerate_excludesSensitiveFieldsFromJson() throws Exception {
    doAnswer(invocation -> {
      OutputStream os = (OutputStream) invocation.getArguments()[0];
      os.write(
          ("{\"secret\": \"secret-key\", \"applicationPassword\": \"password\", \"systemPassword\": \"password\", " +
              "\"password\": \"password\", \"secretAccessKey\": \"password\", \"sessionToken\": \"sToken\", " +
              "\"destination_instance_password\": \"password\"}").getBytes(StandardCharsets.UTF_8));
      return null;
    }).when(databaseExternalizer).export(any(), any());

    underTest.generate(exportFile);

    Model model = mapper.readValue(exportFile, Model.class);

    assertThat(model.secret, is(PasswordSanitizedJsonSource.REPLACEMENT));
    assertThat(model.applicationPassword, is(PasswordSanitizedJsonSource.REPLACEMENT));
    assertThat(model.systemPassword, is(PasswordSanitizedJsonSource.REPLACEMENT));
    assertThat(model.password, is(PasswordSanitizedJsonSource.REPLACEMENT));
    assertThat(model.secretAccessKey, is(PasswordSanitizedJsonSource.REPLACEMENT));
    assertThat(model.sessionToken, is(PasswordSanitizedJsonSource.REPLACEMENT));
    assertThat(model.destinationInstancePassword, is(PasswordSanitizedJsonSource.REPLACEMENT));
  }

  @Test
  public void testGenerate_blankSensitiveFieldsNotReplaced() throws Exception {
    doAnswer(invocation -> {
      OutputStream os = (OutputStream) invocation.getArguments()[0];
      os.write(
          ("{\"secret\": \"\", \"password\": \"password\", \"secretAccessKey\": \"\"}")
              .getBytes(StandardCharsets.UTF_8));
      return null;
    }).when(databaseExternalizer).export(any(), any());

    underTest.generate(exportFile);

    Model model = mapper.readValue(exportFile, Model.class);

    assertThat(model.secret, is(""));
    assertThat(model.password, is(PasswordSanitizedJsonSource.REPLACEMENT));
    assertThat(model.secretAccessKey, is(""));
  }

  public static final class Model
  {
    public String secret;

    public String applicationPassword;

    public String systemPassword;

    public String password;

    public String secretAccessKey;

    public String sessionToken;

    @JsonProperty("destination_instance_password")
    public String destinationInstancePassword;
  }
}
