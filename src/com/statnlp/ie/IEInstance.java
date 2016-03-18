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
package com.statnlp.ie;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.TextSpan;

public abstract class IEInstance extends Instance{
	
	private static final long serialVersionUID = -1879109898850269290L;
	
	protected TextSpan _span;
	
	public IEInstance(int id, TextSpan span) {
		super(id, 1.0);
		this._span = span;
	}
	
}