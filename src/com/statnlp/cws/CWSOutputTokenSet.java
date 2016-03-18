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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author wei_lu
 *
 */
public class CWSOutputTokenSet implements Serializable{
	
	private static final long serialVersionUID = -1212808615811892809L;
	
	private ArrayList<CWSOutputToken> _tokens;
	private HashMap<String, CWSOutputToken> _map;
	
	public static CWSOutputToken _START_TOKEN;
	
	public CWSOutputTokenSet(){
		this._tokens = new ArrayList<CWSOutputToken>();
		this._map = new HashMap<String, CWSOutputToken>();
	}
	
	public CWSOutputToken getOutputTokenById(int id){
		return this._tokens.get(id);
	}
	
	public CWSOutputToken toOutputToken(String form){
		if(this._map.containsKey(form)){
			return this._map.get(form);
		}
		CWSOutputToken token = new CWSOutputToken(form);
		token.setId(this._tokens.size());
		this._tokens.add(token);
		this._map.put(form, token);
		return token;
	}
	
}
