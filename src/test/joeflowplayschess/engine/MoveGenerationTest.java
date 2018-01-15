package joeflowplayschess.engine;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static joeflowplayschess.engine.Constants.WHITE;

public class MoveGenerationTest {

    private MoveGeneration moveGenerator;
    private Constants constants;
    private ZobristKeys zobristKeys;



    @Before
    public void setup(){
        constants = Constants.init(this.getClass().getClassLoader());
        moveGenerator = new MoveGeneration(constants);
        zobristKeys = new ZobristKeys();
    }

    @Test
    public void test(){
        GameState state = new GameState(zobristKeys);
        int[][] moves = moveGenerator.generateAllMovesWithMoveScorePlaceholders(state.getTurn(), state);
        int i = 0;
        for(int[] move : moves){
            move[1] = i++;
        }
        MoveSorter.sortMovesByMoveScore(moves);

        for(int[] move : moves){
            System.out.println(Arrays.toString(move));
        }
    }
}
