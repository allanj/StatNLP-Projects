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
package com.statnlp.ie.linear.head;

import com.statnlp.commons.types.Instance;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.UnlabeledTextSpan;

public abstract class IELinearHeadInstance extends Instance{
	
	private static final long serialVersionUID = -1879109898850269290L;
	
	protected UnlabeledTextSpan _input;
	protected LabeledTextSpan _output;
	protected LabeledTextSpan _prediction;
	
	public IELinearHeadInstance(int id, UnlabeledTextSpan input, LabeledTextSpan output) {
		super(id, 1.0);
		this._input = input;
		this._output = output;
	}
	
	@Override
	public boolean hasOutput(){
		return this._output!=null;
	}
	
	@Override
	public void removeOutput(){
		this._output = null;
	}
	
	@Override
	public int size() {
		return this._input.length();
	}

	@Override
	public void removePrediction() {
		this._prediction = null;
	}
	
	@Override
	public UnlabeledTextSpan getInput() {
		return this._input;
	}
	
	@Override
	public LabeledTextSpan getOutput() {
		return this._output;
	}
	
	@Override
	public LabeledTextSpan getPrediction() {
		return this._prediction;
	}
	
	@Override
	public boolean hasPrediction() {
		return this._prediction!=null;
	}
	
	@Override
	public void setPrediction(Object o) {
		this._prediction = (LabeledTextSpan)o;
	}
	
}