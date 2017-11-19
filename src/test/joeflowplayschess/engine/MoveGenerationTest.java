package joeflowplayschess.engine;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static joeflowplayschess.engine.Constants.WHITE;

public class MoveGenerationTest {

    private MoveGeneration moveGenerator;
    private Constants constants;



    @Before
    public void setup(){
        constants = Constants.init(this.getClass().getClassLoader());
        moveGenerator = new MoveGeneration(constants);
    }

    @Test
    public void generatePawnTargets_startPosition_return16LegalMoves(){
        GameState gameState = new GameState();
        List<Integer> moves = new ArrayList<>();
        moveGenerator.generatePawnTargets(moves, WHITE, gameState);
    }
}
