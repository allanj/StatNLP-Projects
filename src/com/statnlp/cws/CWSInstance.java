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
package com.statnlp.cws;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.WordToken;

/**
 * @author wei_lu
 *
 */
public class CWSInstance extends Instance{
	
	private static final long serialVersionUID = -7054688437971413537L;
	
	private WordToken[] _inputs;
	private CWSOutput _output;
	private CWSOutput _prediction;
	private boolean _hasOutput;
	
	public CWSInstance(int instanceId, double weight, WordToken[] inputs, CWSOutput output) {
		super(instanceId, weight);
		this._inputs = inputs;
		this._output = output;
		this._hasOutput = true;
	}
	
	@Override
	public int size() {
		return this._inputs.length;
	}
	
	@Override
	public Instance duplicate() {
		return new CWSInstance(this._instanceId, this._weight, this._inputs, this._output);
	}
	
	@Override
	public void removeOutput() {
		this._hasOutput = false;
	}
	
	@Override
	public void removePrediction() {
		this._prediction = null;
	}
	
	@Override
	public WordToken[] getInput() {
		return this._inputs;
	}
	
	@Override
	public CWSOutput getOutput() {
		return this._output;
	}
	
	@Override
	public boolean hasOutput() {
		return this._hasOutput;
	}

	@Override
	public CWSOutput getPrediction() {
		return this._prediction;
	}
	
	@Override
	public boolean hasPrediction() {
		return this._prediction != null;
	}
	
	@Override
	public void setPrediction(Object prediction) {
		this._prediction = (CWSOutput)prediction;
	}

}
