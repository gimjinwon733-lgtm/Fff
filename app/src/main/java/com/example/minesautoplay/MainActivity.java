package com.example.minesautoplay;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView connection;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(245,247,252));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(25),dp(36),dp(25),dp(30));
        scroll.addView(content);
        TextView title = label("지뢰찾기 자동 풀이",26,true);
        content.addView(title);
        TextView sub = label("갤럭시 탭용 · 앱에서 설정한 뒤 구글 지뢰찾기로 이동하세요.",14,false);
        sub.setTextColor(Color.DKGRAY); content.addView(sub);
        connection = label("접근성 서비스 확인 중",15,true);
        space(content,18); content.addView(connection); space(content,14);
        Button accessibility = button("1. 접근성 서비스 켜기");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        content.addView(accessibility); space(content,18);
        content.addView(label("2. 난이도 설정",17,true));
        RadioGroup difficulties = new RadioGroup(this); difficulties.setOrientation(1);
        String[] names = {"초급 · 8×10", "중급 · 14×18", "고급 · 20×24"};
        String[] values = {"easy", "medium", "hard"};
        String current = getSharedPreferences("config",MODE_PRIVATE).getString("difficulty","easy");
        for (int i=0;i<3;i++) {
            RadioButton b = new RadioButton(this); b.setId(100+i); b.setText(names[i]); b.setTextSize(16);
            difficulties.addView(b); if (values[i].equals(current)) b.setChecked(true);
        }
        difficulties.setOnCheckedChangeListener((group,id) -> {
            int index=id-100;
            if(index>=0 && index<3) getSharedPreferences("config",MODE_PRIVATE).edit().putString("difficulty",values[index]).apply();
        });
        content.addView(difficulties); space(content,18);
        Button open = button("3. 구글 지뢰찾기 열기");
        open.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=google+minesweeper"))));
        content.addView(open); space(content,22);
        TextView steps = label("게임을 시작하면 화면에 뜨는 작은 조작창에서 ‘판 선택’을 눌러 게임판 전체를 드래그하세요. 그다음 ‘시작’을 누르면 자동으로 터치합니다. ‘중지’로 즉시 멈출 수 있습니다.\n\n참고: 실제 기기에서는 화면 배율과 게임 화면 디자인에 따라 인식이 실패할 수 있습니다. 확실한 답이 없는 경우에는 추측을 하므로 100% 승리를 보장하지 않습니다.\n\n이 앱은 인터넷 권한을 요청하지 않고 화면 데이터를 외부에 전송하지 않습니다. 접근성 권한은 게임 자동 터치에만 사용하세요.",14,false);
        steps.setTextColor(Color.rgb(72,80,95)); content.addView(steps);
        setContentView(scroll);
    }
    @Override protected void onResume() { super.onResume(); if(connection!=null) connection.setText(AutoPlayService.INSTANCE!=null ? "● 서비스 실행 중 · 게임으로 이동하세요" : "○ 설정에서 접근성 서비스를 켜야 합니다"); }
    private TextView label(String t,int s,boolean bold){ TextView v=new TextView(this);v.setText(t);v.setTextSize(s);v.setTextColor(Color.rgb(24,40,58));v.setLineSpacing(dp(3),1); if(bold)v.setTypeface(null,Typeface.BOLD);return v;}
    private Button button(String text) {Button b=new Button(this);b.setText(text);b.setTextSize(15);b.setAllCaps(false);return b;}
    private void space(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
}
