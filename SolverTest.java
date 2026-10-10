import com.example.minesautoplay.Solver;
import java.util.*;
public class SolverTest {
    public static void main(String[] args){
        int[][] board = {{1,-1},{1,-1}};
        Solver.Result r = Solver.solve(board, 1, Collections.emptySet());
        if(r.choice < 0 || r.risk < 0 || r.risk > 1)throw new AssertionError("probability invalid");
        int[][] safeCase={{1,-1,-1},{1,1,1}};
        Solver.Result x=Solver.solve(safeCase,1,Collections.emptySet());
        if(x.choice!=2 || x.risk!=0)throw new AssertionError("expected safe top right cell; got "+x.choice+" risk="+x.risk);
        int[][] allClosed={{-1,-1},{-1,-1}};
        Solver.Result y=Solver.solve(allClosed,1,Collections.emptySet());
        if(y.choice<0) throw new AssertionError("first move unavailable");
        System.out.println("Solver tests OK (probability / deduction / initial move)");
    }
}
