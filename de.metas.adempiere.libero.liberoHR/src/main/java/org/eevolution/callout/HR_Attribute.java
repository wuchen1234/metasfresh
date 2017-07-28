package org.eevolution.callout;

import org.adempiere.ad.callout.annotations.Callout;
import org.adempiere.ad.callout.annotations.CalloutMethod;
import org.eevolution.model.I_HR_Attribute;
import org.eevolution.model.I_HR_Concept;

/*
 * #%L
 * de.metas.adempiere.libero.liberoHR
 * %%
 * Copyright (C) 2017 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@Callout(I_HR_Attribute.class)
public class HR_Attribute
{
	@CalloutMethod(columnNames = I_HR_Attribute.COLUMNNAME_HR_Concept_ID)
	public void onHR_Concept_ID(final I_HR_Attribute hrAttribute)
	{
		final I_HR_Concept hrConcept = hrAttribute.getHR_Concept();
		if (hrConcept == null)
		{
			return;
		}

		hrAttribute.setColumnType(hrConcept.getColumnType());
	}

}
