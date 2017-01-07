package com.statnlp.hybridnetworks;

public class Node extends Component {

	private static final long serialVersionUID = 1983305767931974702L;

	protected int node;
	
	public Node(int node) {
		this.node = node;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + node;
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
		Node other = (Node) obj;
		if (node != other.node)
			return false;
		return true;
	}
	
	
	
}
