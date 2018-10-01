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

import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerFactory;
import org.ops4j.pax.exam.karaf.container.internal.KarafTestContainerFactory;

import static org.sonatype.nexus.pax.exam.TestCounters.nextInstallDirectory;

/**
 * {@link TestContainerFactory} that injects a custom install location into the {@link ExamSystem}.
 *
 * Uses {@link TestCounters} to track a sequence of numeric directories, one per NXRM installation.
 *
 * @since 3.14
 */
public class NexusPaxExamTestFactory
    implements TestContainerFactory
{
  @Override
  public TestContainer[] create(final ExamSystem system) {
    return new KarafTestContainerFactory().create(
        // Pax-Exam ITs call fork just before unpacking each container instance
        new DelegatingExamSystem(system)
        {
          @Override
          public ExamSystem fork(final Option[] options) {
            final File installDir = nextInstallDirectory();
            return new DelegatingExamSystem(super.fork(options))
            {
              @Override
              public File getConfigFolder() {
                return installDir;
              }
            };
          }
        });
  }
}
