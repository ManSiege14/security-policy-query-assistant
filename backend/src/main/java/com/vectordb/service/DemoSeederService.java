package com.vectordb.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DemoSeederService {

    private final VectorStoreService demoStore;

    public DemoSeederService(@Qualifier("demoVectorStore") VectorStoreService demoStore) {
        this.demoStore = demoStore;
    }

    @PostConstruct
    public void seed() {
        // Dims 0-3: CS | Dims 4-7: Math | Dims 8-11: Food | Dims 12-15: Sports

        insert("Linked List: nodes connected by pointers", "cs",
                List.of(0.90,0.85,0.72,0.68, 0.12,0.08,0.15,0.10, 0.05,0.08,0.06,0.09, 0.07,0.11,0.08,0.06));

        insert("Binary Search Tree: O(log n) search and insert", "cs",
                List.of(0.88,0.82,0.78,0.74, 0.15,0.10,0.08,0.12, 0.06,0.07,0.08,0.05, 0.09,0.06,0.07,0.10));

        insert("Dynamic Programming: memoization overlapping subproblems", "cs",
                List.of(0.82,0.76,0.88,0.80, 0.20,0.18,0.12,0.09, 0.07,0.06,0.08,0.07, 0.08,0.09,0.06,0.07));

        insert("Graph BFS and DFS: breadth and depth first traversal", "cs",
                List.of(0.85,0.80,0.75,0.82, 0.18,0.14,0.10,0.08, 0.06,0.09,0.07,0.06, 0.10,0.08,0.09,0.07));

        insert("Hash Table: O(1) lookup with collision chaining", "cs",
                List.of(0.87,0.78,0.70,0.76, 0.13,0.11,0.09,0.14, 0.08,0.07,0.06,0.08, 0.07,0.10,0.08,0.09));

        insert("Calculus: derivatives integrals and limits", "math",
                List.of(0.12,0.15,0.18,0.10, 0.91,0.86,0.78,0.72, 0.08,0.06,0.07,0.09, 0.07,0.08,0.06,0.10));

        insert("Linear Algebra: matrices eigenvalues eigenvectors", "math",
                List.of(0.20,0.18,0.15,0.12, 0.88,0.90,0.82,0.76, 0.09,0.07,0.08,0.06, 0.10,0.07,0.08,0.09));

        insert("Probability: distributions random variables Bayes theorem", "math",
                List.of(0.15,0.12,0.20,0.18, 0.84,0.80,0.88,0.82, 0.07,0.08,0.06,0.10, 0.09,0.06,0.09,0.08));

        insert("Number Theory: primes modular arithmetic RSA cryptography", "math",
                List.of(0.22,0.16,0.14,0.20, 0.80,0.85,0.76,0.90, 0.08,0.09,0.07,0.06, 0.08,0.10,0.07,0.06));

        insert("Combinatorics: permutations combinations generating functions", "math",
                List.of(0.18,0.20,0.16,0.14, 0.86,0.78,0.84,0.80, 0.06,0.07,0.09,0.08, 0.06,0.09,0.10,0.07));

        insert("Neapolitan Pizza: wood-fired dough San Marzano tomatoes", "food",
                List.of(0.08,0.06,0.09,0.07, 0.07,0.08,0.06,0.09, 0.90,0.86,0.78,0.72, 0.08,0.06,0.09,0.07));

        insert("Sushi: vinegared rice raw fish and nori rolls", "food",
                List.of(0.06,0.08,0.07,0.09, 0.09,0.06,0.08,0.07, 0.86,0.90,0.82,0.76, 0.07,0.09,0.06,0.08));

        insert("Ramen: noodle soup with chashu pork and soft-boiled eggs", "food",
                List.of(0.09,0.07,0.06,0.08, 0.08,0.09,0.07,0.06, 0.82,0.78,0.90,0.84, 0.09,0.07,0.08,0.06));

        insert("Tacos: corn tortillas with carnitas salsa and cilantro", "food",
                List.of(0.07,0.09,0.08,0.06, 0.06,0.07,0.09,0.08, 0.78,0.82,0.86,0.90, 0.06,0.08,0.07,0.09));

        insert("Croissant: laminated pastry with buttery flaky layers", "food",
                List.of(0.06,0.07,0.10,0.09, 0.10,0.06,0.07,0.10, 0.85,0.80,0.76,0.82, 0.09,0.07,0.10,0.06));

        insert("Basketball: fast-paced shooting dribbling slam dunks", "sports",
                List.of(0.09,0.07,0.08,0.10, 0.08,0.09,0.07,0.06, 0.08,0.07,0.09,0.06, 0.91,0.85,0.78,0.72));

        insert("Football: tackles touchdowns field goals and strategy", "sports",
                List.of(0.07,0.09,0.06,0.08, 0.09,0.07,0.10,0.08, 0.07,0.09,0.08,0.07, 0.87,0.89,0.82,0.76));

        insert("Tennis: racket volleys groundstrokes and Wimbledon serves", "sports",
                List.of(0.08,0.06,0.09,0.07, 0.07,0.08,0.06,0.09, 0.09,0.06,0.07,0.08, 0.83,0.80,0.88,0.82));

        insert("Chess: openings endgames tactics strategic board game", "sports",
                List.of(0.25,0.20,0.22,0.18, 0.22,0.18,0.20,0.15, 0.06,0.08,0.07,0.09, 0.80,0.84,0.78,0.90));

        insert("Swimming: butterfly freestyle backstroke Olympic competition", "sports",
                List.of(0.06,0.08,0.07,0.09, 0.08,0.06,0.09,0.07, 0.10,0.08,0.06,0.07, 0.85,0.82,0.86,0.80));
            }

    private void insert(String metadata, String category, List<Double> embedding) {
        demoStore.insert(metadata, category, embedding);
    }
}