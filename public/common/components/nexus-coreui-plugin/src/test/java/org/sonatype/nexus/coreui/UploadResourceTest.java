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
package org.sonatype.nexus.coreui;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Validator;

import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.coreui.UploadResource.ErrorPacket;
import org.sonatype.nexus.coreui.UploadResource.Packet;
import org.sonatype.nexus.coreui.internal.UploadService;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ValidationExtension.class)
@ExtendWith(AuthenticationExtension.class)
@WithUser
class UploadResourceTest
{
  @ValidationExecutor
  private final Validator validator =
      new ValidationConfiguration().validatorFactory(new ConstraintValidatorFactoryImpl()).getValidator();

  @Mock
  private UploadService uploadService;

  @Mock
  private HttpServletRequest request;

  private ObjectMapper objectMapper;

  private UploadResource underTest;

  @BeforeEach
  void setup() {
    objectMapper = new ObjectMapper();
    underTest = new UploadResource(uploadService, objectMapper);
  }

  @Test
  void postComponent_successfulUpload() throws IOException {
    when(uploadService.upload("test-repo", request)).thenReturn("/path/to/component");

    String result = underTest.postComponent("test-repo", request);

    assertThat(result, is(notNullValue()));
    assertThat(result, containsString("\"success\":true"));
    assertThat(result, containsString("\"data\":\"/path/to/component\""));
  }

  @Test
  void postComponent_uploadFailure_returnsErrorPacket() throws IOException {
    when(uploadService.upload("test-repo", request)).thenThrow(new IOException("Upload failed"));

    String result = underTest.postComponent("test-repo", request);

    assertThat(result, is(notNullValue()));
    assertThat(result, containsString("\"success\":false"));
    assertThat(result, containsString("\"message\":\"Upload failed\""));
    assertThat(result, containsString("\"action\":\"upload\""));
    assertThat(result, containsString("\"method\":\"upload\""));
    assertThat(result, containsString("\"type\":\"rpc\""));
    assertThat(result, containsString("\"tid\":1"));
  }

  @Test
  void postComponent_runtimeException_returnsErrorPacket() throws IOException {
    when(uploadService.upload("test-repo", request)).thenThrow(new RuntimeException("Something broke"));

    String result = underTest.postComponent("test-repo", request);

    assertThat(result, containsString("\"success\":false"));
    assertThat(result, containsString("\"message\":\"Something broke\""));
  }

  @Test
  void packet_hasCorrectProperties() {
    Packet packet = new Packet("test-data");

    assertThat(packet.isSuccess(), is(true));
    assertThat(packet.getData(), is("test-data"));
  }

  @Test
  void packet_withNullData() {
    Packet packet = new Packet(null);

    assertThat(packet.isSuccess(), is(true));
    assertThat(packet.getData(), is((String) null));
  }

  @Test
  void errorPacket_hasCorrectProperties() {
    ErrorPacket errorPacket = new ErrorPacket("error message");

    assertThat(errorPacket.isSuccess(), is(false));
    assertThat(errorPacket.getTid(), is(1));
    assertThat(errorPacket.getAction(), is("upload"));
    assertThat(errorPacket.getMethod(), is("upload"));
    assertThat(errorPacket.getType(), is("rpc"));
    assertThat(errorPacket.getMessage(), is("error message"));
  }

  @Test
  void errorPacket_withNullMessage() {
    ErrorPacket errorPacket = new ErrorPacket(null);

    assertThat(errorPacket.isSuccess(), is(false));
    assertThat(errorPacket.getMessage(), is((String) null));
  }

  @Test
  void postComponent_successfulUpload_returnsValidJson() throws IOException {
    when(uploadService.upload("test-repo", request)).thenReturn("search/term");

    String result = underTest.postComponent("test-repo", request);

    // Verify it can be parsed as JSON
    Object parsed = objectMapper.readValue(result, Object.class);
    assertThat(parsed, is(notNullValue()));
  }

  @Test
  void postComponent_error_returnsValidJson() throws IOException {
    when(uploadService.upload("test-repo", request)).thenThrow(new IOException("fail"));

    String result = underTest.postComponent("test-repo", request);

    // Verify it can be parsed as JSON
    Object parsed = objectMapper.readValue(result, Object.class);
    assertThat(parsed, is(notNullValue()));
  }
}
