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
package org.sonatype.nexus.repository.http;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Request.Builder;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Status;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import com.google.common.collect.Range;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.net.HttpHeaders.CONTENT_RANGE;
import static com.google.common.net.HttpHeaders.ETAG;
import static com.google.common.net.HttpHeaders.IF_RANGE;
import static com.google.common.net.HttpHeaders.LAST_MODIFIED;
import static com.google.common.net.HttpHeaders.RANGE;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.ArrayUtils.subarray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;
import static org.sonatype.nexus.repository.http.HttpMethods.POST;
import static org.sonatype.nexus.repository.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.sonatype.nexus.repository.http.HttpStatus.NOT_IMPLEMENTED;
import static org.sonatype.nexus.repository.http.HttpStatus.OK;
import static org.sonatype.nexus.repository.http.HttpStatus.PARTIAL_CONTENT;
import static org.sonatype.nexus.repository.http.HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
import static org.sonatype.nexus.repository.view.ContentTypes.TEXT_PLAIN;
import static org.sonatype.nexus.repository.view.Status.failure;
import static org.sonatype.nexus.repository.view.Status.success;

public class PartialFetchHandlerTest
    extends TestSupport
{
  private static final String RANGE_HEADER = "theRangeHeader";

  private static final StringPayload PAYLOAD = new StringPayload("thePayload", TEXT_PLAIN);

  private static final String ETAG_VALUE = "\"theEtag\"";

  private static final String LAST_MODIFIED_VALUE = "Tue, 16 Jun 2015 11:31:00 GMT";

  private static final Range<Long> ZERO_TO_TWO_RANGE = Range.closed(0L, 2L);

  @Mock
  private Context context;

  @Mock
  private RangeParser rangeParser;

  private PartialFetchHandler underTest;

  @Before
  public void setup() {
    underTest = new PartialFetchHandler(rangeParser);
  }

  @Test
  public void testHandleWhenMethodIsNotGet() throws Exception {
    Request requestWithNonGetAction = createRequestBuilderForAction(POST).build();
    Response response = createOkResponseBuilder().build();

    assertThat(doHandle(requestWithNonGetAction, response), is(sameInstance(response)));
  }

  @Test
  public void testHandleWhenResponseIsNotOk() throws Exception {
    Request request = createGetRequestBuilder().build();
    Response errorResponse = createResponseBuilderForStatus(failure(INTERNAL_SERVER_ERROR)).build();

    assertThat(doHandle(request, errorResponse), is(sameInstance(errorResponse)));
  }

  @Test
  public void testHandleWhenPayloadIsNull() throws Exception {
    Request request = createGetRequestBuilder().build();
    Response responseWithNullPayload = createOkResponseBuilder().build();

    assertThat(doHandle(request, responseWithNullPayload), is(sameInstance(responseWithNullPayload)));
  }

  @Test
  public void testHandleWhenPayloadSizeIsUnknown() throws Exception {
    Request request = createGetRequestBuilder().build();
    Response response = createOkResponseBuilder().payload(createPayloadOfUnknownSize()).build();

    assertThat(doHandle(request, response), is(sameInstance(response)));
  }

  @Test
  public void testHandleWhenRangeHeaderIsNull() throws Exception {
    Request requestWithNullRangeHeader = createGetRequestBuilder().build();
    Response response = createOkResponseBuilder().payload(PAYLOAD).build();

    assertThat(doHandle(requestWithNullRangeHeader, response), is(sameInstance(response)));
  }

  @Test
  public void testHandleWhenRangeParserReturnsNull() throws Exception {
    Request request = createGetRequestBuilder().header(RANGE, RANGE_HEADER).build();
    Response response = createOkResponseBuilder().payload(PAYLOAD).build();
    when(rangeParser.parseRangeSpec(RANGE_HEADER, PAYLOAD.getSize())).thenReturn(null);

    Response actualResponse = doHandle(request, response);

    assertThat(actualResponse.getStatus().getCode(), is(REQUESTED_RANGE_NOT_SATISFIABLE));
    assertThat(actualResponse.getHeaders().get(CONTENT_RANGE), is("bytes */" + PAYLOAD.getSize()));
  }

  @Test
  public void testHandleWhenRangeParserReturnsEmptyRangeList() throws Exception {
    Request request = createGetRequestBuilder().header(RANGE, RANGE_HEADER).build();
    Response response = createOkResponseBuilder().payload(PAYLOAD).build();
    when(rangeParser.parseRangeSpec(RANGE_HEADER, PAYLOAD.getSize())).thenReturn(emptyList());

    assertThat(doHandle(request, response), is(sameInstance(response)));
  }

  @Test
  public void testHandleWhenRangeParserReturnsMultipleRanges() throws Exception {
    Request request = createGetRequestBuilder().header(RANGE, RANGE_HEADER).build();
    Response response = createOkResponseBuilder().payload(PAYLOAD).build();
    List<Range<Long>> multipleRanges = asList(ZERO_TO_TWO_RANGE, ZERO_TO_TWO_RANGE);
    when(rangeParser.parseRangeSpec(RANGE_HEADER, PAYLOAD.getSize())).thenReturn(multipleRanges);

    Response actualResponse = doHandle(request, response);

    assertThat(actualResponse.getStatus().getCode(), is(NOT_IMPLEMENTED));
    assertThat(actualResponse.getStatus().getMessage(), is("Multiple ranges not supported."));
  }

  @Test
  public void testHandle() throws Exception {
    Request request = createGetRequestBuilder().header(RANGE, RANGE_HEADER).build();
    Response response = createOkResponseBuilder()
        .header(ETAG, ETAG_VALUE).header(LAST_MODIFIED, LAST_MODIFIED_VALUE).payload(PAYLOAD).build();
    when(rangeParser.parseRangeSpec(RANGE_HEADER, PAYLOAD.getSize())).thenReturn(singletonList(ZERO_TO_TWO_RANGE));

    verifyHandleReturnsPartialResponse(request, response);
  }

  @Test
  public void testHandleWhenIfRangeCheckFailsToMatchOnEtag() throws Exception {
    Request request = createGetRequestBuilder()
        .header(RANGE, RANGE_HEADER).header(IF_RANGE, "\"aNonMatchingEtag\"").build();
    Response response = createOkResponseBuilder()
        .header(ETAG, ETAG_VALUE).header(LAST_MODIFIED, LAST_MODIFIED_VALUE).payload(PAYLOAD).build();
    when(rangeParser.parseRangeSpec(RANGE_HEADER, PAYLOAD.getSize())).thenReturn(singletonList(ZERO_TO_TWO_RANGE));

    assertThat(doHandle(request, response), is(sameInstance(response)));
  }

  @Test
  public void testHandleWhenIfRangeCheckMatchesOnEtag() throws Exception {
    Request request = createGetRequestBuilder().header(RANGE, RANGE_HEADER).header(IF_RANGE, ETAG_VALUE).build();
    Response response = createOkResponseBuilder()
        .header(ETAG, ETAG_VALUE).header(LAST_MODIFIED, LAST_MODIFIED_VALUE).payload(PAYLOAD).build();
    when(rangeParser.parseRangeSpec(RANGE_HEADER, PAYLOAD.getSize())).thenReturn(singletonList(ZERO_TO_TWO_RANGE));

    verifyHandleReturnsPartialResponse(request, response);
  }

  @Test
  public void testHandleWhenIfRangeCheckFailsToMatchOnLastModified() throws Exception {
    Request request = createGetRequestBuilder()
        .header(RANGE, RANGE_HEADER).header(IF_RANGE, "Wed, 17 Jun 2015 11:31:00 GMT").build();
    Response response = createOkResponseBuilder()
        .header(ETAG, ETAG_VALUE).header(LAST_MODIFIED, LAST_MODIFIED_VALUE).payload(PAYLOAD).build();
    when(rangeParser.parseRangeSpec(RANGE_HEADER, PAYLOAD.getSize())).thenReturn(singletonList(ZERO_TO_TWO_RANGE));

    assertThat(doHandle(request, response), is(sameInstance(response)));
  }

  @Test
  public void testHandleWhenIfRangeCheckFailsBecauseIfRangeIsNotValidDateTime() throws Exception {
    Request request = createGetRequestBuilder()
        .header(RANGE, RANGE_HEADER).header(IF_RANGE, "notValidDateTime").build();
    Response response = createOkResponseBuilder()
        .header(ETAG, ETAG_VALUE).header(LAST_MODIFIED, LAST_MODIFIED_VALUE).payload(PAYLOAD).build();
    when(rangeParser.parseRangeSpec(RANGE_HEADER, PAYLOAD.getSize())).thenReturn(singletonList(ZERO_TO_TWO_RANGE));

    assertThat(doHandle(request, response), is(sameInstance(response)));
  }

  @Test
  public void testHandleWhenIfRangeCheckFailsBecauseRequestLastModifiedIsNotValidDateTime() throws Exception {
    Request request = createGetRequestBuilder()
        .header(RANGE, RANGE_HEADER).header(IF_RANGE, LAST_MODIFIED_VALUE).build();
    Response response = createOkResponseBuilder()
        .header(ETAG, ETAG_VALUE).header(LAST_MODIFIED, "notValidDateTime").payload(PAYLOAD).build();
    when(rangeParser.parseRangeSpec(RANGE_HEADER, PAYLOAD.getSize())).thenReturn(singletonList(ZERO_TO_TWO_RANGE));

    assertThat(doHandle(request, response), is(sameInstance(response)));
  }

  @Test
  public void testHandleWhenIfRangeCheckMatchesOnLastModified() throws Exception {
    Request request = createGetRequestBuilder()
        .header(RANGE, RANGE_HEADER).header(IF_RANGE, LAST_MODIFIED_VALUE).build();
    Response response = createOkResponseBuilder()
        .header(ETAG, ETAG_VALUE).header(LAST_MODIFIED, LAST_MODIFIED_VALUE).payload(PAYLOAD).build();
    when(rangeParser.parseRangeSpec(RANGE_HEADER, PAYLOAD.getSize())).thenReturn(singletonList(ZERO_TO_TWO_RANGE));

    verifyHandleReturnsPartialResponse(request, response);
  }

  private void verifyHandleReturnsPartialResponse(final Request request, final Response response) throws Exception {
    Response actualResponse = doHandle(request, response);

    assertThat(actualResponse.getStatus().getCode(), is(PARTIAL_CONTENT));
    assertThat(bytesFromPayload(actualResponse.getPayload()), is(bytesFromPayloadInRange(PAYLOAD, ZERO_TO_TWO_RANGE)));
    String expectedContentRange =
        format("bytes %s-%s/%s", ZERO_TO_TWO_RANGE.lowerEndpoint(), ZERO_TO_TWO_RANGE.upperEndpoint(),
            PAYLOAD.getSize());
    assertThat(actualResponse.getHeaders().get(CONTENT_RANGE), is(expectedContentRange));
    assertThat(actualResponse.getHeaders().get(ETAG), is(ETAG_VALUE));
    assertThat(actualResponse.getHeaders().get(LAST_MODIFIED), is(LAST_MODIFIED_VALUE));
  }

  private Response doHandle(final Request request, final Response response) throws Exception {
    when(context.proceed()).thenReturn(response);
    when(context.getRequest()).thenReturn(request);
    return underTest.handle(context);
  }

  private static byte[] bytesFromPayload(final Payload payload) throws Exception {
    return toByteArray(payload.openInputStream());
  }

  private static byte[] bytesFromPayloadInRange(final Payload payload, final Range<Long> range) throws Exception {
    int startIndexInclusive = range.lowerEndpoint().intValue();
    int endIndexExclusive = range.upperEndpoint().intValue() + 1;
    return subarray(bytesFromPayload(payload), startIndexInclusive, endIndexExclusive);
  }

  private static Request.Builder createGetRequestBuilder() {
    return createRequestBuilderForAction(GET);
  }

  private static Request.Builder createRequestBuilderForAction(final String action) {
    return new Builder().path("/foo").action(action);
  }

  private static Response.Builder createOkResponseBuilder() {
    return createResponseBuilderForStatus(success(OK));
  }

  private static Response.Builder createResponseBuilderForStatus(final Status status) {
    return new Response.Builder().status(status);
  }

  private static Payload createPayloadOfUnknownSize() {
    Payload payloadOfUnknownSize = mock(Payload.class);
    when(payloadOfUnknownSize.getSize()).thenReturn(Payload.UNKNOWN_SIZE);
    return payloadOfUnknownSize;
  }
}
