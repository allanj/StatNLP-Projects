package com.statnlp.projects.dep.utils;

public class StrFreq implements Comparable<StrFreq> {

	public String str;
	public int freq;
	
	public StrFreq(String str, int freq) {
		this.str = str;
		this.freq = freq;
	}

	@Override
	public int compareTo(StrFreq o) {
		if (this.freq < o.freq)
			return 1;
		else if (this.freq == o.freq) 
			return 0;
		else return -1;
	}

	@Override
	public String toString() {
		return "[" + str + ", f=" + freq + "]";
	}

	
}
