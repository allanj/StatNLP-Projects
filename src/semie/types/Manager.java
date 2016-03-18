package semie.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * The manager that stores information about various types of data.
 * Examples include word and tags.
 * @author luwei
 * @version 1.0
 */

public class Manager implements Serializable{
	
	private static final long serialVersionUID = -402491420948328276L;

	/**
	 * Set this to true if you want to save memory, but will make training slower.
	 */
	public boolean _memoryEfficientMode = false;
	
	//this gives mappings from intArray/string to integers...
	private HashMap<String, Integer> _str2intMap;
	
	//below gives mappings from their ids to tags, roles, types, words, events, and phrases
	private HashMap<Integer, Tag> _tagMap;
	private HashMap<Role, Role> _roleMap;
	private HashMap<Integer, Type> _typeMap;
	private HashMap<Integer, Word> _wordsMap;
	private HashMap<Event, Event> _eventMap;
	private HashMap<Phrase, Phrase> _phraseMap;
	
	
	//below sorted by id, starting from 0, 1,..
	private ArrayList<Event> _allEvents;
	private transient ArrayList<EventSpan> _allEventSpans = new ArrayList<EventSpan>();
	
	private HashMap<Event, Role> _arg_beg = new HashMap<Event, Role>();//event id -> begin argument
	private HashMap<Event, Role> _arg_end = new HashMap<Event, Role>();//event id -> end argument
	
	public Manager(){
		
		this._str2intMap = new HashMap<String, Integer>();
		
		this._typeMap = new HashMap<Integer, Type>();
		this._tagMap = new HashMap<Integer, Tag>();
		this._wordsMap = new HashMap<Integer, Word>();
		this._eventMap = new HashMap<Event, Event>();
		this._phraseMap = new HashMap<Phrase, Phrase>();
		this._roleMap = new HashMap<Role, Role>();
		
//		this._allEventSpans = new ArrayList<EventSpan>();
	}
	
	//for type//
	
	public Type toType(String type){
		int type_index = this.indexStr(type);
		if(this._typeMap.containsKey(type_index))
			return this._typeMap.get(type_index);
		Type t = new Type(type_index, type);
		this._typeMap.put(type_index, t);
		return t;
	}
	
	public Type getType(String type){
		int type_index = this.indexStr(type);
		if(this._typeMap.containsKey(type_index))
			return this._typeMap.get(type_index);
		return null;
	}
	
	public Type getType(int id){
		return this._typeMap.get(id);
	}
	
	//for tag//
	
	public Tag toTag(String type){
		int id = this.indexStr(type);
		if(this._tagMap.containsKey(id))
			return this._tagMap.get(id);
		Tag t = new Tag(id, type);
		this._tagMap.put(id, t);
		return t;
	}
	
	public Tag getTag(String type){
		int id = this.indexStr(type);
		if(this._tagMap.containsKey(id))
			return this._tagMap.get(id);
		return null;
	}
	
	public Tag getTag(int id){
		return this._tagMap.get(id);
	}
	
	//for argument//

	public Role getRole(String[] role_names){
		Role role = new Role(-1, role_names);
		if(this._roleMap.containsKey(role))
			return this._roleMap.get(role);
		return null;
	}
	
	public Role toRole(String[] role_names){
		Role role = new Role(-1, role_names);
		if(this._roleMap.containsKey(role))
			return this._roleMap.get(role);
		role.setId(this._roleMap.size());
		this._roleMap.put(role, role);
		return role;
	}
	
	public Role toBeginRole(Event event){
		if(this._arg_beg.containsKey(event.getId()))
			return this._arg_beg.get(event.getId());
		
		String[] role_names = new String[]{"BEGIN", "BEGIN_"+event.getMostSpecificName()};
		Role arg = this.toRole(role_names);
		arg.setTypes(new Type[]{Type._BEGIN_TYPE});
		this._arg_beg.put(event, arg);
		return arg;
	}
	
	public Role toEndRole(Event event){
		if(this._arg_end.containsKey(event.getId()))
			return this._arg_end.get(event.getId());
		
		String[] role_names = new String[]{"END", "END_"+event.getMostSpecificName()};
		Role arg = this.toRole(role_names);
		arg.setTypes(new Type[]{Type._END_TYPE});
		this._arg_end.put(event, arg);
		return arg;
	}
	
	public Role getRole(int id){
		return this._roleMap.get(id);
	}
	
	//for event//
	
	public Event toEvent(String[] names, Role[] roles, String[] triggers){
		Event event = new Event(-1, names, roles, triggers);
		if(this._eventMap.containsKey(event)){
			return this._eventMap.get(event);
		} else {
			event.setId(this._eventMap.size());
			this._eventMap.put(event, event);
		}
		this.toBeginRole(event);
		this.toEndRole(event);
		return event;
	}
	
	public Event toEvent(String[] names, Role[] roles){
		Event event = new Event(-1, names, roles, new String[0]);
		if(this._eventMap.containsKey(event)){
			return this._eventMap.get(event);
		} else {
			event.setId(this._eventMap.size());
			this._eventMap.put(event, event);
		}
		this.toBeginRole(event);
		this.toEndRole(event);
		return event;
	}
	
