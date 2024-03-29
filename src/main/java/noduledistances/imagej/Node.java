package noduledistances.imagej;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;

import ij.IJ;

public class Node extends Point{
	
	/**
	 * Node.type == 0 -> skeleton node
	 * type == 1 -> red nodule
	 * type == 2 -> green nodule
	 * type == 3 -> mixed nodule
	 */
public int type;

public int nodeIndex;

/**
 * nodeNumber represents the number shown on the nodule csv data to easily associate
 * a nodule node with it's nodule number. If the node is not a nodule, the value defaults to -1.
 * The notation is noduleNumber_noduleSubnumber, and the underscore is represented as a decimal.
 */
public double nodeNumber;
/**
 * 2d array where each element A_ij is the distance to the j'th node on the 
 * i'th iteration of dijkstra's. Each row is one iteration of Dijkstra's, 
 * we remove a used edge from the original graph and perform another iteration of 
 * dijsktra's to create a sample of possible paths.
 * DEPRECATED
 */
public int[][] distance;
public int[] prevNode;

/**
 * 2d array where each element Aij is an int[]. The first element is what node 
 * the path goes to, the second element is the pixel distance, and the remaining elements
 * are the nodes of the path used to get there.
 */
ArrayList<ArrayList<int[]>> paths;

public Node(int x, int y, int type, double nodeNumber) {
	super(x,y);
	this.type = type;
	this.nodeNumber = nodeNumber;
	this.distance = null;
	this.prevNode = null;
	this.nodeIndex = -1;
	paths = new ArrayList<>();
	
}


@Override
public boolean equals(Object obj) {
	if(this == obj) {
		return true;
	}
	 if (obj == null || getClass() != obj.getClass()) {
         return false;
     }
	 
	 Node node = (Node) obj;
	 
	if(node.x == this.x && node.y == this.y && node.type == this.type) {
		return true;
	}
	
	if(this.distance(node) <= 5) {
		return true;
	}
	
	return false;
	
}



public double distance(Node node) {
	return Point.distance(this.x, this.y, node.x, node.y);
}



@Override
public String toString() {
	String color = "place holder";
	if(type == 0) {
		color = "skeleton node";
	}
	if (type == 1) {
		color = "red";
	}
	else if(type == 2) {
		color = "green";
	}
	else if(type == 3) {
		color = "mixed";
	}
	else {
		IJ.log("You shouldn't see this. Node has unknown type.");
		color = "unknown";
	}
    return "{x=" + x + ", y=" + y + ", type=" + color + '}';
}

}
