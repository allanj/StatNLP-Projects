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
package com.statnlp.commons.ml.classification.logisticregression;

import com.statnlp.commons.types.InputToken;
import com.statnlp.commons.types.Instance;

public abstract class LRInstance extends Instance{
	
	private static final long serialVersionUID = 3336336220436168888L;
	
	protected InputToken[] _inputs;
	
	public LRInstance(int id, double weight, InputToken[] inputs){
		super(id, weight);
		this._inputs = inputs;
	}
	
	public boolean hasOutput(){
		return this instanceof LabeledLRInstance;
	}
	
	@Override
	public Object getInput() {
		return this._inputs;
	}
	
	@Override
	public void removePrediction() {
		//
	}

	@Override
	public Object getOutput() {
		return null;
	}

	@Override
	public Object getPrediction() {
		return null;
	}

	@Override
	public boolean hasPrediction() {
		return false;
	}

	@Override
	public void setPrediction(Object o) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

}