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
package com.statnlp.commons.ml.classification.logisticregression.latent;

import com.statnlp.commons.ml.classification.logisticregression.OutputLabel;

public class UnlabeledLLRInstance extends LLRInstance{
	
	private static final long serialVersionUID = 3336336220436168888L;
	
	private OutputLabel _output_corr;
	private OutputLabel _output_pred;
	
	protected UnlabeledLLRInstance(int id, String[] inputs){
		super(id, inputs);
	}
	
	public void setCorrectOutput(OutputLabel output){
		this._output_corr = output;
	}
	
	public OutputLabel getCorrectOutput(){
		return this._output_corr;
	}
	
	public void setPredictedOutput(OutputLabel output){
		this._output_pred = output;
	}
	
	public OutputLabel getPredictedOutput(){
		return this._output_pred;
	}
	
	public int countNumCorrectlyPredicted(){
		if(this._output_corr.equals(this._output_pred))
			return 1;
		return 0;
	}

	@Override
	public UnlabeledLLRInstance removeOutput() {
		return this;
	}
	
}