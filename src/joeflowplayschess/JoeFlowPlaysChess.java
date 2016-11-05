/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package joeflowplayschess;

import java.awt.Container;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;


/**
 *
 * @author jboudrea
 */
public class JoeFlowPlaysChess extends JFrame {
    private Container pane;
    
    public JoeFlowPlaysChess(){
        initUI();
    }
    
    public void initUI(){
        
        pane = getContentPane();
        pane.add(setUpChessBoard());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000,1000);
        pane.validate();
    }    
    
    public JPanel setUpChessBoard(){
        JPanel chessPanel = new JPanel();
        
        return chessPanel;
    }
    
    public static void main(String[] args) {        
        
        EventQueue.invokeLater(new Runnable() {
           
            @Override
            public void run() {
                JoeFlowPlaysChess jfpc = new JoeFlowPlaysChess();
                jfpc.setVisible(true);
            }
        });
    }
    
}
