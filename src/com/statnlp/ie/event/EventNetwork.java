/** Statistical Natural Language Processing System
    Copyright (C) 2014-2015  Lu, Wei

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
/**
 * 
 */
package com.statnlp.ie.event;

import com.statnlp.hybridnetworks.LocalNetworkParam;
import com.statnlp.hybridnetworks.TableLookupNetwork;

/**
 * @author wei_lu
 *
 */
public class EventNetwork extends TableLookupNetwork{
	
	private static final long serialVersionUID = 299585458354845180L;
	
	private int _numNodes = -1;
	
	public EventNetwork(){
		super();
	}
	
	public EventNetwork(int networkId, EventInstance inst, LocalNetworkParam param){
		super(networkId, inst, param);
	}
	
	public EventNetwork(int networkId, EventInstance inst, long[] nodes, int[][][] children, LocalNetworkParam param, int numNodes){
		super(networkId, inst, nodes, children, param);
		this._numNodes = numNodes;
	}
	
	@Override
	public int countNodes(){
		if(this._numNodes==-1)
			return super.countNodes();
		return this._numNodes;
	}
	
	//remove the node k from the network.
	@Override
	public void remove(int k){
		//DO NOTHING..
	}
	
	//check if the node k is removed from the network.
	@Override
	public boolean isRemoved(int k){
		return false;
	}
	
}
