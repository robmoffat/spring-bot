package org.finos.springbot.entities;

import java.util.Arrays;
import java.util.List;

import org.finos.springbot.entityjson.ObjectMapperFactory;
import org.finos.springbot.entityjson.VersionSpace;
import org.symphonyoss.Taxonomy;
import org.symphonyoss.fin.Security;
import org.symphonyoss.fin.security.id.Cusip;
import org.symphonyoss.fin.security.id.Isin;
import org.symphonyoss.fin.security.id.Openfigi;
import org.symphonyoss.fin.security.id.Ticker;
import org.symphonyoss.taxonomy.Hashtag;

import com.symphony.user.Mention;
import com.symphony.user.UserId;

public class VersionSpaceHelp {

	/**
	 * The Symphony client itself uses classes like Isin, Ticker, Mention, UserId when reporting hashtags, cashtags etc.
	 * This function provides a default set of VersionSpaces to allow these to be deserialized.
	 */
	public static VersionSpace[] basicSymphonyVersionSpace() {
		return new VersionSpace[] { 
				new VersionSpace(Taxonomy.class, "1.0"),
				new VersionSpace(Security.class, "1.0", "0.*"),
				new VersionSpace(Mention.class, "1.0"), 
				new VersionSpace(UserId.class, "1.0"), 
				new VersionSpace(Hashtag.class, "1.0"), 
				ObjectMapperFactory.noVersion(Ticker.class), 
				ObjectMapperFactory.noVersion(Cusip.class), 
				ObjectMapperFactory.noVersion(Isin.class), 
				ObjectMapperFactory.noVersion(Openfigi.class),
			};
	}

	/**
	 * Provides all of the classes in the basicSymphonyVersionSpace (above), as well as any you provide in the
	 * varargs.
	 */
	public static VersionSpace[] extendedSymphonyVersionSpace(List<VersionSpace> second) {
		VersionSpace[] cc = new VersionSpace[second.size()];
		return extendedSymphonyVersionSpace(second.toArray(cc));
	}

	/**
	 * Provides all of the classes in the basicSymphonyVersionSpace (above), as well as any you provide in the
	 * varargs.
	 */
	public static VersionSpace[] extendedSymphonyVersionSpace(VersionSpace... first) {
		VersionSpace[] second = basicSymphonyVersionSpace();
		VersionSpace[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

}
