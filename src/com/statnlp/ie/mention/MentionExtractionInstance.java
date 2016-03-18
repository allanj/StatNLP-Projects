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

import com.statnlp.commons.types.TextSpan;
import com.statnlp.ie.IEInstance;
import com.statnlp.ie.types.LabeledTextSpan;
import com.statnlp.ie.types.MentionTemplate;
import com.statnlp.ie.types.UnlabeledTextSpan;

public class MentionExtractionInstance extends IEInstance{
	
	private static final long serialVersionUID = -653025587024238399L;
	
	private MentionTemplate _info;
	
	public MentionExtractionInstance(int id, TextSpan span, MentionTemplate info) {
		super(id, span);
		this._info = info;
	}
	
	public MentionExtractionInstance removeOutput(){
		if(this._span instanceof UnlabeledTextSpan)
			return this;
		LabeledTextSpan lspan = (LabeledTextSpan)this._span;
		UnlabeledTextSpan uspan = lspan.removeLabels();
		return new MentionExtractionInstance(-this._instanceId, uspan, this._info);
	}
	
	public MentionTemplate getInfo(){
		return this._info;
	}
	
}