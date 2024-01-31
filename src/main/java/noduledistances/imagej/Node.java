package noduledistances.imagej;

import java.awt.Point;
import java.util.HashMap;

public class Node extends Point{
public int type;
public int nodeIndex;

public int[] distance;
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
    return "{x=" + x + ", y=" + y + ", type=" + type + '}';
}

}
