package semie.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import semie.util.Indexer;

/**
 * The event span is a sequence of words
 * @author luwei
 * @version 1.0
 */

public class EventSpan extends Identifiable{

	private static final long serialVersionUID = 4187439751517962308L;
	
	/**
	 * The name of the file from which the event span comes
	 */
	private String _filename;
	
	/**
	 * The words appearing in the event span
	 */
	private Word[] _words;
	
	/**
	 * The span indices
	 */
	private int[] _spanIndices;
	
	/**
	 * The trigger indices
	 */
	private int[] _triggerIndices;
	
	/**
	 * The gold event annotation
	 */
	private EventAnnotation _gold_annotation;
	
	/**
	 * The mapping from tag names to list of tags
	 */
	private HashMap<String, Tag[][]> _tags;
	
	/**
	 * The event
	 */
	private Event _event;
	
	/**
	 * The lattice
	 */
	private Lattice _lattice;
	
	/**
	 * The child event span. Typically if the current event span is a complete sentence,
	 * then the child event span comes with the exact boundary of the event mention.
	 */
	private EventSpan _childSpan;
	
	public static final EventSpan _dummy = new EventSpan(-100, "", new Word[0], new EventAnnotation());
	
	public EventSpan(int id, String filename, Word[] words, EventAnnotation gold_annotation){
		super(id);
		this._filename = filename;
		this._words = words;
		this._gold_annotation = gold_annotation;
		this._tags = new HashMap<String, Tag[][]>();
	}
	
	/**
	 * Get the name of the file from which the event span is extracted
	 * @return the name of the file
	 */
	public String getFilename(){
		return this._filename;
	}
	
	/**
	 * Get the list of words
	 * @return the list of words
	 */
	public Word[] getWords(){
		return this._words;
	}
	
	/**
	 * Set the boundary of the event span
	 * @param spanIndices the boundary indices of the event span
	 */
	public void setSpanIndices(int[] spanIndices){
		this._spanIndices = spanIndices;
	}
	
	/**
	 * Set the trigger boundary indices
	 * @param triggerIndices the boundary indices of the trigger span
	 */
	public void setTriggerIndices(int[] triggerIndices){
		this._triggerIndices = triggerIndices;
	}
	
	/**
	 * Get the boundary indices of the event span
	 * @return the boundary indices of the event span
	 */
	public int[] getSpanIndices(){
		return this._spanIndices;
	}
	
	/**
	 * Get the boundary indices of the trigger span
	 * @return the boundary indices of the trigger span
	 */
	public int[] getTriggerIndices(){
		return this._triggerIndices;
	}
	
	/**
	 * The length of the event span
	 * @return the length
	 */
	public int length(){
		return this._words.length;
	}
	
	/**
	 * Add tags. Assumption: one word one tag sequence: Tag[] ...
	 * @param tag_name the name of the tags
	 * @param tags the actual tags
	 * @return true if added successfully, false otherwise
	 */
	public boolean addTags(String tag_name, Tag[][] tags){
		if(tags.length != this._words.length)
			return false;
		this._tags.put(tag_name, tags);
		return true;
	}
	
	/**
	 * Get the tags with the given name
	 * @param tag_name the name of the tag
	 * @return the tags
	 */
	public Tag[][] getTags(String tag_name){
		return this._tags.get(tag_name);
	}
	
	/**
	 * Get the gold event annotation
	 * @return the gold event annotation
	 */
	public EventAnnotation getGoldAnnotation(){
		return this._gold_annotation;
	}
	
	/**
	 * Set the event for the event span
	 * @param event the event
	 */
	public void setEvent(Event event){
		if(this._event!=null)
			throw new IllegalStateException("Event has been set previously to "+this._event);
		this._event = event;
	}
	
	/**
	 * Get the event of the event span
	 * @return the event associated with the event span
	 */
	public Event getEvent(){
		return this._event;
	}
	
	/**
	 * Set the lattice
	 * @param lattice the lattice to be set
	 */
	public void setLattice(Lattice lattice){
		this._lattice = lattice;
	}
	
	/**
	 * Get the lattice associated with the event span
	 * @return the lattice 
	 */
	public Lattice getLattice(){
		return this._lattice;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i<this._words.length; i++){
			if(i==0) sb.append(' ');
			sb.append(this._words[i]);
		}
		return sb.toString();
	}

	/**
	 * Get the child event span
	 * @param manager the manager
	 * @return the child event span
	 */
	public EventSpan getChildEventSpan(Manager manager){
		if(this._spanIndices==null){
			return null;
		}
		if(this._childSpan!=null){
			return this._childSpan;
		}
		
		int bIndex = this._spanIndices[0];
		int eIndex = this._spanIndices[1];
		Word[] words = new Word[eIndex-bIndex];
		for(int i = bIndex; i<eIndex; i++){
			words[i-bIndex] = this._words[i];
		}
		
		EventAnnotation ea = new EventAnnotation();
		ArrayList<Integer> annotation_intervals = this._gold_annotation.getAllIntervals();
		for(int interval : annotation_intervals){
			int[] intv_indices = Indexer.decode(interval);
			Role arg = this._gold_annotation.getRole(intv_indices[0], intv_indices[1]);
			ea.annotateInterval(intv_indices[0]-bIndex, intv_indices[1]-bIndex, arg);
		}
		EventSpan span = manager.toEventSpan(this._filename, words, ea);
		
		span.setEvent(this._event);
		
		int[] triggerIndices = new int[]{this._triggerIndices[0]-bIndex, this._triggerIndices[1]-bIndex};
		span.setTriggerIndices(triggerIndices);
		
		//for tags...
		{
			Iterator<String> tagnames = this._tags.keySet().iterator();
			while(tagnames.hasNext()){
				String tagname = tagnames.next();
				Tag[][] tag = this._tags.get(tagname);
				Tag[][] tag_new = new Tag[eIndex-bIndex][];
				for(int i = bIndex; i<eIndex; i++)
					tag_new[i-bIndex] = tag[i];
				span.addTags(tagname, tag_new);
			}
		}
		
		Lattice lattice_new = new Lattice(span);
		
		ArrayList<Integer> intervals = this._lattice.getAllIntervals();
		for(int i = 0; i<intervals.size(); i++){
			int interval = intervals.get(i);
			
			int[] interval_indices = Indexer.decode(interval);
			
			if(interval_indices[0]-bIndex<0)
				continue;
			if(interval_indices[1]-bIndex>span.getWords().length)
				continue;
			
			ArrayList<Type> types = this._lattice.getTypes(interval_indices[0], interval_indices[1]);
			for(Type type : types)
				lattice_new.label(interval_indices[0]-bIndex, interval_indices[1]-bIndex, type);
		}
		
		//for now, just do them here...
		lattice_new.label_postprocess();
		lattice_new.map_phrases(manager);
		
		span.setLattice(lattice_new);
		
		this._childSpan = span;
		
		return span;
	}
	
}
