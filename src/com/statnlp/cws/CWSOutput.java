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
import java.util.Collections;
import java.util.HashMap;

import com.statnlp.commons.types.Segment;

/**
 * @author wei_lu
 *
 */
public class CWSOutput implements Serializable{
	
	private static final long serialVersionUID = -3311149225816393444L;
	
	//this is the ranked list of segments.
	private ArrayList<Segment> _segments;
	private HashMap<Segment, CWSOutputToken> _seg2token;
	
	public CWSOutput(){
		this._segments = new ArrayList<Segment>();
		this._seg2token = new HashMap<Segment, CWSOutputToken>();
	}
	
	public CWSOutput toChunkOutput(CWSOutputTokenSet set){
		ArrayList<Segment> segments_new = new ArrayList<Segment>();
		HashMap<Segment, CWSOutputToken> seg2token_new = new HashMap<Segment, CWSOutputToken>();
		
		for(int k = 0; k<this._segments.size(); k++){
			Segment segment = this._segments.get(k);
			CWSOutputToken token = this.getOutputBySegment(segment);
			if(token.getName().startsWith("B-")){
				String name = token.getName().split("\\-")[1];
				CWSOutputToken token_new = set.toOutputToken(name);
				
				for(int i = k+1; i<this._segments.size(); i++){
					Segment segment2 = this._segments.get(i);
					CWSOutputToken token2 = this.getOutputBySegment(segment2);
					if(token2.getName().startsWith("L-")){
						Segment seg_new = new Segment(segment.getBIndex(), segment2.getEIndex());
						segments_new.add(seg_new);
						seg2token_new.put(seg_new, token_new);
						k=i;
						break;
					}
				}
			} else if(token.getName().startsWith("U-")){
				String name = token.getName().split("\\-")[1];
				CWSOutputToken token_new = set.toOutputToken(name);
				
				for(int i = k+1; i<this._segments.size(); i++){
					Segment segment2 = this._segments.get(i);
					CWSOutputToken token2 = this.getOutputBySegment(segment2);
					if(!token2.getName().equals(token.getName())){
						segment2 = this._segments.get(i-1);
						Segment seg_new = new Segment(segment.getBIndex(), segment2.getEIndex());
						segments_new.add(seg_new);
						seg2token_new.put(seg_new, token_new);
						k=i-1;
						break;
					}
				}
			} else {
				segments_new.add(segment);
				seg2token_new.put(segment, token);
			}
		}
		
		CWSOutput output = new CWSOutput();
		output._segments = segments_new;
		output._seg2token = seg2token_new;
		return output;
	}
	
	public int countOverlaps(CWSOutput output1){
		int count = 0;
		for(Segment segment : this._segments){
			CWSOutputToken token = this._seg2token.get(segment);
			if(output1.contains(segment, token)){
				count++;
			}
		}
		return count;
	}
	
	public boolean contains(Segment seg, CWSOutputToken token){
		if(!this._seg2token.containsKey(seg)){
			return false;
		}
		CWSOutputToken token1 = this._seg2token.get(seg);
//		System.err.println(token1+"\t"+token);
		return token.equals(token1);
	}
	
	public void addOutput(Segment seg, CWSOutputToken token){
		this._segments.add(seg);
		Collections.sort(this._segments);
		this._seg2token.put(seg, token);
	}
	
	public ArrayList<Segment> getSegments(){
		return this._segments;
	}
	
	public CWSOutputToken getOutputBySegment(Segment segment){
		return this._seg2token.get(segment);
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int k = 0; k < this._segments.size(); k++){
			Segment segment = this._segments.get(k);
			CWSOutputToken token = this._seg2token.get(segment);
			sb.append(segment);
			sb.append(token);
			sb.append(' ');
		}
		return sb.toString();
	}
	
}
