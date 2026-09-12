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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.coreui.UploadResource.ErrorPacket;
import org.sonatype.nexus.coreui.UploadResource.Packet;
import org.sonatype.nexus.coreui.internal.UploadService;
import org.sonatype.nexus.repository.ConcurrentOperationException;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.RedeployDisabledException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension.WithUser;
import org.sonatype.nexus.testcommon.validation.ValidationExtension;
import org.sonatype.nexus.testcommon.validation.ValidationExtension.ValidationExecutor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarementors.extjs.djn.EncodingUtils;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorFactoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
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

  @Mock
  private Repository repository;

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

    Response response = underTest.postComponent("test-repo", request);

    assertThat(response.getStatus(), is(200));
    String body = (String) response.getEntity();
    assertThat(body, is(notNullValue()));
    assertThat(body, containsString("\"success\":true"));
    assertThat(body, containsString("\"data\":\"/path/to/component\""));
  }

  @Test
  void postComponent_uploadFailure_returnsErrorPacket() throws IOException {
    when(uploadService.upload("test-repo", request)).thenThrow(new IOException("Upload failed"));

    Response response = underTest.postComponent("test-repo", request);

    // NEXUS-53344: non-policy failures keep the legacy 200 + success:false envelope so the
    // existing UI error-display path continues to work for every other upload format.
    assertThat(response.getStatus(), is(200));
    String body = (String) response.getEntity();
    assertThat(body, is(notNullValue()));
    assertThat(body, containsString("\"success\":false"));
    assertThat(body, containsString("\"message\":\"Upload failed\""));
    assertThat(body, containsString("\"action\":\"upload\""));
    assertThat(body, containsString("\"method\":\"upload\""));
    assertThat(body, containsString("\"type\":\"rpc\""));
    assertThat(body, containsString("\"tid\":1"));
  }

  @Test
  void postComponent_runtimeException_returnsErrorPacket() throws IOException {
    when(uploadService.upload("test-repo", request)).thenThrow(new RuntimeException("Something broke"));

    Response response = underTest.postComponent("test-repo", request);

    assertThat(response.getStatus(), is(200));
    String body = (String) response.getEntity();
    assertThat(body, containsString("\"success\":false"));
    assertThat(body, containsString("\"message\":\"Something broke\""));
  }

  @Test
  void postComponent_concurrentOperationException_returns409() throws IOException {
    when(uploadService.upload("test-repo", request))
        .thenThrow(new ConcurrentOperationException(
            "Upload failed: component was concurrently deleted. Please retry."));

    Response response = underTest.postComponent("test-repo", request);

    assertThat(response.getStatus(), is(409));
    String body = (String) response.getEntity();
    assertThat(body, containsString("\"success\":false"));
    assertThat(body, containsString("Upload failed: component was concurrently deleted"));
  }

  @Test
  void postComponent_illegalOperationException_returns400WithEnvelope() throws IOException {
    // NEXUS-53344: read-only deployment policy and duplicate-asset rejections surface as
    // IllegalOperationException from the facet layer (see TerraformHostedFacetImpl).
    // UploadResource maps that to HTTP 400 while keeping the same JSON envelope shape so the
    // React form machine and the legacy ExtJS form continue to render the error message.
    when(uploadService.upload("test-repo", request))
        .thenThrow(new IllegalOperationException("Repository is read only: test-repo"));

    Response response = underTest.postComponent("test-repo", request);

    assertThat(response.getStatus(), is(400));
    assertThat(response.getMediaType().toString(), is(MediaType.APPLICATION_JSON));
    String body = (String) response.getEntity();
    assertThat(body, containsString("\"success\":false"));
    assertThat(body, containsString("\"message\":\"Repository is read only: test-repo\""));
  }

  @Test
  void postComponent_validationErrorsException_propagatesToMapper() throws IOException {
    // NEXUS-54217: ValidationErrorsException is rethrown so JAX-RS' ValidationErrorsExceptionMapper
    // maps it to HTTP 400 automatically. Verify the exception propagates out of the resource
    // instead of being swallowed by the generic catch (Exception) and logged at ERROR.
    ValidationErrorsException thrown = new ValidationErrorsException("Invalid npm package name: bad");
    when(uploadService.upload("test-repo", request)).thenThrow(thrown);

    ValidationErrorsException actual = assertThrows(ValidationErrorsException.class,
        () -> underTest.postComponent("test-repo", request));
    assertThat(actual, is(thrown));
  }

  @Test
  void postComponentWithHtmlResponse_validationErrorsException_propagatesToMapper() throws IOException {
    // NEXUS-54217: same behavior on the HTML response path — let the mapper handle it.
    ValidationErrorsException thrown = new ValidationErrorsException("Invalid npm package name: bad");
    when(uploadService.upload("test-repo", request)).thenThrow(thrown);

    ValidationErrorsException actual = assertThrows(ValidationErrorsException.class,
        () -> underTest.postComponentWithHtmlResponse("test-repo", request));
    assertThat(actual, is(thrown));
  }

  @Test
  void postComponentWithHtmlResponse_success_wrapsBodyAndSetsHtmlContentType() throws IOException {
    // NEXUS-53344: the legacy iframe-based upload path consumes a text/html response whose body
    // is the same ExtJS-RPC envelope wrapped in <textarea>. Verify both the Content-Type and the
    // wrapper survive the doUpload refactor.
    //
    // EncodingUtils.htmlEncode (from directjngine) transitively requires the legacy commons-lang
    // artifact, which is supplied by the runtime classpath in production but not by the focused
    // unit-test classpath. Mocking the static call keeps this test self-contained and asserts
    // the structural contract that matters: the wrapper survives the refactor.
    when(uploadService.upload("test-repo", request)).thenReturn("/path/to/component");

    Response response;
    try (MockedStatic<EncodingUtils> encoding = mockStatic(EncodingUtils.class)) {
      encoding.when(() -> EncodingUtils.htmlEncode(anyString())).thenAnswer(inv -> inv.getArgument(0));
      response = underTest.postComponentWithHtmlResponse("test-repo", request);
    }

    assertThat(response.getStatus(), is(200));
    assertThat(response.getMediaType().toString(), is(MediaType.TEXT_HTML));
    String body = (String) response.getEntity();
    assertThat(body, containsString("<html><body><textarea>"));
    assertThat(body, containsString("</textarea></body></html>"));
    assertThat(body, containsString("\"success\":true"));
    assertThat(body, containsString("\"data\":\"/path/to/component\""));
  }

  @Test
  void postComponentWithHtmlResponse_illegalOperationException_returns400HtmlEnvelope() throws IOException {
    // NEXUS-53344: the HTML response path must also map IllegalOperationException to HTTP 400
    // while preserving the textarea-wrapped envelope so the legacy form surfaces the message.
    when(uploadService.upload("test-repo", request))
        .thenThrow(new IllegalOperationException("Repository is read only: test-repo"));

    Response response;
    try (MockedStatic<EncodingUtils> encoding = mockStatic(EncodingUtils.class)) {
      encoding.when(() -> EncodingUtils.htmlEncode(anyString())).thenAnswer(inv -> inv.getArgument(0));
      response = underTest.postComponentWithHtmlResponse("test-repo", request);
    }

    assertThat(response.getStatus(), is(400));
    assertThat(response.getMediaType().toString(), is(MediaType.TEXT_HTML));
    String body = (String) response.getEntity();
    assertThat(body, containsString("<html><body><textarea>"));
    assertThat(body, containsString("</textarea></body></html>"));
    assertThat(body, containsString("\"success\":false"));
    assertThat(body, containsString("Repository is read only: test-repo"));
  }

  @Test
  void postComponent_redeployDisabledException_returns409() throws IOException {
    // RedeployDisabledException (extends IllegalOperationException) must be caught before
    // IllegalOperationException — this test guards the catch-ordering constraint.
    // Exception is constructed before the when() call to avoid triggering Mockito's
    // unfinished-stubbing detection when getName() is invoked inside the constructor.
    when(repository.getName()).thenReturn("test-repo");
    RedeployDisabledException ex = new RedeployDisabledException(repository, "/path/to/asset",
        "asset already exists and re-deploy is not allowed");
    when(uploadService.upload("test-repo", request)).thenThrow(ex);

    Response response = underTest.postComponent("test-repo", request);

    assertThat(response.getStatus(), is(409));
    assertThat(response.getMediaType().toString(), is(MediaType.APPLICATION_JSON));
    String body = (String) response.getEntity();
    assertThat(body, containsString("\"success\":false"));
    assertThat(body, containsString("already exists and re-deploy is not allowed"));
  }

  @Test
  void postComponentWithHtmlResponse_redeployDisabledException_returns409() throws IOException {
    // HTML response path must also return 409 for RedeployDisabledException.
    when(repository.getName()).thenReturn("test-repo");
    RedeployDisabledException ex = new RedeployDisabledException(repository, "/path/to/asset",
        "asset already exists and re-deploy is not allowed");
    when(uploadService.upload("test-repo", request)).thenThrow(ex);

    Response response;
    try (MockedStatic<EncodingUtils> encoding = mockStatic(EncodingUtils.class)) {
      encoding.when(() -> EncodingUtils.htmlEncode(anyString())).thenAnswer(inv -> inv.getArgument(0));
      response = underTest.postComponentWithHtmlResponse("test-repo", request);
    }

    assertThat(response.getStatus(), is(409));
    assertThat(response.getMediaType().toString(), is(MediaType.TEXT_HTML));
    String body = (String) response.getEntity();
    assertThat(body, containsString("<html><body><textarea>"));
    assertThat(body, containsString("</textarea></body></html>"));
    assertThat(body, containsString("\"success\":false"));
    assertThat(body, containsString("already exists and re-deploy is not allowed"));
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

    Response response = underTest.postComponent("test-repo", request);

    // Verify it can be parsed as JSON
    Object parsed = objectMapper.readValue((String) response.getEntity(), Object.class);
    assertThat(parsed, is(notNullValue()));
  }

  @Test
  void postComponent_error_returnsValidJson() throws IOException {
    when(uploadService.upload("test-repo", request)).thenThrow(new IOException("fail"));

    Response response = underTest.postComponent("test-repo", request);

    // Verify it can be parsed as JSON
    Object parsed = objectMapper.readValue((String) response.getEntity(), Object.class);
    assertThat(parsed, is(notNullValue()));
  }
}
