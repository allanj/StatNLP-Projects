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
package com.statnlp.ie.mention;

import com.statnlp.ie.linear.latent.IELinearLatentInstance;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.MentionTemplate;
import com.statnlp.ie.types.UnlabeledTextSpan;

public class MentionLinearLatentInstance extends IELinearLatentInstance{
	
	private static final long serialVersionUID = -653025587024238399L;
	
	private MentionTemplate _info;
	
	public MentionLinearLatentInstance(int id, LabeledTextSpan output, MentionTemplate info) {
		super(id, output.removeLabels(), output);
		this._info = info;
		this._isLabeled = true;
	}
	
	public MentionLinearLatentInstance(int id, UnlabeledTextSpan input, LabeledTextSpan output, MentionTemplate info) {
		super(id, input, output);
		this._info = info;
		this._isLabeled = true;
	}
	
	public MentionTemplate getInfo(){
		return this._info;
	}
	
	@Override
	public MentionLinearLatentInstance duplicate() {
		return new MentionLinearLatentInstance(this._instanceId, this._input, this._output, this._info);
	}
	
}