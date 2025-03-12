package noduledistances.imagej;


import java.awt.Point;
import java.awt.geom.Point2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;




import ij.IJ;
import ij.gui.Line;
import ij.gui.ShapeRoi;


import com.programmerare.edu.asu.emit.algorithm.graph.EdgeYanQi;
import com.programmerare.edu.asu.emit.algorithm.graph.GraphWithConstructor;

import edu.asu.emit.algorithm.graph.Path;
import edu.asu.emit.algorithm.graph.abstraction.BaseVertex;
import edu.asu.emit.algorithm.graph.shortestpaths.YenTopKShortestPathsAlg;





/**
 * The purpose of the class RootGraph is to hold all information related to the graph derived from 
 * the initial image, including the data and methods to translate between the abstract graph and
 * the point and edges on the skeleton of the image. 
 * The class also holds all computational methods relating to the abstract graph.
 * 
 * 
 * 
 * 
 */
public class RootGraph {
	
	public ArrayList<ArrayList<int[]>> skeleton;
	
	public int numNodules = 0;
	
	/**
	 * Node is an extension of java.awt.point that
	 * includes an identifier for the node, i.e.
	 * Node.type == 0 -> skeleton node
	 * type == 1 -> red nodule
	 * type == 2 -> green nodule
	 * type == 3 -> mixed nodule
	 *
	 */
	public ArrayList<Node> nodes = new ArrayList<>();
	/**
	 * forward star representation of the graph. makes all edges bi-directional by adding
	 * the reverse arc for each arc added.
	 * in the form [n1, n2, length]
	 */
	ArrayList<int[]> fsRep = new ArrayList<>();
	/**
	 * The pointer array tells you at which index of the forward star array a given node
	 *  starts appearing as the first element of an edge.
	 */
	int[] pointer;
	GraphOverlay graphOverlay;
	

	/**
	 * constructor. Constructs the graph object from the skeleton. LingDong's 
	 * algorithm wasn't designed to convert the skeleton into a graph, so sometimes
	 * the graph becomes disconnected when the provided object it's skeletonizing is connected. 
	 * We attempt to amend this by saying that any two nodes from the skeleton are the same
	 * if they are within 8 pixels (refer to Node.equals). We also add edges at the end
	 * if two nodes are within 9 pixels.
	 * 
	 * 
	 * @param skeleton : skeleton object from LingDong's skeletonization algorithm.
	 * @param graphOverlay : graphOverlay object which holds the root system image 
	 * and the graph overlaying it.
	 */
	public RootGraph(ArrayList<ArrayList<int[]>> skeleton, GraphOverlay graphOverlay) {
		this.graphOverlay = graphOverlay;
		this.skeleton = skeleton;
		
		// enumerate all nodes and creates the forward star representation.
		// chunk object is a list of of points. Each set of two points is the starting
		// and ending point for an edge of the skeleton.
		for( ArrayList<int[]> chunk : skeleton) {
			
			for(int ii = 0; ii < chunk.size(); ii+=2) {
				
				int[] start = chunk.get(ii);
			    int[] end = chunk.get(ii+1);
			    if(nodes.size() == 116) {
			    	System.out.println("Breakpoint.");
			    }
			    Node startPt = new Node(start[0], start[1], 0, -1);
			    Node endPt = new Node(end[0], end[1], 0, -1);
			    
			    // sometimes the skeletonization algorithm will create 
			    // a line that is very very small, so I will say those 
			    // nodes are "equal"

			    if(startPt.equals(endPt)) {
			    	continue;
			    }
			    if(endPt.equals(startPt)) {
			    	continue;
			    }
			    
			    if(!nodes.contains(startPt)) {
			    	nodes.add(startPt);
			    }
			  
			    
			    if(!nodes.contains(endPt)) {
			    	nodes.add(endPt);
			    }
			   
			    
			    Line line = new Line(startPt.x, startPt.y, endPt.x, endPt.y);
			    int length = (int) line.getLength();
			    
			    int node1 = nodes.indexOf(startPt);
			    int node2 = nodes.indexOf(endPt);
			    
			    startPt.nodeIndex = node1;
			    endPt.nodeIndex = node2;
			    if(node1 == node2) {
			    	System.out.println("Nodes passed first non-equivalence test, failed second. Skipping"
			    			+ "the given edge");
			    	continue;
			    }
			    fsRep.add(new int[] {node1, node2, length});
			    fsRep.add(new int[] {node2, node1, length});
			}
		}
		
		
		Collections.sort(fsRep, (arr1, arr2) -> {
		    // Compare the first values
		    int compareFirst = Integer.compare(arr1[0], arr2[0]);
		    
		    // If the first values are equal, compare the second values
		    if (compareFirst == 0) {
		        return Integer.compare(arr1[1], arr2[1]);
		    } else {
		        return compareFirst;
		    }
		});
		
		
		for(int ii =0 ;ii < fsRep.size()-1; ii++) {
			if(fsRep.get(ii)[0] > fsRep.get(ii+1)[0]) {
				System.out.println(ii);
				System.out.println("breakpoint");
				
			}
		}
		
		updatePointer();
		
		addMissingEdges();
		
		System.out.println("number of nodes: " + nodes.size());
		System.out.println("number of edges: " + (fsRep.size() / 2));
		}//enumerate nodes
	
	
	
	
	/**
	 * The skeletonization algorithm sometimes does not add edges/arcs
	 * where it makes sense to. This happens in cases where the nodes are very
	 * close together, so to compensate we will add arcs when nodes are within 
	 * a small distance.
	 */
	private void addMissingEdges() {
		
		for (Node node : nodes) {
			
			for(Node node1 : nodes) {
				
				if(node == node1) {
					continue;
				}
				if(node.distance(node1) <= 9) {
				
				}
				
			}
		}
		
		
		
	}
	
	
	
