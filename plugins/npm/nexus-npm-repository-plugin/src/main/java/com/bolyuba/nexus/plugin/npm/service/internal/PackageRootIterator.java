/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service.internal;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.bolyuba.nexus.plugin.npm.service.PackageRoot;

/**
 * {@link Iterator} of {@link PackageRoot}s. Should be handled as resources, hence is {@link Closeable}. Preferred use
 * is within try-with-resource block.
 */
public interface PackageRootIterator
    extends Iterator<PackageRoot>, Closeable
{
  PackageRootIterator EMPTY = new PackageRootIterator()
  {
    @Override
    public void close() throws IOException {
      // nop
    }

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public PackageRoot next() {
      throw new NoSuchElementException("Empty iterator");
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove not supported!");
    }
  };
}
