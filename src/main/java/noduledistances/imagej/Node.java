package noduledistances.imagej;

import java.awt.Point;
import java.util.HashMap;

import ij.IJ;

public class Node extends Point{
	
public int type;
public int nodeIndex;

/**
 * 2d array where each element A_ij is the distance to the j'th node on the 
 * i'th iteration of dijkstra's. Each row is one iteration of Dijkstra's, 
 * we remove a used edge from the original graph and perform another iteration of 
 * dijsktra's to create a sample of possible paths.
 */
public int[][] distance;
public int[] prevNode;


public Node(int x, int y, int type) {
	super(x,y);
	this.type = type;
	this.distance = null;
	this.prevNode = null;
	this.nodeIndex = -1;
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