	public Event getEvent(int id){
		return this._eventMap.get(id);
	}
	
	public Event getEvent(String[] names){
		Event event = new Event(-1, names, new Role[0], new String[0]);
		return this._eventMap.get(event);
	}
	
	public Event getEvent(String mostSpecificName){
		this.getAllEvents();
		for(int i = 0; i<this._allEvents.size(); i++){
			Event event = this._allEvents.get(i);
			if(event.getMostSpecificName().equalsIgnoreCase(mostSpecificName)){
				return event;
			}
		}
		return null;
	}
	
	public ArrayList<Event> getAllEvents(){
		if(this._allEvents==null || this._allEvents.size()!=this._eventMap.size()){
			this._allEvents = new ArrayList<Event>();
			Iterator<Event> keys = this._eventMap.keySet().iterator();
			while(keys.hasNext()){
				Event key = keys.next();
				Event event = this._eventMap.get(key);
				this._allEvents.add(event);
			}
		}
		Collections.sort(this._allEvents);
		return this._allEvents;
	}
	
	//for word//
	
	public Word toWord(String word){
		int id = this.indexStr(word);
		if(this._wordsMap.containsKey(id))
			return this._wordsMap.get(id);
		Word w = new Word(id, word);
		this._wordsMap.put(id, w);
		return w;
	}
	
	public Word getWord(int id){
		return this._wordsMap.get(id);
	}

	//for phrase//
	
	public Phrase toPhrase(EventSpan span, int bIndex, int eIndex){
		
		if(bIndex == -1 && eIndex == 0)
			return Phrase._BEG_PHRASE;
		if(span.getWords().length == bIndex && span.getWords().length + 1 == eIndex)
			return Phrase._END_PHRASE;
		
		Phrase phrase = new Phrase(-1, span, bIndex, eIndex);
		if(this._phraseMap.containsKey(phrase))
			return this._phraseMap.get(phrase);
		
		phrase.setId(this._phraseMap.size());
		this._phraseMap.put(phrase, phrase);
		return phrase;
		
	}
	
	public Phrase getPhrase(int id){
		return this._phraseMap.get(id);
	}
	
	//for event span//
	
	public void resetEventSpans(){
		this._allEventSpans = new ArrayList<EventSpan>();
	}
	
	public EventSpan toEventSpan(String filename, Word[] words, EventAnnotation gold_annotation){
		if(this._allEventSpans==null)
			this._allEventSpans = new ArrayList<EventSpan>();
		EventSpan span = new EventSpan(this._allEventSpans.size(), filename, words, gold_annotation);
		this._allEventSpans.add(span);
		return span;
	}
	
	public EventSpan getEventSpanAt(int id){
		return this._allEventSpans.get(id);
	}
	
	public ArrayList<EventSpan> getAllEventSpans(){
		return this._allEventSpans;
	}
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	//below auxiliary methods for indexing purposes//
	
	// 3/3
	public int indexStr(String s){
		
		if(s.trim().equals(""))
			throw new IllegalArgumentException("You should not index an empty string!");
		if(this._str2intMap.containsKey(s))
			return this._str2intMap.get(s);
		int index = this._str2intMap.size();
		this._str2intMap.put(s, index);
		return index;
		
	}
	
	public int getStr(String s){
		
		if(this._str2intMap.containsKey(s))
			return this._str2intMap.get(s);
		return -1;
		
	}

	//end of auxiliary indexing methods//

	/********************************************************
	     Auxiliary methods, for viewing/printing purposes    
	*********************************************************/
	
	public static String viewEventSpan(EventSpan eventspan){
		
		StringBuilder sb = new StringBuilder();
		Word[] words = eventspan.getWords();
		for(int i = 0; i<words.length; i++){
			if(i!=0) sb.append(' ');
			sb.append(words[i].getWord());
		}
		return sb.toString();
		
	}
	
	public static String viewRoleSpan(RoleSpan rolespan, EventSpan eventspan){
		
		StringBuilder sb = new StringBuilder();
		String phrase;
		if(rolespan.getBIndex() == -1 || rolespan.getBIndex() == eventspan.getWords().length){
			phrase = "";
		} else {
			for(int i = rolespan.getBIndex(); i < rolespan.getEIndex(); i++){
				sb.append(' ');
				sb.append(eventspan.getWords()[i].getWord());
			}
			phrase = sb.toString();
		}
		
		if(rolespan.getPrev()==null)
			return rolespan.getRole()+" ["+rolespan.getBIndex()+","+rolespan.getEIndex()+"]"+phrase+" {"+rolespan.getScore()+"}";
		
		return viewRoleSpan(rolespan.getPrev(), eventspan).toString()+"\n"+rolespan.getRole()+" ["+
			rolespan.getBIndex()+","+rolespan.getEIndex()+"]"+phrase+" {"+rolespan.getScore()+"}";
		
	}
	
}
