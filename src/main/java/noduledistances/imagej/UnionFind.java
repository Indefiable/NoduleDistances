package noduledistances.imagej;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class UnionFind {
    // Initialize parent array and rank array for Union-Find
    int[] parent;
    int[] rank;

    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;  // Each vertex is its own parent initially
            rank[i] = 0;    // Initialize rank to 0
        }
    }

    // Find operation to find the root (parent) of a vertex
    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);  // Path compression
        }
        return parent[x];
    }

    // Union operation to merge two sets
    public void union(int x, int y) {
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


public boolean isConnected(int[][] edges, int n) {
    UnionFind uf = new UnionFind(n);
   
    for (int[] edge : edges) {
        int u = edge[0];
        int v = edge[1];
        if (uf.find(u) == uf.find(v)) {
            // If the roots of u and v are the same, there's a cycle
            return false;
        } else {
            // Merge the sets containing u and v
            uf.union(u, v);
        }
    }
    // If there's only one disjoint set left, the graph is connected
    return Arrays.stream(uf.parent).distinct().count() == 1;
}



public static ArrayList<int[]> connectedComponents(ArrayList<int[]> edges, int n) {
    UnionFind uf = new UnionFind(n);
    
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
    for (int i = 0; i < n; i++) {
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
