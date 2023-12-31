package noduledistances.imagej;

import java.awt.Point;
import java.util.ArrayList;

import ij.gui.Line;

/**
 * The purpose of the class Graph is to hold all information related to the graph derived from 
 * the initial image, including the data and methods to translate between the abstract graph and
 * the point and edges on the skeleton of the image. 
 * The class also holds all computational methods relating to the abstract graph.
 * 
 * 
 * 
 * one pixel is (2601/ 4064256) mm^2
 */
public class Graph {

	ArrayList<int[]> graph;
	public ArrayList<ArrayList<int[]>> skeleton;
	public ArrayList<Point> nodes = new ArrayList<>();
	ArrayList<int[]> fsRep = new ArrayList<>();
	
	/**
	 * initializer. Constructs the graph object from the skeleton.
	 * @param skeleton
	 */
	public Graph(ArrayList<ArrayList<int[]>> skeleton) {
		
		this.skeleton = skeleton;
		
		// enumerate all nodes.
		for( ArrayList<int[]> chunk : skeleton) {
			
			for(int ii = 0; ii < chunk.size(); ii+=2) {
				int[] start = chunk.get(ii);
			    int[] end = chunk.get(ii+1);
			    
			    Point startPt = new Point(start[0], start[1]);
			    Point endPt = new Point(end[0], end[1]);
			    
			    if(!nodes.contains(startPt)) {
			    	nodes.add(startPt);
			    }
			    if(!nodes.contains(endPt)) {
			    	nodes.add(endPt);
			    }
			    
			    Line line = new Line(startPt.x, startPt.y, endPt.x, endPt.y);
			    int length = line.size();
			    int node1 = nodes.indexOf(startPt);
			    int node2 = nodes.indexOf(endPt);
			    
			    fsRep.add(new int[] {node1, node2, length});
			    fsRep.add(new int[] {node2, node1, length});
			}
		}//enumerate nodes
		
		System.out.println("number of nodes: " + nodes.size());
		System.out.println("number of edges: " + (fsRep.size() / 2));
	}
	
	
	
}
