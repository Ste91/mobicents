package javax.megaco.pkg.BaseRootPkg;

import javax.megaco.message.DescriptorType;
import javax.megaco.pkg.MegacoPkg;
import javax.megaco.pkg.ParamValueType;
import javax.megaco.pkg.PkgPrptyItem;

/**
 * The MEGACO MaxTermsPerCtxt property class extends the PkgPrptyItem class.
 * This is a final class. This class defines MaxTermsPerCtxt property of MEGACO
 * Root package. The methods shall define that this property item belongs to the
 * Root package.
 */
public final class RootMaxTermsPerCtxtPrpt extends PkgPrptyItem {

	/**
	 * Identifies MaxTermsPerCtxt property of the MEGACO Base Root Package. Its
	 * value shall be set equal to 0x0002.
	 */
	public static final int ROOT_MAX_TERMS_PER_CTXT_PRPT = 0x0002;

	protected int[] itemsDescriptorIds = null;

	public RootMaxTermsPerCtxtPrpt() {
		super();
		super.propertyId = ROOT_MAX_TERMS_PER_CTXT_PRPT;
		super.itemId = ROOT_MAX_TERMS_PER_CTXT_PRPT;
		super.packageId = new BaseRootPkg();
		super.itemValueType = ParamValueType.M_ITEM_PARAM_VALUE_INTEGER;
		this.itemsDescriptorIds = new int[] { DescriptorType.M_TERMINATION_STATE_DESC };
	}

	/**
	 * This method is used to get the item identifier from an Item object. The
	 * implementations of this method in this class returns the id of the
	 * Maximum Terminations per Context property of ROOT Package.
	 * 
	 * @return It shall return {@link ROOT_MAX_TERMS_PER_CTXT_PRPT}
	 */
	public int getItemId() {
		return super.itemId;
	}

	/**
	 * The method can be used to get the type of the value as defined in the
	 * MEGACO packages. These could be one of string or enumerated value or
	 * integer or double value or boolean.
	 * 
	 * @return It returns {@link ITEM_PARAM_VALUE_INTEGER} indicating that the
	 *         parameter is a double.
	 */
	public int getItemValueType() {

		return super.itemValueType;
	}

	/**
	 * This method is used to get the property identifier from an Property Item
	 * object. The implementations of this method in this class returns the id
	 * of the Maximum Termination per Context property of ROOT Package.
	 * 
	 * @return It shall return {@link ROOT_MAX_TERMS_PER_CTXT_PRPT}
	 */
	public int getPropertyId() {

		return super.propertyId;
	}

	/**
	 * This method gets the package id to which the item belongs. Since the
	 * Maximum Termination per Context property is defined in the Base ROOT
	 * Package of MEGACO protocol, this method returns the value
	 * BASE_ROOT_PACKAGE constant. This constant is defined in the PkgConsts
	 * class.
	 * 
	 * @return The package id {@link BASE_ROOT_PACKAGE}.
	 */
	public MegacoPkg getItemsPkgId() {
		return this.packageId;
	}

	/**
	 * The method can be used to get the descriptor ids corresponding to the
	 * parameters to which the parameter can be set.
	 * 
	 * @return This parameter can be present in Event descriptor. It shall thus
	 *         return a value {@link M_TERMINATION_STATE_DESC} as a part of
	 *         integer vector.
	 */
	public int[] getItemsDescriptorIds() {
		return itemsDescriptorIds;
	}

}
