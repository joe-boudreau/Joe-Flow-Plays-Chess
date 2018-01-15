package joeflowplayschess.engine;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static joeflowplayschess.engine.Constants.*;
import static org.junit.Assert.assertEquals;

public class GameStateTest{

    private byte initialGameFlags;
    private int[] initialGameBoard;
    private GameState initialGameState;
    private ZobristKeys zobristKeys = new ZobristKeys();

    @Before
    public void setup() throws IOException {
        initialGameFlags = (byte) (WHITE_KINGSIDE_CASTLE | WHITE_QUEENSIDE_CASTLE |
                                        BLACK_KINGSIDE_CASTLE | BLACK_QUEENSIDE_CASTLE);
        String boardStr = EngineTestUtils.loadBoardString("start.board");
        initialGameBoard = EngineTestUtils.toGameBoard(boardStr);
        initialGameState = new GameState(initialGameFlags, initialGameBoard, WHITE, 0, 0, zobristKeys);
    }

    @Test
    public void GameState_Initial_ReturnStartBoard() {

        GameState gameState = new GameState(zobristKeys);

        assertEquals(initialGameState, gameState);

        System.out.println(initialGameState);
        System.out.println(gameState);
    }

    @Test
    public void GameState_Initial_ReturnFriendlyPiecesWhiteEnemyPiecesBlack() throws IOException {

        long whiteFriendlyPieces = initialGameState.getFriendlyPieces(Constants.WHITE);
        long blackEnemyPieces = initialGameState.getEnemyPieces(Constants.BLACK);
        long expectedResult = EngineTestUtils.bitBoardToLong(
                0b00000000L,
                0b00000000L,
                0b00000000L,
                0b00000000L,
                0b00000000L,
                0b00000000L,
                0b11111111L,
                0b11111111L);

        assertEquals(expectedResult, whiteFriendlyPieces);
        assertEquals(expectedResult, blackEnemyPieces);
    }

    @Test
    public void GameState_Initial_ReturnEmptySquares() throws IOException {

        long emptySquares = initialGameState.getEmptySquares();
        long expectedResult = EngineTestUtils.bitBoardToLong(
                0b00000000L,
                0b00000000L,
                0b11111111L,
                0b11111111L,
                0b11111111L,
                0b11111111L,
                0b00000000L,
                0b00000000L);

        assertEquals(expectedResult, emptySquares);
    }

    @Test
    public void GameState_Initial_ReturnAllPieces() throws IOException {
        long emptySquares = initialGameState.getAllPieces();
        long expectedResult = EngineTestUtils.bitBoardToLong(
                0b11111111L,
                0b11111111L,
                0b00000000L,
                0b00000000L,
                0b00000000L,
                0b00000000L,
                0b11111111L,
                0b11111111L);



        assertEquals(expectedResult, emptySquares);
    }








}