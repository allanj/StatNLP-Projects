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
package com.statnlp.ie.event;

import com.statnlp.commons.types.Instance;
import com.statnlp.commons.types.Segment;
import com.statnlp.ie.types.UnlabeledTextSpan;

/**
 * @author wei_lu
 *
 */
public class EventInstance extends Instance{
	
	private static final long serialVersionUID = 2433256808576348909L;
	
	private Segment _eventSpan;
	//this is the input
	private UnlabeledTextSpan _input;
	//this is labeled with mention information
	private MentionLabeledTextSpan _mentions;
	//this is labeled with event information
	private EventLabeledTextSpan _output;
	//this is labeled with event information
	private EventLabeledTextSpan _prediction;
	
	public EventInstance(int instanceId, double weight, Segment eventSpan, UnlabeledTextSpan input, MentionLabeledTextSpan mentions, EventLabeledTextSpan events) {
		super(instanceId, weight);
		this._eventSpan = eventSpan;
		this._input = input;
		this._mentions = mentions;
		this._output = events;
	}
	
	public EventInstance(int instanceId, double weight, UnlabeledTextSpan input, MentionLabeledTextSpan mentions) {
		super(instanceId, weight);
		this._input = input;
		this._mentions = mentions;
	}
	
	@Override
	public int size() {
		return this._input.length();
	}
	
	@Override
	public Instance duplicate() {
		return new EventInstance(this._instanceId, this._weight, this._eventSpan, this._input, this._mentions, this._output);
	}
	
	public Segment getEventSpan(){
		return this._eventSpan;
	}
	
	@Override
	public void removeOutput() {
		this._output = null;
	}
	
	@Override
	public void removePrediction() {
		this._prediction = null;
	}
	
	@Override
	public UnlabeledTextSpan getInput() {
		return this._input;
	}
	
	public MentionLabeledTextSpan getMentionSpan(){
		return this._mentions;
	}
	
	@Override
	public EventLabeledTextSpan getOutput() {
		return this._output;
	}
	
	@Override
	public EventLabeledTextSpan getPrediction() {
		return this._prediction;
	}
	
	@Override
	public boolean hasOutput() {
		return this._output!=null;
	}
	
	@Override
	public boolean hasPrediction() {
		return this._prediction!=null;
	}
	
	@Override
	public void setPrediction(Object o) {
		this._prediction = (EventLabeledTextSpan)o;
	}
	
}
