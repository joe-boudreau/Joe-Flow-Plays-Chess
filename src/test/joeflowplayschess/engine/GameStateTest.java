package joeflowplayschess.engine;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static joeflowplayschess.engine.Constants.*;
import static org.junit.Assert.assertEquals;

public class GameStateTest{

    private byte initialGameFlags;
    private int[] initialGameBoard;

    @Before
    public void setup() throws IOException {
        initialGameFlags = (byte) (WHITE_KINGSIDE_CASTLE | WHITE_QUEENSIDE_CASTLE |
                                        BLACK_KINGSIDE_CASTLE | BLACK_QUEENSIDE_CASTLE);
        String boardStr = EngineTestUtils.loadBoardString("start.board");
        initialGameBoard = EngineTestUtils.toGameBoard(boardStr);
    }

    @Test
    public void GameState_Initial_ReturnStartBoard() {
        GameState expectedGameState = new GameState(initialGameFlags, initialGameBoard);
        GameState gameState = new GameState();

        assertEquals(expectedGameState, gameState);

        System.out.println(expectedGameState);
        System.out.println(gameState);
    }

    @Test
    public void GameState_Initial_ReturnFriendlyPiecesWhite() throws IOException {

        GameState gameState = new GameState(initialGameFlags, initialGameBoard);
        long friendlyPieces = gameState.getFriendlyPieces(Constants.WHITE);
        long expectedFriendlyPieces = EngineTestUtils.bitBoardToLong(0b00000000L,
                                                                     0b00000000L,
                                                                     0b00000000L,
                                                                     0b00000000L,
                                                                     0b00000000L,
                                                                     0b00000000L,
                                                                     0b11111111L,
                                                                     0b11111111L);

        assertEquals(expectedFriendlyPieces, friendlyPieces);
    }








}