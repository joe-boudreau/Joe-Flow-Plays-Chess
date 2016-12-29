/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Sandbox;

/**
 *
 * @author jboudrea
 */
public class Sandbox {
    
    public static void main(String args[]){
        
        long[][] a = new long[2][];
        
        a[0] = new long[1];
        a[1] = new long[2];
        
        a[0][0] = 1L;
        a[1][0] = 2;
        
        
        System.out.println(a[1].length);
    }
}
