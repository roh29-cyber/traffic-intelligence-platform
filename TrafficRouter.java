import java.util.*;

/**
 * Traffic Intelligence Platform — Core Algorithm (Java)
 * ======================================================
 * Data Structures Used:
 *   - HashMap        → Adjacency List, distances, previous
 *   - PriorityQueue  → Min-Heap for Dijkstra
 *   - HashSet        → visited nodes
 *   - ArrayList      → neighbor lists, path reconstruction
 */

public class TrafficRouter {

    // ─────────────────────────────────────────────────
    //  Edge: represents a road between two intersections
    // ─────────────────────────────────────────────────
    static class Edge {
        String to;
        int    baseWeight;
        int    currentWeight;   // affected by traffic
        String trafficLevel;    // "low", "medium", "high"

        Edge(String to, int weight) {
            this.to            = to;
            this.baseWeight    = weight;
            this.currentWeight = weight;
            this.trafficLevel  = "low";
        }

        void applyTraffic(String level) {
            Map<String, Double> mult = Map.of("low", 1.0, "medium", 1.6, "high", 2.5);
            this.trafficLevel  = level;
            this.currentWeight = (int) Math.round(baseWeight * mult.get(level));
        }
    }

    // ─────────────────────────────────────────────────
    //  Graph using Adjacency List (HashMap)
    // ─────────────────────────────────────────────────
    static class TrafficGraph {
        // HashMap: node → list of edges   (Adjacency List)
        HashMap<String, ArrayList<Edge>> adjacency = new HashMap<>();

        void addNode(String id) {
            adjacency.putIfAbsent(id, new ArrayList<>());
        }

        void addEdge(String from, String to, int weight) {
            adjacency.get(from).add(new Edge(to, weight));
            adjacency.get(to).add(new Edge(from, weight));
        }

        void setTraffic(String from, String to, String level) {
            for (Edge e : adjacency.getOrDefault(from, new ArrayList<>()))
                if (e.to.equals(to)) e.applyTraffic(level);
            for (Edge e : adjacency.getOrDefault(to, new ArrayList<>()))
                if (e.to.equals(from)) e.applyTraffic(level);
        }

        Edge getEdge(String from, String to) {
            for (Edge e : adjacency.getOrDefault(from, new ArrayList<>()))
                if (e.to.equals(to)) return e;
            return null;
        }

        // ── Dijkstra's Algorithm ──────────────────────
        // Uses Java's built-in PriorityQueue (min-heap)
        Map<String, Integer>  dijkstra(String source, boolean useTraffic) {
            // HashMap for distances and previous node
            HashMap<String, Integer> dist = new HashMap<>();
            HashMap<String, String>  prev = new HashMap<>();
            HashSet<String>        visited = new HashSet<>();

            for (String node : adjacency.keySet()) {
                dist.put(node, Integer.MAX_VALUE);
                prev.put(node, null);
            }
            dist.put(source, 0);

            // PriorityQueue: [cost, node] sorted by cost (min-heap)
            PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
            // Map node string to int for PQ
            List<String> nodeList = new ArrayList<>(adjacency.keySet());
            Map<String, Integer> nodeIdx = new HashMap<>();
            for (int i = 0; i < nodeList.size(); i++) nodeIdx.put(nodeList.get(i), i);

            pq.offer(new int[]{0, nodeIdx.get(source)});

            while (!pq.isEmpty()) {
                int[] top     = pq.poll();
                String current = nodeList.get(top[1]);

                if (visited.contains(current)) continue;
                visited.add(current);

                for (Edge edge : adjacency.getOrDefault(current, new ArrayList<>())) {
                    if (visited.contains(edge.to)) continue;
                    int weight  = useTraffic ? edge.currentWeight : edge.baseWeight;
                    int newDist = dist.get(current) + weight;

                    if (newDist < dist.get(edge.to)) {
                        dist.put(edge.to, newDist);
                        prev.put(edge.to, current);
                        pq.offer(new int[]{newDist, nodeIdx.get(edge.to)});
                    }
                }
            }

            // Store previous for path reconstruction
            this.lastPrev = prev;
            return dist;
        }

        HashMap<String, String> lastPrev; // set after dijkstra()

        List<String> getPath(String source, String dest) {
            LinkedList<String> path = new LinkedList<>();
            String node = dest;
            while (node != null) {
                path.addFirst(node);
                node = lastPrev.get(node);
                if (path.size() > adjacency.size() + 1) return new ArrayList<>(); // cycle guard
            }
            if (!path.getFirst().equals(source)) return new ArrayList<>();
            return path;
        }
    }

    // ─────────────────────────────────────────────────
    //  Build City Graph (14 intersections, 26 roads)
    // ─────────────────────────────────────────────────
    static TrafficGraph buildCityGraph() {
        TrafficGraph g = new TrafficGraph();
        String[] nodes = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N"};
        for (String n : nodes) g.addNode(n);

        int[][] edges = {
            // encoded as pairs with weight — using helper below
        };

        // [from, to, weight]
        String[][] roadList = {
            {"A","N","12"},{"A","I","18"},
            {"B","N","10"},{"B","M","8"}, {"B","C","15"},
            {"C","L","11"},{"C","D","9"},
            {"D","E","14"},{"D","L","8"},
            {"E","F","12"},{"E","L","16"},
            {"F","G","9"}, {"F","K","10"},{"F","L","14"},
            {"G","H","11"},{"G","K","13"},
            {"H","I","15"},{"H","J","10"},
            {"I","J","9"}, {"I","N","16"},
            {"J","K","12"},{"J","M","14"},{"J","N","11"},
            {"K","L","7"}, {"K","M","9"},
            {"M","N","6"}
        };

        for (String[] r : roadList)
            g.addEdge(r[0], r[1], Integer.parseInt(r[2]));

        return g;
    }

