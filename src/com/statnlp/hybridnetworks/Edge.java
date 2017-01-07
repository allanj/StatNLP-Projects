package com.statnlp.hybridnetworks;

import java.util.Arrays;

public class Edge extends Component{

	private static final long serialVersionUID = -8246188141062626402L;
	protected int parent;
	protected int[] children;
	
	public Edge(int parent, int[] children) {
		this.parent = parent;
		this.children = children;
	}

	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(children);
		result = prime * result + parent;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Edge other = (Edge) obj;
		if (!Arrays.equals(children, other.children))
			return false;
		if (parent != other.parent)
			return false;
		return true;
	}

}
