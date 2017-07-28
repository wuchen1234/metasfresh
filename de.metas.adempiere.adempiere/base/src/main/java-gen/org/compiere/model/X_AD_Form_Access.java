/** Generated Model - DO NOT CHANGE */
package org.compiere.model;

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for AD_Form_Access
 *  @author Adempiere (generated) 
 */
@SuppressWarnings("javadoc")
public class X_AD_Form_Access extends org.compiere.model.PO implements I_AD_Form_Access, org.compiere.model.I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = -798455141L;

    /** Standard Constructor */
    public X_AD_Form_Access (Properties ctx, int AD_Form_Access_ID, String trxName)
    {
      super (ctx, AD_Form_Access_ID, trxName);
      /** if (AD_Form_Access_ID == 0)
        {
			setAD_Form_Access_ID (0);
			setAD_Form_ID (0);
			setAD_Role_ID (0);
			setIsReadWrite (false);
        } */
    }

    /** Load Constructor */
    public X_AD_Form_Access (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }


    /** Load Meta Data */
    @Override
    protected org.compiere.model.POInfo initPO (Properties ctx)
    {
      org.compiere.model.POInfo poi = org.compiere.model.POInfo.getPOInfo (ctx, Table_Name, get_TrxName());
      return poi;
    }

	/** Set AD_Form_Access.
		@param AD_Form_Access_ID AD_Form_Access	  */
	@Override
	public void setAD_Form_Access_ID (int AD_Form_Access_ID)
	{
		if (AD_Form_Access_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Form_Access_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Form_Access_ID, Integer.valueOf(AD_Form_Access_ID));
	}

	/** Get AD_Form_Access.
		@return AD_Form_Access	  */
	@Override
	public int getAD_Form_Access_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Form_Access_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Override
	public org.compiere.model.I_AD_Form getAD_Form() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_AD_Form_ID, org.compiere.model.I_AD_Form.class);
	}

	@Override
	public void setAD_Form(org.compiere.model.I_AD_Form AD_Form)
	{
		set_ValueFromPO(COLUMNNAME_AD_Form_ID, org.compiere.model.I_AD_Form.class, AD_Form);
	}

	/** Set Special Form.
		@param AD_Form_ID 
		Special Form
	  */
	@Override
	public void setAD_Form_ID (int AD_Form_ID)
	{
		if (AD_Form_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Form_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Form_ID, Integer.valueOf(AD_Form_ID));
	}

	/** Get Special Form.
		@return Special Form
	  */
	@Override
	public int getAD_Form_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Form_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Override
	public org.compiere.model.I_AD_Role getAD_Role() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_AD_Role_ID, org.compiere.model.I_AD_Role.class);
	}

	@Override
	public void setAD_Role(org.compiere.model.I_AD_Role AD_Role)
	{
		set_ValueFromPO(COLUMNNAME_AD_Role_ID, org.compiere.model.I_AD_Role.class, AD_Role);
	}

	/** Set Rolle.
		@param AD_Role_ID 
		Responsibility Role
	  */
	@Override
	public void setAD_Role_ID (int AD_Role_ID)
	{
		if (AD_Role_ID < 0) 
			set_ValueNoCheck (COLUMNNAME_AD_Role_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Role_ID, Integer.valueOf(AD_Role_ID));
	}

	/** Get Rolle.
		@return Responsibility Role
	  */
	@Override
	public int getAD_Role_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Role_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Lesen und Schreiben.
		@param IsReadWrite 
		Field is read / write
	  */
	@Override
	public void setIsReadWrite (boolean IsReadWrite)
	{
		set_Value (COLUMNNAME_IsReadWrite, Boolean.valueOf(IsReadWrite));
	}

	/** Get Lesen und Schreiben.
		@return Field is read / write
	  */
	@Override
	public boolean isReadWrite () 
	{
		Object oo = get_Value(COLUMNNAME_IsReadWrite);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}
}