	/**
	 * Adds an edge to the graph.
	 * @param edge : edge to add to the graph.
	 */
	public void addEdge(Node[] edge) {
		
		if (!nodes.contains(edge[0])) {
			IJ.log("Edge contains unknown Node. Adding the following node:");
			IJ.log(edge[0].toString());
			nodes.add(edge[0]);
		}
		
		if (!nodes.contains(edge[1])) {
			IJ.log("Edge contains unknown Node. Adding the following node:");
			IJ.log(edge[1].toString());
			nodes.add(edge[1]);
		}
		
		int node1 = nodes.indexOf(edge[0]);
		int node2 = nodes.indexOf(edge[1]);
		if(node1 == node2) {
			System.out.println("Will not add self-looping edges.");
			return;
		}
		Line line = new Line(edge[0].x, edge[0].y, edge[1].x, edge[1].y);
		int length = (int) line.getLength();
		
		fsRep.add(new int[] {node1, node2, length});
		fsRep.add(new int[] {node2, node1, length});
		
		Collections.sort(fsRep, Comparator.comparingInt(arr -> arr[0]));
		
		updatePointer();
	}
	
	/**
	 * method for removing the given edge. 
	 * 
	 * @param nodeEdge : edge to remove. nodeEdge should be an array of size 2, 
	 * one for each endpoint of the edge
	 */
	public void removeEdge(Node[] nodeEdge) {
		int node1 = nodes.indexOf(nodeEdge[0]);
		int node2 = nodes.indexOf(nodeEdge[1]);
		
		int[] edge = new int[] {node1, node2};
		
		int edgeIndex1 = -1;
		
		for (int index = pointer[node1]; index < pointer[node1+1]; index++) {
			int[] cEdge = fsRep.get(index);
			if(cEdge[0] == edge[0] && cEdge[1] == edge[1]) {
				edgeIndex1 = index;
				break;
			}
		}
		
		if(edgeIndex1 == -1) {
			IJ.log("error, the edge you're trying to remove does not exist.");
			return;
		}
		
		int edgeIndex2 = -1;
		
		for (int index = pointer[node2]; index < pointer[node2+1]; index++) {
			int[] cEdge = fsRep.get(index);
			if(cEdge[0] == edge[1] && cEdge[1] == edge[0]) {
				edgeIndex2 = index;
				break;
			}
		}
		
		if(edgeIndex2 == -1 && edgeIndex1 != -1) {
			IJ.log("Error, an edge exists in one direction but not the other.");
		}
		else if(edgeIndex2 == -1 && edgeIndex1 == -1) {
			IJ.log("Technically, you shouldn't be able to see this. But error, the edge"
					+ "you're trying to remove does not exist.");
			return;
		}
		
		if (edgeIndex2 < edgeIndex1) {
			fsRep.remove(edgeIndex1);
			fsRep.remove(edgeIndex2);
		}
		else {
			fsRep.remove(edgeIndex2);
			fsRep.remove(edgeIndex1);
		}
		
		updatePointer();
	}
	
	
	/**
	 * creates/updates the forward star pointer array. The pointer array tells you at which index
	 * of the forward star array a given node starts appearing as the first element of an edge.
	 */
	private void updatePointer() {
		pointer = new int[nodes.size()+1];
		
		int newNum = -1;
		
		int firstNode = fsRep.get(0)[0];
		
		int current = firstNode;
		
		for(int ii = 0; ii < firstNode+1; ii++) {
			pointer[ii] = 0;
		}
			
		for(int ii = 0; ii < fsRep.size(); ii++) {
			
			newNum = fsRep.get(ii)[0];
			
			if(current > newNum) {
				System.out.println(fsRep.get(ii-1));
				System.out.println(fsRep.get(ii));
				System.out.println(fsRep.get(ii+1));	
			}
			
			if(newNum > current) {
				
				for(int jj = 1; jj < newNum-current+1; jj++) {
					pointer[current+jj] = ii;
				}
				
				current = newNum;
			}
		}
		
		pointer[pointer.length-1] = fsRep.size(); 
	}
	
	
	/**
	 * Used to merge disconnected parts of the graph to create one connected graph.
	 * 
	 * @param components : parts of the graph that are disconnected.
	 */
	public void mergeNonemptyComponents(ArrayList<int[]> components) {
		removeDeadComponents(components);
		for(int ii = 0; ii < components.size(); ii++) {
			mergeComponents(components, ii);
		}
	}
	
	
	/**
	 * Find a second component to merge with index0 of the components array by 
	 * attempting to find another component that is within 15 pixels.
	 * 
	 * @param components : list of all disjoint components of the graph. 
	 * @param index0 : index to merge with another component.
	 */
	private void mergeComponents(ArrayList<int[]> components, int index0) {
		
		int[] comp1 = components.get(index0);
		int node1Index = -1;
		int componentIndex = -1;
		
		
		for(int index1 : comp1) {
			
			Node node1 = this.nodes.get(index1);
			if(node1 == null) {
				System.out.println("Null node. ");
			}
			
			for(int ii = 0; ii < components.size(); ii++) {
				if(ii == index0) {
					continue;
				}
				
				for(int index : components.get(ii)) {
					Node node = this.nodes.get(index);
					if(node == null) {
						System.out.println("Null node.");
						continue;
					} 	
					double distance1 = node.distance(node1);
					
					if(distance1 < 15) {
						node1Index = index1;
						componentIndex = ii;
						
						if(node1Index == -1) {
							System.out.println("Breakpoint.");
						}
						if(node1 == node) {
							continue;
						}
						this.addEdge(new Node[] {node1,node});
						merge(componentIndex,index0, components);
						mergeComponents(components, index0);
						return;
					}
				}
			}
		}
		
		
		
	}
	
