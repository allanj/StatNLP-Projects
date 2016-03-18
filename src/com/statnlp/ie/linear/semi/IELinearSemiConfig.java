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
package com.statnlp.ie.linear.semi;

public class IELinearSemiConfig {
	
	public static final int _MAX_SENT_LENGTH = 200;
	public static final int _MAX_LBFGS_ITRS = 200;
	public static final int _MAX_MENTION_LEN = 6;
	
	public static MENTION_TYPE _type = MENTION_TYPE.FINE_TYPE;
	
	public enum NODE_TYPE
	{
		TERMINATE,
		MENTION_TAG,
		MENTION_TAG_PARENT,
		EXACT_START_TAG,
		EXACT_START,
		AFTER_START,
		ROOT
	};
	
	public enum MENTION_TYPE {NO_TYPE, COARSE_TYPE, FINE_TYPE}; 
	
}