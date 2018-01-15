package joeflowplayschess.engine;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MoveSorter {

    public static int[][] sortMovesWithBestMove(int[][] moves, int bestMove) {
        List<int[]> sortedList = Arrays.stream(moves)
                                         .sorted(Comparator.comparingInt(a ->  (a[0] >>> 28) - ((a[0] << 4) >>> 28)))
                                         .collect(Collectors.toList());

        for(int[] move : sortedList){
            if(move[0] == bestMove){
                sortedList.remove(move);
                sortedList.add(0, move);
                break;
            }
        }

        return sortedList.toArray(new int[][]{});

    }

    public static void sortMovesByMoveHeuristics(int[][] moves) {
        Arrays.sort(moves, Comparator.comparingInt(a ->  (a[0] >>> 28) - ((a[0] << 4) >>> 28)));

    }

    public static void sortMovesByMoveScore(int[][] moves) {
        Arrays.sort(moves, Comparator.comparingInt(a -> -a[1]));
    }
}
