package data.preprocess.complexity;

public class Edge implements Comparable<Edge>{

	int left;
	int right;
	
	public Edge(int left, int right){
		this.left= left<right? left:right;
		this.right = left<right? right:left;
	}

	@Override
	public String toString() {
		return "Edge [left=" + left + ", right=" + right + "]";
	}

	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + left;
		result = prime * result + right;
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
		if (left != other.left)
			return false;
		if (right != other.right)
			return false;
		return true;
	}

	@Override
	public int compareTo(Edge arg0) {
		if(this.left<arg0.left)
			return -1;
		else if(this.left==arg0.left){
			if(this.right<arg0.right) return -1;
			else if(this.right==arg0.right) return 0;
			else return 1;
		}else{
			return 1;
		}
	}
	
	
	
}
