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
package org.sonatype.nexus.repository.search;

import org.sonatype.nexus.rest.ValidationErrorsException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchFilterValidatorTest
{
  @Test
  void nullValueIsAccepted() {
    assertDoesNotThrow(() -> SearchFilterValidator.validate(null));
  }

  @Test
  void emptyValueIsAccepted() {
    assertDoesNotThrow(() -> SearchFilterValidator.validate(""));
  }

  @Test
  void ordinaryValuesAreAccepted() {
    assertDoesNotThrow(() -> SearchFilterValidator.validate("junit"));
    assertDoesNotThrow(() -> SearchFilterValidator.validate("org.apache.commons"));
    assertDoesNotThrow(() -> SearchFilterValidator.validate("test'quote"));
    assertDoesNotThrow(() -> SearchFilterValidator.validate("with\ttab-and-newline\n"));
  }

  @Test
  void nulByteIsRejected() {
    assertThrows(ValidationErrorsException.class, () -> SearchFilterValidator.validate("test\u0000x"));
    assertThrows(ValidationErrorsException.class, () -> SearchFilterValidator.validate("\u0000"));
    assertThrows(ValidationErrorsException.class, () -> SearchFilterValidator.validate("leading\u0000"));
    assertThrows(ValidationErrorsException.class, () -> SearchFilterValidator.validate("\u0000trailing"));
    assertThrows(ValidationErrorsException.class, () -> SearchFilterValidator.validate("mid\u0000dle"));
  }

  /*
   * Contract test: freezes the user-facing error message so accidental changes are caught.
   * Do not soften this assertion — the message is part of the public API contract.
   */
  @Test
  void rejectionMessageMatchesUserFacingContract() {
    ValidationErrorsException e = assertThrows(ValidationErrorsException.class,
        () -> SearchFilterValidator.validate("anything\u0000here"));
    assertThat(e.getMessage(), is("Search parameters must not contain NUL (0x00) bytes"));
  }

  /*
   * NEXUS-54105 reproduction paths as decoded by the servlet container. Encoding each path
   * literally, alongside the assertion, keeps the ticket's evidence auditable in-tree and
   * prevents accidental drift when the validator is later refactored.
   */

  // From ticket: /service/rest/v1/search?q=test%00x — %00 decodes to \u0000.
  @Test
  void ticketPath_qWithNulByte_isRejected() {
    assertThrows(ValidationErrorsException.class, () -> SearchFilterValidator.validate("test\u0000x"));
  }

  // From ticket: /service/rest/v1/search/assets?q=test%00x&format=maven2 — the assets endpoint
  // shares the same validator; both q and format must be checked independently.
  @Test
  void ticketPath_qWithNulByteOnAssetsEndpoint_isRejected() {
    assertThrows(ValidationErrorsException.class, () -> SearchFilterValidator.validate("test\u0000x"));
    assertDoesNotThrow(() -> SearchFilterValidator.validate("maven2"));
  }

  // From ticket: /service/rest/v1/search?q=%c0%ae%c0%ae%00x — %c0%ae is an overlong UTF-8
  // encoding of '.'. Jetty rejects overlong sequences at the connector level, so what actually
  // reaches this validator is either a decoded '..' or nothing at all. Either way the trailing
  // %00 still fails the validator. Asserting the NUL-portion here documents the split of
  // responsibility: overlong UTF-8 is the container's job, NUL is ours.
  @Test
  void ticketPath_qWithOverlongUtf8AndNulByte_isRejected() {
    assertThrows(ValidationErrorsException.class, () -> SearchFilterValidator.validate("..\u0000x"));
  }

  // From ticket: /service/rest/v1/search?q=test%27-- — %27 decodes to a single quote, forming
  // a SQL-injection-shaped pattern. This validator is intentionally scoped to NUL bytes;
  // downstream tsquery escaping is what neutralises the quote, so the payload must pass here
  // without a 400. This test locks the contract in place so future changes don't silently start
  // rejecting punctuation.
  @Test
  void ticketPath_qWithSingleQuoteAndCommentMarker_isAccepted() {
    assertDoesNotThrow(() -> SearchFilterValidator.validate("test'--"));
  }

  // From ticket: /service/rest/v1/search?q=test&continuationToken=eyJ0ZXN0IjoidGVzdCJ9 — a
  // well-formed base64-ish token must never trip this validator; only NUL is disallowed.
  @Test
  void ticketPath_validBase64ContinuationToken_isAccepted() {
    assertDoesNotThrow(() -> SearchFilterValidator.validate("eyJ0ZXN0IjoidGVzdCJ9"));
  }
}
