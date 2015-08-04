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
package org.sonatype.nexus.rest.formfield;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.Selectable;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.model.FormFieldResource;

public abstract class AbstractFormFieldResource
    extends AbstractNexusPlexusResource
{
  protected List<? extends FormFieldResource> formFieldToDTO(List<FormField> fields,
                                                             Class<? extends FormFieldResource> clazz)
  {
    List<FormFieldResource> dtoList = new ArrayList<FormFieldResource>();

    for (FormField field : fields) {
      try {
        FormFieldResource dto = clazz.newInstance();
        dto.setHelpText(field.getHelpText());
        dto.setId(field.getId());
        dto.setLabel(field.getLabel());
        dto.setRegexValidation(field.getRegexValidation());
        dto.setRequired(field.isRequired());
        dto.setType(field.getType());
        if (field.getInitialValue() != null) {
          dto.setInitialValue(field.getInitialValue().toString());
        }
        if (field instanceof Selectable) {
          dto.setStorePath(((Selectable) field).getStorePath());
          dto.setStoreRoot(((Selectable) field).getStoreRoot());
          dto.setIdMapping(((Selectable) field).getIdMapping());
          dto.setNameMapping(((Selectable) field).getNameMapping());
        }
        dtoList.add(dto);
      }
      catch (InstantiationException | IllegalAccessException e) {
        getLogger().error("Unable to properly translate DTO", e);
      }
    }

    return dtoList;
  }
}
