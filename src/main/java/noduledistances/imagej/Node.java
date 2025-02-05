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
public static final int SKELETON = 0;
public static final int RED = 1;
public final static int GREEN = 2;
public final static int MIXED = 3;

/**
 * type == 0 -> skeleton node<br>
 * type == 1 -> red nodule<br>
 * type == 2 -> green nodule <br>
 * type == 3 -> mixed nodule <br>
 */
public int type;
public int area;

//?
public int nodeIndex;

/**
 * nodeNumber represents the number shown on the nodule csv data to easily associate
 * a nodule node with it's nodule number. If the node is not a nodule, the value defaults to -1.
 * The notation is noduleNumber_noduleSubnumber, and the underscore is represented as a decimal.
 */
public double nodeNumber;


/**
 * DEPRECATED
 * 2d array where each element A_ij is the distance to the j'th node on the 
 * i'th iteration of dijkstra's. Each row is one iteration of Dijkstra's, 
 * we remove a used edge from the original graph and perform another iteration of 
 * dijsktra's to create a sample of possible paths.
 * DEPRECATED
 */
public int[][] distance;
public int[] prevNode;

/**
 * 2d array where each element Aij is an int[] p. First index of paths is what node we're going to. The second index is 
 * what iteration of shortest path. The first element of p is what node 
 * the path goes to, the second element is the pixel distance, and the remaining elements
 * are the nodes of the path used to get there.
 */
ArrayList<ArrayList<int[]>> paths;


/**
 * constructor method
 * @param x : x coordinate of the node.
 * @param y : y coordinate of the node.
 * @param type : type of the node. 
 * @param nodeNumber : node number of the node if it is a nodule.
 * @param area : nodule area of the node if it is a nodule.
 */
public Node(int x, int y, int type, double nodeNumber, int area) {
	super(x,y);
	this.type = type;
	this.nodeNumber = nodeNumber;
	this.distance = null;
	this.prevNode = null;
	this.nodeIndex = -1;
	this.area = area;
	paths = new ArrayList<>();
	
}
/**
 * constructor method
 * @param x : x coordinate of the node.
 * @param y : y coordinate of the node.
 * @param type : type of the node.
 * @param nodeNumber : node number of the node if it is a noduel.
 */
public Node(int x, int y, int type, double nodeNumber) {
	super(x,y);
	this.type = type;
	this.nodeNumber = nodeNumber;
	this.distance = null;
	this.prevNode = null;
	this.nodeIndex = -1;
	this.area = -1;
	paths = new ArrayList<>();
	
}

/**
 * method for updating the node.
 * @param x : new x coordinate.
 * @param y : new y coordinate.
 * @param type : new type of the node.
 * @param nodeNumber : new node number for the node.
 * @param area : new nodule area for the node.
 */
public void update(int x, int y, int type, double nodeNumber, int area) {
	this.x = x;
	this.y=y;
	this.type=type;
	this.nodeNumber=nodeNumber;
	this.area=area;
}

/**
 * method for updating the node
 * @param type : new type for the node.
 * @param nodeNumber : new node number for the node.
 * @param area : new nodule area for the node.
 */
public void update(int type, double nodeNumber, int area) {
	this.type=type;
	this.nodeNumber=nodeNumber;
	this.area=area;
}


/** 
 * method for updating the nodule.
 * @param type : new type for the node.
 */
public void update(int type) {
	this.type=type;
} 


/**
 * We say two nodes are equal when any of the following are true:<br>
 * 1. they reference the same object in memory<br>
 * 2. their x/y values are the same<br>
 * 3. if they are the same type of node( skeleton/nodule) AND are within 8 pixels<br>
 */
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
	boolean typeDiff = true;
	if(this.type != 0 && this.type != 0) {
		return false;
	}
	else if((this.type <1 && node.type != 0 )|| this.type !=0 && node.type <1) {
		typeDiff = true;
	}
	
	if(this.distance(node) <= 8 && typeDiff) {
		return true;
	}
	
	return false;
	
}


/**
 * Finds and returns the set of paths from this node to the given node.
 * @param node : node to search from.
 * @return : the set of paths from this node to the given node. 
 */
public ArrayList<int[]> getPaths(int node){
	ArrayList<int[]> paths = new ArrayList<>();
	
	for (ArrayList<int[]> ps : this.paths) {
		if(ps == null) {
			continue;
		}
		//System.out.println("out node:" + ps.get(0)[0]);
		if(ps.get(0)[0] == node) {
			paths = ps;
			break;
		}
	}
	
	/**
	if(paths.size() == 0) {
		System.out.println("Could not find paths from " + this.nodeNumber +" to " + node);
	}*/
	
	return paths;
}


/**
 * computes the euclidean distance between this node and the given node.
 * @param node : node to compute the distance betwee.
 * @return : the euclidean distance between this node and the given node.
 */
public double distance(Node node) {
	return Point.distance(this.x, this.y, node.x, node.y);
}



@Override
public String toString() {
	String color = "place holder";
	if(type == SKELETON) {
		color = "skeleton node";
	}
	if (type == RED) {
		color = "red";
	}
	else if(type == GREEN) {
		color = "green";
	}
	else if(type == MIXED) {
		color = "mixed";
	}
	else {
		//IJ.log("You shouldn't see this. Node has unknown type.");
		color = "unknown";
	}
    return nodeNumber + "{x=" + x + ", y=" + y + ", type=" + color + '}';
}

}
