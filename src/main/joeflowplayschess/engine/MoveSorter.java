package joeflowplayschess.engine;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MoveSorter {

    public static int[] sortMovesWithBestMove(int[] moves, int bestMove) {
        List<Integer> sortedList = Arrays.stream(moves)
                                         .boxed()
                                         .sorted(Comparator.comparingInt(a ->  (a >>> 28) - ((a << 4) >>> 28)))
                                         .collect(Collectors.toList());

        sortedList.remove((Integer)bestMove);
        sortedList.add(0, bestMove);

        return sortedList.stream().mapToInt(i->i).toArray();

    }

    public static int[] sortMoves(int[] movesToSearch) {
        return Arrays.stream(movesToSearch)
                     .boxed()
                     .sorted(Comparator.comparingInt(a ->  (a >>> 28) - ((a << 4) >>> 28)))
                     .mapToInt(i -> i)
                     .toArray();
    }
}
