"""
Traffic Intelligence Platform — Core Algorithm (Python)
========================================================
Data Structures: Graph (Adjacency List), Min-Heap Priority Queue,
                 Dictionary (distances/previous), Set (visited)
Algorithm     : Dijkstra's Shortest Path
"""

import heapq   # Python's built-in min-heap (Priority Queue)


# ─────────────────────────────────────────────────
#  Graph (Adjacency List)
# ─────────────────────────────────────────────────
class TrafficGraph:
    def __init__(self):
        self.graph = {}        # adjacency list: node → [(neighbor, weight)]
        self.traffic = {}      # edge key → current weight (with traffic)
        self.base_weight = {}  # edge key → original weight

    def add_edge(self, u, v, weight):
        """Add undirected edge between u and v."""
        if u not in self.graph: self.graph[u] = []
        if v not in self.graph: self.graph[v] = []
        self.graph[u].append(v)
        self.graph[v].append(u)
        key = tuple(sorted([u, v]))
        self.base_weight[key] = weight
        self.traffic[key]     = weight   # start with no traffic

    def set_traffic(self, u, v, level):
        """Apply traffic multiplier to an edge."""
        multiplier = {'low': 1.0, 'medium': 1.6, 'high': 2.5}
        key = tuple(sorted([u, v]))
        self.traffic[key] = round(self.base_weight[key] * multiplier[level])

    def get_weight(self, u, v, use_traffic=True):
        key = tuple(sorted([u, v]))
        return self.traffic[key] if use_traffic else self.base_weight[key]

    # ─── Dijkstra's Algorithm ───────────────────────
    def dijkstra(self, source, use_traffic=True):
        """
        Min-heap based Dijkstra.
        Returns (distances dict, previous dict)
        """
        distances = {node: float('inf') for node in self.graph}
        previous  = {node: None         for node in self.graph}
        visited   = set()

        distances[source] = 0
        heap = [(0, source)]   # (cost, node) — min-heap

        while heap:
            cost, node = heapq.heappop(heap)  # O(log n)

            if node in visited:
                continue
            visited.add(node)

            for neighbor in self.graph[node]:
                if neighbor in visited:
                    continue
                weight   = self.get_weight(node, neighbor, use_traffic)
                new_cost = cost + weight

                if new_cost < distances[neighbor]:
                    distances[neighbor] = new_cost
                    previous[neighbor]  = node
                    heapq.heappush(heap, (new_cost, neighbor))  # O(log n)

        return distances, previous

    def get_path(self, previous, source, destination):
        """Reconstruct path by tracing back through previous[]."""
        path = []
        node = destination
        while node is not None:
            path.append(node)
            node = previous[node]
        path.reverse()
        return path if path[0] == source else []  # no path found


# ─────────────────────────────────────────────────
#  Build City Graph (same as the JS version)
# ─────────────────────────────────────────────────
def build_city_graph():
    g = TrafficGraph()
    edges = [
        ('A','N',12), ('A','I',18),
        ('B','N',10), ('B','M', 8), ('B','C',15),
        ('C','L',11), ('C','D', 9),
        ('D','E',14), ('D','L', 8),
        ('E','F',12), ('E','L',16),
        ('F','G', 9), ('F','K',10), ('F','L',14),
        ('G','H',11), ('G','K',13),
        ('H','I',15), ('H','J',10),
        ('I','J', 9), ('I','N',16),
        ('J','K',12), ('J','M',14), ('J','N',11),
        ('K','L', 7), ('K','M', 9),
        ('M','N', 6),
    ]
    node_names = {
        'A':'Airport',   'B':'Bank Sq',   'C':'City Hall',
        'D':'Downtown',  'E':'East End',  'F':'Fairview',
        'G':'Garden',    'H':'Harbor',    'I':'Industrial',
        'J':'Junction',  'K':'Kings Rd',  'L':'Lake Bridge',
        'M':'Metro Park','N':'North Gate',
    }
    for u, v, w in edges:
        g.add_edge(u, v, w)
    return g, node_names


# ─────────────────────────────────────────────────
#  Run Demo
# ─────────────────────────────────────────────────
def run(source, destination, traffic_level='low'):
    g, names = build_city_graph()

    print(f"\n{'='*55}")
    print(f"  🚦 TRAFFIC INTELLIGENCE PLATFORM (Python)")
    print(f"{'='*55}")
    print(f"  Source      : {source} — {names[source]}")
    print(f"  Destination : {destination} — {names[destination]}")
    print(f"  Traffic     : {traffic_level.upper()}")
    print(f"{'='*55}\n")

    # ── BEFORE TRAFFIC ──────────────────────────
    dist_base, prev_base = g.dijkstra(source, use_traffic=False)
    path_base = g.get_path(prev_base, source, destination)
    cost_base = dist_base[destination]

    print(f"  📍 BEFORE TRAFFIC (base weights)")
    print(f"     Path : {' → '.join(path_base)}")
    print(f"     Cost : {cost_base}\n")

    # ── APPLY TRAFFIC ────────────────────────────
    import random
    probs = {
        'low':    ['low']*7 + ['medium']*2 + ['high']*1,
        'medium': ['low']*2 + ['medium']*5 + ['high']*3,
        'high':   ['low']*1 + ['medium']*2 + ['high']*7,
    }
    seen = set()
    for u in g.graph:
        for v in g.graph[u]:
            key = tuple(sorted([u, v]))
            if key not in seen:
                seen.add(key)
                g.set_traffic(u, v, random.choice(probs[traffic_level]))

    # ── AFTER TRAFFIC ────────────────────────────
    dist_traffic, prev_traffic = g.dijkstra(source, use_traffic=True)
    path_traffic = g.get_path(prev_traffic, source, destination)
    cost_traffic = dist_traffic[destination]

    print(f"  🚨 AFTER TRAFFIC ({traffic_level.upper()} — biased random per edge)")
    print(f"     Path : {' → '.join(path_traffic)}")
    print(f"     Cost : {cost_traffic}\n")

    # ── COMPARISON ───────────────────────────────
    diff = cost_traffic - cost_base
    pct  = round((abs(diff) / cost_base) * 100) if cost_base else 0
    same = path_base == path_traffic

    print(f"  📊 COMPARISON")
    print(f"     Same path? : {'Yes ✅' if same else 'No — rerouted ♻️'}")
    if diff > 0:
        print(f"     Extra cost : +{diff} units (+{pct}%) — traffic hurts")
    elif diff < 0:
        print(f"     Saved      : {abs(diff)} units ({pct}%) — smart reroute!")
    else:
        print(f"     No impact  : Traffic didn't change the cost")

    # ── ROUTE DETAIL ─────────────────────────────
    print(f"\n  🛤️  OPTIMAL ROUTE DETAIL")
    for i in range(len(path_traffic) - 1):
        u, v = path_traffic[i], path_traffic[i+1]
        key  = tuple(sorted([u, v]))
        base = g.base_weight[key]
        curr = g.traffic[key]
        lvl  = 'HIGH' if curr >= base*2 else ('MED' if curr > base else 'LOW')
        icon = '🔴' if lvl=='HIGH' else ('🟡' if lvl=='MED' else '🟢')
        print(f"     {icon} {u}({names[u][:6]}) → {v}({names[v][:6]})  base={base}  current={curr}  [{lvl}]")

    print(f"\n{'='*55}\n")


if __name__ == '__main__':
    # Try different routes and traffic levels
    run('A', 'E', traffic_level='high')
    run('A', 'B', traffic_level='medium')
    run('I', 'D', traffic_level='low')
