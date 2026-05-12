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
package org.sonatype.nexus.repository.manager.internal;

import java.util.Collection;
import java.util.Optional;

import org.sonatype.nexus.repository.manager.internal.FailedRepositoryTracker.RepositoryFailure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FailedRepositoryTrackerTest
{
  private FailedRepositoryTracker underTest;

  @BeforeEach
  void setUp() {
    underTest = new FailedRepositoryTracker();
  }

  @Test
  void recordFailure_tracksRepositoryFailure() {
    Exception cause = new RuntimeException("Connection refused");

    underTest.recordFailure("my-repo", cause);

    assertThat(underTest.hasFailed("my-repo"), is(true));
    assertThat(underTest.getFailureCount(), is(1));

    Optional<RepositoryFailure> failure = underTest.getFailure("my-repo");
    assertThat(failure.isPresent(), is(true));
    assertThat(failure.get().getName(), is("my-repo"));
    assertThat(failure.get().getReason(), is("Connection refused"));
    assertThat(failure.get().getFailedAt(), is(notNullValue()));
  }

  @Test
  void recordFailure_usesExceptionClassNameWhenMessageIsNull() {
    Exception cause = new NullPointerException();

    underTest.recordFailure("my-repo", cause);

    Optional<RepositoryFailure> failure = underTest.getFailure("my-repo");
    assertThat(failure.isPresent(), is(true));
    assertThat(failure.get().getReason(), is("NullPointerException"));
  }

  @Test
  void recordFailure_isCaseInsensitive() {
    underTest.recordFailure("My-Repo", new RuntimeException("error"));

    assertThat(underTest.hasFailed("my-repo"), is(true));
    assertThat(underTest.hasFailed("MY-REPO"), is(true));
    assertThat(underTest.hasFailed("My-Repo"), is(true));

    assertThat(underTest.getFailure("my-repo").isPresent(), is(true));
    assertThat(underTest.getFailure("MY-REPO").isPresent(), is(true));
  }

  @Test
  void recordFailure_preservesOriginalName() {
    underTest.recordFailure("My-Repo", new RuntimeException("error"));

    Optional<RepositoryFailure> failure = underTest.getFailure("my-repo");
    assertThat(failure.get().getName(), is("My-Repo"));
  }

  @Test
  void recordFailure_replacesExistingFailure() {
    underTest.recordFailure("my-repo", new RuntimeException("first error"));
    underTest.recordFailure("my-repo", new RuntimeException("second error"));

    assertThat(underTest.getFailureCount(), is(1));
    assertThat(underTest.getFailure("my-repo").get().getReason(), is("second error"));
  }

  @Test
  void recordFailure_throwsOnNullName() {
    assertThrows(NullPointerException.class, () -> underTest.recordFailure(null, new RuntimeException("error")));
  }

  @Test
  void recordFailure_throwsOnNullCause() {
    assertThrows(NullPointerException.class, () -> underTest.recordFailure("my-repo", null));
  }

  @Test
  void clearFailure_removesTrackedFailure() {
    underTest.recordFailure("my-repo", new RuntimeException("error"));
    assertThat(underTest.hasFailed("my-repo"), is(true));

    underTest.clearFailure("my-repo");

    assertThat(underTest.hasFailed("my-repo"), is(false));
    assertThat(underTest.getFailure("my-repo").isPresent(), is(false));
    assertThat(underTest.getFailureCount(), is(0));
  }

  @Test
  void clearFailure_isCaseInsensitive() {
    underTest.recordFailure("My-Repo", new RuntimeException("error"));

    underTest.clearFailure("MY-REPO");

    assertThat(underTest.hasFailed("my-repo"), is(false));
  }

  @Test
  void clearFailure_doesNothingForUnknownRepository() {
    underTest.clearFailure("unknown-repo");

    assertThat(underTest.getFailureCount(), is(0));
  }

  @Test
  void clearFailure_throwsOnNullName() {
    assertThrows(NullPointerException.class, () -> underTest.clearFailure(null));
  }

  @Test
  void getFailure_returnsEmptyForUnknownRepository() {
    Optional<RepositoryFailure> failure = underTest.getFailure("unknown-repo");

    assertThat(failure.isPresent(), is(false));
  }

  @Test
  void getFailure_throwsOnNullName() {
    assertThrows(NullPointerException.class, () -> underTest.getFailure(null));
  }

  @Test
  void hasFailed_returnsFalseForUnknownRepository() {
    assertThat(underTest.hasFailed("unknown-repo"), is(false));
  }

  @Test
  void hasFailed_throwsOnNullName() {
    assertThrows(NullPointerException.class, () -> underTest.hasFailed(null));
  }

  @Test
  void getAllFailures_returnsEmptyCollectionWhenNoFailures() {
    Collection<RepositoryFailure> failures = underTest.getAllFailures();

    assertThat(failures, is(empty()));
  }

  @Test
  void getAllFailures_returnsAllTrackedFailures() {
    underTest.recordFailure("repo-1", new RuntimeException("error 1"));
    underTest.recordFailure("repo-2", new RuntimeException("error 2"));
    underTest.recordFailure("repo-3", new RuntimeException("error 3"));

    Collection<RepositoryFailure> failures = underTest.getAllFailures();

    assertThat(failures, hasSize(3));
  }

  @Test
  void getAllFailures_returnsUnmodifiableCollection() {
    underTest.recordFailure("my-repo", new RuntimeException("error"));

    Collection<RepositoryFailure> failures = underTest.getAllFailures();

    assertThrows(UnsupportedOperationException.class, () -> failures.clear());
  }

  @Test
  void getFailureCount_returnsZeroWhenNoFailures() {
    assertThat(underTest.getFailureCount(), is(0));
  }

  @Test
  void getFailureCount_returnsCorrectCount() {
    underTest.recordFailure("repo-1", new RuntimeException("error 1"));
    underTest.recordFailure("repo-2", new RuntimeException("error 2"));

    assertThat(underTest.getFailureCount(), is(2));
  }

  @Test
  void repositoryFailure_toStringContainsAllFields() {
    underTest.recordFailure("my-repo", new RuntimeException("test error"));

    RepositoryFailure failure = underTest.getFailure("my-repo").get();
    String toString = failure.toString();

    assertThat(toString.contains("my-repo"), is(true));
    assertThat(toString.contains("test error"), is(true));
  }
}
