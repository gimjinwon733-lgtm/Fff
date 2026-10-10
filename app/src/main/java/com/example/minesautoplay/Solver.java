package com.example.minesautoplay;

import java.util.*;

/** Pure Java Minesweeper constraint solver, independent of Android APIs. */
public final class Solver {
    private Solver() {}
    static final class Rule {
        BitSet vars; int count;
        Rule(BitSet v,int c){vars=v;count=c;}
    }
    public static final class Result {
        public final int choice;
        public final double risk;
        public final boolean guessed;
        public final Set<Integer> mines;
        Result(int choice,double risk,boolean guessed,Set<Integer> mines){this.choice=choice;this.risk=risk;this.guessed=guessed;this.mines=mines;}
    }
    private static final class Counts {
        long solutions=0; long[] mine;
        Counts(int n){mine=new long[n];}
    }
    private static BitSet diff(BitSet a,BitSet b){BitSet z=(BitSet)a.clone();z.andNot(b);return z;}
    private static int[] dims(String difficulty){
        if("hard".equals(difficulty))return new int[]{20,24,99};
        if("medium".equals(difficulty))return new int[]{14,18,40};
        return new int[]{8,10,10};
    }
    public static int[] difficulty(String difficulty){return dims(difficulty);}
    public static Result solve(int[][] board,int totalMines,Set<Integer> persistentMines) {
        int rows=board.length,cols=board[0].length,size=rows*cols;
        BitSet closed=new BitSet(size), userFlags=new BitSet(size);
        for(int r=0;r<rows;r++)for(int c=0;c<cols;c++) {
            int id=r*cols+c;
            if(board[r][c]==-1) closed.set(id);
            if(board[r][c]==-2) userFlags.set(id);
        }
        BitSet mined=new BitSet(size);
        for(Integer id:persistentMines)if(id!=null && closed.get(id))mined.set(id);
        BitSet safe=new BitSet(size);
        ArrayList<Rule> source=new ArrayList<>();
        for(int r=0;r<rows;r++)for(int c=0;c<cols;c++) {
            int number=board[r][c];if(number<1)continue;
            BitSet unknown=new BitSet(size);int flagged=0;
            for(int y=Math.max(0,r-1);y<=Math.min(rows-1,r+1);y++)
                for(int x=Math.max(0,c-1);x<=Math.min(cols-1,c+1);x++){
                    if(x==c&&y==r)continue;
                    int id=y*cols+x;
                    if(closed.get(id))unknown.set(id);
                    else if(userFlags.get(id))flagged++;
                }
            int need=number-flagged;
            if(need<0 || need>unknown.cardinality())throw new IllegalArgumentException("숫자/깃발이 모순됩니다. 판 선택을 확인하세요.");
            if(!unknown.isEmpty())source.add(new Rule(unknown,need));
            else if(need!=0)throw new IllegalArgumentException("판 인식 오류: 주변 지뢰 수가 맞지 않습니다.");
        }
        for(int iter=0;iter<90;iter++) {
            ArrayList<Rule> rules=reduce(source,mined,safe);
            BitSet newMines=new BitSet(size),newSafe=new BitSet(size);
            for(Rule rule:rules){
                if(rule.count==0)newSafe.or(rule.vars);
                else if(rule.count==rule.vars.cardinality())newMines.or(rule.vars);
            }
            int remaining=totalMines-userFlags.cardinality()-mined.cardinality();
            BitSet unresolved=diff(diff(closed,mined),safe);
            if(remaining<0||remaining>unresolved.cardinality())throw new IllegalArgumentException("난이도/지뢰 개수와 화면이 맞지 않습니다.");
            if(remaining==0)newSafe.or(unresolved);
            else if(remaining==unresolved.cardinality())newMines.or(unresolved);
            for(int i=0;i<rules.size();i++)for(int j=0;j<rules.size();j++){
                if(i==j)continue;
                Rule a=rules.get(i),b=rules.get(j);
                if(a.vars.cardinality()>b.vars.cardinality())continue;
                BitSet subset=diff(a.vars,b.vars);if(!subset.isEmpty())continue;
                BitSet extra=diff(b.vars,a.vars);
                int diff=b.count-a.count;
                if(diff<0 || diff>extra.cardinality()) throw new IllegalArgumentException("숫자 인식 오류: 제약조건이 충돌합니다.");
                if(diff==0)newSafe.or(extra);
                else if(diff==extra.cardinality())newMines.or(extra);
            }
            newMines.andNot(mined);newSafe.andNot(safe);
            BitSet collision=(BitSet)newMines.clone();collision.and(newSafe);
            if(!collision.isEmpty())throw new IllegalArgumentException("서로 충돌하는 숫자를 인식했습니다.");
            if(newMines.isEmpty() && newSafe.isEmpty())break;
            mined.or(newMines);safe.or(newSafe);
            collision=(BitSet)mined.clone();collision.and(safe);
            if(!collision.isEmpty())throw new IllegalArgumentException("숫자 인식 오류: 같은 칸이 지뢰이면서 안전하다고 판정됐습니다.");
        }
        Set<Integer> detected=new HashSet<>();for(int p=mined.nextSetBit(0);p>=0;p=mined.nextSetBit(p+1))detected.add(p);
        if(!safe.isEmpty())return new Result(bestSpace(safe,board,rows,cols),0,false,detected);
        BitSet pending=diff(closed,mined);
        if(pending.isEmpty())return new Result(-1,0,false,detected);
        ArrayList<Rule> rules=reduce(source,mined,safe);
        int remaining=totalMines-userFlags.cardinality()-mined.cardinality();
        double baseRisk=Math.max(0,Math.min(1,remaining/(double)pending.cardinality()));
        double[] p=new double[size];Arrays.fill(p,baseRisk);
        BitSet frontier=new BitSet(size);
        for(Rule rule:rules)frontier.or(rule.vars);
        BitSet visited=new BitSet(size);
        for(int first=frontier.nextSetBit(0);first>=0;first=frontier.nextSetBit(first+1)){
            if(visited.get(first))continue;
            BitSet component=new BitSet(size);component.set(first);
            boolean change=true;
            while(change){
                change=false;
                for(Rule rule:rules){BitSet intersection=(BitSet)rule.vars.clone();intersection.and(component);
                    if(!intersection.isEmpty()){
                        BitSet added=diff(rule.vars,component);
                        if(!added.isEmpty()){component.or(added);change=true;}
                    }
                }
            }
            visited.or(component);
            if(component.cardinality()<=18) {
                int[] ids=toArray(component);
                List<Rule> partRules=new ArrayList<>();
                for(Rule r:rules){BitSet test=(BitSet)r.vars.clone();test.and(component);if(!test.isEmpty())partRules.add(r);}
                Counts counts=enumerate(ids,partRules);
                if(counts.solutions==0)throw new IllegalArgumentException("숫자 인식 오류: 가능한 지뢰 배치가 없습니다.");
                for(int i=0;i<ids.length;i++)p[ids[i]]=counts.mine[i]/(double)counts.solutions;
            } else {
                // Large components: conservative local risk heuristic.
                for(int id=component.nextSetBit(0);id>=0;id=component.nextSetBit(id+1)) {
                    double local=1;
                    for(Rule rule:rules)if(rule.vars.get(id))local=Math.min(local,rule.count/(double)rule.vars.cardinality());
                    p[id]=local;
                }
            }
        }
        double min=2;
        for(int id=pending.nextSetBit(0);id>=0;id=pending.nextSetBit(id+1)) min=Math.min(min,p[id]);
        BitSet bests=new BitSet(size);
        for(int id=pending.nextSetBit(0);id>=0;id=pending.nextSetBit(id+1))if(p[id]<=min+1e-9)bests.set(id);
        int chosen=bestSpace(bests,board,rows,cols);
        if(chosen<0)return new Result(-1,0,false,detected);
        return new Result(chosen,p[chosen],p[chosen]>1e-9,detected);
    }
    private static ArrayList<Rule> reduce(List<Rule> rules,BitSet mined,BitSet safe){
        ArrayList<Rule> out=new ArrayList<>();
        for(Rule old:rules){
            BitSet vars=diff(diff(old.vars,mined),safe);
            BitSet bombs=(BitSet)old.vars.clone();bombs.and(mined);
            int need=old.count-bombs.cardinality();
            if(need<0 || need>vars.cardinality())throw new IllegalArgumentException("인식한 숫자들 사이에 모순이 있습니다.");
            if(!vars.isEmpty())out.add(new Rule(vars,need));
            else if(need!=0)throw new IllegalArgumentException("인식한 숫자들 사이에 모순이 있습니다.");
        }
        return out;
    }
    private static int[] toArray(BitSet bits){int[] arr=new int[bits.cardinality()];int i=0;for(int p=bits.nextSetBit(0);p>=0;p=bits.nextSetBit(p+1))arr[i++]=p;return arr;}
    private static Counts enumerate(int[] ids,List<Rule> rules){
        int n=ids.length,m=rules.size();Counts result=new Counts(n);
        int[][] links=new int[n][];int[] required=new int[m],left=new int[m],count=new int[m];
        for(int j=0;j<m;j++){required[j]=rules.get(j).count;left[j]=rules.get(j).vars.cardinality();}
        for(int i=0;i<n;i++){
            ArrayList<Integer> indexes=new ArrayList<>();
            for(int j=0;j<m;j++)if(rules.get(j).vars.get(ids[i]))indexes.add(j);
            links[i]=new int[indexes.size()];for(int k=0;k<indexes.size();k++)links[i][k]=indexes.get(k);
        }
        boolean[] selected=new boolean[n];dfs(0,n,links,required,left,count,selected,result);
        return result;
    }
    private static void dfs(int step,int n,int[][] links,int[] required,int[] left,int[] count,boolean[] selected,Counts result){
        if(step==n){result.solutions++;for(int i=0;i<n;i++)if(selected[i])result.mine[i]++;return;}
        for(int v=0;v<=1;v++){
            boolean good=true;
            for(int j:links[step]){left[j]--;count[j]+=v;if(count[j]>required[j]||count[j]+left[j]<required[j])good=false;}
            if(good){selected[step]=(v==1);dfs(step+1,n,links,required,left,count,selected,result);}
            for(int j:links[step]){left[j]++;count[j]-=v;}
        }
    }
    private static int bestSpace(BitSet available,int[][] board,int rows,int cols){
        int best=-1,score=-1;
        for(int id=available.nextSetBit(0);id>=0;id=available.nextSetBit(id+1)){
            int y=id/cols,x=id%cols,s=0;
            for(int yy=Math.max(0,y-1);yy<=Math.min(rows-1,y+1);yy++)
                for(int xx=Math.max(0,x-1);xx<=Math.min(cols-1,x+1);xx++)if(board[yy][xx]==-1)s++;
            // Prefer opening a large blank region if all information is equal.
            if(s>score){score=s;best=id;}
        }
        return best;
    }
}
