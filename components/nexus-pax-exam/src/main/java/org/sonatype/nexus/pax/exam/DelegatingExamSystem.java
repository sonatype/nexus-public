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
package org.sonatype.nexus.pax.exam;

import java.io.File;
import java.io.IOException;

import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.RelativeTimeout;
import org.ops4j.pax.exam.TestProbeBuilder;

/**
 * Simple delegating {@link ExamSystem}.
 *
 * @since 3.14
 */
public class DelegatingExamSystem
    implements ExamSystem
{
  private final ExamSystem delegate;

  public DelegatingExamSystem(final ExamSystem delegate) {
    this.delegate = delegate;
  }

  @Override
  public <T extends Option> T getSingleOption(final Class<T> optionType) {
    return delegate.getSingleOption(optionType);
  }

  @Override
  public <T extends Option> T[] getOptions(final Class<T> optionType) {
    return delegate.getOptions(optionType);
  }

  @Override
  public ExamSystem fork(final Option[] options) {
    return delegate.fork(options);
  }

  @Override
  public File getConfigFolder() {
    return delegate.getConfigFolder();
  }

  @Override
  public File getTempFolder() {
    return delegate.getTempFolder();
  }

  @Override
  public RelativeTimeout getTimeout() {
    return delegate.getTimeout();
  }

  @Override
  public TestProbeBuilder createProbe() throws IOException {
    return delegate.createProbe();
  }

  @Override
  public String createID(final String purposeText) {
    return delegate.createID(purposeText);
  }

  @Override
  public void clear() {
    delegate.clear();
  }
}
