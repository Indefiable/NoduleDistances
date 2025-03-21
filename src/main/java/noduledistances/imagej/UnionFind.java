package noduledistances.imagej;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the union find algorithm. Used to merge 
 * disconnected components of the graph under the standing assumption that the root 
 * system of the image is connected. 
 * 
 * @author Brandin Farris
 *
 */
class UnionFind {
    // Initialize parent array and rank array for Union-Find
    int[] parent;
    int[] rank;

    /**
     * constructor method for the union find algorithm.
     * @param size : size of the union find object. 
     */
    private UnionFind(int size) {
        parent = new int[size];
        rank = new int[size];
        for (int i = 0; i < size; i++) {
            parent[i] = i;  // Each vertex is its own parent initially
            rank[i] = 0;    // Initialize rank to 0
        }
    }

    // Find operation to find the root (parent) of a vertex
    private int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);  // Path compression
        }
        return parent[x];
    }

    // Union operation to merge two sets
    private void union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);
        if (rootX != rootY) {
            if (rank[rootX] < rank[rootY]) {
                parent[rootX] = rootY;
            } else if (rank[rootX] > rank[rootY]) {
                parent[rootY] = rootX;
            } else {
                parent[rootY] = rootX;
                rank[rootX]++;
            }
        }
    }


/**
 * Uses the union find algorithm to create an ArrayList of connected components.
 * 
 * 
 * @param edges : graph to compute the connected components of.
 * @param numNodes : number of nodes in the graph.
 * @return : an array where each entry is the list of connected nodes of the disjoint graph. If the
 * first layer of the nested array has only one component, it is a connected graph.
 */
public static ArrayList<int[]> connectedComponents(ArrayList<int[]> edges, int numNodes) {
    UnionFind uf = new UnionFind(numNodes);
    
    // Map to store the root vertex and its connected vertices
    Map<Integer, List<Integer>> componentMap = new HashMap<>();
    
    for (int[] edge : edges) {
        int u = edge[0];
        int v = edge[1];
        int rootU = uf.find(u);
        int rootV = uf.find(v);
        
        if (rootU == rootV) {
            // If the roots of u and v are the same, they belong to the same component
            continue;
        }
        
        // Merge the sets containing u and v
        uf.union(u, v);
    }
    
    // Group vertices based on their roots (connected components)
    for (int i = 0; i < numNodes; i++) {
        int root = uf.find(i);
        componentMap.computeIfAbsent(root, k -> new ArrayList<>()).add(i);
    }
    
    ArrayList<int[]> connectedComponents = new ArrayList<>();
    for (List<Integer> component : componentMap.values()) {
        int[] componentArray = component.stream().mapToInt(Integer::intValue).toArray();
        connectedComponents.add(componentArray);
    }
    
    return connectedComponents;

}






}
