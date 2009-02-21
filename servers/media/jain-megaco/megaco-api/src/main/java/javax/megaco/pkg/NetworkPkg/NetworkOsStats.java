package javax.megaco.pkg.NetworkPkg;

import javax.megaco.pkg.PkgConsts;
import javax.megaco.pkg.PkgStatsItem;

/**
 * The MEGACO Octet sent statistics class extends the PkgStatsItem class. This
 * is a final class. This class defines Octet sent statistics of MEGACO Network
 * package. The methods shall define that this statistics item belongs to the
 * Network package.
 */
public final class NetworkOsStats extends PkgStatsItem {

	/**
	 * Identifies Octets sent statistics of the MEGACO Network Package. Its
	 * value shall be set equal to 0x0002.
	 */
	public static final int NETWORK_OS_STATS = 0x0002;

	/**
	 * Constructs a Jain MEGACO Object representing statistics Item of the
	 * MEGACO Package for statistics Octet sent and Package as Network.
	 */
	public NetworkOsStats() {
		super();
		super.itemId = NETWORK_OS_STATS;
		super.statisticsId = NETWORK_OS_STATS;
		super.packageId = new NetworkPkg();
	}

	/**
	 * This method is used to get the item identifier from an Item object. The
	 * implementations of this method in this class returns the id of the Octet
	 * Sent statistics of Network Package.
	 * 
	 * @return It shall return {@link NETWORK_OS_STATS}.
	 */
	public int getItemId() {
		return super.itemId;
	}

	/**
	 * This method is used to get the statistics identifier from an Statistics
	 * Item object. The implementations of this method in this class returns the
	 * id of the Octet Sent statistics of Network Package.
	 * 
	 * @return It shall return {@link NETWORK_OS_STATS}.
	 */
	public int getStatisticsId() {
		return super.statisticsId;
	}

	/**
	 * This method gets the package id to which the item belongs. Since the
	 * Octet Sent statistics is defined in the Network Package of MEGACO
	 * protocol, this method returns the value {@link PkgConsts.NETWORK_PACKAGE}
	 * constant. This constant is defined in the PkgConsts class.
	 * 
	 * @return The package id {@link PkgConsts.NETWORK_PACKAGE}.
	 */
	public int getItemsPkgId() {
		return super.packageId.getPkgId();
	}

	FIXME: whats that ??
	@Override
	public int getItemValueType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int[] getItemsDescriptorIds() {
		// TODO Auto-generated method stub
		return null;
	}

}
