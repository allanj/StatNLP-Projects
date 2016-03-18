/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tmp.student.pos.tagging;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Hazar
 * This class model HMM for tags as hidden states and sentence as observation 
 *
 */
public class HMM {

    //Data Structures
    private Map<String, Integer> Tag_frequences;//count(y)
    private Map<String, HashMap<String, Integer>> Tag_Word_frequences;//count(y->x)
    private Map<String, HashMap<String, Integer>> Tag_Tag_frequences;//count(i,j)
    private ArrayList<String> Tags;// all the tags from training set with out (* and STOP)
    private ArrayList<String> Alphabet;// all the words from training set

 /**
 *  path_to_train_data: string location the train data file
 * initalize data structures from the training data
 */  
  public HMM(String path_to_train_data)
  {   //----------initilize the data structure--------------------------
        Tag_frequences = new HashMap<String, Integer>();
        Tag_Word_frequences = new HashMap<String, HashMap<String, Integer>>();
        Tag_Tag_frequences = new HashMap<String, HashMap<String, Integer>>();
        Tags = new ArrayList<String>();
        Alphabet = new ArrayList<String>();
 
        //reading the train data set and build the data structures
        try {
            train(path_to_train_data);
            
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

 /**
 *  file : string location the train data file
 * open train data file and fill data structures with frequence
 */ 
    private void train(String file) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        String pre_Tag = "*";
        increment_Tag_Freq("*");
        String line = "";
        //read each line to be Word \t postag or empty for end of sentence
        while ((line = br.readLine()) != null) {
            //end of sentence
            if (line.isEmpty()) {
                addTag_Tag(pre_Tag, "STOP");// last tag-> STOP
                increment_Tag_Freq("*");
                increment_Tag_Freq("STOP");
                pre_Tag = "*";
            } //token line
            else {
                String[] tokens = line.split("\\t");
                addWord(tokens[0]);//add word to alphapet
                addTag(tokens[1]);// add tag to states
                increment_Tag_Freq(tokens[1]);// increment the tag frequency
                addTag_Word(tokens[0], tokens[1]);//increment count(tag->word)
                addTag_Tag(pre_Tag, tokens[1]);//increment count(tag->tag)
                pre_Tag = tokens[1];                
            }
            
        }
        addTag_Tag(pre_Tag, "STOP");// last tag-> STOP
        increment_Tag_Freq("STOP");
    }

//----------------deal with vectors-----------------------
    private void addTag(String tag) {
        if (!Tags.contains(tag)) {
            Tags.add(tag);
        }
    }
    
    private void addWord(String word) {
        if (!Alphabet.contains(word)) {
            Alphabet.add(word);
        }
    }

  //-------------------deal with hash maps--------------------    
    private void addTag_Word(String word, String tag) {
        if (Tag_Word_frequences.containsKey(tag)) // if previous seen add 1
        {
            if (Tag_Word_frequences.get(tag).containsKey(word)) {
                Tag_Word_frequences.get(tag).put(word, Tag_Word_frequences.get(tag).get(word) + 1);
            } else //first time with this word 
            {
                Tag_Word_frequences.get(tag).put(word, 1);
            }
        } else // first time to see that tag
        {
            Tag_Word_frequences.put(tag, new HashMap<String, Integer>());
            Tag_Word_frequences.get(tag).put(word, 1);
        }
    }
    
    private void addTag_Tag(String tag1, String tag2) {
        if (Tag_Tag_frequences.containsKey(tag1)) // if previous seen add 1
        {
            if (Tag_Tag_frequences.get(tag1).containsKey(tag2)) {
                Tag_Tag_frequences.get(tag1).put(tag2, Tag_Tag_frequences.get(tag1).get(tag2) + 1);
            } else //first time with this word 
            {
                Tag_Tag_frequences.get(tag1).put(tag2, 1);
            }
        } else // first time to see that tag
        {
            Tag_Tag_frequences.put(tag1, new HashMap<String, Integer>());
            Tag_Tag_frequences.get(tag1).put(tag2, 1);
        }
    }
    
    private void increment_Tag_Freq(String tag1) {
        if (Tag_frequences.containsKey(tag1)) // if previous seen add 1
        {
            Tag_frequences.put(tag1, Tag_frequences.get(tag1) + 1);
        } else // first time to see that tag
        {
            Tag_frequences.put(tag1, 1);
        }
    }

//----------------- get frequances ----------------------
    private int count_X_Y(String X, String Y) {
        if (Tag_Word_frequences.containsKey(Y)) {
            if (Tag_Word_frequences.get(Y).containsKey(X)) {
                return Tag_Word_frequences.get(Y).get(X);
            }
        }        
        return 0;
    }
    
    private int count_Y_Y(String y1, String y2) {
        if (Tag_Tag_frequences.containsKey(y1)) {
            if (Tag_Tag_frequences.get(y1).containsKey(y2)) {
                return Tag_Tag_frequences.get(y1).get(y2);
            }
        }
        
        return 0;
    }
    
    private int count_Y(String Y) {
        if (Tag_frequences.containsKey(Y)) {
            return Tag_frequences.get(Y);
        }
        return 0;
    }

  //------------ caculate probabilities--------------------------
    public double MLE_Transition(String tag1, String tag2) {        
        return (double) count_Y_Y(tag1, tag2) / count_Y(tag1);        
    }
    
    public double MLE_Emission(String myTag, String myWord) {
        if (Alphabet.contains(myWord)) {
            return (double) count_X_Y(myWord, myTag) / (1 + count_Y(myTag));
        }
        return (double) 1 / (1 + count_Y(myTag));
        
    }

    //-------------------- getters----------------------------
    public ArrayList<String> getTagset()
    {return Tags;}
    
     public ArrayList<String> getAlphabet()
    {return Alphabet;}
     
    public int numTags()
    {return Tags.size();}
    
    public int numWords()
    {return Alphabet.size();}
    
    public void printA()
    {
        for(String tag1:Tags)
        { System.out.print("A ("+tag1+") -> { ");
            for(String tag2:Tags)
            { double value=MLE_Transition(tag1, tag2);
                if(value>0)
                System.out.print(tag2+" = "+value+"  ");
            }
            
         System.out.print("} \n");
        }
    }
    
     public void printB()
    {
        for(String tag:Tags)
        { System.out.print("B ("+tag+") -> { ");
            for(String word:Alphabet){
            double value=MLE_Emission(tag,word);
            if(value>0)
            System.out.print(word+" = "+value+"  ");
            }
            System.out.print("} \n");
        }
        
        }
        
}