	/**
	 * We merge the given components.
	 * 
	 * @param index index of component to merge
	 * @param index0 second index of component to merge
	 * @param components array of disconnected components
	 */
	private void merge(int index,int index0, ArrayList<int[]> components) {
		
		if(index > components.size()) {
			System.out.println("Error, index > number of components.");
			return;
		}
		else if(index0 > components.size()) {
			System.out.println("Error, index > number of components.");
			return;
		}
		
		int[] comp = components.get(index);
		
		int[] master = components.get(index0);
		
		int[] newMaster = new int[comp.length + master.length];
		
		System.arraycopy(master, 0, newMaster, 0, master.length);

		System.arraycopy(comp, 0, newMaster, master.length, comp.length);
		
		components.remove(index);
		components.set(index0, newMaster);
	}
	
	
	/**
	 * Deletes disconnected pieces of the graph that contain no nodules. i.e. they are not needed
	 * for the analysis, so they're removed instead of added to the parent graph.
	 * 
	 * @param components : set of disjoint components of the graph.
	 */
	private void removeDeadComponents(ArrayList<int[]> components) {
		
		ArrayList<Integer> deadComps = new ArrayList<>();
		
		for(int ii = 0; ii < components.size(); ii++) {
			
			if(components.get(ii).length == 0) {
				deadComps.add(ii);
				continue;
			}
			
			int[] comp = components.get(ii);
			boolean containsNodules = false;
			
			for(int vertex : comp) {
				if( nodes.get(vertex).type >0) {
					containsNodules = true;
					break;
				}
			}
			
			if(!containsNodules) {
				deadComps.add(ii);
			}
		}
		 deadComps.sort((a, b) -> b.compareTo(a));
		
		 for (int index : deadComps) {
	            if (index >= 0 && index < components.size()) {
	                components.remove(index);
	            }
	     }
		 
		 
	}
	

	
	/**
	* Finds searches for all in-arcs of the given node.
	* 
	 * @param node : node to find the in-arc for
	 * @return : all points who has arcs entering the given node.
	 */
	public ArrayList<Point[]> getInstances(Point node){
		ArrayList<Point[]> instances = new ArrayList<>();
		
		for(ArrayList<int[]> chunk : skeleton) {
			
			for(int ii = 0; ii < chunk.size(); ii+=2 ) {
				
				int[] start = chunk.get(ii);
			    int[] end = chunk.get(ii+1);
			    
			    if(start[0] == node.x && start[1] == node.y) {
			    	instances.add(new Point[] {node, new Point(end[0], end[1])});
			    	}
			    }
			
			
			}
		
		return instances;
	}
	
	
   /**
    * adds the set of nodules as Node objects to the graph by connecting each nodule node 
    * to the nearest node currently within the graph with an edge.
    * 
    * @param nodLocations a list of x,y coordinates of the contour centroids of the polygon outlines
    * of the nodules.
    */
    public void addNodules(ArrayList<int[]> nodLocations) {
    	
		if(nodLocations == null) {
			System.out.println("Error reading nodule locations. Unable to add them.");
			return;
			}
		
	double noduleNumber = 1;
    for(int[] nodule : nodLocations) {
    	int ii = 0;
    	while(ii < nodule.length) {
    		double subNumber = (ii + 4)/4;
    		if(subNumber != (int) subNumber) {
    			System.out.println("Error with sub-numbering.");
    			System.out.println("ii: " + ii);
    			System.out.println("subnumber: " + subNumber);
    		}
    		
    		double number;
    		if(nodule.length == 4) {
    			number = (int) noduleNumber;
    		}
    		else {
    			number = noduleNumber + subNumber / Math.pow(10, String.valueOf(subNumber).length());
    		}
    		
    		Node nod = new Node(nodule[ii+1],nodule[ii+2], nodule[ii], number, nodule[ii+3]);
    		
    		if(nodes.contains(nod)) {
    			int index = nodes.indexOf(nod);
    			if(nodes.get(index).type != 0) {
    				System.out.println("Breakpoint.");
    			}
    			else {
    				nodes.get(index).update(nodule[ii+1],nodule[ii+2], nodule[ii], number, nodule[ii+3]);
        			nodes.get(index).nodeIndex = index;
        			ii+=4;
        			numNodules++;
        			continue;
    			}
    			
    		}
    			
    			
    			
    		if(nod.type <1 || nod.type > 3) {
    			System.out.println("nod type is " + nod.type + " and unclear. breakpointing.");
    			System.out.println("breakpoint.");
    		}
	    	nodes.add(nod);
	    	int nodeIndex = nodes.indexOf(nod);
	    	nod.nodeIndex = nodeIndex;
	    	
	    	Node closestNode = calculateClosestNode(nod);
	    	
	    	int closestIndex = nodes.indexOf(closestNode);
	    	
	    	int length = (int) Node.distance(nod.x, nod.y, closestNode.x, closestNode.y);
	    	
	    	fsRep.add(new int[] {nodeIndex,closestIndex , length});
	    	fsRep.add(new int[] {closestIndex,nodeIndex , length});
	    	numNodules++;
	    	ii +=4;
    	}
    	noduleNumber+=1;
    }
    
    Collections.sort(fsRep, Comparator.comparingInt(arr -> arr[0]));
	
    
	pointer = new int[nodes.size()+1];
	
	int current = 0;
	int newNum = -1;
	
	int firstNum = fsRep.get(0)[0];
	
	for(int ii = 0; ii < firstNum-0+1; ii++) {
		pointer[ii] = 0;
	}
		
	for(int ii = 0; ii < fsRep.size(); ii++) {
		newNum = fsRep.get(ii)[0];
		
		if(current > newNum) {
			System.out.println(fsRep.get(ii-1));
			System.out.println(fsRep.get(ii));
			System.out.println(fsRep.get(ii+1));
			System.out.println("breakpoint");
		}
		
		if(newNum > current) {
			
			for(int jj = 1; jj < newNum-current+1; jj++) {
				pointer[current+jj] = ii;
			}
			
			current = newNum;
		}
	}
	pointer[pointer.length-1] = fsRep.size(); 
    
	System.out.println("number of nodes: " + nodes.size());
	System.out.println("number of edges: " + (fsRep.size() / 2));
	
    }// addNodes()
    
