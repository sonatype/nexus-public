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
package org.sonatype.nexus.validation

import javax.validation.Valid
import javax.validation.Validation
import javax.validation.ValidatorFactory
import javax.validation.constraints.NotNull

import org.sonatype.goodies.testsupport.TestSupport

import org.junit.Before
import org.junit.Test

/**
 * Bean validation trials.
 */
class BeanValidationTrial
    extends TestSupport
{
    def ValidatorFactory factory

    @Before
    void setUp() {
        factory = Validation.buildDefaultValidatorFactory()
    }

    class Parent
    {
        @Valid
        Child child
    }

    class Child
    {
        @Valid
        GrandChild grandChild
    }

    class GrandChild
    {
        @NotNull
        String name
    }

    @Test
    void 'validate hierarchy'() {
        def parent = new Parent(
            child: new Child(
                grandChild: new GrandChild(
                    name: null
                )
            )
        )
        def violations = factory.validator.validate(parent)
        log(violations)
        assert violations.size() == 1

        // child.grandChild.name
        def violation = violations.iterator().next()
        log(violation.propertyPath.class)
        violation.propertyPath.each {
            println it
            println it.class
            println it.name
            println it.index
            println it.key
            println it.kind
            println it.inIterable
            println '-'
        }
    }

    class WithMap
    {
        @Valid
        Map<String,Object> contents
    }

    @Test
    void 'validate with map'() {
        def parent = new Parent(
            child: new Child(
                grandChild: new GrandChild(
                    name: null
                )
            )
        )
        def withMap = new WithMap(contents: [ 'foo': parent ])
        def violations = factory.validator.validate(withMap)
        log(violations)
        assert violations.size() == 1

        // contents[foo].child.grandChild.name
        def violation = violations.iterator().next()
        log(violation.propertyPath.class)
        violation.propertyPath.each {
            println it
            println it.class
            println it.name
            println it.index
            println it.key
            println it.kind
            println it.inIterable
            println '-'
        }
    }
}
