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

public class LabeledLLRInstance extends LLRInstance{
	
	private static final long serialVersionUID = 3336336220436168888L;
	
	private OutputLabel _output;
	
	public LabeledLLRInstance(int id, String[] inputs, OutputLabel output){
		super(id, inputs);
		this._output = output;
	}
	
	public UnlabeledLLRInstance removeOutput(){
		UnlabeledLLRInstance unlabeled_inst = new UnlabeledLLRInstance(-this._instanceId, this._inputs);
		unlabeled_inst.setCorrectOutput(this._output);
		return unlabeled_inst;
	}
	
	public OutputLabel getOutput(){
		return this._output;
	}
	
}