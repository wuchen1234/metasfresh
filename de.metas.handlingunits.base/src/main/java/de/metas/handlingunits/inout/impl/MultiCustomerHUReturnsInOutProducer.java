package de.metas.handlingunits.inout.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.bpartner.service.IBPartnerDAO;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.IContextAware;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.GuavaCollectors;
import org.adempiere.util.Services;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.I_C_Order;
import org.compiere.model.I_M_Warehouse;
import org.compiere.model.X_M_Transaction;
import org.compiere.util.Env;
import org.compiere.util.Util.ArrayKey;

import de.metas.adempiere.model.I_C_BPartner_Location;
import de.metas.document.engine.IDocumentBL;
import de.metas.handlingunits.IHUAssignmentDAO;
import de.metas.handlingunits.IHandlingUnitsBL;
import de.metas.handlingunits.IHandlingUnitsDAO;
import de.metas.handlingunits.hutransaction.IHUTrxBL;
import de.metas.handlingunits.inout.IHUInOutBL;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_HU_Assignment;
import de.metas.handlingunits.model.I_M_HU_Item;
import de.metas.handlingunits.model.I_M_InOut;
import de.metas.handlingunits.model.I_M_InOutLine;
import de.metas.inout.event.ReturnInOutProcessedEventBus;

/*
 * #%L
 * de.metas.handlingunits.base
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

/**
 * Note: For the time being ( task #1306) there is no requirement to have returns from customer created for more than 1 customer at the same time.
 * But nevertheless, I am writing the implementation similar with the Vendor Return part, to have them structured and to allow the possibility to perform the return from customer also from a POS.
 *
 * @author metas-dev <dev@metasfresh.com>
 *
 */
public class MultiCustomerHUReturnsInOutProducer
{
	public static final MultiCustomerHUReturnsInOutProducer newInstance()
	{
		return new MultiCustomerHUReturnsInOutProducer();
	}

	// services
	private final transient IHUAssignmentDAO huAssignmentDAO = Services.get(IHUAssignmentDAO.class);
	private final transient IHandlingUnitsBL handlingUnitsBL = Services.get(IHandlingUnitsBL.class);
	private final transient IHandlingUnitsDAO handlingUnitsDAO = Services.get(IHandlingUnitsDAO.class);
	private final transient IHUTrxBL huTrxBL = Services.get(IHUTrxBL.class);
	//
	private final transient ITrxManager trxManager = Services.get(ITrxManager.class);

	//
	// Parameters
	private Timestamp _movementDate;
	private final List<I_M_HU> _husToReturn = new ArrayList<>();

	private I_M_InOut _manualCustomerReturn;

	private MultiCustomerHUReturnsInOutProducer()
	{
	}

	public List<I_M_InOut> create()
	{
		return trxManager.call(ITrx.TRXNAME_ThreadInherited, this::createInTrx);
	}

