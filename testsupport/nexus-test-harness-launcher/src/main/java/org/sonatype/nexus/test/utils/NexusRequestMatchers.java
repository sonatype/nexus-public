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
package org.sonatype.nexus.test.utils;

import java.io.IOException;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.TypeSafeMatcher;
import org.restlet.data.Reference;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class NexusRequestMatchers
{

  // **************** Status Matchers ********************

  public static abstract class BaseStatusMatcher
      extends TypeSafeMatcher<Status>
  {
    @Override
    protected void describeMismatchSafely(Status status, Description mismatchDescription) {
      mismatchDescription.appendText("was ").appendText(status.toString());
    }
  }

  public static class HasCode
      extends BaseStatusMatcher
  {
    private int expectedCode;

    public HasCode(final int expectedCode) {
      this.expectedCode = expectedCode;
    }

    @Override
    protected boolean matchesSafely(Status item) {
      return item.getCode() == expectedCode;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("status code of ").appendValue(this.expectedCode);
    }
  }

  public static class IsSuccess
      extends BaseStatusMatcher
  {

    @Override
    protected boolean matchesSafely(Status item) {
      return item.isSuccess();
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("success");
    }
  }

  public static class IsError
      extends BaseStatusMatcher
  {

    @Override
    protected boolean matchesSafely(Status item) {
      return item.isError();
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("error");
    }
  }

  public static class IsClientError
      extends BaseStatusMatcher
  {

    @Override
    protected boolean matchesSafely(Status item) {
      return item.isClientError();
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("client error");
    }
  }

  // **************** Response Matchers ******************

  public static abstract class BaseResponseMatcher
      extends TypeSafeMatcher<Response>
  {
    @Override
    protected void describeMismatchSafely(Response item, Description mismatchDescription) {
      mismatchDescription.appendText(item.getStatus().toString());

      // provide some more info if it's validation error
      if (item.getStatus().getCode() == 400) {
        try {
          mismatchDescription.appendText(item.getEntity().getText());
        }
        catch (IOException e) {
          mismatchDescription.appendText("response entity could not be converted to text: " + e.getMessage());
        }
      }
    }
  }

  public static class RespondsWithStatusCode
      extends BaseResponseMatcher
  {

    private int expectedStatusCode;

    public RespondsWithStatusCode(int expectedStatusCode) {
      this.expectedStatusCode = expectedStatusCode;
    }

    @Override
    protected boolean matchesSafely(Response item) {
      return item.getStatus().getCode() == expectedStatusCode;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("Status code " + expectedStatusCode);
    }
  }

  public static class InError
      extends BaseResponseMatcher
  {

    @Override
    protected boolean matchesSafely(Response resp) {
      return resp.getStatus().isError();
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("response status in error");
    }

  }

  public static class IsSuccessful
      extends BaseResponseMatcher
  {

    @Override
    protected boolean matchesSafely(Response resp) {
      return resp.getStatus().isSuccess();
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("response status to indicate success status 200");
    }

  }

  public static class ResponseTextMatches
      extends BaseResponseMatcher
  {

    private final Matcher<String> matcher;

    private IOException exception;

    public ResponseTextMatches(Matcher<String> matcher) {
      this.matcher = matcher;
    }

    @Override
    public void describeTo(Description description) {
      if (exception != null) {
        description.appendText("got exception " + exception.getMessage());
      }
      else {
        description.appendText("response text is ").appendDescriptionOf(matcher);
      }
    }

    @Override
    protected boolean matchesSafely(Response item) {
      final Representation entity = item.getEntity();
      MatcherAssert.assertThat(entity, CoreMatchers.notNullValue());
      String responseText;
      try {
        responseText = entity.getText();
      }
      catch (IOException e) {
        this.exception = e;
        return false;
      }
      return matcher.matches(responseText);
    }

  }

  public static class RedirectLocationMatches
      extends BaseResponseMatcher
  {

    private final Matcher<String> matcher;

    public RedirectLocationMatches(Matcher<String> matcher) {
      this.matcher = matcher;
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("redirect location is ").appendDescriptionOf(matcher);
    }

    @Override
    protected boolean matchesSafely(Response item) {
      final Reference ref = item.getLocationRef();
      MatcherAssert.assertThat(ref, notNullValue());
      return matcher.matches(ref.toString());
    }
  }

  public static class IsRedirecting
      extends BaseResponseMatcher
  {

    @Override
    public void describeTo(Description description) {
      description.appendText("redirecting");
    }

    @Override
    protected boolean matchesSafely(Response item) {
      Status status = item.getStatus();
      assertThat(status, notNullValue());
      return status.isRedirection();
    }

  }

  public static abstract class BaseCodeMatcher
      extends TypeSafeMatcher<Integer>
  {
    @Override
    protected void describeMismatchSafely(Integer code, Description mismatchDescription) {
      mismatchDescription.appendText("was ").appendText(code.toString());
    }
  }

  public static class IsSuccessfulCode
      extends BaseCodeMatcher
  {
    @Override
    protected boolean matchesSafely(Integer resp) {
      return Status.isSuccess(resp);
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("response status to indicate success status 20x");
    }
  }

  @Factory
  public static <T> IsSuccess isSuccess() {
    return new IsSuccess();
  }

  @Factory
  public static <T> IsError isError() {
    return new IsError();
  }

  @Factory
  public static <T> IsClientError isClientError() {
    return new IsClientError();
  }

  @Factory
  public static <T> HasCode hasStatusCode(int expectedCode) {
    return new HasCode(expectedCode);
  }

  @Factory
  public static <T> RespondsWithStatusCode respondsWithStatus(Status status) {
    return respondsWithStatusCode(status.getCode());
  }

  @Factory
  public static <T> RespondsWithStatusCode respondsWithStatusCode(final int expectedStatusCode) {
    return new RespondsWithStatusCode(expectedStatusCode);
  }

  @Factory
  public static <T> InError inError() {
    return new InError();
  }

  @Factory
  public static <T> IsSuccessful isSuccessful() {
    return new IsSuccessful();
  }

  @Factory
  public static <T> ResponseTextMatches textMatches(Matcher<String> matcher) {
    return new ResponseTextMatches(matcher);
  }

  @Factory
  public static <T> IsSuccessfulCode isSuccessCode() {
    return new IsSuccessfulCode();
  }
}
