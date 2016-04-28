/** Statistical Natural Language Processing System
    Copyright (C) 2014-2016  Lu, Wei

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
package com.statnlp.hybridnetworks;

import java.io.Serializable;

import com.statnlp.commons.types.Instance;

/**
 * The base class for network compiler, a class to convert a problem representation between 
 * {@link Instance} (the surface form) and {@link Network} (the modeled form)<br>
 * When implementing the {@link #compile(int, Instance, LocalNetworkParam)} method, you might 
 * want to split the case into two cases: labeled and unlabeled, where the labeled network contains
 * only the existing nodes and edges in the instance, and the unlabeled network contains all
 * possible nodes and edges in the instance.
 * @author Wei Lu <luwei@statnlp.com>
 *
 */
public abstract class NetworkCompiler implements Serializable{
	
	private static final long serialVersionUID = 1052885626598299680L;
	
	/**
	 * Convert an instance into the network representation.<br>
	 * This process is also called the encoding part (e.g., to create the trellis network 
	 * of POS tags for a given sentence)<br>
	 * Subclasses might want to split this method into two, one for labeled instance, and 
	 * another for unlabeled instance.
	 * @param networkId
	 * @param inst
	 * @param param
	 * @return
	 */
	public abstract Network compile(int networkId, Instance inst, LocalNetworkParam param);
	
	/**
	 * Convert a network into an instance, the surface form.<br>
	 * This process is also called the decoding part (e.g., to get the sequence with maximum 
	 * probability in an HMM)
	 * @param network
	 * @return
	 */
	public abstract Instance decompile(Network network);
	
}