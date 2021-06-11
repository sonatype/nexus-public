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
package org.sonatype.nexus.testsuite.testsupport.rest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.ValidationErrorXO;
import org.sonatype.nexus.testsuite.testsupport.system.NexusTestSystemSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.testsuite.testsupport.system.RestTestHelper.assertValidationResponse;
import static org.sonatype.nexus.testsuite.testsupport.system.RestTestHelper.hasStatus;

/**
 * Factory for {@link ApiVerifier} which assists with testing REST endpoitns
 * @since 3.31
 */
@Named
@Singleton
public class ApiVerifierFactory
    extends ComponentSupport
{
  private NexusTestSystemSupport<?, ?> nexus;

  @Inject
  public ApiVerifierFactory(final NexusTestSystemSupport<?, ?> nexus) {
    this.nexus = nexus;
  }

  /**
   * @param <P> the type of the request payload
   * @param requestFn a BiFunction which accepts a username, and a payload making the remote request.
   * @param unauthorizedPrivilege a privilege to use for unauthorized requests
   * @param requiredPrivileges the set or privileges required for authorized requests
   */
  public <P> ApiVerifier<P> create(
      final BiFunction<String, P, Response> requestFn,
      final String unauthorizedPrivilege,
      final String... requiredPrivileges)
  {
    return new ApiVerifier<>(requestFn, unauthorizedPrivilege, requiredPrivileges);
  }

  /**
   * @param requestFn a Function which accepts a username and makes the remote request
   * @param unauthorizedPrivilege a privilege to use for unauthorized requests
   * @param requiredPrivileges the set or privileges required for authorized requests
   */
  public ApiVerifier<?> create(
      final Function<String, Response> requestFn,
      final String unauthorizedPrivilege,
      final String... requiredPrivileges)
  {
    return new ApiVerifier<>((user, x) -> requestFn.apply(user), unauthorizedPrivilege, requiredPrivileges);
  }

  /**
   * Create an {@link ApiVerifier} for GET requests to the provided endpoint.
   *
   * @param <P> the payload type, ignored for GET requests
   * @param path the request path
   * @param unauthorizedPrivilege the privilege to test the endpoint for unauthorized access
   * @param requiredPrivileges the privileges required to access the endpoint
   */
  public <P> ApiVerifier<P> forGet(
      final String path,
      final String unauthorizedPrivilege,
      final String... requiredPrivileges)
  {
    return create(handle((user, payload) -> nexus.rest().get(path, user, user)), unauthorizedPrivilege,
        requiredPrivileges);
  }

  /**
   * Create an {@link ApiVerifier} for POST requests to the provided endpoint.
   *
   * @param <P> the payload type
   * @param path the request path
   * @param unauthorizedPrivilege the privilege to test the endpoint for unauthorized access
   * @param requiredPrivileges the privileges required to access the endpoint
   */
  public <P> ApiVerifier<P> forPost(
      final String path,
      final String unauthorizedPrivilege,
      final String... requiredPrivileges)
  {
    return create(handle((user, payload) -> nexus.rest().post(path, payload, user, user)), unauthorizedPrivilege,
        requiredPrivileges);
  }

  /**
   * Create an {@link ApiVerifier} for PUT requests to the provided endpoint.
   *
   * @param <P> the payload type
   * @param path the request path
   * @param unauthorizedPrivilege the privilege to test the endpoint for unauthorized access
   * @param requiredPrivileges the privileges required to access the endpoint
   */
  public <P> ApiVerifier<P> forPut(
      final String path,
      final String unauthorizedPrivilege,
      final String... requiredPrivileges)
  {
    return create(handle((user, payload) -> nexus.rest().put(path, payload, user, user)), unauthorizedPrivilege,
        requiredPrivileges);
  }

  /**
   * Create an {@link ApiVerifier} for DELETE requests to the provided endpoint.
   *
   * @param <P> the payload type, ignored for DELETE requests
   * @param path the request path
   * @param unauthorizedPrivilege the privilege to test the endpoint for unauthorized access
   * @param requiredPrivileges the privileges required to access the endpoint
   */
  public <P> ApiVerifier<P> forDelete(
      final String path,
      final String unauthorizedPrivilege,
      final String... requiredPrivileges)
  {
    return create(handle((user, payload) -> nexus.rest().delete(path, user, user)), unauthorizedPrivilege,
        requiredPrivileges);
  }

  private static <U, P, R> BiFunction<U, P, R> handle(final ExceptionThrowingBiFunction<U, P, R> fn) {
    return (u, p) -> {
      try {
        return fn.apply(u, p);
      }
      catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

  @FunctionalInterface
  private interface ExceptionThrowingBiFunction<T, U, R> {
    R apply(T t, U u) throws IOException;
  }

  /**
   * A utility class to help tests verify the behaviour of a REST endpoint
   */
  public class ApiVerifier<P>
  {
    private final BiFunction<String, P, Response> requestFn;

    private final String[] requiredPrivileges;

    private final String unauthorizedPrivilege;

    private ApiVerifier(
        final BiFunction<String, P, Response> requestFn,
        final String unauthorizedPrivilege,
        final String... requiredPrivileges)
    {
      this.requestFn = requestFn;
      this.unauthorizedPrivilege = unauthorizedPrivilege;
      this.requiredPrivileges = requiredPrivileges;
    }

    /**
     * Verifies that a request without authentication fails, generally used to test with GET or DELETE.
     * If the requesting function expects a payload calling this may throw an exception.
     */
    public ApiVerifier<P> assertUnauthenticatedAccess() {
      return assertUnauthenticatedAccess(null);
    }

    /**
     * Verifies that a request without authentication fails.
     */
    public ApiVerifier<P> assertUnauthenticatedAccess(@Nullable final P payload) {
      try {
        nexus.security().setAnonymousEnabled(false);

        String unauthorizedUser = nexus.security().createUserWithPrivileges("", unauthorizedPrivilege).getUserId();
        Response response = requestFn.apply(unauthorizedUser, payload);
        assertThat(response, hasStatus(403));

        return this;
      }
      finally {
        nexus.security().setAnonymousEnabled(true);
      }
    }

    /**
     * Verifies that an unauthorized user cannot access the endpoint, generally used to test with GET or DELETE.
     * If the requesting function expects a payload calling this may throw an exception.
     */
    public ApiVerifier<P> assertUnauthorizedAccess() {
      return assertUnauthorizedAccess(null);
    }

    /**
     * Verifies that an unauthorized user cannot access the endpoint
     */
    public ApiVerifier<P> assertUnauthorizedAccess(@Nullable final P payload) {
      String unauthorizedUser = nexus.security().createUserWithPrivileges("", unauthorizedPrivilege).getUserId();
      Response response = requestFn.apply(unauthorizedUser, payload);
      assertThat(response, hasStatus(403));

      return this;
    }

    /**
     * Verifies that authorized requests to the endpoint succeed and returns the expected result, generally used to test
     * with GET or DELETE. If the requesting function expects a payload calling this may throw an exception.
     *
     * @param responseVerifier called with the response to verify the body of the response.
     */
    public ApiVerifier<P> assertAccess(final Consumer<Response> responseVerifier) {
      return assertAccess(null, responseVerifier);
    }

    /**
     * Verifies that authorized requests to the endpoint succeed and returns the expected result.
     *
     * @param responseVerifier called with the response to verify the body of the response.
     */
    public ApiVerifier<P> assertAccess(final P payload, final Consumer<Response> responseVerifier) {
      String authorizedUser = nexus.security().createUserWithPrivileges("", requiredPrivileges).getUserId();
      Response response = requestFn.apply(authorizedUser, payload);

      responseVerifier.accept(response);

      return this;
    }

    /**
     * Verifies that an invalid payload results in a response containing the specified validation errors.
     *
     * @param payload the invalid payload to use for the request
     * @param expectedErrors the validation errors expected to comprise the response
     */
    public ApiVerifier<P> assertValidationError(final P payload, final ValidationErrorXO... expectedErrors) {
      String authorizedUser = nexus.security().createUserWithPrivileges("", requiredPrivileges).getUserId();
      Response response = requestFn.apply(authorizedUser, payload);

      assertValidationResponse(response, expectedErrors);

      return this;
    }
  }
}