	private List<I_M_InOut> createInTrx()
	{
		//
		// Iterate all HUs, group them by Partner and HU's warehouse
		// and create one vendor returns producer for each group.
		final Map<ArrayKey, CustomerReturnsInOutProducer> customerReturnProducers = new HashMap<>();
		final int inOutLineTableId = InterfaceWrapperHelper.getTableId(I_M_InOutLine.class); // The M_InOutLine's table id
		for (final I_M_HU hu : getHUsToReturn())
		{
			// activate hu's children
			{
				final Set<I_M_HU> childHUs = new HashSet<>();

				{
					final List<I_M_HU_Item> huItems = handlingUnitsDAO.retrieveItems(hu);

					for (final I_M_HU_Item huItem : huItems)
					{
						childHUs.addAll(handlingUnitsDAO.retrieveChildHUsForItem(huItem));
					}
				}

			}
			InterfaceWrapperHelper.setTrxName(hu, ITrx.TRXNAME_ThreadInherited);
			final IContextAware ctxAware = InterfaceWrapperHelper.getContextAware(hu);

			final int warehouseId = hu.getM_Locator().getM_Warehouse_ID();

			//
			// Find out the HU assignments to original vendor material receipt
			List<I_M_HU_Assignment> inOutLineHUAssignments = huAssignmentDAO.retrieveTableHUAssignmentsNoTopFilter(ctxAware, inOutLineTableId, hu);
			// if the given HU does not have any inout line HU assignments, it might be that it is an aggregated HU.
			// fallback on the HU assignments of the top level HU
			if (inOutLineHUAssignments.isEmpty())
			{
				final I_M_HU topLevelHU = handlingUnitsBL.getTopLevelParent(hu);
				inOutLineHUAssignments = huAssignmentDAO.retrieveTableHUAssignmentsNoTopFilter(ctxAware, inOutLineTableId, topLevelHU);
			}

			// there were no HU Asignments for inoutlines.
			if (inOutLineHUAssignments.isEmpty())
			{
				throw new AdempiereException("No InOutLine HUAssignments for selected HU");
			}

			//
			// If the HU is not a top level one, extract it first
			huTrxBL.extractHUFromParentIfNeeded(hu);

			//
			// Get the HU and the original vendor receipt M_InOutLine_ID and add it to the right producer
			for (final I_M_HU_Assignment assignment : inOutLineHUAssignments)
			{
				final int originalShipmentInOutLineId = assignment.getRecord_ID();

				// Find out the the Vendor BPartner
				final I_M_InOutLine inoutLine = InterfaceWrapperHelper.loadOutOfTrx(originalShipmentInOutLineId, I_M_InOutLine.class);

				if (inoutLine == null)
				{
					continue;
				}
				final org.compiere.model.I_M_InOut inout = inoutLine.getM_InOut();

				if (!inout.isSOTrx())
				{
					// do not allow HUs from receipts to get into customer returns
					continue;
				}

				if (!Services.get(IDocumentBL.class).isDocumentCompletedOrClosed(inout))
				{
					// do not allow HUs from uncompleted inouts to get into customer returns
					continue;
				}

				if (Services.get(IHUInOutBL.class).isCustomerReturn(inout))
				{
					continue;
				}

				final int bpartnerId = inout.getC_BPartner_ID();

				final I_C_Order order = inout.getC_Order();
				// Add the HU to the right producer
				// NOTE: There will be one return inout for each partner and warehouse
				// The return inout lines will be created based on the origin inoutlines (from receipts)
				final ArrayKey customerReturnProducerKey = ArrayKey.of(warehouseId, bpartnerId);
				customerReturnProducers.computeIfAbsent(customerReturnProducerKey, k -> createCustomerReturnInOutProducer(bpartnerId, warehouseId, order))
						.addHUToReturn(hu, originalShipmentInOutLineId);
			}
		}

		//
		// Iterate all vendor return producers and actually create the vendor returns InOut
		final List<I_M_InOut> returnInOuts = customerReturnProducers.values().stream()
				.map(CustomerReturnsInOutProducer::create) // create vendor return
				.map(returnInOut -> InterfaceWrapperHelper.create(returnInOut, I_M_InOut.class)) // wrap it
				.collect(GuavaCollectors.toImmutableList());

		//
		// Send notifications
		if (!returnInOuts.isEmpty())
		{
			if (_manualCustomerReturn == null)
			{
				ReturnInOutProcessedEventBus.newInstance()
						.queueEventsUntilTrxCommit(ITrx.TRXNAME_ThreadInherited)
						.notify(returnInOuts);
			}

			else
			{
				InterfaceWrapperHelper.refresh(_manualCustomerReturn);
			}
			final Properties ctx = InterfaceWrapperHelper.getCtx(returnInOuts.get(0));
			// mark HUs as active and create movements to QualityReturnWarehouse for them
			Services.get(IHUInOutBL.class).moveHUsForCustomerReturn(ctx, getHUsToReturn());

			handlingUnitsBL.setHUStatusActive(_husToReturn);
		}

		// return the created vendor returns
		return returnInOuts;
	}

	/**
	 * Create customer return producer, set the details and use it to create the customer return inout.
	 *
	 * @param partnerId
	 * @param hus
	 * @return
	 */
	private CustomerReturnsInOutProducer createCustomerReturnInOutProducer(final int partnerId, final int warehouseId, final I_C_Order originOrder)
	{
		final IBPartnerDAO bpartnerDAO = Services.get(IBPartnerDAO.class);
		final Properties ctx = Env.getCtx();
		final I_C_BPartner partner = InterfaceWrapperHelper.loadOutOfTrx(partnerId, I_C_BPartner.class);
		final I_C_BPartner_Location shipFromLocation = bpartnerDAO.retrieveShipToLocation(ctx, partnerId, ITrx.TRXNAME_None);
		final I_M_Warehouse warehouse = InterfaceWrapperHelper.loadOutOfTrx(warehouseId, I_M_Warehouse.class);

		final CustomerReturnsInOutProducer producer = CustomerReturnsInOutProducer.newInstance();
		producer.setC_BPartner(partner);
		producer.setC_BPartner_Location(shipFromLocation);

		producer.setMovementType(X_M_Transaction.MOVEMENTTYPE_CustomerReturns);
		producer.setM_Warehouse(warehouse);

		producer.setMovementDate(getMovementDate());

		producer.setC_Order(originOrder);

		if (_manualCustomerReturn != null)
		{
			producer.setManualReturnInOut(_manualCustomerReturn);
		}

		return producer;
	}

	public MultiCustomerHUReturnsInOutProducer setMovementDate(final Timestamp movementDate)
	{
		_movementDate = movementDate;
		return this;
	}

	public MultiCustomerHUReturnsInOutProducer setManualCustomerReturn(final I_M_InOut manualCustomerReturn)
	{
		_manualCustomerReturn = manualCustomerReturn;
		return this;
	}

	private Timestamp getMovementDate()
	{
		if (_movementDate == null)
		{
			_movementDate = Env.getDate(Env.getCtx()); // use login date by default
		}
		return _movementDate;
	}

	private final List<I_M_HU> getHUsToReturn()
	{
		return _husToReturn;
	}

	public MultiCustomerHUReturnsInOutProducer addHUsToReturn(final Collection<I_M_HU> hus)
	{
		_husToReturn.addAll(hus);
		return this;
	}

}
