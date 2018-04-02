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
package org.sonatype.nexus.repository.npm.internal.search.v1;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.npm.internal.search.v1.NpmSearchParameterExtractor;
import org.sonatype.nexus.repository.view.Parameters;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class NpmSearchParameterExtractorTest
    extends TestSupport
{
  NpmSearchParameterExtractor underTest = new NpmSearchParameterExtractor();

  @Test
  public void extractTextWithoutValue() {
    Parameters parameters = new Parameters();

    String text = underTest.extractText(parameters);
    assertThat(text, is(""));
  }

  @Test
  public void extractTextWithOnlyWhitespace() {
    Parameters parameters = new Parameters();
    parameters.set("text", "    ");

    String text = underTest.extractText(parameters);
    assertThat(text, is(""));
  }

  @Test
  public void extractTextWithoutKeywordsIncludesWildcards() {
    Parameters parameters = new Parameters();
    parameters.set("text", "foo");

    String text = underTest.extractText(parameters);
    assertThat(text, is("*foo*"));
  }

  @Test
  public void extractTextWithKeywordsRewrite() {
    Parameters parameters = new Parameters();
    parameters.set("text", "keywords:foo");

    String text = underTest.extractText(parameters);
    assertThat(text, is("assets.attributes.npm.keywords:foo"));
  }

  @Test
  public void extractTextWithAuthorRewrite() {
    Parameters parameters = new Parameters();
    parameters.set("text", "author:foo");

    String text = underTest.extractText(parameters);
    assertThat(text, is("assets.attributes.npm.author:foo"));
  }

  @Test
  public void extractTextWithMaintainerRewrite() {
    Parameters parameters = new Parameters();
    parameters.set("text", "maintainer:foo");

    String text = underTest.extractText(parameters);
    assertThat(text, is("assets.attributes.npm.author:foo"));
  }

  @Test
  public void extractTextWithIsRewrite() {
    Parameters parameters = new Parameters();
    parameters.set("text", "is:foo");

    String text = underTest.extractText(parameters);
    assertThat(text, is("assets.attributes.npm.tagged_is:foo"));
  }

  @Test
  public void extractTextWithNotRewrite() {
    Parameters parameters = new Parameters();
    parameters.set("text", "not:foo");

    String text = underTest.extractText(parameters);
    assertThat(text, is("assets.attributes.npm.tagged_not:foo"));
  }

  @Test
  public void extractTextWithScopeRewrite() {
    Parameters parameters = new Parameters();
    parameters.set("text", "scope:foo");

    String text = underTest.extractText(parameters);
    assertThat(text, is("assets.attributes.npm.scope:foo"));
  }

  @Test
  public void extractSizeWithoutValue() {
    Parameters parameters = new Parameters();

    int size = underTest.extractSize(parameters);
    assertThat(size, is(20));
  }

  @Test
  public void extractSize() {
    Parameters parameters = new Parameters();
    parameters.set("size", "50");

    int size = underTest.extractSize(parameters);
    assertThat(size, is(50));
  }

  @Test
  public void extractSizeOverLimit() {
    Parameters parameters = new Parameters();
    parameters.set("size", "500");

    int size = underTest.extractSize(parameters);
    assertThat(size, is(250));
  }

  @Test
  public void extractUnparseableSize() {
    Parameters parameters = new Parameters();
    parameters.set("size", "foo");

    int size = underTest.extractSize(parameters);
    assertThat(size, is(20));
  }

  @Test
  public void extractFromWithoutValue() {
    Parameters parameters = new Parameters();

    int from = underTest.extractFrom(parameters);
    assertThat(from, is(0));
  }

  @Test
  public void extractFrom() {
    Parameters parameters = new Parameters();
    parameters.set("from", "50");

    int from = underTest.extractFrom(parameters);
    assertThat(from, is(50));
  }

  @Test
  public void extractUnparseableFrom() {
    Parameters parameters = new Parameters();
    parameters.set("from", "foo");

    int from = underTest.extractFrom(parameters);
    assertThat(from, is(0));
  }
}