    /**
     * Checks whether the graph contains the edge [a,b].
     * 
     * @param a : source node of arc in question.
     * @param b : sink node of arc in question.
     * @return : true if such an arc exists. False otherwise.
     */
    public boolean containsEdge(int a, int b) {
    	
    	for (int[] edge : fsRep) {
    	    if (edge[0] == a && edge[1] == b) {
    	        // Edge found
    	       return true;
    	    }
    	}
    	return false;
    }
    
   /**
    * Calculates the Node on the graph closest to the Node given as input. Primarily
    *  used for determining which Node to connect the Nodule Nodes being added to.
    *  
    * @param Node p : node to search around.
    * @return Node closest to Node p (euclidean distance).
    */
    private Node calculateClosestNode(Node p) {
    	
    	Node closest = null;
    	double dist = Integer.MAX_VALUE;
    	double compDist;
    	
    	for(Node node : nodes) {
    		if(node.equals(p)) {
    			continue;
    		}
    		compDist = Point.distance(node.x, node.y, p.x, p.y);
    		
    		if(compDist < dist) {
    			closest = node;
    			dist = compDist;
    		}
    	}
    	
    	return closest;
    }
    
    
    /**
     * Computes and stores the all-pairs distance calculations between all 
     * Nodules numIteration times. In each iteration, we remove a different edge 
     * 
     * @param numIterations : the number of times we compute the distance calculations.
     */
    public void computeShortestDistances(int numIterations) {
    	
    	GraphWithConstructor yanGraph = convertToYanGraph();
    	
    	YenTopKShortestPathsAlg yenAlg = new YenTopKShortestPathsAlg(yanGraph);
    	
    	for(Node nodule : getNodules()) {
    		int startingNodule = nodes.indexOf(nodule);
    		
    		for(Node nodule1 : getNodules()) {
    			int endingNodule = nodes.indexOf(nodule1);
    			
    			if(startingNodule == endingNodule) {
    				nodule.paths.add(null);
    				continue;
    			}
    			
    			
    			List<Path> shortest_paths_list = yenAlg.getShortestPaths(
    					yanGraph.getVertex(startingNodule), yanGraph.getVertex(endingNodule), numIterations);
    			
    			
    			if(shortest_paths_list == null) {
    				System.out.println("No paths between the two nodules.");
    				System.out.println("breakpoint.");
    			}
    			
    		
    			if(shortest_paths_list.size() == 0) {
    				System.out.println("no paths between." + nodule.nodeNumber + "/" 
    			+ startingNodule+ " and " + nodule1.nodeNumber + "/" + endingNodule );
    			}	
    			ArrayList<int[]> paths = shortestPathsToList(shortest_paths_list);
    			
    			if(paths.size() == 0) {
    				continue;
    			}
    			
    			nodule.paths.add(paths);
    		}
    		
    	}
    	
    }
    
    
    /**
     * converts the list of computed shortest paths to an ArrayList of Arrays of ints.
     * 
     * @param shortest_paths_list : object to convert.
     * @return : converted object.
     */
    protected ArrayList<int[]> shortestPathsToList(List<Path> shortest_paths_list) {
    	
    	ArrayList<int[]> paths = new ArrayList<>();
		
		for( Path path1 : shortest_paths_list) {
			
			List<BaseVertex> path = path1.getVertexList();
			
			int length = computeLengthOfPath(path);
			
			ArrayList<Integer> nodePath = path.stream().map(BaseVertex::getId)
                    .collect(Collectors.toCollection(ArrayList::new));
			
			nodePath.add(0, nodePath.get(nodePath.size()-1));
			nodePath.add(1,length);
			
			 int[] intPath = nodePath.stream()
                     .mapToInt(Integer::intValue)
                     .toArray();
			 
			 paths.add(intPath);
		}
		
	
		
		return paths;
    }
    
    
    /**
     * Computes the length of the given path. i.e. the sum of the 
     * lengths of the lines between each arc in the path.
     * 
     * @param path : path to compute the length of.
     * @return : length of the given path.
     */
    protected int computeLengthOfPath(List<BaseVertex> path) {
    	int length = 0;
    	
    	
    	for(int ii = 0; ii < path.size()-1; ii++) {
    		int node1 = path.get(ii).getId();
    		int node2 = path.get(ii+1).getId();
    		
    		for(int jj = pointer[node1]; jj < pointer[node1+1]; jj++) {
    			
    			int[] edge = fsRep.get(jj);
    			if(node2 == edge[1]) {
    				length += edge[2];
    				break;
    			}
    			if(jj == pointer[node1+1]-1) {
    				System.out.println("Could not find the following edge:" + node1 + "," + node2);
    			}
    		}
    		
    	}
    	
    	return length;
    }
    
    
    /**
     * creates a graph structure using Yan Qi's graph package to utilize
     * their implementation of k shortest paths. 
     * @return YanQi's graph object.
     */
    protected GraphWithConstructor convertToYanGraph() {
    	
    	List<EdgeYanQi> edges = new ArrayList<>();
    	
    	for (int[] edge : fsRep) {
    		if(edge[0] == edge[1]) {
    			System.out.println("Error, graph contains an edge [e,e] (self loop)");
    			continue;
    		}
    		
    		EdgeYanQi yanEdge = new EdgeYanQi(edge[0], edge[1], edge[2]);
    		edges.add(yanEdge);
    		
    	}
    	
    	GraphWithConstructor yanQiGraph = new GraphWithConstructor(nodes.size(), edges);
    	
    	
    	return yanQiGraph;
    }
    
   
    /** 
     * 
     * @return : the set of nodes of the graph that are nodules.
     */
       public Node[] getNodules() {
    	
    	ArrayList<Node> nodules = new ArrayList<>();
    	
    	
    	for(int ii = 0; ii < nodes.size(); ii++) {
    		Node node = nodes.get(ii);
    		
    		if(node.type > 0) {
    			node.nodeIndex = ii;
    			nodules.add(node);
    		}
    		
    	}
    	
    	 return nodules.toArray(new Node[0]);
    }
    
    
   
      
	/**
	 * Find the numNods closest nodes to the given point, then creates an arrayList
	 * of lines that are all edges containing any of the closest nodes.<br>
	 * i.e. it records all edges containing the numNodes closest nodes, returning them as 
	 * Roi objects.
	 * 
	 * @param numNodes : number of nodes to find the edges of
	 * @param pt : point to search around.
	 * @return : an arraylist of ShapeRoi lines which are the edges containing the 
	 * edges of the numNodes closest nodes.
	 */
	public ArrayList<ShapeRoi> ballSubgraphLines(int numNodes, Point2D pt){
		ArrayList<ShapeRoi> lines = new ArrayList<>();
		int[] rettNodes = new int[numNodes];
		

		// Priority queue to store the k closest nodes
        PriorityQueue<Node> closestNodesQueue = new PriorityQueue<>
        (Comparator.comparingDouble(node -> ((Node) node).distance(pt)).reversed());
        
        // Iterate over each node
        for (Node node : nodes) {
            double distance = node.distance(pt);
            
            // If the queue is not full or the current node is closer than the farthest node in the queue
            if (closestNodesQueue.size() < numNodes || distance < closestNodesQueue.peek().distance(pt)) {
                closestNodesQueue.offer(node);
            }
            
            // If the queue exceeds the limit, remove the farthest node
            int size = closestNodesQueue.size();
            if (size > numNodes) {
                closestNodesQueue.poll();
            }
        }
        
        // Convert the priority queue to an array
        Node[] retNodes = closestNodesQueue.toArray(new Node[0]);
		
        for(int ii = 0; ii < numNodes; ii++) {
        	rettNodes[ii] = nodes.indexOf(retNodes[ii]);
        }
		
		
		for(Node node : retNodes) {
			if(node == null) {
				System.out.println("Null node in closest Nodes method.");
			}
		}
		
		for(int[] edge : fsRep) {
			
			int n1 = edge[0];
			int n2 = edge[1];
			
		    
			boolean n11 = false;
			boolean n22 = false;
			
			for(int node : rettNodes) {
				
				if(n1 == node) {
					n11 = true;
				}
				else if(n2 == node) {
					n22 = true;
				}
				
			}
			
			
			if(n11 || n22) {
				//processedEdges.add(edgeKey);
				Line line = new Line(nodes.get(n1).x,nodes.get(n1).y, nodes.get(n2).x,nodes.get(n2).y);
				ShapeRoi lineRoi = new ShapeRoi(line);
				lines.add(lineRoi);
			}
			
		}
		
		
		return lines;
	}
   

         
    
