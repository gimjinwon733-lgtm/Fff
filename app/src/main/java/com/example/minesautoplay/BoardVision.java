package com.example.minesautoplay;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import java.util.*;

/** Google Search Minesweeper's standard green/beige tile color palette. */
public final class BoardVision {
    public static final int CLOSED=-1, FLAG=-2;
    private static final int[][] GREEN={{170,215,81},{162,209,73}};
    private static final int[][] BEIGE={{229,194,159},{215,184,153}};
    private static final int[][] DIGITS={{25,118,210},{56,142,60},{211,47,47},{123,31,162},{240,149,54},{66,149,165},{66,66,66},{158,158,158}};
    private static final int[] FLAG_RGB={215,75,40};
    private BoardVision() {}
    static int dist(int color,int[] target) {
        int dr=Color.red(color)-target[0],dg=Color.green(color)-target[1],db=Color.blue(color)-target[2];
        return dr*dr+dg*dg+db*db;
    }
    private static boolean near(int color,int[][] colors,int tol) {
        for(int[] c: colors)if(dist(color,c)<=tol*tol)return true;
        return false;
    }
    static int[][] read(Bitmap bitmap,Rect zone,int rows,int cols) throws IllegalArgumentException {
        if (zone.width()<cols*14 || zone.height()<rows*14) throw new IllegalArgumentException("선택한 게임판이 너무 작아요. 확대 후 다시 선택하세요.");
        if(zone.left<0||zone.top<0||zone.right>bitmap.getWidth()||zone.bottom>bitmap.getHeight()) throw new IllegalArgumentException("선택한 판이 화면 밖에 있어요.");
        int[][] board=new int[rows][cols]; int missed=0;
        for(int r=0;r<rows;r++) for(int c=0;c<cols;c++) {
            int l=zone.left+Math.round(c*zone.width()/(float)cols), t=zone.top+Math.round(r*zone.height()/(float)rows);
            int rr=zone.left+Math.round((c+1)*zone.width()/(float)cols), bb=zone.top+Math.round((r+1)*zone.height()/(float)rows);
            int w=rr-l,h=bb-t;
            int green=0,beige=0;
            float[] positions={.19f,.81f};
            for(float yy: positions) for(float xx:positions) for(int oy=-2;oy<=2;oy+=2) for(int ox=-2;ox<=2;ox+=2) {
                int x=limit(l+Math.round(w*xx)+ox,l,rr-1), y=limit(t+Math.round(h*yy)+oy,t,bb-1);
                int col=bitmap.getPixel(x,y);
                if(near(col,GREEN,40))green++;
                if(near(col,BEIGE,40))beige++;
            }
            if(Math.max(green,beige)<11){ board[r][c]=CLOSED;missed++;continue; }
            int[] digitVotes=new int[8]; int red=0,total=0;
            for(int y=t+Math.round(h*.22f);y<t+Math.round(h*.78f);y+=2)
                for(int x=l+Math.round(w*.22f);x<l+Math.round(w*.78f);x+=2){
                    int col=bitmap.getPixel(x,y);total++;
                    if(dist(col,FLAG_RGB)<48*48) red++;
                    if(beige>green) {
                        int best=-1,dmin=42*42;
                        for(int k=0;k<8;k++){int d=dist(col,DIGITS[k]);if(d<dmin){dmin=d;best=k;}}
                        if(best>=0)digitVotes[best]++;
                    }
                }
            if(green>=beige) {
                board[r][c]=(red>=Math.max(3,total/70))?FLAG:CLOSED;
            } else {
                int best=-1,n=0;
                for(int k=0;k<8;k++)if(digitVotes[k]>n){n=digitVotes[k];best=k;}
                board[r][c]=(n>=Math.max(2,total/150))?best+1:0;
            }
        }
        if(missed>0)throw new IllegalArgumentException(missed+"개 칸의 색을 인식하지 못했어요. 전체 격자를 정확히 선택하거나 화면 배율을 조정하세요.");
        return board;
    }
    private static int limit(int x,int min,int max){return Math.max(min,Math.min(max,x));}
}
