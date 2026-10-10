package com.example.minesautoplay;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;

/** Only interacts with the user's current Google Minesweeper board. No network calls. */
public final class AutoPlayService extends AccessibilityService {
    static AutoPlayService INSTANCE;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private LinearLayout toolbar;
    private View selectionView;
    private TextView status;
    private Rect boardRect;
    private boolean running=false, busy=false;
    private final Set<Integer> knownMines=new HashSet<>();
    private int attempts=0,unchanged=0;
    private String previousBoard="";
    private final Runnable frameTask=this::capture;

    @Override public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {}
    @Override public void onInterrupt() {stop("서비스가 중단됐습니다.");}
    @Override protected void onServiceConnected(){
        super.onServiceConnected();
        INSTANCE=this;windowManager=(WindowManager)getSystemService(Context.WINDOW_SERVICE);
        handler.post(this::showToolbar);
    }
    @Override public void onDestroy(){
        stop("서비스 종료");
        if(selectionView!=null){try{windowManager.removeView(selectionView);}catch(Exception ignored){} selectionView=null;}
        if(toolbar!=null){try{windowManager.removeView(toolbar);}catch(Exception ignored){} toolbar=null;}
        INSTANCE=null;super.onDestroy();
    }
    private int dp(int px){return (int)(px*getResources().getDisplayMetrics().density+.5f);}
    private WindowManager.LayoutParams layout(int width,int height){
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.LEFT|Gravity.TOP;return p;
    }
    private void showToolbar(){
        if(toolbar!=null)return;
        toolbar=new LinearLayout(this);toolbar.setOrientation(LinearLayout.VERTICAL);
        toolbar.setPadding(dp(7),dp(5),dp(7),dp(5));toolbar.setBackgroundColor(0xE526313B);
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button select=button("판 선택"); Button play=button("시작");Button pause=button("중지");
        select.setOnClickListener(v->beginSelection());
        play.setOnClickListener(v->start());
        pause.setOnClickListener(v->stop("중지됨"));
        actions.addView(select);actions.addView(play);actions.addView(pause);
        toolbar.addView(actions);
        status=new TextView(this);status.setText("① 판 선택 → ② 시작");status.setTextColor(Color.WHITE);
        status.setTextSize(11);status.setMaxLines(2);status.setPadding(dp(5),dp(2),dp(5),0);
        toolbar.addView(status);
        WindowManager.LayoutParams lp=layout(-2,-2);lp.x=dp(10);lp.y=dp(75);
        windowManager.addView(toolbar,lp);
    }
    private Button button(String title){
        Button button=new Button(this);button.setText(title);button.setAllCaps(false);button.setTextSize(12);
        button.setMinWidth(0);button.setMinimumWidth(0);button.setMinHeight(0);button.setMinimumHeight(0);
        button.setPadding(dp(5),0,dp(5),0);
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(76),dp(42)));
        return button;
    }
    private void setStatus(String text){if(status!=null)status.setText(text);}
    private void stop(String reason){
        running=false;busy=false;handler.removeCallbacks(frameTask);setStatus(reason);
    }
    private void beginSelection(){
        stop("판 영역을 선택하세요");
        if(selectionView!=null)return;
        selectionView=new SelectionOverlay(this,(selected)->{
            if(selectionView!=null){try{windowManager.removeView(selectionView);}catch(Exception ignored){}selectionView=null;}
            if(selected.width()<80||selected.height()<80){setStatus("너무 작은 영역입니다. 다시 선택하세요.");return;}
            boardRect=selected;knownMines.clear();previousBoard="";unchanged=0;attempts=0;
            setStatus("선택 완료. ‘시작’을 누르세요.");
        });
        WindowManager.LayoutParams p=layout(-1,-1);p.x=0;p.y=0;
        windowManager.addView(selectionView,p);
    }
    private void start(){
        if(boardRect==null){setStatus("‘판 선택’부터 눌러 주세요.");return;}
        if(selectionView!=null){setStatus("먼저 판 영역을 선택하세요.");return;}
        if(running)return;
        running=true;busy=false;attempts=0;unchanged=0;previousBoard="";
        setStatus("화면 분석 중...");handler.postDelayed(frameTask,300);
    }
    private String level(){return getSharedPreferences("config",MODE_PRIVATE).getString("difficulty","easy");}
    private void capture(){
        if(!running||busy)return;
        busy=true;
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(),new TakeScreenshotCallback(){
                @Override public void onFailure(int errorCode){
                    busy=false;
                    stop("화면 캡처 실패 ("+errorCode+"). 다시 시도하세요.");
                }
                @Override public void onSuccess(ScreenshotResult result){
                    Bitmap frame=null;
                    try {
                        HardwareBuffer buffer=result.getHardwareBuffer();
                        try {
                            Bitmap wrapped=Bitmap.wrapHardwareBuffer(buffer,result.getColorSpace());
                            if(wrapped==null)throw new IllegalStateException("화면 이미지를 읽을 수 없습니다.");
                            frame=wrapped.copy(Bitmap.Config.ARGB_8888,false);
                        } finally {buffer.close();}
                        if(frame==null)throw new IllegalStateException("화면 이미지 복사 실패");
                        processFrame(frame);
                    }catch(Exception e){stop("인식 중단: "+e.getMessage());}
                    finally {if(frame!=null)frame.recycle();busy=false;}
                }
            });
        }catch(Exception e){busy=false;stop("캡처 오류: "+e.getMessage());}
    }
    private void processFrame(Bitmap image){
        if(!running)return;
        int[] dims=Solver.difficulty(level());int rows=dims[0],cols=dims[1],mines=dims[2];
        int[][] cells=BoardVision.read(image,boardRect,rows,cols);
        StringBuilder signature=new StringBuilder(rows*cols);
        int opened=0;
        for(int[] row:cells)for(int c:row){signature.append((char)(c+65));if(c>=0)opened++;}
        String signatureNow=signature.toString();
        if(attempts>0 && signatureNow.equals(previousBoard))unchanged++;
        else unchanged=0;
        previousBoard=signatureNow;
        if(unchanged>=2){stop("게임판이 변하지 않습니다. 종료 또는 영역 오류 확인");return;}
        Solver.Result result=Solver.solve(cells,mines,knownMines);
        knownMines.addAll(result.mines);
        if(result.choice<0){stop("더 열 안전한 칸이 없습니다. 게임 결과를 확인하세요.");return;}
        if(result.risk>.99){stop("안전하게 클릭할 칸이 없어 중지했습니다.");return;}
        int r=result.choice/cols,c=result.choice%cols;
        float x=boardRect.left+(c+.5f)*boardRect.width()/cols;
        float y=boardRect.top+(r+.5f)*boardRect.height()/rows;
        attempts++;
        setStatus("열린 칸 "+opened+" · 시도 "+attempts+(result.guessed?" · 추측 "+Math.round(result.risk*100)+"%":" · 확정 안전"));
        tap(x,y);
    }
    private void tap(float x,float y){
        if(!running)return;
        Path path=new Path();path.moveTo(x,y);
        GestureDescription gesture=new GestureDescription.Builder().addStroke(
                new GestureDescription.StrokeDescription(path,0,80)).build();
        boolean accepted=dispatchGesture(gesture,new GestureResultCallback(){
            @Override public void onCompleted(GestureDescription ignored){if(running)handler.postDelayed(frameTask,650);}
            @Override public void onCancelled(GestureDescription ignored){stop("자동 터치가 취소됐습니다.");}
        },null);
        if(!accepted)stop("자동 터치 요청이 거부됐습니다.");
    }
    private interface OnRegionSelected{void accept(Rect selected);}
    private final class SelectionOverlay extends View {
        private final OnRegionSelected listener;
        private final Paint paint=new Paint(3);
        private float localX0,localY0,localX1,localY1;
        private float rawX0,rawY0,rawX1,rawY1;
        private boolean dragging=false;
        SelectionOverlay(Context context,OnRegionSelected listener){super(context);this.listener=listener;}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);
            canvas.drawColor(0x88000000);
            paint.setColor(Color.WHITE);paint.setTextSize(dp(19));paint.setStyle(Paint.Style.FILL);
            canvas.drawText("게임판 바깥쪽 모서리부터 반대쪽까지 드래그",dp(18),dp(42),paint);
            canvas.drawText("숫자판만 포함하고 위쪽 메뉴는 제외",dp(18),dp(70),paint);
            if(dragging){
                Rect box=new Rect(Math.round(Math.min(localX0,localX1)),Math.round(Math.min(localY0,localY1)),
                        Math.round(Math.max(localX0,localX1)),Math.round(Math.max(localY0,localY1)));
                paint.setColor(0x4453D99A);paint.setStyle(Paint.Style.FILL);canvas.drawRect(box,paint);
                paint.setColor(0xFF7EFFF1);paint.setStrokeWidth(dp(3));paint.setStyle(Paint.Style.STROKE);canvas.drawRect(box,paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:
                    dragging=true;localX0=e.getX();localY0=e.getY();localX1=localX0;localY1=localY0;
                    rawX0=e.getRawX();rawY0=e.getRawY();rawX1=rawX0;rawY1=rawY0;invalidate();return true;
                case MotionEvent.ACTION_MOVE:
                    localX1=e.getX();localY1=e.getY();rawX1=e.getRawX();rawY1=e.getRawY();invalidate();return true;
                case MotionEvent.ACTION_UP:
                    localX1=e.getX();localY1=e.getY();rawX1=e.getRawX();rawY1=e.getRawY();
                    listener.accept(new Rect(Math.round(Math.min(rawX0,rawX1)),Math.round(Math.min(rawY0,rawY1)),
                            Math.round(Math.max(rawX0,rawX1)),Math.round(Math.max(rawY0,rawY1))));return true;
                default:return true;
            }
        }
    }
}