    /**
	 * Find the numNodes closest Nodes to the given pt, and finds all edges
	 * that contain any of those nodes.
	 * 
	 * @param numNodes : number of nodes to find.
	 * @param pt : point to search around.
	 * @return : subgraph containing the numNodes closest nodes and all edges related 
	 * to,them.
	 */
	public ArrayList<int[]> ballSubgraph(int numNodes, Point2D pt) {
		
		ArrayList<int[]> subgraph = new ArrayList<>();
		
		Node[] retNodes = new Node[numNodes];
		int[] rettNodes = new int[numNodes];
		double[] distances = new double [numNodes];
		
		
		
		for(Node node : nodes) {
			
			int minIndex = -1;
			double minDistance = Double.MAX_VALUE;
			
			for(int ii = 0; ii < numNodes; ii++) {
				distances[ii] = node.distance(pt);
				
				if(distances[ii] < minDistance) {
					minDistance = distances[ii];
					minIndex = ii;
				}
				
			}
			
			int i =0;
			for(Node retNode : retNodes) {
				if(retNode == null) {
					retNodes[i] = node;
					rettNodes[i] = nodes.indexOf(node);
					break;
				}
				i++;
			}
			
			if( node.distance(pt) < minDistance) {
				retNodes[minIndex] = node;
				rettNodes[minIndex] = nodes.indexOf(node);
			}
			
		}
		
		for(Node node : retNodes) {
			if(node == null) {
				System.out.println("Null node in closest Nodes method.");
				System.out.println("Breakpoint");
				
			}
		}
		
		
		for(int[] edge : fsRep) {
			
			int n1 = edge[0];
			int n2 = edge[1];
			
			boolean n11 = false;
			boolean n22 = false;
			
			for(int node : rettNodes) {
				
				if(n1 == node) {
					n11 = true;
				}
				else if(n2 == node) {
					n22 = true;
				}
				
			}
			
			if(n11 || n22) {
				subgraph.add(edge);
			}
			
		}
		
		
		
		return subgraph;
	}
	
	
    /**
     * Returns the subgraph that is all edges containing a nodule node as at least one of the nodes.
     * 
     * @return The forward star representation as an ArrayList of Arrays of ints.
     */
      public ArrayList<int[]> noduleFSRep(){
      	ArrayList<int[]> nodFSRep = new ArrayList<>();
      	
      	Node[] nodules = getNodules();
      	
      	for(int[] edge : this.fsRep) {
      		
      		for(Node nod : nodules) {
      			int enumNode = nodes.indexOf(nod);
      			
      			if(enumNode == edge[0] || enumNode == edge[1]) {
      				nodFSRep.add(edge);
      			}
      		}
      		
      	}
      	
      	
      	return nodFSRep;
      	
      }
    
    
}