    // ─────────────────────────────────────────────────
    //  Simulate traffic (biased random per edge)
    // ─────────────────────────────────────────────────
    static void applyTraffic(TrafficGraph g, String level) {
        String[][] biasTable = {
            {"low","low","low","low","low","low","low","medium","medium","high"},     // low
            {"low","low","medium","medium","medium","medium","medium","high","high","high"}, // medium
            {"low","medium","medium","high","high","high","high","high","high","high"}  // high
        };
        int tableIdx = level.equals("low") ? 0 : level.equals("medium") ? 1 : 2;
        String[] options = biasTable[tableIdx];

        Random rand = new Random();
        Set<String> done = new HashSet<>();
        for (String from : g.adjacency.keySet()) {
            for (Edge e : g.adjacency.get(from)) {
                String key = from.compareTo(e.to) < 0 ? from+"-"+e.to : e.to+"-"+from;
                if (!done.contains(key)) {
                    done.add(key);
                    g.setTraffic(from, e.to, options[rand.nextInt(options.length)]);
                }
            }
        }
    }

    // ─────────────────────────────────────────────────
    //  Main — Run Demo
    // ─────────────────────────────────────────────────
    public static void main(String[] args) {
        Map<String, String> names = Map.ofEntries(
            Map.entry("A","Airport"),   Map.entry("B","Bank Sq"),
            Map.entry("C","City Hall"), Map.entry("D","Downtown"),
            Map.entry("E","East End"),  Map.entry("F","Fairview"),
            Map.entry("G","Garden"),    Map.entry("H","Harbor"),
            Map.entry("I","Industrial"),Map.entry("J","Junction"),
            Map.entry("K","Kings Rd"), Map.entry("L","Lake Bridge"),
            Map.entry("M","Metro Park"),Map.entry("N","North Gate")
        );

        runDemo("A", "E", "high",   names);
        runDemo("A", "B", "medium", names);
        runDemo("I", "D", "low",    names);
    }

    static void runDemo(String src, String dst, String level, Map<String, String> names) {
        TrafficGraph g = buildCityGraph();

        System.out.println("\n" + "=".repeat(55));
        System.out.println("  Traffic Intelligence Platform (Java)");
        System.out.println("=".repeat(55));
        System.out.printf("  Source      : %s — %s%n", src, names.get(src));
        System.out.printf("  Destination : %s — %s%n", dst, names.get(dst));
        System.out.printf("  Traffic     : %s%n%n", level.toUpperCase());

        // BEFORE traffic
        Map<String, Integer> distBase = g.dijkstra(src, false);
        List<String> pathBase = g.getPath(src, dst);
        int costBase = distBase.get(dst);
        System.out.println("  BEFORE (base weights):");
        System.out.printf("    Path : %s%n", String.join(" -> ", pathBase));
        System.out.printf("    Cost : %d%n%n", costBase);

        // Apply traffic
        applyTraffic(g, level);

        // AFTER traffic
        Map<String, Integer> distTraffic = g.dijkstra(src, true);
        List<String> pathTraffic = g.getPath(src, dst);
        int costTraffic = distTraffic.get(dst);
        System.out.printf("  AFTER (%s traffic — per-edge random):%n", level.toUpperCase());
        System.out.printf("    Path : %s%n", String.join(" -> ", pathTraffic));
        System.out.printf("    Cost : %d%n%n", costTraffic);

        // Comparison
        int diff = costTraffic - costBase;
        int pct  = costBase > 0 ? Math.abs(diff) * 100 / costBase : 0;
        System.out.println("  COMPARISON:");
        System.out.printf("    Same path? : %s%n", pathBase.equals(pathTraffic) ? "Yes" : "No — rerouted!");
        if (diff > 0)      System.out.printf("    Extra cost : +%d units (+%d%%)%n", diff, pct);
        else if (diff < 0) System.out.printf("    Saved      : %d units (%d%%)%n", -diff, pct);
        else               System.out.println("    No impact on cost");

        // Route detail
        System.out.println("\n  ROUTE DETAIL:");
        for (int i = 0; i < pathTraffic.size() - 1; i++) {
            String u = pathTraffic.get(i), v = pathTraffic.get(i + 1);
            Edge e = g.getEdge(u, v);
            String icon = e.trafficLevel.equals("high") ? "[HIGH]" :
                          e.trafficLevel.equals("medium") ? "[MED] " : "[LOW] ";
            System.out.printf("    %s %s -> %s  base=%-3d current=%d%n",
                icon, u, v, e.baseWeight, e.currentWeight);
        }
        System.out.println("=".repeat(55));
    }
}
