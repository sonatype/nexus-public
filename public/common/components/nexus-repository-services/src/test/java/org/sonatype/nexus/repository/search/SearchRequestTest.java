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

import java.util.List;

import org.sonatype.nexus.repository.search.query.SearchFilter;

import org.sonatype.nexus.rest.ValidationErrorsException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * NEXUS-54105 - SearchRequest.Builder.build() should reject NUL (0x00) byte as DBs may fail.
 * Constructing an invalid request is impossible, which guards every SearchService caller —
 * REST v1 endpoints, format-specific facets, ExtJS DirectMethod, tag and staging — with a
 * single check no matter which entry point they use.
 */
class SearchRequestTest
{
  @Test
  void build_withNulByteInFilterValue_throwsBadRequest() {
    SearchRequest.Builder builder = SearchRequest.builder().searchFilter("keyword", "abc\u0000x");

    assertThrows(ValidationErrorsException.class, builder::build);
  }

  @Test
  void build_withNulByteInFilterProperty_throwsBadRequest() {
    SearchRequest.Builder builder = SearchRequest.builder()
        .searchFilters(List.of(new SearchFilter("keyword\u0000x", "abc")));

    assertThrows(ValidationErrorsException.class, builder::build);
  }

  @Test
  void build_withNulByteInContinuationToken_throwsBadRequest() {
    SearchRequest.Builder builder = SearchRequest.builder().continuationToken("42\u0000");

    assertThrows(ValidationErrorsException.class, builder::build);
  }

  @Test
  void build_withNulByteInSortField_throwsBadRequest() {
    SearchRequest.Builder builder = SearchRequest.builder().sortField("name\u0000x");

    assertThrows(ValidationErrorsException.class, builder::build);
  }

  @Test
  void build_withNulByteInRepository_throwsBadRequest() {
    SearchRequest.Builder builder = SearchRequest.builder().repository("maven-central\u0000");

    assertThrows(ValidationErrorsException.class, builder::build);
  }

  @Test
  void build_withOrdinaryValues_succeeds() {
    assertDoesNotThrow(() -> SearchRequest.builder()
        .searchFilter("keyword", "junit")
        .continuationToken("42")
        .sortField("name")
        .repository("maven-central")
        .build());
  }

  /*
   * NEXUS-54105 ticket vectors beyond a bare NUL byte.
   *
   * The ticket's reproduction URLs include %c0%ae%c0%ae%00x (overlong UTF-8 followed by a NUL)
   * and test%27-- (single quote + SQL comment marker). The overlong UTF-8 sequence is never
   * decoded into user-facing content: modern JAX-RS URI decoders reject it up front, and even
   * when a client manages to smuggle the replacement character through, the trailing NUL still
   * trips the validator. The quote+comment marker is not a NUL byte and must reach the query
   * layer untouched — parameter binding, not string sanitisation, is what defuses it — so the
   * builder must accept it.
   *
   * Non-numeric continuationToken values (mangled base64 in the ticket, e.g.
   * "eyJ0ZXN0IjoidGVzdCJ9") are also non-NUL and therefore accepted here: they are rejected one
   * layer down in SqlSearchService.offsetFromToken, which is the correct boundary for that
   * check.
   */
  @Test
  void build_withOverlongUtf8DecodedToReplacementCharAndNulByte_throwsBadRequest() {
    // Best a JAX-RS URI decoder can do with %c0%ae is emit U+FFFD; the ticket URL still has a
    // trailing %00 which flips this string into the NUL path.
    SearchRequest.Builder builder = SearchRequest.builder()
        .searchFilter("keyword", "\ufffd\ufffd\u0000x");

    assertThrows(ValidationErrorsException.class, builder::build);
  }

  @Test
  void build_withSqlQuoteAndCommentMarker_succeeds() {
    // %27-- decodes to '--. It must not be sanitised at this layer: it is safe because the query
    // is bound as a parameter, not concatenated into SQL.
    assertDoesNotThrow(() -> SearchRequest.builder()
        .searchFilter("keyword", "test'--")
        .build());
  }

  @Test
  void build_withMangledBase64ContinuationToken_succeeds() {
    // The ticket example (eyJ0ZXN0IjoidGVzdCJ9) is well-formed base64 with no NUL byte, so
    // SearchRequest accepts it. The 400 for "not a number" is asserted at the layer that
    // parses it (SqlSearchService), not here.
    assertDoesNotThrow(() -> SearchRequest.builder()
        .continuationToken("eyJ0ZXN0IjoidGVzdCJ9")
        .build());
  }
}
