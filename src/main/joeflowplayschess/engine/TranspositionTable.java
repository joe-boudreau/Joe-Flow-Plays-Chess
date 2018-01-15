package joeflowplayschess.engine;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class TranspositionTable {

    static Logger logger = Logger.getLogger(TranspositionTable.class);

    private final int TABLE_SIZE;

    private final int INDEX_MOD;

    public enum NodeType {EXACT, ALPHA, BETA};

    public final HashMap<Integer, PositionEntry> tt;

    public long entriesFound = 0;
    
    public long exactEntriesUsed = 0;
    public long alphaEntriesUsed = 0;
    public long betaEntriesUsed = 0;
    public long bestMovesUsed = 0;

    public long entriesAdded = 0;
    public long collisions = 0;

    public TranspositionTable(int size){
        TABLE_SIZE = size;
        INDEX_MOD = size / 2; //Use half of table size for the index modulo to account for negative integers after shifting the 64-bit zobrist key >> 32

        tt = new HashMap<>(TABLE_SIZE, 1f);
    }

    public void put(GameState state, int depth, int score, int bestMove, NodeType type){

        long zobrist = state.getZobristKey();
        int index = (int) (zobrist >> 32) % INDEX_MOD;

        if(tt.containsKey(index)){
            PositionEntry existingPosition = tt.get(index);
            if(!existingPosition.ancient && depth < existingPosition.depth){
                return; //dont replace
            }
            collisions++;
        }
        entriesAdded++;
        tt.put(index, new PositionEntry(zobrist, depth, score, bestMove, type, false));

    }

    public PositionEntry getEntryIfExists(GameState state){

        long zobrist = state.getZobristKey();
        int index = (int) (zobrist >> 32) % INDEX_MOD;

        PositionEntry entry = tt.get(index);
        if (entry != null && entry.zobrist == zobrist){
            entriesFound++;
            return tt.get(index);
        }
        else{
            return null;
        }

    }

    public Integer getValueFromEntry(PositionEntry entry, GameState state, int depth, int alpha, int beta){

        long zobrist = state.getZobristKey();
        
        if(entry.depth >= depth) {
            
            switch (entry.type){

                case EXACT:
                    exactEntriesUsed++;
                    return entry.evalScore;
                case BETA:
                    if (entry.evalScore >= beta) {
                        betaEntriesUsed++;
                        return beta;
                    }
                    break;
                case ALPHA:
                    if (entry.evalScore <= alpha) {
                        alphaEntriesUsed++;
                        return alpha;
                    }
                    break;
            }
        }
        bestMovesUsed++;
        return null;
    }


    public void setAllAncient(){
        tt.entrySet().parallelStream().forEach(p -> p.getValue().setToAncient());
    }

    public void printInfo(){

        logger.info("----Transposition Table Information----");
        logger.info("Table size:" + tt.size());
        logger.info("Transposition Table number of entries:" + tt.entrySet().stream().filter(e -> e.getValue() != null).count());

        logger.info("Entries found: " + entriesFound);
        logger.info("Entries used: " + entriesFound + " , Percentage: " + getPercentage(entriesFound, entriesFound) + "%");

        logger.info("Percentage exact: " + getPercentage(exactEntriesUsed, entriesFound));
        logger.info("Percentage alpha: " + getPercentage(alphaEntriesUsed, entriesFound));
        logger.info("Percentage beta: " + getPercentage(betaEntriesUsed, entriesFound));
        logger.info("Percentage best moves: " + getPercentage(bestMovesUsed, entriesFound));

        logger.info("Collision percentage: " + getPercentage(collisions, entriesAdded));
    }

    private float getPercentage(long a, long b) {
        return ((float)a*100)/b;
    }

    public static class PositionEntry{

        public long zobrist;
        public int depth;
        public int evalScore;
        public int bestMove;
        public NodeType type;
        public boolean ancient;

        public PositionEntry(long zobrist, int depth, int score, int bestMove, NodeType type, boolean ancient){
            this.zobrist = zobrist;
            this.depth = depth;
            this.evalScore = score;
            this.bestMove = bestMove;
            this.type = type;
            this.ancient = ancient;
        }

        public void setToAncient(){
            ancient = true;
        }
    }
}
