/** Statistical Natural Language Processing System
    Copyright (C) 2014  Lu, Wei

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.statnlp.translation.types;

import com.statnlp.commons.BitextInstance;
import com.statnlp.hybridnetworks.FeatureManager;
import com.statnlp.hybridnetworks.TableLookupNetwork;
import com.statnlp.hybridnetworks.GlobalNetworkParam.EXP_MODE;

public class BitextNetwork extends TableLookupNetwork{

	public BitextNetwork(BitextInstance inst, FeatureManager fm, long[] nodes, int[][][] children, EXP_MODE exp_mode) {
		super(inst, fm, nodes, children, exp_mode);
	}

	public BitextNetwork(BitextInstance inst, FeatureManager fm, EXP_MODE exp_mode) {
		super(inst, fm, exp_mode);
		throw new RuntimeException("not allowed.");
	}
	
}