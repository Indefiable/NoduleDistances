package noduledistances.imagej;

import java.awt.Point;

public class Node extends Point{
public int x;
public int y;
public int type;



public Node(int x, int y, int type) {
	super(x,y);
	this.type = type;
	
}


public boolean equals(Node node) {
	
	if(node.x == this.x && node.y == this.y && node.type == this.type) {
		return true;
	}
	
	return false;
	
}

@Override
public String toString() {
    return "{x=" + x + ", y=" + y + ", type=" + type + '}';
}

}
