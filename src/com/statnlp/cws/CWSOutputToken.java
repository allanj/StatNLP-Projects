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

import com.statnlp.commons.types.OutputToken;

/**
 * @author wei_lu
 *
 */
public class CWSOutputToken extends OutputToken{
	
	private static final long serialVersionUID = 4339173568909630435L;
	
	private int _id;
	
	public static CWSOutputToken _START;
	
	public CWSOutputToken(String name) {
		super(name);
	}
	
	public int getId(){
		return this._id;
	}
	
	public void setId(int id){
		this._id = id;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof CWSOutputToken){
			return this._name.equals(((CWSOutputToken)o)._name);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this._name.hashCode();
	}
	
	@Override
	public String toString() {
		return this._name;
	}

}
